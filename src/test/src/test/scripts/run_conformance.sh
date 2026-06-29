#!/usr/bin/env bash
# Run NGFF coordinate transformation conformance tests against NgffTransformsConformance.
#
# Usage: run_conformance.sh [conformance_repo_path] [extra args passed to transformation_conformance.py]
#
# Builds the project once via Maven, then passes a java dingus wrapper to the
# Python conformance harness so each test case is a fast JVM invocation (not a
# full Maven build).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../../../../.." && pwd)"

CONFORMANCE_REPO="${1:-"$(cd "$PROJECT_DIR/../../ngff/ome_zarr_transformations_conformance" && pwd)"}"
shift || true   # remaining args are forwarded to transformation_conformance.py

MAIN_CLASS="org.janelia.saalfeldlab.n5.universe.metadata.ngff.coordinateTransformations.NgffTransformsConformance"
CLASSPATH_FILE="$PROJECT_DIR/target/test-classpath.txt"

# ── Build ────────────────────────────────────────────────────────────────────

echo "Building $PROJECT_DIR ..." >&2
mvn -f "$PROJECT_DIR/pom.xml" test-compile \
    dependency:build-classpath -Dmdep.outputFile="$CLASSPATH_FILE" \
    -q

CLASSPATH="$PROJECT_DIR/target/test-classes:$PROJECT_DIR/target/classes:$(cat "$CLASSPATH_FILE")"

# ── Dingus wrapper ────────────────────────────────────────────────────────────

DINGUS=$(mktemp /tmp/ngff-dingus-XXXXXX.sh)
trap 'rm -f "$DINGUS"' EXIT

cat > "$DINGUS" <<EOF
#!/usr/bin/env bash
exec java -cp "$CLASSPATH" "$MAIN_CLASS" "\$@"
EOF
chmod +x "$DINGUS"

# ── Run conformance harness ───────────────────────────────────────────────────

HARNESS="$CONFORMANCE_REPO/transformation_conformance.py"
CASES="$CONFORMANCE_REPO/cases"

exec python3 "$HARNESS" "$CASES" "$@" -- "$DINGUS"
