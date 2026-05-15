# CLAUDE.md — Snipcraft

Project-level instructions for Claude Code. These extend the global rules in `~/.claude/CLAUDE.md`.

---

## Git workflow

- Always work off a feature branch — never commit implementation directly to `main`.
- Rebase `main` into the feature branch before raising a PR.
- Raise a PR to `main` only after all tests pass.
- Merge the PR if there are no issues.
