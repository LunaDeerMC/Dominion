#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 2 || $# -gt 3 ]]; then
    echo "Usage: $0 PRE_FALLBACK_API.jar PAPER_API.jar [OUTPUT.jar]" >&2
    exit 2
fi

fixture_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
repository_root=$(cd -- "$fixture_dir/../../.." && pwd)
legacy_api_jar=$(realpath -- "$1")
paper_api_jar=$(realpath -- "$2")
fixture_output=${3:-"$repository_root/core/src/test/resources/compat/legacy-flags/legacy-flag-reader.jar"}
fixture_work=$(mktemp -d)
trap 'rm -rf -- "$fixture_work"' EXIT

javac --release 17 -g:none -Xlint:-deprecation \
    -classpath "$legacy_api_jar:$paper_api_jar" \
    -d "$fixture_work" "$fixture_dir/LegacyFlagReader.java"
mkdir -p -- "$(dirname -- "$fixture_output")"
jar --create --file "$fixture_output" --no-manifest --date=2020-01-01T00:00:00Z \
    -C "$fixture_work" dominion/compat/legacy/LegacyFlagReader.class

sha256sum "$legacy_api_jar" "$paper_api_jar" "$fixture_dir/LegacyFlagReader.java" "$fixture_output"
