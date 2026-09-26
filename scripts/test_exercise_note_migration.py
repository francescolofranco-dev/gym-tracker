"""Check the production note migration and DAO writes against SQLite."""
import json
import re
import sqlite3
import unittest

from test_logged_set_queries import ROOT, query


class ExerciseNoteMigrationTest(unittest.TestCase):
    def setUp(self):
        self.db = sqlite3.connect(':memory:')
        self.addCleanup(self.db.close)
        schemas = ROOT / 'app/schemas/dev.francescolofranco.gymtracker.data.db.AppDatabase'
        schema = json.loads((schemas / '5.json').read_text())
        entity = next(e for e in schema['database']['entities'] if e['tableName'] == 'session_exercise')
        self.db.execute(entity['createSql'].replace('${TABLE_NAME}', 'session_exercise'))
        self.db.executemany(
            'INSERT INTO session_exercise VALUES (?, 1, 1, 0, ?, ?, ?, 0)',
            [(1, 'Safety bars', 1, 1), (2, 'Try 40 kg', 0, 1),
             (3, 'Consumed note', 0, 0), (4, None, 0, 0)],
        )
        source = (ROOT / 'app/src/main/java/dev/francescolofranco/gymtracker/data/db/AppDatabase.kt').read_text()
        migration = source.split('val MIGRATION_5_6 =')[1].split('\n        /**')[0]
        statements = re.findall(r'db.execSQL\(\s*(?:"""(.*?)"""\.trimIndent\(\)|"([^"\n]*)")', migration, re.S)
        self.assertEqual(2, len(statements))
        for multiline, single in statements:
            self.db.execute(multiline or single)

    def test_existing_notes_preserve_type_and_lifetime(self):
        rows = self.db.execute(
            'SELECT notes, pinnedNotes, isNotePinned, noteCarryForward FROM session_exercise ORDER BY id',
        ).fetchall()
        self.assertEqual([
            (None, 'Safety bars', 0, 0), ('Try 40 kg', None, 0, 1),
            ('Consumed note', None, 0, 0), (None, None, 0, 0),
        ], rows)

    def test_add_other_type_and_clear_independently(self):
        sql = query('SessionDao.kt', 'updateExerciseNote')
        for row_id in (1, 2):
            self.db.execute(sql, dict(id=row_id, notes='Try 40 kg', pinnedNotes='Safety bars', carryForward=1))
            self.assertEqual(('Try 40 kg', 'Safety bars'), self.db.execute(
                'SELECT notes, pinnedNotes FROM session_exercise WHERE id = ?', (row_id,),
            ).fetchone())
        self.db.execute(sql, dict(id=1, notes=None, pinnedNotes='Safety bars', carryForward=0))
        self.db.execute(sql, dict(id=2, notes='Try 40 kg', pinnedNotes=None, carryForward=1))
        self.assertEqual([(None, 'Safety bars'), ('Try 40 kg', None)], self.db.execute(
            'SELECT notes, pinnedNotes FROM session_exercise WHERE id IN (1, 2) ORDER BY id',
        ).fetchall())
