import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Utility class for logging messages.
 */
public class Logger {
    private static final String LOG_FILE_PATH = "log/log.txt";

    /**
     * Logs a message to a log file.
     *
     * @param message The message to be logged.
     */
    public static void log(String message) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE_PATH, true))) {
            writer.write(message + "\n");
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }
    }
}
