"""Allow CD only when production has no outstanding review or developer action.

Uses the read-only release lifecycle API, not edits.tracks' rollout status:
https://developers.google.com/android-publisher/api-ref/rest/v3/applications.tracks.releases
"""

import json
import os
import subprocess
from urllib.error import HTTPError
from urllib.parse import quote
from urllib.request import Request, urlopen

PREFIX = "RELEASE_LIFECYCLE_STATE_"


def release_decision(releases, version_code):
    """Fail closed for pending, rejected, draft, or unknown release states."""
    if not isinstance(releases, list):
        raise ValueError("Play returned an invalid releases list")
    for release in releases:
        state = release.get("releaseLifecycleState", "UNKNOWN")
        if state != PREFIX + "PUBLISHED":
            return False, f"Waiting: production release state is {state}."
    versions = [
        int(artifact["versionCode"])
        for release in releases
        for artifact in release.get("activeArtifacts", [])
    ]
    if versions and max(versions) >= version_code:
        return False, f"Version {version_code} is already published or superseded."
    return True, f"No outstanding production release; version {version_code} can ship."


def has_release_changes(paths):
    return any(
        not (
            path.endswith(".md")
            or path.startswith("docs/")
            or path.startswith("app/src/main/play/listings/")
            or (path.startswith("scripts/") and path != "scripts/play_release_gate.py")
        )
        for path in paths
    )


def git(*args):
    return subprocess.check_output(["git", *args], text=True).strip()


def fetch_releases(credentials_json, track):
    # Import only for live calls, keeping decision tests dependency-free.
    from google.auth.transport.requests import Request as AuthRequest
    from google.oauth2.service_account import Credentials

    credentials = Credentials.from_service_account_info(
        json.loads(credentials_json),
        scopes=["https://www.googleapis.com/auth/androidpublisher"],
    )
    credentials.refresh(AuthRequest())
    package = "dev.lofrancolabs.gymtracker"
    url = (
        "https://androidpublisher.googleapis.com/androidpublisher/v3/"
        f"applications/{package}/tracks/{quote(track, safe='')}/releases"
    )
    request = Request(url, headers={"Authorization": f"Bearer {credentials.token}"})
    with urlopen(request, timeout=60) as response:
        return json.load(response).get("releases", [])


def main():
    track = os.environ.get("PLAY_TRACK", "production")
    version = int(git("rev-list", "--count", "HEAD"))
    tags = git("tag", "--points-at", "HEAD").splitlines()
    if f"play/{version}" in tags:
        ready, reason = False, f"Version {version} was already submitted."
    else:
        last = subprocess.run(
            ["git", "describe", "--tags", "--abbrev=0", "--match", "play/*"],
            text=True, capture_output=True, check=False,
        )
        if last.returncode == 0:
            paths = git("diff", "--name-only", last.stdout.strip(), "HEAD").splitlines()
        else:
            paths = git("ls-files").splitlines()
        if not has_release_changes(paths):
            ready, reason = False, "No app or release workflow changes since the last submission."
        else:
            releases = fetch_releases(os.environ["ANDROID_PUBLISHER_CREDENTIALS"], track)
            ready, reason = release_decision(releases, version)
    print(reason)
    with open(os.environ["GITHUB_OUTPUT"], "a") as output:
        output.write(f"ready={str(ready).lower()}\nsha={git('rev-parse', 'HEAD')}\n")
    with open(os.environ["GITHUB_STEP_SUMMARY"], "a") as summary:
        summary.write(f"### Play release gate\n{reason}\n")


if __name__ == "__main__":
    try:
        main()
    except HTTPError as error:
        # Do not log request headers, credentials, or the API response body.
        raise SystemExit(f"Play release lookup failed (HTTP {error.code}); upload blocked.")
