#!/bin/bash
set -e
cd "$(dirname "$0")"

CMD="${1:-help}"

find_keytool() {
  KEYTOOL=""
  if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/keytool" ]; then
    KEYTOOL="$JAVA_HOME/bin/keytool"
  else
    KEYTOOL=$(command -v keytool 2>/dev/null || true)
  fi
  if [ -z "$KEYTOOL" ]; then
    echo "[ERROR] No se encontro keytool. Instala un JDK completo o configura JAVA_HOME."
    exit 1
  fi
}

setup() {
  mkdir -p lib
  for jar in gson-2.11.0.jar sqlite-jdbc-3.45.0.0.jar slf4j-api-1.7.36.jar; do
    if [ ! -f "lib/$jar" ]; then
      echo "[ERROR] Falta lib/$jar"
      exit 1
    fi
  done
  echo "[OK] Dependencias servidor listas."
}

compile() {
  setup
  mkdir -p bin
  javac -cp "lib/*" -d bin -sourcepath src \
    src/common/*.java \
    src/server/model/*.java \
    src/server/security/*.java \
    src/server/persistence/*.java \
    src/server/manager/*.java \
    src/server/service/NotificationService.java \
    src/server/*.java
  echo "[OK] Servidor compilado."
}

initdb() {
  if [ ! -f "bin/server/DatabaseInit.class" ]; then
    compile
  fi
  java -cp "lib/*:bin" server.DatabaseInit
}

certs() {
  mkdir -p certs
  CERT_MODE="${2:-}"

  if [ -f "certs/ca.p12" ] && [ -f "certs/ca.cer" ] && [ -f "certs/servidor.p12" ] && [ -f "certs/server.cer" ]; then
    if [ "$CERT_MODE" != "--force" ] && [ "$CERT_MODE" != "--reset-ca" ]; then
      echo "[OK] Certificados existentes reutilizados."
      return 0
    fi
  fi

  find_keytool

  if [ "$CERT_MODE" = "--reset-ca" ]; then
    rm -f certs/ca.p12 certs/ca.cer
  fi
  rm -f certs/servidor.p12 certs/server.cer certs/server.csr certs/truststore.p12

  # Build SAN entries: localhost, hostname, 127.0.0.1, plus local IPs
  HOSTNAME_VAL=$(hostname)
  SAN_ENTRIES="dns:localhost,dns:${HOSTNAME_VAL},ip:127.0.0.1"
  for ip in $(ifconfig 2>/dev/null | grep 'inet ' | awk '{print $2}' | grep -v '127.0.0.1'); do
    SAN_ENTRIES="${SAN_ENTRIES},ip:${ip}"
  done

  # 1. Generate CA keypair if needed
  if [ ! -f "certs/ca.p12" ]; then
    "$KEYTOOL" -genkeypair -alias netauction-ca \
      -keyalg RSA -keysize 2048 \
      -validity 3650 \
      -keystore certs/ca.p12 \
      -storetype PKCS12 \
      -storepass netauction123 \
      -keypass netauction123 \
      -dname "CN=NetAuction CA, OU=NetAuction, O=PSP, L=Madrid, ST=Madrid, C=ES" \
      -ext bc:c
  fi

  # 2. Export CA cert
  "$KEYTOOL" -exportcert -alias netauction-ca \
    -keystore certs/ca.p12 \
    -storetype PKCS12 \
    -storepass netauction123 \
    -rfc \
    -file certs/ca.cer

  # 3. Generate server keypair
  "$KEYTOOL" -genkeypair -alias servidor \
    -keyalg RSA -keysize 2048 \
    -validity 825 \
    -keystore certs/servidor.p12 \
    -storetype PKCS12 \
    -storepass netauction123 \
    -keypass netauction123 \
    -dname "CN=${HOSTNAME_VAL}, OU=NetAuction, O=PSP, L=Madrid, ST=Madrid, C=ES"

  # 4. Create CSR
  "$KEYTOOL" -certreq -alias servidor \
    -keystore certs/servidor.p12 \
    -storetype PKCS12 \
    -storepass netauction123 \
    -file certs/server.csr

  # 5. Sign with CA
  "$KEYTOOL" -gencert -alias netauction-ca \
    -keystore certs/ca.p12 \
    -storetype PKCS12 \
    -storepass netauction123 \
    -keypass netauction123 \
    -infile certs/server.csr \
    -outfile certs/server.cer \
    -rfc \
    -validity 825 \
    -ext "KU=digitalSignature,keyEncipherment" \
    -ext "EKU=serverAuth" \
    -ext "SAN=${SAN_ENTRIES}"

  # 6. Import CA cert into server keystore
  "$KEYTOOL" -importcert -alias netauction-ca \
    -file certs/ca.cer \
    -keystore certs/servidor.p12 \
    -storetype PKCS12 \
    -storepass netauction123 \
    -noprompt

  # 7. Import signed server cert
  "$KEYTOOL" -importcert -alias servidor \
    -file certs/server.cer \
    -keystore certs/servidor.p12 \
    -storetype PKCS12 \
    -storepass netauction123 \
    -noprompt

  # 8. Create truststore
  rm -f certs/truststore.p12
  "$KEYTOOL" -importcert -alias netauction-ca \
    -file certs/ca.cer \
    -keystore certs/truststore.p12 \
    -storetype PKCS12 \
    -storepass netauction123 \
    -noprompt

  # 9. Cleanup
  rm -f certs/server.csr

  echo "[OK] CA y certificado de servidor generados. El cliente debe confiar en certs/ca.cer."
}

server() {
  if [ ! -f "bin/server/NetAuctionServer.class" ]; then
    compile
  fi
  PORT="${2:-9999}"
  if [ ! -f "certs/servidor.p12" ] || [ ! -f "certs/truststore.p12" ]; then
    certs "$@"
  fi
  java -cp "lib/*:bin" server.NetAuctionServer "$PORT" --ssl
}

show_help() {
  echo "Uso:"
  echo "  ./run.sh compile"
  echo "  ./run.sh initdb"
  echo "  ./run.sh certs [--force|--reset-ca]"
  echo "  ./run.sh server [puerto]"
}

case "$CMD" in
  setup)   setup ;;
  compile) compile ;;
  initdb)  initdb ;;
  certs)   certs "$@" ;;
  server)  server "$@" ;;
  help|*)  show_help ;;
esac
