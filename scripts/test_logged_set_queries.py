"""Exercise the production DAO SQL against SQLite, using the exported Room schema.

Run with: python3 -m unittest discover -s scripts -p 'test_*.py'
"""
import json
from pathlib import Path
import re
import sqlite3
import unittest

ROOT = Path(__file__).resolve().parents[1]
DAO = ROOT / 'app/src/main/java/dev/francescolofranco/gymtracker/data/db/dao'


def query(file, method):
    source = (DAO / file).read_text()
    for match in re.finditer(r'@Query\(\s*"""(.*?)"""\s*,?\s*\)\s*(?:suspend\s+)?fun\s+(\w+)', source, re.S):
        if match[2] == method:
            return match[1]
    raise AssertionError(f'Query not found: {method}')


class LoggedSetQueriesTest(unittest.TestCase):
    def setUp(self):
        self.db = sqlite3.connect(':memory:')
        self.addCleanup(self.db.close)
        self.db.row_factory = sqlite3.Row
        schemas = ROOT / 'app/schemas/dev.francescolofranco.gymtracker.data.db.AppDatabase'
        schema = max(schemas.glob('*.json'), key=lambda p: int(p.stem))
        for entity in json.loads(schema.read_text())['database']['entities']:
            self.db.execute(entity['createSql'].replace('${TABLE_NAME}', entity['tableName']))
        self.db.execute("INSERT INTO exercise VALUES (1, 'Row', 'BACK', '', 3, 8, 12, 0, 1, 0, NULL)")
        for session_id in (1, 2):
            self.db.execute('INSERT INTO session (id, startedAt, acceptedAt, endedAt) VALUES (?, ?, ?, ?)',
                            (session_id, session_id * 100, session_id * 100, session_id * 100 + 50))
            self.db.execute('INSERT INTO session_exercise (id, sessionId, exerciseId, orderInSession, notes, isNotePinned, noteCarryForward, isSkipped) VALUES (?, ?, 1, 0, NULL, 0, 0, 0)',
                            (session_id, session_id))
        # The later session has a completed left side and a drafted right side.
        self.db.executemany('INSERT INTO set_log VALUES (?, ?, 1, ?, ?, ?, ?, ?)', [
            (1, 1, 'LEFT', 10, 10, 0, 110),
            (2, 1, 'RIGHT', 8, 12, 0, 120),
            (3, 2, 'LEFT', 12, 10, 0, 210),
            (4, 2, 'RIGHT', 10, 12, 0, None),
            (5, 2, 'RIGHT', 20, 99, 1, 220),  # skipped
            (6, 2, 'RIGHT', None, 99, 0, None),  # planned
        ])

    def run_query(self, file, method, **params):
        return self.db.execute(query(file, method), params).fetchall()

    def assert_stats(self, count, volume):
        summary = self.run_query('SessionDao.kt', 'observeAllSummaries')[0]
        self.assertEqual((count, volume), (summary['setCount'], summary['totalVolume']))
        for method in ('observeLoggedSetsBetween', 'loggedSetsBetween'):
            rows = self.run_query('SessionDao.kt', method, startInclusive=200, endExclusive=300)
            self.assertEqual(count, len(rows))
            self.assertEqual(volume, sum(r['reps'] * (r['kg'] or 0) for r in rows))
        history = self.run_query('ExerciseDao.kt', 'observeSetHistory', exerciseId=1)
        current = [r for r in history if r['sessionId'] == 2]
        self.assertEqual(count, len(current))
        self.assertEqual(volume, sum(r['reps'] * (r['kg'] or 0) for r in current))

    def test_only_confirmed_sides_count_and_unchecking_removes_volume(self):
        self.assert_stats(1, 120)
        self.db.execute('UPDATE set_log SET loggedAt = 230 WHERE id = 4')
        self.assert_stats(2, 240)
        self.db.execute('UPDATE set_log SET loggedAt = NULL WHERE id = 3')
        self.assert_stats(1, 120)
        self.db.execute('UPDATE set_log SET loggedAt = NULL WHERE id = 4')
        self.assert_stats(0, 0)

    def test_skipped_exercise_has_no_volume(self):
        self.db.execute('UPDATE session_exercise SET isSkipped = 1 WHERE id = 2')
        self.assert_stats(0, 0)

    def test_history_hints_skip_drafts_and_skipped_occurrences(self):
        params = dict(exerciseId=1, setNumber=1, side='RIGHT', beforeStartedAtEpochMs=300)
        hint = self.run_query('SetLogDao.kt', 'lastLoggedSetBefore', **params)
        self.assertEqual([2], [r['id'] for r in hint])
        for method in ('lastSessionSets', 'lastSessionSetsBefore'):
            args = dict(exerciseId=1, limit=20)
            if method.endswith('Before'):
                args['beforeStartedAtEpochMs'] = 300
            self.assertEqual([3], [r['id'] for r in self.run_query('SetLogDao.kt', method, **args)])
        self.db.execute('UPDATE session_exercise SET isSkipped = 1 WHERE id = 2')
        params['side'] = 'LEFT'
        self.assertEqual([1], [r['id'] for r in self.run_query('SetLogDao.kt', 'lastLoggedSetBefore', **params)])
        for method in ('lastSessionSets', 'lastSessionSetsBefore'):
            args = dict(exerciseId=1, limit=20)
            if method.endswith('Before'):
                args['beforeStartedAtEpochMs'] = 300
            self.assertEqual([1, 2], [r['id'] for r in self.run_query('SetLogDao.kt', method, **args)])


if __name__ == '__main__':
    unittest.main()
