package util;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * 
 * util class to handle loggers
 * 
 * @author Holger Ebhart
 * @version 1.0
 * 
 */
public class LoggerUtil {

    /**
     * returns an instance of a logger that writes logs in the file LogFile.log
     * 
     * @param filename
     *            the filename of the logfile as String without the ending .log
     * @return an instance of a logger that writes logs in the file LogFile.log
     *         as Logger
     * @throws SecurityException
     *             thrown if the access-permissions weren't sufficient
     * @throws IOException
     *             thrown if an error with the filesystems occurs
     */
    public static Logger getLogger(String filename) throws SecurityException,
            IOException {
        // get logger instance
        Logger l = Logger.getLogger(filename);
        // get/create logfile
        new File(filename + ".log").createNewFile();
        FileHandler fh = new FileHandler(filename + ".log", true);
        SimpleFormatter formatter = new SimpleFormatter();
        // set format of the logs
        fh.setFormatter(formatter);
        // set logfile
        l.addHandler(fh);
        // true: print output on console and into file
        // false: only store output in logFile
        l.setUseParentHandlers(false);
        return l;
    }

    /**
     * returns an instance of a logger that writes logs in the file LogFile.log
     * 
     * @return an instance of a logger that writes logs in the file LogFile.log
     *         as Logger
     * @throws SecurityException
     *             thrown if the access-permissions weren't sufficient
     * @throws IOException
     *             thrown if an error with the filesystems occurs
     */
    public static Logger getLogger() throws SecurityException, IOException {
        return getLogger("LogFile");
    }

}
