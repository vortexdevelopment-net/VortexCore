package net.vortexdevelopment.vortexcore;

/**
 * Thrown from {@link VortexPlugin#verifyLicense()} when the plugin must stop.
 *
 * <p>Consumer cores and plugins own their own trial/player-limit policy. VortexCore
 * does not interpret this exception as a soft trial; it disables the plugin.</p>
 */
public class PluginVerificationException extends Exception {
    public PluginVerificationException(String message) {
        super(message);
    }

    public PluginVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
