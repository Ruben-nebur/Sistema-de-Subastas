package common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * Clase que representa un mensaje del protocolo de comunicación de NetAuction.
 * Cada mensaje contiene una acción, un token opcional y datos adicionales en formato JSON.
 *
 * <p>Estructura del mensaje JSON:</p>
 * <pre>
 * {
 *   "action": "NOMBRE_ACCION",
 *   "token": "uuid-token-o-null",
 *   "data": { ... }
 * }
 * </pre>
 *
 * @author NetAuction Team
 * @version 1.0
 */
public class Message {

    /** Instancia compartida de Gson para serialización/deserialización */
    private static final Gson GSON = new GsonBuilder().create();

    /** Nombre de la acción a ejecutar */
    private String action;

    /** Token de sesión del usuario (null si no está autenticado) */
    private String token;

    /** Datos adicionales del mensaje en formato JSON */
    private JsonObject data;

    /**
     * Constructor por defecto.
     * Inicializa data como un JsonObject vacío.
     */
    public Message() {
        this.data = new JsonObject();
    }

    /**
     * Constructor con acción.
     *
     * @param action nombre de la acción
     */
    public Message(String action) {
        this.action = action;
        this.data = new JsonObject();
    }

    /**
     * Constructor completo.
     *
     * @param action nombre de la acción
     * @param token token de sesión (puede ser null)
     * @param data datos adicionales
     */
    public Message(String action, String token, JsonObject data) {
        this.action = action;
        this.token = token;
        this.data = data != null ? data : new JsonObject();
    }

    /**
     * Obtiene el nombre de la acción.
     *
     * @return nombre de la acción
     */
    public String getAction() {
        return action;
    }

    /**
     * Establece el nombre de la acción.
     *
     * @param action nombre de la acción
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * Obtiene el token de sesión.
     *
     * @return token de sesión o null si no existe
     */
    public String getToken() {
        return token;
    }

    /**
     * Establece el token de sesión.
     *
     * @param token token de sesión
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Obtiene los datos adicionales del mensaje.
     *
     * @return datos en formato JsonObject
     */
    public JsonObject getData() {
        return data;
    }

    /**
     * Establece los datos adicionales del mensaje.
     *
     * @param data datos en formato JsonObject
     */
    public void setData(JsonObject data) {
        this.data = data != null ? data : new JsonObject();
    }

    /**
     * Añade un valor String a los datos.
     *
     * @param key clave del dato
     * @param value valor del dato
     * @return esta instancia para encadenamiento
     */
    public Message addData(String key, String value) {
        this.data.addProperty(key, value);
        return this;
    }

    /**
     * Añade un valor numérico a los datos.
     *
     * @param key clave del dato
     * @param value valor del dato
     * @return esta instancia para encadenamiento
     */
    public Message addData(String key, Number value) {
        this.data.addProperty(key, value);
        return this;
    }

    /**
     * Añade un valor booleano a los datos.
     *
     * @param key clave del dato
     * @param value valor del dato
     * @return esta instancia para encadenamiento
     */
    public Message addData(String key, Boolean value) {
        this.data.addProperty(key, value);
        return this;
    }

    /**
     * Obtiene un valor String de los datos.
     *
     * @param key clave del dato
     * @return valor del dato o null si no existe
     */
    public String getDataString(String key) {
        if (data.has(key) && !data.get(key).isJsonNull()) {
            return data.get(key).getAsString();
        }
        return null;
    }

    /**
     * Obtiene un valor int de los datos.
     *
     * @param key clave del dato
     * @param defaultValue valor por defecto si no existe
     * @return valor del dato o defaultValue si no existe
     */
    public int getDataInt(String key, int defaultValue) {
        if (data.has(key) && !data.get(key).isJsonNull()) {
            return data.get(key).getAsInt();
        }
        return defaultValue;
    }

    /**
     * Obtiene un valor double de los datos.
     *
     * @param key clave del dato
     * @param defaultValue valor por defecto si no existe
     * @return valor del dato o defaultValue si no existe
     */
    public double getDataDouble(String key, double defaultValue) {
        if (data.has(key) && !data.get(key).isJsonNull()) {
            return data.get(key).getAsDouble();
        }
        return defaultValue;
    }

    /**
     * Obtiene un valor boolean de los datos.
     *
     * @param key clave del dato
     * @param defaultValue valor por defecto si no existe
     * @return valor del dato o defaultValue si no existe
     */
    public boolean getDataBoolean(String key, boolean defaultValue) {
        if (data.has(key) && !data.get(key).isJsonNull()) {
            return data.get(key).getAsBoolean();
        }
        return defaultValue;
    }

    /**
     * Verifica si el mensaje tiene un campo de datos específico.
     *
     * @param key clave a verificar
     * @return true si el campo existe
     */
    public boolean hasData(String key) {
        return data.has(key);
    }

    /**
     * Serializa el mensaje a JSON.
     *
     * @return representación JSON del mensaje en una línea
     */
    public String toJson() {
        return GSON.toJson(this);
    }

    /**
     * Deserializa un mensaje desde JSON.
     *
     * @param json cadena JSON a parsear
     * @return instancia de Message
     * @throws com.google.gson.JsonSyntaxException si el JSON es inválido
     */
    public static Message fromJson(String json) {
        return GSON.fromJson(json, Message.class);
    }

    /**
     * Crea un mensaje de respuesta para una acción específica.
     *
     * @param originalAction acción original del cliente
     * @param status estado de la respuesta (OK o ERROR)
     * @param message mensaje descriptivo
     * @return mensaje de respuesta
     */
    public static Message createResponse(String originalAction, String status, String message) {
        Message response = new Message(originalAction + Constants.RESPONSE_SUFFIX);
        response.addData("status", status);
        response.addData("message", message);
        return response;
    }

    /**
     * Crea un mensaje de respuesta exitosa.
     *
     * @param originalAction acción original del cliente
     * @param message mensaje descriptivo
     * @return mensaje de respuesta exitosa
     */
    public static Message createSuccessResponse(String originalAction, String message) {
        return createResponse(originalAction, Constants.STATUS_OK, message);
    }

    /**
     * Crea un mensaje de respuesta de error.
     *
     * @param originalAction acción original del cliente
     * @param message mensaje de error
     * @return mensaje de respuesta de error
     */
    public static Message createErrorResponse(String originalAction, String message) {
        return createResponse(originalAction, Constants.STATUS_ERROR, message);
    }

    /**
     * Crea un mensaje de notificación push.
     *
     * @param notificationType tipo de notificación (NEW_BID, OUTBID, AUCTION_CLOSED)
     * @return mensaje de notificación
     */
    public static Message createNotification(String notificationType) {
        return new Message(notificationType);
    }

    /**
     * Verifica si este mensaje es una respuesta (termina en _RESPONSE).
     *
     * @return true si es una respuesta
     */
    public boolean isResponse() {
        return action != null && action.endsWith(Constants.RESPONSE_SUFFIX);
    }

    /**
     * Verifica si este mensaje es una notificación push.
     *
     * @return true si es una notificación
     */
    public boolean isNotification() {
        return action != null && (
            action.equals(Constants.NOTIFY_NEW_BID) ||
            action.equals(Constants.NOTIFY_OUTBID) ||
            action.equals(Constants.NOTIFY_AUCTION_CLOSED)
        );
    }

    /**
     * Verifica si la respuesta indica éxito.
     *
     * @return true si el status es OK
     */
    public boolean isSuccess() {
        String status = getDataString("status");
        return Constants.STATUS_OK.equals(status);
    }

    @Override
    public String toString() {
        return toJson();
    }
}
