package Utils;

import org.dreambot.api.utilities.Logger;

public class CustomLogger {
    private static String lastLog = "";

    public static void log(String message) {
        lastLog = message;
        Logger.log(message);
    }

    public static String getLastLog() {
        return lastLog;
    }
}