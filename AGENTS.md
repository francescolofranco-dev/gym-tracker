# Working agreements

- Integrate completed implementation changes into `main`, commit them, and push to `origin/main` after relevant checks pass. Commit and push only complete, passing work.
- Use optimized, production-like builds for device acceptance. Use debug builds when required for debugging or tests. For Android, provide a non-debuggable, release-optimized, locally installable `benchmark` variant with profiling enabled. Verify the optimized artifact before delivery, distinguish performance-ready builds from measured benchmark results, and keep the setup minimal.

## Agent skills

### Issue tracker

Track issues and specs in GitHub Issues for `francescolofranco-dev/gym-tracker`. Before issue tracker operations, read `docs/agents/issue-tracker.md`.

### Triage labels

Use the default five triage labels. Before triaging or applying triage labels, read `docs/agents/triage-labels.md`.

### Domain docs

Use a single-context layout with root `GLOSSARY.md` and `docs/adr/`. Before exploring the codebase, read `docs/agents/domain.md`.
