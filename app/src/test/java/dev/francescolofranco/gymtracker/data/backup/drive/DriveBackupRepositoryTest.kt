package dev.francescolofranco.gymtracker.data.backup.drive

import dev.francescolofranco.gymtracker.data.backup.BackupExport
import dev.francescolofranco.gymtracker.data.backup.BackupExporter
import dev.francescolofranco.gymtracker.data.backup.BackupImporter
import dev.francescolofranco.gymtracker.data.backup.BackupRecoveryStore
import dev.francescolofranco.gymtracker.data.backup.BackupSchema
import dev.francescolofranco.gymtracker.data.backup.BackupSummary
import dev.francescolofranco.gymtracker.data.db.dao.ExerciseDao
import dev.francescolofranco.gymtracker.data.db.dao.SessionDao
import dev.francescolofranco.gymtracker.data.db.dao.SetLogDao
import dev.francescolofranco.gymtracker.data.db.dao.TemplateDao
import dev.francescolofranco.gymtracker.data.prefs.UserPrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy
import java.time.Instant

class DriveBackupRepositoryTest {

    private class FakeDriveAuth : DriveAuth() {
        var token: String? = "test_token"
        override suspend fun freshAccessToken(): String? = token
    }

    private class FakeDriveBackupClient : DriveBackupClient(okhttp3.OkHttpClient()) {
        val snapshots = mutableListOf<DriveSnapshot>()
        val files = mutableMapOf<String, ByteArray>()
        var uploadCalls = 0
        var downloadCalls = 0

        override fun list(token: String): List<DriveSnapshot> = snapshots.toList()

        override fun upload(token: String, name: String, bytes: ByteArray, mimeType: String): String {
            uploadCalls++
            val fileId = "id_${snapshots.size + 1}"
            files[fileId] = bytes
            snapshots.add(0, DriveSnapshot(id = fileId, name = name, createdAt = Instant.now(), sizeBytes = bytes.size.toLong()))
            return fileId
        }

        override fun download(token: String, fileId: String): ByteArray {
            downloadCalls++
            return files[fileId] ?: throw DriveApiException("File not found")
        }

        override fun delete(token: String, fileId: String) {
            snapshots.removeAll { it.id == fileId }
            files.remove(fileId)
        }
    }

    private class FakeUserPrefs : UserPrefs() {
        var currentHash: String? = null
        var currentLastBackupAt: Instant? = null

        override val lastBackupHash: Flow<String?> get() = flowOf(currentHash)
        override val lastBackupAt: Flow<Instant?> get() = flowOf(currentLastBackupAt)

        override suspend fun setLastBackupHash(hash: String?) {
            currentHash = hash
        }

        override suspend fun setLastBackupAt(at: Instant) {
            currentLastBackupAt = at
        }
    }

    private class FakeBackupExporter(
        exerciseDao: ExerciseDao,
        templateDao: TemplateDao,
        sessionDao: SessionDao,
        setLogDao: SetLogDao,
    ) : BackupExporter(exerciseDao, templateDao, sessionDao, setLogDao) {
        var exportResult = BackupExport(
            json = createSampleJson("original"),
            contentHash = "hash_original",
        )

        override suspend fun export(exportedAt: Instant): BackupExport = exportResult
        override suspend fun exportToJson(): String = exportResult.json
    }

    private class FakeBackupImporter : BackupImporter() {
        override fun inspectJson(json: String): BackupSummary =
            BackupSummary(1, 0, 0, 1, 1, 1, Instant.now())

        override suspend fun importFromJson(json: String): BackupSummary =
            BackupSummary(1, 0, 0, 1, 1, 1, Instant.now())
    }

    private class FakeRecoveryStore : BackupRecoveryStore() {
        override suspend fun captureCurrent() {}
    }

    private val auth = FakeDriveAuth()
    private val client = FakeDriveBackupClient()
    private val userPrefs = FakeUserPrefs()
    private val exerciseDao = createProxy<ExerciseDao>()
    private val sessionDao = createProxy<SessionDao>()
    private val templateDao = createProxy<TemplateDao>()
    private val setLogDao = createProxy<SetLogDao>()
    private val exporter = FakeBackupExporter(exerciseDao, templateDao, sessionDao, setLogDao)
    private val importer = FakeBackupImporter()
    private val recoveryStore = FakeRecoveryStore()

    private val repo = DriveBackupRepository(
        auth = auth,
        client = client,
        exporter = exporter,
        importer = importer,
        recoveryStore = recoveryStore,
        userPrefs = userPrefs,
        exerciseDao = exerciseDao,
        sessionDao = sessionDao,
        templateDao = templateDao,
    )

    @Test
    fun initialRun_uploadsAndSavesHash() = runBlocking {
        exporter.exportResult = BackupExport(
            json = createSampleJson("v1"),
            contentHash = "hash_v1",
        )

        val outcome = repo.runBackup()

        assertTrue(outcome is DriveBackupResult.Success)
        assertEquals(1, client.uploadCalls)
        assertEquals("hash_v1", userPrefs.currentHash)
        assertNotNull(userPrefs.currentLastBackupAt)
    }

    @Test
    fun subsequentRun_withUnchangedData_returnsUpToDateWithoutUploading() = runBlocking {
        exporter.exportResult = BackupExport(
            json = createSampleJson("v1"),
            contentHash = "hash_v1",
        )
        userPrefs.currentHash = "hash_v1"

        val outcome = repo.runBackup()

        assertEquals(DriveBackupResult.UpToDate, outcome)
        assertEquals(0, client.uploadCalls)
    }

    @Test
    fun subsequentRun_withChangedData_uploadsAndUpdatesHash() = runBlocking {
        userPrefs.currentHash = "hash_v1"
        exporter.exportResult = BackupExport(
            json = createSampleJson("v2"),
            contentHash = "hash_v2",
        )

        val outcome = repo.runBackup()

        assertTrue(outcome is DriveBackupResult.Success)
        assertEquals(1, client.uploadCalls)
        assertEquals("hash_v2", userPrefs.currentHash)
    }

    @Test
    fun runBackup_whenForceIsTrue_uploadsEvenIfUnchanged() = runBlocking {
        exporter.exportResult = BackupExport(
            json = createSampleJson("v1"),
            contentHash = "hash_v1",
        )
        userPrefs.currentHash = "hash_v1"

        val outcome = repo.runBackup(force = true)

        assertTrue(outcome is DriveBackupResult.Success)
        assertEquals(1, client.uploadCalls)
    }

    @Test
    fun initialRun_whenLocalHashNull_matchesExistingDriveSnapshot_skipsUpload() = runBlocking {
        val existingJson = createSampleJson("same_session")
        val expectedHash = BackupExporter.computeContentHash(existingJson)

        exporter.exportResult = BackupExport(
            json = existingJson,
            contentHash = expectedHash,
        )

        // Drive already has this snapshot uploaded
        client.upload("token", "gymtracker-existing.json", existingJson.toByteArray())
        client.uploadCalls = 0 // reset counter
        userPrefs.currentHash = null

        val outcome = repo.runBackup()

        assertEquals(DriveBackupResult.UpToDate, outcome)
        assertEquals(0, client.uploadCalls)
        assertEquals(expectedHash, userPrefs.currentHash)
    }

    @Test
    fun restore_updatesLastBackupHash() = runBlocking {
        val snapshotJson = createSampleJson("restored")
        val snapshotHash = BackupExporter.computeContentHash(snapshotJson)
        val fileId = client.upload("token", "snapshot.json", snapshotJson.toByteArray())

        val restoreOutcome = repo.restore(fileId)

        assertTrue(restoreOutcome is DriveRestoreResult.Success)
        assertEquals(snapshotHash, userPrefs.currentHash)

        // After restore, runBackup with the same data returns UpToDate
        exporter.exportResult = BackupExport(
            json = snapshotJson,
            contentHash = snapshotHash,
        )
        val backupOutcome = repo.runBackup()
        assertEquals(DriveBackupResult.UpToDate, backupOutcome)
    }

    private inline fun <reified T> createProxy(): T {
        return Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java),
        ) { _, _, _ -> null } as T
    }

    companion object {
        fun createSampleJson(tag: String): String {
            val root = JSONObject()
            root.put(BackupSchema.K_SCHEMA_VERSION, BackupSchema.CURRENT_SCHEMA_VERSION)
            root.put(BackupSchema.K_EXPORTED_AT, Instant.now().toString())
            root.put(BackupSchema.K_EXERCISES, org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("id", 1L)
                    put("name", "Exercise $tag")
                    put("primaryMuscles", org.json.JSONArray().apply { put("CHEST") })
                    put("secondaryMuscles", org.json.JSONArray())
                    put("targetSets", 3)
                    put("repRangeMin", 8)
                    put("repRangeMax", 12)
                    put("isBodyweight", false)
                    put("isUnilateral", false)
                    put("createdAt", Instant.EPOCH.toString())
                    put("deletedAt", JSONObject.NULL)
                })
            })
            root.put(BackupSchema.K_TEMPLATES, org.json.JSONArray())
            root.put(BackupSchema.K_TEMPLATE_EXERCISES, org.json.JSONArray())
            root.put(BackupSchema.K_SESSIONS, org.json.JSONArray())
            root.put(BackupSchema.K_SESSION_EXERCISES, org.json.JSONArray())
            root.put(BackupSchema.K_SET_LOGS, org.json.JSONArray())
            return root.toString(2)
        }
    }
}
