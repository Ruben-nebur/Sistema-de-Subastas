package client;

import common.Constants;
import common.Message;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Encapsula la conexión con el servidor NetAuction.
 * Proporciona métodos para enviar y recibir mensajes JSON.
 *
 * @author NetAuction Team
 * @version 1.0
 */
public class ServerConnection {

    /** Socket de conexión */
    private Socket socket;

    /** Writer para enviar mensajes */
    private PrintWriter out;

    /** Reader para recibir mensajes */
    private BufferedReader in;

    /** Indica si la conexión está activa */
    private boolean connected;

    /** Indica si SSL está habilitado */
    private boolean sslEnabled;

    /**
     * Constructor por defecto.
     */
    public ServerConnection() {
        this.connected = false;
        this.sslEnabled = false;
    }

    /**
     * Constructor con opción SSL.
     *
     * @param sslEnabled habilitar SSL/TLS
     */
    public ServerConnection(boolean sslEnabled) {
        this.connected = false;
        this.sslEnabled = sslEnabled;
    }

    /**
     * Establece conexión con el servidor.
     *
     * @param host dirección del servidor
     * @param port puerto del servidor
     * @throws IOException si no se puede conectar
     */
    public void connect(String host, int port) throws IOException {
        if (sslEnabled) {
            try {
                socket = createSSLSocket(host, port);
                System.out.println("[CONNECTION] Conexión SSL establecida");
            } catch (Exception e) {
                System.err.println("[CONNECTION] Error SSL: " + e.getMessage());
                System.out.println("[CONNECTION] Usando conexión sin cifrar...");
                socket = new Socket(host, port);
            }
        } else {
            socket = new Socket(host, port);
        }
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        connected = true;
    }

    /**
     * Crea un socket SSL para conexión segura.
     */
    private SSLSocket createSSLSocket(String host, int port) throws Exception {
        // Cargar truststore
        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(Constants.CLIENT_TRUSTSTORE_PATH)) {
            trustStore.load(fis, Constants.KEYSTORE_PASSWORD.toCharArray());
        }

        // Crear TrustManagerFactory
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        // Crear SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);

        SSLSocketFactory factory = sslContext.getSocketFactory();
        SSLSocket sslSocket = (SSLSocket) factory.createSocket(host, port);
        sslSocket.setEnabledProtocols(new String[]{"TLSv1.2", "TLSv1.3"});
        sslSocket.startHandshake();

        return sslSocket;
    }

    /**
     * Envía un mensaje al servidor.
     *
     * @param message mensaje a enviar
     * @throws IOException si hay error de comunicación
     */
    public void send(Message message) throws IOException {
        if (!connected) {
            throw new IOException("No conectado al servidor");
        }
        out.println(message.toJson());
    }

    /**
     * Lee un mensaje del servidor (bloqueante).
     *
     * @return mensaje recibido o null si se cerró la conexión
     * @throws IOException si hay error de comunicación
     */
    public Message receive() throws IOException {
        if (!connected) {
            throw new IOException("No conectado al servidor");
        }
        String line = in.readLine();
        if (line == null) {
            return null;
        }
        return Message.fromJson(line);
    }

    /**
     * Obtiene el reader para lectura asíncrona.
     *
     * @return BufferedReader de entrada
     */
    public BufferedReader getReader() {
        return in;
    }

    /**
     * Verifica si está conectado.
     *
     * @return true si hay conexión activa
     */
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    /**
     * Cierra la conexión.
     */
    public void disconnect() {
        connected = false;

        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            // Ignorar
        }

        if (out != null) {
            out.close();
        }

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Ignorar
        }
    }
}
