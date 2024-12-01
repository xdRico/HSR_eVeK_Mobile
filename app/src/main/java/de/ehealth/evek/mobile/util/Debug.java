package de.ehealth.evek.mobile.util;

/**
 * Class to be used for Debugging Information
 *
 * @author ricow
 *
 */
public class Debug {

    private static boolean isDebug = false;

    /**
     * sets the Debug- and Log-mode by the given arguments
     *
     * @param isDebug - sets the Debug Mode
     *
     */
    public static void setDebugMode(boolean isDebug) {

        Debug.isDebug = isDebug;
    }

    /**
     * Private Method to get, if the program is currently in Debug-mode
     *
     * @return boolean isDebugMode
     */
    public static boolean isDebug() {

        return isDebug;
    }

    /**
     * Method to send a debug message; Gets printed by debugMessageFinal
     *
     * @param message - the message to be printed
     */
    public static void sendMessage(String message) {

        sendMessage("[D]" + Log.logPrefix(), message);
    }

    /**
     * Method to send a debug message, also for Log-messages; Gets printed by
     * debugMessageFinal
     *
     * @param message - the message to be printed
     * @param isLog   - boolean, if the message is a Log-message
     */
    public static void sendMessage(String message, boolean isLog) {

        if (isLog)
            sendMessage("[L]" + Log.logPrefix(), message);
        else
            sendMessage(message);
    }

    /**
     * Private Method to finally print a message to the console
     *
     * @param message - the message to be printed
     */
    private static void sendMessage(String prefix, String message) {

        if (isDebug())
            System.out.println(prefix + message + "\n");
    }

    /**
     * Method to send a debug message; Only for Exceptions
     *
     * @param e - the Exception to be printed
     */
    public static void sendException(Throwable e) {

        if (isDebug()) {
            System.err.println("[E]" + Log.logPrefix() + e.getCause() + " in " + e.getClass() + ": "
                    + e.getLocalizedMessage() + "\n");
            for (StackTraceElement el : e.getStackTrace())
                System.err.println(el.toString());

        }
    }
}
