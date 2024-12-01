package de.ehealth.evek.mobile.util;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;

/**
 * Class to be used to Log information to a file
 *
 * @author ricow
 *
 */
public class Log {

    private static String folder;
    private static PrintWriter file;

    private static boolean writeToFile = false;

    /**
     * Method to initialize the Logger; creates a file at the given path, if
     * writing´to files is active; File named by current time
     */
    public static void initLogging() {
        if (!writeToFile)
            return;
        try {
            File f = new File(folder + getTimeStamp().replace(" ", "_") + ".txt");
            new File(folder).mkdirs();
            f.createNewFile();

            file = new PrintWriter(new FileWriter(f));

            file.println(getTimeStamp() + "\n");
            sendMessage("Logging enabled!");

        } catch (Exception e) {
            Debug.sendException(e);
        }
    }

    /**
     * Method to Log a message; Gets written in Logfile and printed as Debug-message
     *
     * @param message - the message to print as Log
     */
    public static void sendMessage(String message) {

        sendMessage(message, null);
    }

    /**
     * Private Method to finally print the Log to the Logfile and the console
     *
     * @param message - the message to be print as Log
     * @param e       - the Exception to be print
     */
    private static void sendMessage(String message, Throwable e) {

        if (writeToFile && file == null)
            initLogging();

        try {
            if (writeToFile) {
                if (file == null)
                    return;
                if (message == null && e == null) {
                    file.println();
                    file.flush();
                    return;
                } else if (message != null)
                    file.println(logPrefix() + message);
                else {
                    file.println(
                            "\n" + logPrefix() + e.getCause() + " in " + e.getClass() + ": " + e.getLocalizedMessage());
                    for (StackTraceElement el : e.getStackTrace())
                        file.println(el.toString());
                }
                file.flush();
            }
            if (e != null)
                Debug.sendException(e);
            else
                Debug.sendMessage(message);

        } catch (Exception ex) {
            Debug.sendException(ex);
        }
    }

    /**
     * Method to Log a empty message; Gets written in Logfile
     */
    public static void sendMessage() {

        sendMessage(null, null);
    }

    /**
     * Method to Log a message; Only for Exceptions
     *
     * @param e - the Exception to be printed
     */
    public static void sendException(Throwable e) {
        sendMessage(null, e);
    }

    /**
     * Method to set the Logs being written to a file
     *
     * @param setWriteToFile - boolean to set, if logs are written to files
     */
    public static void setWriteToFile(boolean setWriteToFile) {

        writeToFile = setWriteToFile;

        if (writeToFile && folder != null)
            initLogging();
    }

    /**
     * Method to set the Path the Logfiles are written to
     *
     * @param path - the path, the Logfiles are written to
     */
    public static void setPath(String path) {

        folder = path;

        if (writeToFile && folder != null)
            initLogging();
    }

    /**
     * Method to get the current Timestamp Format: "Log YYYY.MM.DD hh.mm.ss"
     *
     * @return String - the current formatted Timestamp
     */
    private static String getTimeStamp() {

        LocalDateTime time = LocalDateTime.now();

        return "Log " + time.getYear() + "." + time.getMonthValue() + "." + time.getDayOfMonth() + " " + time.getHour()
                + "." + time.getMinute() + "." + time.getSecond();
    }

    /**
     * Method to get the Prefix of a Log line, including: Current Threadname Current
     * Threadid
     *
     * @return String - the formatted Log Prefix
     */
    public static String logPrefix() {

        LocalDateTime time = LocalDateTime.now();

        return "[" + time + "][" + Thread.currentThread().getName() + " #" + Thread.currentThread().getId() + "] ";
    }
}
