package server.security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utilidades criptográficas para el sistema NetAuction.
 * Proporciona funciones de hashing (SHA-256 + salt) y cifrado (AES-256-CBC).
 *
 * @author NetAuction Team
 * @version 1.0
 */
public final class CryptoUtils {

    /** Algoritmo de hash */
    private static final String HASH_ALGORITHM = "SHA-256";

    /** Algoritmo de cifrado */
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";

    /** Longitud del salt en bytes */
    private static final int SALT_LENGTH = 16;

    /** Longitud del IV para AES en bytes */
    private static final int IV_LENGTH = 16;

    /** Longitud de la clave AES en bits */
    private static final int AES_KEY_LENGTH = 256;

    /** Generador de números aleatorios seguros */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** Clave AES para cifrado de datos sensibles (se genera al iniciar) */
    private static SecretKey aesKey;

    static {
        try {
            // Generar clave AES al inicio (en producción se cargaría de forma segura)
            aesKey = generateAESKey();
        } catch (Exception e) {
            throw new RuntimeException("Error inicializando clave AES", e);
        }
    }

    /**
     * Constructor privado para evitar instanciación.
     */
    private CryptoUtils() {
        throw new UnsupportedOperationException("CryptoUtils cannot be instantiated");
    }

    // ==================== HASHING ====================

    /**
     * Genera un salt aleatorio.
     *
     * @return salt codificado en Base64
     */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Calcula el hash SHA-256 de una contraseña con salt.
     *
     * @param password contraseña en texto plano
     * @param salt salt a usar
     * @return hash codificado en Base64
     */
    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);

            // Combinar password + salt
            String combined = password + salt;
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));

            // Aplicar múltiples iteraciones para mayor seguridad
            for (int i = 0; i < 10000; i++) {
                hash = digest.digest(hash);
            }

            return Base64.getEncoder().encodeToString(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo de hash no disponible: " + HASH_ALGORITHM, e);
        }
    }

    /**
     * Verifica si una contraseña coincide con un hash.
     *
     * @param password contraseña en texto plano
     * @param salt salt usado originalmente
     * @param expectedHash hash esperado
     * @return true si coinciden
     */
    public static boolean verifyPassword(String password, String salt, String expectedHash) {
        String actualHash = hashPassword(password, salt);
        return constantTimeEquals(actualHash, expectedHash);
    }

    /**
     * Comparación en tiempo constante para evitar timing attacks.
     *
     * @param a primera cadena
     * @param b segunda cadena
     * @return true si son iguales
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    // ==================== CIFRADO AES ====================

    /**
     * Genera una clave AES-256.
     *
     * @return clave AES generada
     * @throws Exception si hay error generando la clave
     */
    public static SecretKey generateAESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(AES_KEY_LENGTH, SECURE_RANDOM);
        return keyGen.generateKey();
    }

    /**
     * Cifra un texto usando AES-256-CBC.
     *
     * @param plaintext texto a cifrar
     * @return texto cifrado codificado en Base64 (IV + ciphertext)
     */
    public static String encrypt(String plaintext) {
        try {
            // Generar IV aleatorio
            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // Cifrar
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, ivSpec);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Concatenar IV + ciphertext
            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(result);

        } catch (Exception e) {
            throw new RuntimeException("Error cifrando datos", e);
        }
    }

    /**
     * Descifra un texto usando AES-256-CBC.
     *
     * @param ciphertext texto cifrado en Base64
     * @return texto descifrado
     */
    public static String decrypt(String ciphertext) {
        try {
            byte[] data = Base64.getDecoder().decode(ciphertext);

            // Extraer IV
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(data, 0, iv, 0, iv.length);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // Extraer ciphertext
            byte[] encrypted = new byte[data.length - IV_LENGTH];
            System.arraycopy(data, IV_LENGTH, encrypted, 0, encrypted.length);

            // Descifrar
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, ivSpec);
            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("Error descifrando datos", e);
        }
    }

    /**
     * Cifra datos con una clave específica.
     *
     * @param plaintext texto a cifrar
     * @param keyBase64 clave en Base64
     * @return texto cifrado en Base64
     */
    public static String encryptWithKey(String plaintext, String keyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(result);

        } catch (Exception e) {
            throw new RuntimeException("Error cifrando con clave específica", e);
        }
    }

    /**
     * Descifra datos con una clave específica.
     *
     * @param ciphertext texto cifrado en Base64
     * @param keyBase64 clave en Base64
     * @return texto descifrado
     */
    public static String decryptWithKey(String ciphertext, String keyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

            byte[] data = Base64.getDecoder().decode(ciphertext);

            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(data, 0, iv, 0, iv.length);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            byte[] encrypted = new byte[data.length - IV_LENGTH];
            System.arraycopy(data, IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("Error descifrando con clave específica", e);
        }
    }

    /**
     * Genera una clave AES y la devuelve en Base64.
     *
     * @return clave AES en Base64
     */
    public static String generateAESKeyBase64() {
        try {
            SecretKey key = generateAESKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Error generando clave AES", e);
        }
    }

    // ==================== UTILIDADES ====================

    /**
     * Genera un token UUID único.
     *
     * @return token UUID como String
     */
    public static String generateToken() {
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * Genera un ID único para subastas.
     *
     * @return ID único
     */
    public static String generateAuctionId() {
        return "AUC-" + System.currentTimeMillis() + "-" +
               Integer.toHexString(SECURE_RANDOM.nextInt(0xFFFF)).toUpperCase();
    }
}
