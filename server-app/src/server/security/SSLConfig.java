package server.security;

import common.Constants;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;

/**
 * Configuración SSL/TLS para comunicación segura.
 * Proporciona métodos para crear sockets SSL para servidor y cliente.
 *
 * @author NetAuction Team
 * @version 1.0
 */
public final class SSLConfig {

    /**
     * Constructor privado para evitar instanciación.
     */
    private SSLConfig() {
        throw new UnsupportedOperationException("SSLConfig cannot be instantiated");
    }

    /**
     * Crea un SSLServerSocketFactory para el servidor.
     *
     * @param keystorePath ruta del keystore
     * @param keystorePassword contraseña del keystore
     * @return factory para crear SSLServerSocket
     * @throws Exception si hay error configurando SSL
     */
    public static SSLServerSocketFactory createServerSocketFactory(String keystorePath, String keystorePassword)
            throws Exception {

        // Cargar keystore
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            keyStore.load(fis, keystorePassword.toCharArray());
        }

        // Crear KeyManagerFactory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keystorePassword.toCharArray());

        // Crear SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
        sslContext.init(kmf.getKeyManagers(), null, null);

        return sslContext.getServerSocketFactory();
    }

    /**
     * Crea un SSLServerSocketFactory con configuración por defecto.
     *
     * @return factory para crear SSLServerSocket
     * @throws Exception si hay error configurando SSL
     */
    public static SSLServerSocketFactory createServerSocketFactory() throws Exception {
        return createServerSocketFactory(
            Constants.SERVER_KEYSTORE_PATH,
            Constants.KEYSTORE_PASSWORD
        );
    }

    /**
     * Crea un SSLSocketFactory para el cliente.
     *
     * @param truststorePath ruta del truststore
     * @param truststorePassword contraseña del truststore
     * @return factory para crear SSLSocket
     * @throws Exception si hay error configurando SSL
     */
    public static SSLSocketFactory createClientSocketFactory(String truststorePath, String truststorePassword)
            throws Exception {

        // Cargar truststore
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(truststorePath)) {
            trustStore.load(fis, truststorePassword.toCharArray());
        }

        // Crear TrustManagerFactory
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        // Crear SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
        sslContext.init(null, tmf.getTrustManagers(), null);

        return sslContext.getSocketFactory();
    }

    /**
     * Crea un SSLSocketFactory con configuración por defecto.
     *
     * @return factory para crear SSLSocket
     * @throws Exception si hay error configurando SSL
     */
    public static SSLSocketFactory createClientSocketFactory() throws Exception {
        return createClientSocketFactory(
            Constants.CLIENT_TRUSTSTORE_PATH,
            Constants.KEYSTORE_PASSWORD
        );
    }

    /**
     * Crea un SSLServerSocket configurado.
     *
     * @param port puerto del servidor
     * @return SSLServerSocket configurado
     * @throws Exception si hay error creando el socket
     */
    public static SSLServerSocket createServerSocket(int port) throws Exception {
        SSLServerSocketFactory factory = createServerSocketFactory();
        SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(port);

        // Configurar protocolos y cifrados
        serverSocket.setEnabledProtocols(new String[]{"TLSv1.3"});

        return serverSocket;
    }

    /**
     * Crea un SSLSocket configurado para conectar al servidor.
     *
     * @param host dirección del servidor
     * @param port puerto del servidor
     * @return SSLSocket configurado
     * @throws Exception si hay error creando el socket
     */
    public static SSLSocket createClientSocket(String host, int port) throws Exception {
        SSLSocketFactory factory = createClientSocketFactory();
        SSLSocket socket = (SSLSocket) factory.createSocket(host, port);

        // Configurar protocolos
        socket.setEnabledProtocols(new String[]{"TLSv1.3"});

        // Iniciar handshake
        socket.startHandshake();

        return socket;
    }

    /**
     * Verifica si los archivos de certificados existen.
     *
     * @return true si todos los archivos necesarios existen
     */
    public static boolean certificatesExist() {
        java.io.File keystore = new java.io.File(Constants.SERVER_KEYSTORE_PATH);
        java.io.File truststore = new java.io.File(Constants.CLIENT_TRUSTSTORE_PATH);
        return keystore.exists() && truststore.exists();
    }
}
