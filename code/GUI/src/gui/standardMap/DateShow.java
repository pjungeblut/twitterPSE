package gui.standardMap;


import gui.GUIController;
import gui.Labels;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import unfolding.MyDataEntry;
import unfolding.MyUnfoldingMap;
import mysql.result.TweetsAndRetweets;

/**
 * Implements a "DiaShow" of the data
 * @author Matthias
 *
 */
public class DateShow extends Thread {
    
    private GUIController superController;
    private LocalDate startDate;
    private LocalDate endDate;
    private HashMap<String, MyDataEntry> calculatedData;
    private MyUnfoldingMap map;
    private TweetsAndRetweets uneditedData;
    private final int DELAY_PER_DAY = 2000;
    
    /**
     * instantiate a new DateShow, every DateShow has to be a new object.
     * Calling a method on the same object twice returns the same results. 
     * @param superController instance of the GUIController
     * @param startDate start date of the 'show'
     * @param endDate end date of the 'show'
     */
    public DateShow(GUIController superController, LocalDate startDate, LocalDate endDate) {
        this.superController = superController;
        map = MyUnfoldingMap.getInstance(superController);
        this.startDate = startDate;
        this.endDate = endDate;
        uneditedData = superController.getDataByLocationAndDate();
        
 
    }
    
    /**
     * collect all retweets in the interval startDate to date
     * @param date end date of the current intervall
     * @return relevant data or null if  input is invalid
     */
    private HashMap<String, Integer> collectDataPerDate(LocalDate date) {
        
        if (uneditedData == null) {
            return null;
        }
        
        HashMap<String, Integer> forCalc = new HashMap<String, Integer>();
        
        // look for data in the right interval
        for (mysql.result.Retweets r : uneditedData.getRetweets()) {
            
            // convert date
            LocalDate test = Dates.buildLocalDate(r.getDate());
           
            
            // Check if Tweet/Retweet-objct Date is in the needed interval
            if (Dates.inRange(startDate, date, test)) {

                // Check if counter for location is already in the hashMap
                if (forCalc.containsKey(r.getLocationCode())) {
                    int count = forCalc.get(r.getLocationCode());
                    count += r.getCounter();
                    forCalc.put(r.getLocationCode(), count);
                } else {
                    int counter = r.getCounter();
                    String id = r.getLocationCode();
                    forCalc.put(id, counter);
                }
            }
        }
        return forCalc;
    }
    
    /**
     * starts the show
     */
    @Override 
    public void run() {
        List<LocalDate> list = new ArrayList<LocalDate>();
        LocalDate currentDate = startDate.plusDays(0);
        
        while(!isInterrupted() && (currentDate.isBefore(endDate) || currentDate.isEqual(endDate))) {
            // stops if end of period is reached or thread is interrupted
            HashMap<String, Integer> unCalcData = collectDataPerDate(currentDate);
            
            calculatedData = superController.getDisplayValuePerCountry(unCalcData,1);
            if (calculatedData != null) {
                map.update(calculatedData);
                map.redraw();
            }
            // show the displayed day
            superController.setInfo(Labels.SHOW_PERIOD + startDate + " to " + currentDate);
            try {
                Thread.sleep(DELAY_PER_DAY);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                superController.setInfo("", Labels.SHOW_PERIOD + startDate + " to " + currentDate);
                return;
            }
            
            // delete message
            superController.setInfo("", "Map currently shows the period from " + startDate + " to " + currentDate);
            
            calculatedData = null;
            
            currentDate = currentDate.plusDays(1);
            
        }
        
        
    }
    

}
