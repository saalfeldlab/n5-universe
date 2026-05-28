#!/usr/bin/env bash
# Dingus CLI wrapper for NgffTransformsConformance.
#
# Usage: ngff_dingus.sh <zarr_path> <source_space> <target_space> <coordinates_json>
#
# Builds the project via Maven on first run (writes classpath to
# target/test-classpath.txt), then delegates to the Java class.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../../../../.." && pwd)"

MAIN_CLASS="org.janelia.saalfeldlab.n5.universe.metadata.ngff.coordinateTransformations.NgffTransformsConformance"
CLASSPATH_FILE="$PROJECT_DIR/target/test-classpath.txt"

if [ ! -f "$CLASSPATH_FILE" ]; then
    echo "Building $PROJECT_DIR ..." >&2
    mvn -f "$PROJECT_DIR/pom.xml" test-compile \
        dependency:build-classpath -Dmdep.outputFile="$CLASSPATH_FILE" \
        -q
fi

CLASSPATH="$PROJECT_DIR/target/test-classes:$PROJECT_DIR/target/classes:$(cat "$CLASSPATH_FILE")"

exec java -cp "$CLASSPATH" "$MAIN_CLASS" "$@"
