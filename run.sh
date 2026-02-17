#!/bin/bash
set -euo pipefail

cmd="${1:-help}"

case "$cmd" in
  setup)
    exec scripts/setup-deps.sh
    ;;
  compile)
    exec scripts/compile.sh
    ;;
  certs)
    exec scripts/generate-certs.sh
    ;;
  server)
    exec scripts/run-server.sh "${2:-9999}" "${3:-}"
    ;;
  client)
    exec scripts/run-client.sh "${2:-localhost}" "${3:-9999}" "${4:-}"
    ;;
  gui)
    exec scripts/run-gui.sh
    ;;
  help|*)
    echo "Uso:"
    echo "  ./run.sh setup"
    echo "  ./run.sh compile"
    echo "  ./run.sh certs"
    echo "  ./run.sh server [puerto] [--ssl]"
    echo "  ./run.sh client [host] [puerto] [--ssl]"
    echo "  ./run.sh gui"
    ;;
esac
