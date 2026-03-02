#!/bin/bash
set -e
cd "$(dirname "$0")"

CMD="${1:-help}"

setup() {
  mkdir -p lib
  if [ ! -f "lib/gson-2.11.0.jar" ]; then
    echo "[ERROR] Falta lib/gson-2.11.0.jar"
    exit 1
  fi
  mkdir -p certs
  if [ ! -f "certs/ca.cer" ]; then
    echo "[ERROR] Falta certs/ca.cer"
    exit 1
  fi
  echo "[OK] Dependencias cliente listas."
}

compile() {
  mkdir -p lib
  if [ ! -f "lib/gson-2.11.0.jar" ]; then
    echo "[ERROR] Falta lib/gson-2.11.0.jar"
    exit 1
  fi
  mkdir -p certs
  if [ ! -f "certs/ca.cer" ]; then
    echo "[ERROR] Falta certs/ca.cer"
    exit 1
  fi
  mkdir -p bin
  javac -cp "lib/*" -d bin -sourcepath src \
    src/common/*.java \
    src/client/*.java
  if [ -n "$JAVAFX_HOME" ]; then
    javac -cp "lib/*:bin" -d bin \
      --module-path "$JAVAFX_HOME/lib" \
      --add-modules javafx.controls,javafx.fxml \
      src/client/gui/*.java
  fi
  echo "[OK] Cliente compilado."
}

client() {
  mkdir -p lib
  if [ ! -f "lib/gson-2.11.0.jar" ]; then
    echo "[ERROR] Falta lib/gson-2.11.0.jar"
    exit 1
  fi
  mkdir -p certs
  if [ ! -f "certs/ca.cer" ]; then
    echo "[ERROR] Falta certs/ca.cer."
    echo "        El cliente necesita el certificado de la CA del proyecto."
    exit 1
  fi
  if [ ! -f "bin/client/NetAuctionClient.class" ]; then
    mkdir -p bin
    javac -cp "lib/*" -d bin -sourcepath src \
      src/common/*.java \
      src/client/*.java
  fi
  echo "[OK] Material SSL cliente listo."
  HOST="${2:-localhost}"
  PORT="${3:-9999}"
  java -cp "lib/*:bin" client.NetAuctionClient "$HOST" "$PORT" --ssl
}

gui() {
  mkdir -p lib
  if [ ! -f "lib/gson-2.11.0.jar" ]; then
    echo "[ERROR] Falta lib/gson-2.11.0.jar"
    exit 1
  fi
  mkdir -p certs
  if [ ! -f "certs/ca.cer" ]; then
    echo "[ERROR] Falta certs/ca.cer."
    echo "        El cliente necesita el certificado de la CA del proyecto."
    exit 1
  fi
  if [ ! -f "bin/client/gui/MainApp.class" ]; then
    mkdir -p bin
    javac -cp "lib/*" -d bin -sourcepath src \
      src/common/*.java \
      src/client/*.java
    if [ -n "$JAVAFX_HOME" ]; then
      javac -cp "lib/*:bin" -d bin \
        --module-path "$JAVAFX_HOME/lib" \
        --add-modules javafx.controls,javafx.fxml \
        src/client/gui/*.java
    fi
  fi
  echo "[OK] Material SSL cliente listo."
  if [ -z "$JAVAFX_HOME" ]; then
    echo "[ERROR] JAVAFX_HOME no esta definido."
    exit 1
  fi
  java -cp "lib/*:bin" \
    --module-path "$JAVAFX_HOME/lib" \
    --add-modules javafx.controls,javafx.fxml \
    client.gui.MainApp
}

show_help() {
  echo "Uso:"
  echo "  ./run.sh setup"
  echo "  ./run.sh compile"
  echo "  ./run.sh client [host] [puerto]"
  echo "  ./run.sh gui"
}

case "$CMD" in
  setup)   setup ;;
  compile) compile ;;
  client)  client "$@" ;;
  gui)     gui ;;
  help|*)  show_help ;;
esac
