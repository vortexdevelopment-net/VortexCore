package net.vortexdevelopment.vortexcore.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * License verification utility for Vortex plugins.
 * Handles both online and offline license verification.
 *
 * Usage:
 * 
 * <pre>
 * try {
 *     LicenseVerifier.verify("VortexCore:plugin-name", "1.0.0", getDataFolder());
 *     // Plugin can enable
 * } catch (LicenseVerificationException e) {
 *     // Prevent plugin from enabling
 * }
 * </pre>
 */
public class LicenseVerifier {

    // Injected values (replaced during JAR injection)
    public static final String LICENSE;
    public static final String USER_ID;
    public static final String USERNAME;
    public static final String PRODUCT_KEY;

    static {
        LICENSE = "%%__LICENSE__%%";
        USER_ID = "%%__USERID__%%";
        USERNAME = "%%__USERNAME__%%";
        PRODUCT_KEY = "%%__PRODUCT_KEY__%%";
    }

    // Configuration
    private static final String AUTH_SERVER_URL = "https://auth.vortexdevelopment.net";
    private static final String VERIFY_ENDPOINT = AUTH_SERVER_URL + "/license/plugins/verify";

    // Public key resource path
    private static final String PUBLIC_KEY_RESOURCE_PATH = "/license/public_key.pem";

    // License file name
    private static final String LICENSE_FILE_NAME = ".license";

    // Cached public key (loaded once from resource)
    private static ECPublicKey cachedPublicKey;

    // Cached token (in-memory cache)
    private static String cachedToken;
    private static long tokenExpiresAt;

    // Verification settings
    private static final long VERIFICATION_INTERVAL_MS = TimeUnit.HOURS.toMillis(24); // Re-verify every 24 hours
    private static final long TOKEN_REFRESH_THRESHOLD_MS = TimeUnit.HOURS.toMillis(1); // Refresh if expires in < 1 hour
    private static final long CLOCK_SKEW_LEEWAY_SECONDS = TimeUnit.HOURS.toSeconds(24); // Allow 24 hours leeway for clock skew and timezone differences

    // HTTP client (reused)
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();

    private static final String LICENSE_PLACEHOLDER;
    private static final String USER_ID_PLACEHOLDER;
    private static final String USERNAME_PLACEHOLDER;
    private static final String PRODUCT_KEY_PLACEHOLDER;

    static {
        StringBuilder licensePlaceholderBuilder = new StringBuilder("%%__");
        licensePlaceholderBuilder.append("LICENSE__%%");
        LICENSE_PLACEHOLDER = licensePlaceholderBuilder.toString();

        StringBuilder userIdPlaceholderBuilder = new StringBuilder("%%__");
        userIdPlaceholderBuilder.append("USERID__%%");
        USER_ID_PLACEHOLDER = userIdPlaceholderBuilder.toString();

        StringBuilder usernamePlaceholderBuilder = new StringBuilder("%%__");
        usernamePlaceholderBuilder.append("USERNAME__%%");
        USERNAME_PLACEHOLDER = usernamePlaceholderBuilder.toString();

        StringBuilder productKeyPlaceholderBuilder = new StringBuilder("%%__");
        productKeyPlaceholderBuilder.append("PRODUCT_KEY__%%");
        PRODUCT_KEY_PLACEHOLDER = productKeyPlaceholderBuilder.toString();
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static boolean isDevLicense() {
        //Get file from server root "DEV" and reade JWT token
        File file = new File("VORTEX_DEV_LICENSE");
        if (!file.exists() || !file.isFile()) {
            return false;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String token = reader.readLine();
            if (token == null || token.isEmpty()) {
                return false;
            }
            // Decode the content with the public key
            DecodedJWT decodedJWT = JWT.require(getAlgorithm())
                    .acceptLeeway(CLOCK_SKEW_LEEWAY_SECONDS)
                    .build()
                    .verify(token);
            String licenseType = decodedJWT.getClaim("license_uuid").asString();
            return licenseType != null && licenseType.equals("DEVELOPMENT_LICENSE");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean isFreeLicense(Class<?> plugin) {
        if (isDevLicense()) {
            return true;
        }

        try (InputStream inputStream = plugin.getResourceAsStream("/license/freemium")) {
            if (inputStream == null) {
                return false;
            }
            // Decode the content with the public key
            String freemiumData = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            DecodedJWT decodedJWT = JWT.require(getAlgorithm())
                    .acceptLeeway(CLOCK_SKEW_LEEWAY_SECONDS)
                    .build()
                    .verify(freemiumData);
            String licenseUuid = decodedJWT.getClaim("license_uuid").asString();
            return licenseUuid != null && licenseUuid.equals(plugin.getName().toLowerCase(Locale.ENGLISH));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Main entry point for license verification.
     * Call this in your plugin's onEnable() method.
     *
     * @param productName   The product name (e.g., "VortexCore:plugin-name")
     * @param pluginVersion The plugin version (e.g., "1.0.0") - used for User-Agent
     * @param dataFolder    The plugin's data folder (where .license file will be
     *                      stored)
     * @throws LicenseVerificationException if verification fails
     */
    public static void verify(Class<?> plugin, String productName, String pluginVersion, File dataFolder) throws LicenseVerificationException {

        if (isFreeLicense(plugin)) {
            return;
        }

        // Do not use the string literal anywhere else or it gets replaced during
        // injection
        // Validate injected values
        // LICENSE is optional (VIP users may not have a license UUID)
        // USER_ID is required for VIP check when no license is found
        if ((LICENSE == null || LICENSE.equals(LICENSE_PLACEHOLDER) || LICENSE.isEmpty()) &&
                (USER_ID == null || USER_ID.equals(USER_ID_PLACEHOLDER) || USER_ID.isEmpty())) {
            throw new LicenseVerificationException(
                    "License UUID or User ID not found. Plugin may not be properly licensed.");
        }

        if (PRODUCT_KEY == null || PRODUCT_KEY.equals(PRODUCT_KEY_PLACEHOLDER) || PRODUCT_KEY.isEmpty()) {
            throw new LicenseVerificationException("Product key not found. Plugin may not be properly licensed.");
        }

        if (productName == null || productName.isEmpty()) {
            throw new LicenseVerificationException("Product name cannot be null or empty.");
        }

        if (dataFolder == null) {
            throw new LicenseVerificationException("Data folder cannot be null.");
        }

        // Ensure data folder exists
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        // First, try to load token from .license file
        File licenseFile = new File(dataFolder, LICENSE_FILE_NAME);
        String tokenFromFile = loadTokenFromFile(licenseFile);

        if (tokenFromFile != null && !tokenFromFile.isEmpty()) {
            // Token found in file, try offline verification
            try {
                verifyTokenOffline(tokenFromFile);

                // Check if token expires soon (refresh threshold)
                long now = System.currentTimeMillis() / 1000;
                long expiresAt = getTokenExpiration(tokenFromFile);

                if (expiresAt > 0 && expiresAt - now < TOKEN_REFRESH_THRESHOLD_MS / 1000) {
                    // Token expires soon, refresh it online
                    try {
                        verifyOnline(productName, pluginVersion, licenseFile);
                        return;
                    } catch (LicenseVerificationException e) {
                        // If refresh fails, token is still valid for now
                        // Continue with offline verification
                    }
                }

                // Token is valid, cache it and return
                cachedToken = tokenFromFile;
                tokenExpiresAt = expiresAt;
                return;

            } catch (LicenseVerificationException e) {
                // Offline verification failed, token may be invalid or expired
                // Delete the file and try online verification
                licenseFile.delete();
                cachedToken = null;
            }
        }

        // No valid token in file, perform online verification
        verifyOnline(productName, pluginVersion, licenseFile);
    }

    /**
     * Overloaded method for backward compatibility (requires data folder).
     *
     * @param productName   The product name
     * @param pluginVersion The plugin version
     * @throws LicenseVerificationException if verification fails
     * @deprecated Use verify(String, String, File) instead
     */
    @Deprecated
    public static void verify(String productName, String pluginVersion) throws LicenseVerificationException {
        throw new LicenseVerificationException(
                "Data folder is required. Use verify(String productName, String pluginVersion, File dataFolder) instead.");
    }

    /**
     * Verifies license online with the auth server.
     *
     * @param productName   The product name
     * @param pluginVersion The plugin version
     * @param licenseFile   The license file to save the token to
     * @throws LicenseVerificationException if verification fails
     */
    private static void verifyOnline(String productName, String pluginVersion, File licenseFile)
            throws LicenseVerificationException {
        try {
            // Build request body
            JsonObject requestBody = new JsonObject();

            // Add license_uuid if available (may be null for VIP users)
            if (LICENSE != null && !LICENSE.equals(LICENSE_PLACEHOLDER) && !LICENSE.isEmpty()) {
                requestBody.addProperty("license_uuid", LICENSE);
            }

            // Use PRODUCT_KEY for product_name (injected value, matches database)
            // productName parameter may include "VortexCore:" prefix which is only for
            // User-Agent
            String productKeyForVerification = PRODUCT_KEY;
            if (productKeyForVerification == null || productKeyForVerification.equals("%%__PRODUCT_KEY__%%")
                    || productKeyForVerification.isEmpty()) {
                // Fallback: strip "VortexCore:" prefix from productName if PRODUCT_KEY not
                // available
                if (productName != null && productName.startsWith("VortexCore:")) {
                    productKeyForVerification = productName.substring("VortexCore:".length());
                } else {
                    productKeyForVerification = productName;
                }
            }
            requestBody.addProperty("product_name", productKeyForVerification);

            // Add user_id if available (required for VIP check when no license found)
            if (USER_ID != null && !USER_ID.equals("%%__USERID__%%") && !USER_ID.isEmpty()) {
                try {
                    long userId = Long.parseLong(USER_ID);
                    requestBody.addProperty("user_id", userId);
                } catch (NumberFormatException e) {
                    // Invalid user_id format, skip it
                }
            }

            // Build HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(VERIFY_ENDPOINT))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", productName + "/" + pluginVersion)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString(), StandardCharsets.UTF_8))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .build();

            // Send request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Parse response
            if (response.statusCode() != 200) {
                String errorMessage = "License verification failed with status code: " + response.statusCode();
                try {
                    JsonObject errorJson = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (errorJson.has("reason")) {
                        String reason = errorJson.get("reason").getAsString();
                        errorMessage = "License verification failed: " + reason;

                        // Include detailed error information if available
                        if (errorJson.has("details")) {
                            JsonObject details = errorJson.getAsJsonObject("details");
                            StringBuilder detailsMessage = new StringBuilder(errorMessage);
                            detailsMessage.append(" (");

                            if (details.has("stored_product_name")) {
                                detailsMessage.append("stored: '")
                                        .append(details.get("stored_product_name").getAsString()).append("'");
                            }
                            if (details.has("requested_product_name")) {
                                if (detailsMessage.length() > errorMessage.length() + 1) {
                                    detailsMessage.append(", ");
                                }
                                detailsMessage.append("requested: '")
                                        .append(details.get("requested_product_name").getAsString()).append("'");
                            }
                            if (details.has("license_uuid")) {
                                if (detailsMessage.length() > errorMessage.length() + 1) {
                                    detailsMessage.append(", ");
                                }
                                detailsMessage.append("license: ").append(details.get("license_uuid").getAsString());
                            }

                            detailsMessage.append(")");
                            errorMessage = detailsMessage.toString();
                        }
                    }
                } catch (Exception e) {
                    // Ignore JSON parsing errors
                }
                throw new LicenseVerificationException(errorMessage);
            }

            // Parse response JSON
            JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();

            if (!jsonResponse.has("valid") || !jsonResponse.get("valid").getAsBoolean()) {
                String reason = jsonResponse.has("reason")
                        ? jsonResponse.get("reason").getAsString()
                        : "Unknown error";
                throw new LicenseVerificationException("License verification failed: " + reason);
            }

            // Extract token
            if (!jsonResponse.has("token")) {
                throw new LicenseVerificationException("License verification response missing token.");
            }

            String token = jsonResponse.get("token").getAsString();
            long validUntil = jsonResponse.has("valid_until")
                    ? jsonResponse.get("valid_until").getAsLong()
                    : System.currentTimeMillis() / 1000 + (3 * 24 * 60 * 60); // Default 3 days

            // Verify token offline (validate signature)
            verifyTokenOffline(token);

            // Cache token
            cachedToken = token;
            tokenExpiresAt = validUntil;

            // Save token to file for future offline verification
            saveTokenToFile(licenseFile, token, validUntil);

        } catch (IOException e) {
            throw new LicenseVerificationException("Network error during license verification: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LicenseVerificationException("License verification interrupted: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof LicenseVerificationException) {
                throw e;
            }
            throw new LicenseVerificationException("Unexpected error during license verification: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Loads token from the .license file.
     *
     * @param licenseFile The license file
     * @return The token string, or null if file doesn't exist or is empty
     */
    private static String loadTokenFromFile(File licenseFile) {
        if (!licenseFile.exists() || !licenseFile.isFile()) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(licenseFile))) {
            return reader.readLine();
        } catch (IOException e) {
            // File read error, return null to trigger online verification
            return null;
        }
    }

    /**
     * Saves token to the .license file.
     *
     * @param licenseFile The license file
     * @param token       The JWT token
     * @param expiresAt   The expiration timestamp
     * @throws LicenseVerificationException if file cannot be written
     */
    private static void saveTokenToFile(File licenseFile, String token, long expiresAt)
            throws LicenseVerificationException {
        try (FileWriter writer = new FileWriter(licenseFile)) {
            writer.write(token);
            writer.write("\n");
            writer.write(String.valueOf(expiresAt));
        } catch (IOException e) {
            throw new LicenseVerificationException(
                    "Failed to save license token to file: " + licenseFile.getAbsolutePath() +
                            ". Error: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Gets the expiration time from a token.
     *
     * @param token The JWT token
     * @return The expiration timestamp, or 0 if cannot be determined
     */
    private static long getTokenExpiration(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            Long validUntil = jwt.getClaim("valid_until").asLong();
            return validUntil != null ? validUntil : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Verifies JWT token offline using the public key.
     *
     * @param token The JWT token to verify
     * @throws LicenseVerificationException if verification fails
     */
    private static void verifyTokenOffline(String token) throws LicenseVerificationException {
        try {
            // Parse and verify token
            DecodedJWT jwt = JWT.require(getAlgorithm())
                    .acceptLeeway(CLOCK_SKEW_LEEWAY_SECONDS)
                    .build()
                    .verify(token);

            // Verify token claims
            String licenseUuid = jwt.getClaim("license_uuid").asString();
            if (licenseUuid == null) {
                throw new LicenseVerificationException("Token license UUID is missing.");
            }

            // For regular licenses, token must match the injected LICENSE
            if (LICENSE != null && !LICENSE.equals("%%__LICENSE__%%") && !LICENSE.isEmpty()) {
                if (!licenseUuid.equals(LICENSE)) {
                    throw new LicenseVerificationException("Token license UUID does not match plugin license.");
                }
            }
            // For VIP users (no LICENSE set), accept tokens with VIP- prefix
            else if (!licenseUuid.startsWith("VIP-")) {
                throw new LicenseVerificationException("Token license UUID is invalid for VIP user.");
            }

            // Check expiration
            long validUntil = jwt.getClaim("valid_until").asLong();
            long now = System.currentTimeMillis() / 1000;

            if (validUntil <= now) {
                throw new LicenseVerificationException("License token has expired.");
            }

        } catch (JWTVerificationException e) {
            throw new LicenseVerificationException("Token verification failed: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof LicenseVerificationException) {
                throw e;
            }
            throw new LicenseVerificationException("Error verifying token: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if a token is valid (not expired).
     *
     * @param token The JWT token
     * @return true if token is valid, false otherwise
     */
    private static boolean isTokenValid(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            long validUntil = jwt.getClaim("valid_until").asLong();
            long now = System.currentTimeMillis() / 1000;
            return validUntil > now;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets the ECDSA algorithm for JWT verification.
     *
     * @return The ECDSA algorithm
     * @throws LicenseVerificationException if public key cannot be loaded
     */
    private static Algorithm getAlgorithm() throws LicenseVerificationException {
        try {
            ECPublicKey publicKey = getPublicKey();
            return Algorithm.ECDSA256(publicKey);
        } catch (Exception e) {
            throw new LicenseVerificationException("Failed to load public key: " + e.getMessage(), e);
        }
    }

    /**
     * Loads and caches the public key from the JAR resource file.
     *
     * @return The ECPublicKey
     * @throws LicenseVerificationException if key cannot be loaded or parsed
     */
    private static ECPublicKey getPublicKey() throws LicenseVerificationException {
        // Return cached key if already loaded
        if (cachedPublicKey != null) {
            return cachedPublicKey;
        }

        try {
            // Load public key from resource file
            String publicKeyPem = loadPublicKeyFromResource();

            // Remove PEM headers and whitespace
            String publicKeyBase64 = publicKeyPem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            // Decode base64
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);

            // Parse as X509 encoded key
            X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");

            // Cache the public key
            cachedPublicKey = (ECPublicKey) keyFactory.generatePublic(spec);

            return cachedPublicKey;

        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException e) {
            throw new LicenseVerificationException("Failed to parse public key: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new LicenseVerificationException("Failed to load public key from resource: " + e.getMessage(), e);
        }
    }

    /**
     * Loads the public key from the JAR resource file.
     *
     * @return The public key in PEM format
     * @throws LicenseVerificationException if resource file cannot be found or read
     */
    private static String loadPublicKeyFromResource() throws LicenseVerificationException {
        try (InputStream inputStream = LicenseVerifier.class.getResourceAsStream(PUBLIC_KEY_RESOURCE_PATH)) {
            if (inputStream == null) {
                throw new LicenseVerificationException(
                        "Public key resource not found: " + PUBLIC_KEY_RESOURCE_PATH +
                                ". Make sure the public key file is included in the JAR at license/public_key.pem");
            }

            // Read the resource file
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }

        } catch (IOException e) {
            throw new LicenseVerificationException(
                    "Failed to read public key resource: " + PUBLIC_KEY_RESOURCE_PATH +
                            ". Error: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Clears the cached token and deletes the .license file (forces next
     * verification to be online).
     *
     * @param dataFolder The plugin's data folder
     */
    public static void clearCache(File dataFolder) {
        cachedToken = null;
        tokenExpiresAt = 0;

        if (dataFolder != null) {
            File licenseFile = new File(dataFolder, LICENSE_FILE_NAME);
            if (licenseFile.exists()) {
                licenseFile.delete();
            }
        }
    }

    public static void dumpState() {
        System.out.println("LicenseVerifier State Dump:");
        System.out.println("LICENSE: " + (LICENSE != null ? LICENSE : "<null>"));
        System.out.println("USER_ID: " + (USER_ID != null ? USER_ID : "<null>"));
        System.out.println("USERNAME: " + (USERNAME != null ? USERNAME : "<null>"));
        System.out.println("PRODUCT_KEY: " + (PRODUCT_KEY != null ? PRODUCT_KEY : "<null>"));
        System.out.println("Cached Token: " + (cachedToken != null ? cachedToken : "<null>"));
        System.out.println("Token Expires At: " + tokenExpiresAt);
    }

    /**
     * Gets the cached token (for debugging).
     *
     * @return The cached token, or null if not cached
     */
    public static String getCachedToken() {
        return cachedToken;
    }

    /**
     * Gets the token expiration time (for debugging).
     *
     * @return The expiration time in Unix timestamp, or 0 if not cached
     */
    public static long getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    /**
     * Exception thrown when license verification fails.
     */
    public static class LicenseVerificationException extends IllegalStateException {
        public LicenseVerificationException(String message) {
            super(message);
        }

        public LicenseVerificationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
