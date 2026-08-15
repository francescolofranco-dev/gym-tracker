package dev.francescolofranco.gymtracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.francescolofranco.gymtracker.data.db.dao.ExerciseDao
import dev.francescolofranco.gymtracker.data.db.dao.SessionDao
import dev.francescolofranco.gymtracker.data.db.dao.SetLogDao
import dev.francescolofranco.gymtracker.data.db.dao.TemplateDao
import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.data.db.entities.SessionEntity
import dev.francescolofranco.gymtracker.data.db.entities.SessionExerciseEntity
import dev.francescolofranco.gymtracker.data.db.entities.SetLogEntity
import dev.francescolofranco.gymtracker.data.db.entities.TemplateEntity
import dev.francescolofranco.gymtracker.data.db.entities.TemplateExerciseEntity

@Database(
    entities = [
        ExerciseEntity::class,
        SessionEntity::class,
        SessionExerciseEntity::class,
        SetLogEntity::class,
        TemplateEntity::class,
        TemplateExerciseEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun sessionDao(): SessionDao
    abstract fun setLogDao(): SetLogDao
    abstract fun templateDao(): TemplateDao

    companion object {
        const val NAME = "gym-tracker.db"

        /**
         * v1 → v2: single `primaryMuscle` column on `exercise` becomes a multi-valued
         * `primaryMuscles` set (still stored as comma-separated names via Converters). The
         * old indexed `primaryMuscle` column is dropped via a copy-table migration because
         * SQLite < 3.35 can't drop a column in place. Pre-existing rows keep their single
         * primary by copying it into the new set field.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE exercise_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        primaryMuscles TEXT NOT NULL,
                        secondaryMuscles TEXT NOT NULL,
                        targetSets INTEGER NOT NULL,
                        repRangeMin INTEGER NOT NULL,
                        repRangeMax INTEGER NOT NULL,
                        isBodyweight INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        deletedAt INTEGER
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO exercise_new (
                        id, name, primaryMuscles, secondaryMuscles, targetSets,
                        repRangeMin, repRangeMax, isBodyweight, createdAt, deletedAt
                    )
                    SELECT id, name, primaryMuscle, secondaryMuscles, targetSets,
                           repRangeMin, repRangeMax, isBodyweight, createdAt, deletedAt
                    FROM exercise
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE exercise")
                db.execSQL("ALTER TABLE exercise_new RENAME TO exercise")
                db.execSQL("CREATE INDEX index_exercise_deletedAt ON exercise(deletedAt)")
            }
        }

        /**
         * v2 → v3: session gains an `acceptedAt` timestamp. Null = draft (created on tap-Start
         * but not yet promoted via the "Start workout" button), non-null = a real session.
         * Backfills every existing session as already-accepted (acceptedAt = startedAt) so the
         * user doesn't suddenly lose any history.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE session ADD COLUMN acceptedAt INTEGER")
                db.execSQL("UPDATE session SET acceptedAt = startedAt")
            }
        }

        /** v3 → v4: unilateral exercise metadata plus a side on every set log. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercise ADD COLUMN isUnilateral INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE set_log ADD COLUMN side TEXT NOT NULL DEFAULT 'BOTH'")
            }
        }

        /**
         * v4 → v5: exercise notes gain an explicit one-session carry lifecycle and an
         * indefinite pinned state. Existing non-blank notes are eligible to appear once in the
         * next occurrence so upgrading does not silently strand the user's latest instruction.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE session_exercise " +
                        "ADD COLUMN isNotePinned INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE session_exercise " +
                        "ADD COLUMN noteCarryForward INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    """
                    UPDATE session_exercise
                    SET noteCarryForward = 1
                    WHERE notes IS NOT NULL AND TRIM(notes) != ''
                    """.trimIndent(),
                )
            }
        }
    }
}
