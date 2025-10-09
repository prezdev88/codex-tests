#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

print_menu() {
    cat <<'EOF'
Available projects:
  1) Java - Envio (Swing HTTP client)
  2) HTML - Snake game (serves files locally)
  q) Quit
EOF
}

require_command() {
    local cmd="$1"
    local package_hint="$2"
    if ! command -v "${cmd}" >/dev/null 2>&1; then
        echo "Error: command '${cmd}' not found. ${package_hint}" >&2
        exit 1
    fi
}

run_envio() {
    require_command mvn "Install Maven 3.x and JDK 17+."
    "${ROOT_DIR}/java/envio/run.sh"
}

open_browser() {
    local url="$1"
    if command -v xdg-open >/dev/null 2>&1; then
        xdg-open "${url}" >/dev/null 2>&1 &
        return 0
    fi
    if command -v open >/dev/null 2>&1; then
        open "${url}" >/dev/null 2>&1 &
        return 0
    fi
    echo "Could not detect how to open the browser automatically. Please open ${url} manually." >&2
    return 1
}

run_snake() {
    local game_path="${ROOT_DIR}/html/snake/game.html"
    if [[ ! -f "${game_path}" ]]; then
        echo "Snake game file not found at ${game_path}" >&2
        return 1
    fi
    echo "Opening Snake game in the default browser..."
    open_browser "file://${game_path}"
}

main() {
    while true; do
        print_menu
        read -rp "Choose an option: " choice
        case "${choice}" in
            1)
                run_envio
                ;;
            2)
                run_snake
                ;;
            q|Q)
                echo "Bye!"
                exit 0
                ;;
            *)
                echo "Invalid choice. Try again."
                ;;
        esac
        echo
    done
}

main "$@"
