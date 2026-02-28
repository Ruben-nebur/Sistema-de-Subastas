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
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Encapsula la conexi??n con el servidor NetAuction.
 * Proporciona m??todos para enviar y recibir mensajes JSON.
 *
 * @author NetAuction Team
 * @version 1.0
 */
public class ServerConnection {

    /** Socket de conexi??n */
    private Socket socket;

    /** Writer para enviar mensajes */
    private PrintWriter out;

    /** Reader para recibir mensajes */
    private BufferedReader in;

    /** Indica si la conexi??n est?? activa */
    private boolean connected;

    /** Indica si SSL est?? habilitado */
    private boolean sslEnabled;

    /**
     * Constructor por defecto.
     */
    public ServerConnection() {
        this.connected = false;
        this.sslEnabled = true;
    }

    /**
     * Constructor con opci??n SSL.
     *
     * @param sslEnabled habilitar SSL/TLS
     */
    public ServerConnection(boolean sslEnabled) {
        this.connected = false;
        this.sslEnabled = sslEnabled;
    }

    /**
     * Establece conexi??n con el servidor.
     *
     * @param host direcci??n del servidor
     * @param port puerto del servidor
     * @throws IOException si no se puede conectar
     */
    public void connect(String host, int port) throws IOException {
        if (sslEnabled) {
            try {
                socket = createSSLSocket(host, port);
                System.out.println("[CONNECTION] Conexion SSL establecida");
            } catch (Exception e) {
                throw new IOException("No se pudo establecer SSL/TLS: " + e.getMessage(), e);
            }
        } else {
            socket = new Socket(host, port);
        }
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        connected = true;
    }

    /**
     * Crea un socket SSL para conexion segura.
     */
    private SSLSocket createSSLSocket(String host, int port) throws Exception {
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);

        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        try (FileInputStream fis = new FileInputStream(Constants.CA_CERTIFICATE_PATH)) {
            var caCertificate = certificateFactory.generateCertificate(fis);
            trustStore.setCertificateEntry("netauction-ca", caCertificate);
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);

        SSLSocketFactory factory = sslContext.getSocketFactory();
        SSLSocket sslSocket = (SSLSocket) factory.createSocket(host, port);
        sslSocket.setEnabledProtocols(getEnabledProtocols(sslSocket.getSupportedProtocols()));
        SSLParameters sslParameters = sslSocket.getSSLParameters();
        sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
        sslSocket.setSSLParameters(sslParameters);
        sslSocket.startHandshake();

        return sslSocket;
    }

    private String[] getEnabledProtocols(String[] supportedProtocols) {
        List<String> enabled = new ArrayList<>();
        List<String> supported = Arrays.asList(supportedProtocols);
        if (supported.contains("TLSv1.3")) {
            enabled.add("TLSv1.3");
        }
        if (supported.contains("TLSv1.2")) {
            enabled.add("TLSv1.2");
        }
        if (enabled.isEmpty()) {
            throw new IllegalStateException("La JVM no soporta TLSv1.2 ni TLSv1.3");
        }
        return enabled.toArray(new String[0]);
    }

    /**
     * Env??a un mensaje al servidor.
     *
     * @param message mensaje a enviar
     * @throws IOException si hay error de comunicaci??n
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
     * @return mensaje recibido o null si se cerr?? la conexi??n
     * @throws IOException si hay error de comunicaci??n
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
     * Obtiene el reader para lectura as??ncrona.
     *
     * @return BufferedReader de entrada
     */
    public BufferedReader getReader() {
        return in;
    }

    /**
     * Verifica si est?? conectado.
     *
     * @return true si hay conexi??n activa
     */
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    /**
     * Cierra la conexi??n.
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
