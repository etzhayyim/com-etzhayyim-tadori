#!/usr/bin/env bash
# tadori 辿 — run the whole test suite with one command.
# Fully migrated to cljc (ADR-2606160842): the actor boundary has a local
# babashka entrypoint, and deeper method suites live in methods/*.cljc and tests/*.cljc.
set -uo pipefail
cd "$(dirname "$0")"

if ! command -v bb >/dev/null 2>&1; then
  echo "!! bb (babashka) not found — install it to run the tadori cljc suite" >&2
  exit 1
fi

if bb test; then
  echo "── tadori: ALL suites green ──"
else
  echo "── tadori: FAILURES above ──"; exit 1
fi
