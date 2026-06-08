#!/usr/bin/env bash
# One-time setup: init git, create public GitHub repo, push main.
#
#   ./scripts/github-setup.sh
#   ./scripts/github-setup.sh myuser/FreedomSuite   # custom repo name
#
# Requires: git, gh auth login
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
REPO="${1:-}"

require_gh() {
  command -v gh >/dev/null 2>&1 || { echo "Install gh: https://cli.github.com/" >&2; exit 1; }
  gh api user -q .login >/dev/null 2>&1 || { echo "Run: gh auth login" >&2; exit 1; }
}

cd "$ROOT"
require_gh

if [[ -z "$REPO" ]]; then
  user="$(gh api user -q .login)"
  REPO="${user}/FreedomSuite"
fi

if ! git rev-parse --git-dir >/dev/null 2>&1; then
  echo "Initializing git repository…"
  git init -b main
fi

if ! git rev-parse HEAD >/dev/null 2>&1; then
  git add -A
  git commit -m "$(cat <<'EOF'
Initial Freedom Suite commit.

Apache-2.0 monorepo: encrypted Android apps, CI, F-Droid metadata.
EOF
)"
fi

if git remote get-url origin >/dev/null 2>&1; then
  echo "origin already set: $(git remote get-url origin)"
else
  echo "Creating public repo $REPO …"
  gh repo create "$REPO" --public --source=. --remote=origin \
    --description "Parasite-free encrypted Android apps — Freedom Suite"
fi

echo "Pushing main…"
git push -u origin main

echo ""
echo "Done. Repository: https://github.com/$REPO"
echo "Next: ./scripts/publish.sh"
