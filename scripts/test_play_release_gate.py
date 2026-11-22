import unittest

from play_release_gate import PREFIX, has_release_changes, release_decision


def release(state, version=152):
    return {"releaseLifecycleState": PREFIX + state, "activeArtifacts": [{"versionCode": version}]}


class PlayReleaseGateTest(unittest.TestCase):
    def test_waits_for_pending_review_even_with_published_release(self):
        self.assertFalse(release_decision([release("PUBLISHED", 151), release("IN_REVIEW")], 153)[0])

    def test_blocks_states_requiring_developer_action_and_unknown_states(self):
        for state in ("DRAFT", "NOT_SENT_FOR_REVIEW", "APPROVED_NOT_PUBLISHED", "NOT_APPROVED", "UNSPECIFIED", "FUTURE_STATE"):
            with self.subTest(state=state):
                self.assertFalse(release_decision([release(state)], 153)[0])
        self.assertFalse(release_decision([{}], 153)[0])

    def test_published_previous_version_allows_latest_candidate(self):
        self.assertTrue(release_decision([release("PUBLISHED")], 155)[0])

    def test_already_uploaded_or_newer_published_build_is_not_resubmitted(self):
        for candidate in (151, 152):
            self.assertFalse(release_decision([release("PUBLISHED")], candidate)[0])

    def test_empty_track_allows_first_release(self):
        self.assertTrue(release_decision([], 152)[0])

    def test_invalid_response_fails_closed(self):
        for invalid in (None, {}, "published"):
            with self.assertRaises(ValueError):
                release_decision(invalid, 152)

    def test_scheduled_retry_ignores_non_release_changes(self):
        self.assertFalse(has_release_changes(["README.md", "docs/design/a.svg", "scripts/test_logged_set_queries.py", "app/src/main/play/listings/en-GB/title.txt"]))
        for path in ("app/src/main/AndroidManifest.xml", ".github/workflows/release.yml", "scripts/play_release_gate.py"):
            self.assertTrue(has_release_changes(["README.md", path]))


if __name__ == "__main__":
    unittest.main()
