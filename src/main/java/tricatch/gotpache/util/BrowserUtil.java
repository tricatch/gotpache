package tricatch.gotpache.util;

import java.io.IOException;

/**
 * Browser execution utility for cross-platform support
 * Supports Windows, macOS, and Linux operating systems
 */
public class BrowserUtil {

    /**
     * Opens a URL in the default browser
     * 
     * @param url The URL to open
     * @throws IOException if the browser cannot be launched
     */
    public static void openUrl(String url) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder processBuilder;

        if (os.contains("win")) {
            processBuilder = createWindowsProcessBuilder(url);
        } else if (os.contains("mac")) {
            processBuilder = createMacProcessBuilder(url);
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            processBuilder = createLinuxProcessBuilder(url);
        } else {
            throw new IOException("Unsupported operating system: " + os);
        }

        processBuilder.start();
    }

    /**
     * Creates a ProcessBuilder for Windows systems
     */
    private static ProcessBuilder createWindowsProcessBuilder(String url) {
        return new ProcessBuilder("cmd", "/c", "start", url);
    }

    /**
     * Creates a ProcessBuilder for macOS systems
     */
    private static ProcessBuilder createMacProcessBuilder(String url) {
        return new ProcessBuilder("open", url);
    }

    /**
     * Creates a ProcessBuilder for Linux systems
     */
    private static ProcessBuilder createLinuxProcessBuilder(String url) {
        return new ProcessBuilder("xdg-open", url);
    }
}
