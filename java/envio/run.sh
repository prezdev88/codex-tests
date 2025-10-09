#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

mvn -f "${SCRIPT_DIR}/pom.xml" clean package
mvn -f "${SCRIPT_DIR}/pom.xml" exec:java
