#!/usr/bin/env bash
# Cut a Play Store release: bump versionName, commit, tag vX.Y.Z and push.
# The tag push triggers .github/workflows/release.yml, which tests, builds and uploads
# the signed bundle to the Play internal track.
#
# Usage: scripts/release.sh 1.2.0
# Edit app/src/main/play/release-notes/en-US/default.txt before running.
set -euo pipefail

version="${1:-}"
if [[ ! "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "usage: $0 X.Y.Z" >&2
  exit 1
fi

cd "$(git rev-parse --show-toplevel)"
gradle_file="app/build.gradle.kts"
notes_file="app/src/main/play/release-notes/en-US/default.txt"

[[ "$(git branch --show-current)" == "main" ]] || { echo "Release from main." >&2; exit 1; }
git diff --quiet HEAD -- . ":(exclude)$notes_file" || { echo "Commit or stash other changes first." >&2; exit 1; }
git rev-parse -q --verify "refs/tags/v$version" >/dev/null && { echo "Tag v$version already exists." >&2; exit 1; }
git pull --ff-only

echo "Release notes ($notes_file):"
cat "$notes_file"
read -r -p "Release v$version with these notes? [y/N] " answer
[[ "$answer" == [yY] ]] || exit 1

sed -i '' -E "s/versionName = \"[^\"]+\"/versionName = \"$version\"/" "$gradle_file"
./gradlew :app:testDebugUnitTest :app:bundleRelease -q

git add "$gradle_file" "$notes_file"
git commit -m "Release v$version"
git tag -a "v$version" -m "Release v$version"
git push origin main "v$version"
echo "Pushed v$version. Follow the upload with: gh run watch"
