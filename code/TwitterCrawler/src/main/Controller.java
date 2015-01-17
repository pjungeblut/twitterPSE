package main;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import mysql.AccessData;
import mysql.DBcrawler;
import twitter4j.Status;

/**
 * controller to control the other threads and start dynamically more
 * worker-threads, restarts also the crawler if it has been shut down
 * 
 * @author Holger Ebhart
 * @version 1.0
 * 
 */
public class Controller extends Thread {

    // max. size of the buffer between streamListener and statusProcessors
    private final static int MAX_SIZE = 100000;
    // interval to wait in seconds
    private final static int INTERVAL = 30;

    private final int threadNum;
    private final int runtime;
    private AccessData accessData;

    private ConcurrentLinkedQueue<Status> statusQueue;
    private boolean run = true;
    private Logger logger;
    private StreamListener streamListener;
    private Thread thrdStreamListener;
    private Thread[] thrdStatusProcessor;
    private AccountUpdate accountUpdate;
    private Thread thrdAccountUpdate;
    private StatusProcessor[] statusProcessor;
    // note: no Locator is set
    private DBcrawler dbc;
    private Date dateForDB;
    private Logger statisticLogger;

    /**
     * 
     * @param timeout
     *            the timeout in seconds (0 for infinity) as Integer
     * @param accessData
     *            the access data to the database as AccessData
     * @throws IOException
     *             if an error with the LogFile.log has occurred
     */
    public Controller(int timeout, AccessData accessData, int numberOfThreads)
            throws IOException {
        threadNum = numberOfThreads;
        thrdStatusProcessor = new Thread[threadNum];
        statusProcessor = new StatusProcessor[threadNum];

        statisticLogger = getStatisticLogger();

        logger = getLogger();
        logger.info("Controller started");
        this.accessData = accessData;
        runtime = timeout;
        dateForDB = new Date();
        // add 800 days
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(dateForDB);
        cal.add(Calendar.DATE, 800);
        dateForDB = cal.getTime();

        statusQueue = new ConcurrentLinkedQueue<Status>();
    }

    @Override
    public void run() {
        // create thread to pull status from twitter:
        streamListener = new StreamListener(statusQueue, logger);
        thrdStreamListener = new Thread(streamListener);
        thrdStreamListener.start();
        logger.info("Crawler started");

        ConcurrentHashMap<Long, Object> hashSet = new ConcurrentHashMap<Long, Object>();
        try {
            accountUpdate = new AccountUpdate(logger, hashSet, accessData);
        } catch (SQLException e) {
            logger.severe("AccountUpdate not started - running without AccountUpdate:\n"
                    + e.getMessage());
        }
        thrdAccountUpdate = new Thread(accountUpdate);
        thrdAccountUpdate.start();

        // create threads that extract informations of status and store them in
        // the db
        for (int i = 0; i < threadNum; ++i) {
            try {
                statusProcessor[i] = new StatusProcessor(statusQueue, hashSet,
                        logger, accessData);
                thrdStatusProcessor[i] = new Thread(statusProcessor[i]);
                thrdStatusProcessor[i].start();
            } catch (InstantiationException e) {
                statusProcessor[i] = null;
                thrdStatusProcessor[i] = null;
                logger.warning("Couldn't start a statusProcessor: "
                        + e.getMessage());
            }
        }
        logger.info("StatusProcessors started");

        // runtime always >= 0
        if (runtime > 0) {
            new java.util.Timer().schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    shutdown(false);
                    System.exit(0);
                }
            }, runtime * 1000);
        }

        try {
            this.dbc = new DBcrawler(accessData, null, logger);
        } catch (InstantiationException | IllegalAccessException
                | ClassNotFoundException | SQLException e) {
            logger.warning("No dates will be insert into the database because of: "
                    + e.getMessage());
        }
        limitQueue();

    }

    /**
     * Shut down server
     */
    public void shutdown(boolean kill) {

        boolean success = true;

        // send stop message
        run = false;
        streamListener.exit();
        // exit and interrupt AccountUpdate
        accountUpdate.exit();
        thrdAccountUpdate.interrupt();
        for (int i = 0; i < threadNum; i++) {
            if (statusProcessor[i] != null) {
                statusProcessor[i].run = false;
            }
            if (thrdStatusProcessor[i] != null) {
                thrdStatusProcessor[i].interrupt();
            }
        }

        // join/stop Controller
        this.interrupt();

        // join crawler
        System.out.print("\n Terminating crawler..");
        try {
            thrdStreamListener.join();
        } catch (InterruptedException e) {
            System.out.print(". Error (" + e.getMessage() + ").");
            logger.warning(e.getMessage());
            success = false;
        }
        if (success) {
            System.out.println(". done.");
        }

        // join accountUpdate
        success = true;
        System.out.print(" Terminating accountUpdate..");
        try {
            thrdAccountUpdate.join();
        } catch (InterruptedException e) {
            System.out.print(". Error (" + e.getMessage() + ").");
            logger.warning(e.getMessage());
            success = false;
        }
        if (success) {
            System.out.println(". done.");
        }

        success = true;
        System.out.print(" Terminating status processors..");

        if (kill) {
            statusQueue.clear();
        } else {
            // waiting for empty queue
            while (getQueueSize() > 0) {
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    System.out.print(". Error (" + e.getMessage() + ").");
                    logger.warning(e.getMessage());
                    success = false;
                } catch (IllegalArgumentException e) {
                    System.out.print(". Error (" + e.getMessage() + ").");
                    logger.warning(e.getMessage());
                    success = false;
                }
            }
        }

        // join status processors
        if (success) {
            for (int i = 0; i < threadNum; ++i) {
                try {
                    if (thrdStatusProcessor[i] != null) {
                        thrdStatusProcessor[i].join();
                    }
                } catch (InterruptedException e) {
                    System.out.print(". Error (" + e.getMessage() + ").");
                    logger.warning(e.getMessage());
                    success = false;
                    break;
                }
            }
            if (success) {
                System.out.println(". done.");
            }
        }

        logger.info("Program terminated by user");
    }

    private Logger getLogger() throws SecurityException, IOException {
        Logger l = Logger.getLogger("logger");
        new File("LogFile.log").createNewFile();
        FileHandler fh = new FileHandler("LogFile.log", true);
        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);
        l.addHandler(fh);
        // true: print output on console and into file
        // false: only store output in logFile
        l.setUseParentHandlers(false);
        return l;
    }

    public int getQueueSize() {
        return statusQueue.size();
    }

    @Override
    public String toString() {
        int threadsAlive = 0;
        for (int i = 0; i < thrdStatusProcessor.length; i++) {
            if (thrdStatusProcessor[i] != null
                    && thrdStatusProcessor[i].isAlive()) {
                threadsAlive++;
            }
        }
        return " STATE OF THE CRAWLER: " + "\n"
                + " Number of status-objects in queue: " + getQueueSize()
                + "\n" + " Status of the Streamlistener: "
                + streamListener.toString() + "\n"
                + " Status of the Accountupdater: "
                + (thrdAccountUpdate.isAlive() ? "running" : "crashed") + "\n"
                + " Number of running workers: " + threadsAlive + "/"
                + threadNum;
    }

    private void limitQueue() {
        int m = 0;
        int count = 0;
        while (run) {

            if (m >= 2) {
                m = 0;
                writeStatistic();
            }
            m += INTERVAL;

            // one reconnect to twitter per day
            if (count >= 86400) {// one day = 86400 seconds
                count = 0;

                // add a new date to the database
                try {
                    dbc.connect();
                    GregorianCalendar cal = new GregorianCalendar();
                    cal.setTime(dateForDB);
                    if (dbc.addDay(dateForDB)) {
                        cal.add(Calendar.DATE, 1);
                        dateForDB = cal.getTime();
                    }
                    dbc.disconnect();
                } catch (SQLException e) {
                    logger.info("A date couldn't be insert into the database: "
                            + e.getMessage());
                }
            }

            if (statusQueue.size() > MAX_SIZE) {

                for (int i = 0; i < MAX_SIZE / 2; i++) {
                    statusQueue.poll();
                }
            }

            try {
                Thread.sleep(INTERVAL * 1000); // wait for INTERVAL seconds
            } catch (InterruptedException e) {
                logger.info("Controller has been interrupted: \n"
                        + e.getMessage());
            }
            count += INTERVAL;
        }
    }

    private void writeStatistic() {

        String msg = "";
        msg += "Summe aller empfangener Status-Objekte: "
                + streamListener.getCounter() + "\n";
        int[] sum = new int[11];
        for (int j = 0; j < sum.length; j++) {
            sum[j] = 0;
        }
        for (StatusProcessor sp : statusProcessor) {
            int[] temp = sp.getCounter();
            for (int i = 0; i < sum.length; i++) {
                sum[i] += temp[i];
            }
        }
        msg += "Summe aller interessanten Status-Objekte: " + sum[10] + "\n";
        msg += "LOKALISIERUNG\n";
        msg += "Summe Anfragen: " + sum[0] + "\n";
        msg += "Erfolgreicher Locationen: " + sum[1] + "\n";
        msg += "davon �ber place lokalisiert: " + sum[2] + "\n";
        msg += "davon �ber geotag lokalisiert: " + sum[3] + "\n";
        msg += "davon �ber String und Zeitzone lokalisiert: " + sum[4] + "\n";
        msg += "�ber Webservice lokalisiert: " + sum[5] + "\n";
        msg += "�ber HashMap lokalisiert" + sum[6] + "\n";
        msg += "Summe vorhandener Places: " + sum[7] + "\n";
        msg += "Summe vorhandener Geotags: " + sum[8] + "\n";
        msg += "Summe vorhandener location-Information: " + sum[9] + "\n";
        
        statisticLogger.info(msg);
    }

    private Logger getStatisticLogger() {
        Logger l = Logger.getLogger("statlog");
        try {
            new File("Statistic.log").createNewFile();
            FileHandler fh = new FileHandler("Statistic.log", true);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
            l.addHandler(fh);
        } catch (IOException e) {
            logger.severe("Couldn't instantiate statistic-logger: "
                    + e.getMessage());
        }

        l.setUseParentHandlers(false);

        return l;
    }

}
