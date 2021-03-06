package myUnfolding;

/**
 * 
 */



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import myUnfolding.MyDataEntry;

import processing.core.PApplet;
import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.data.Feature;
import de.fhpotsdam.unfolding.data.GeoJSONReader;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.utils.MapUtils;

/**
 * Class of a unfolding map that is used to visualize Tweets und Retweets spread 
 * @author Lidia
 * 
 */
public class MyUnfoldingMap extends PApplet {

    /**
     * default serial version uid
     */
    private static final long serialVersionUID = 1L;
    private UnfoldingMap map1;
    //private UnfoldingMap map2;
    private UnfoldingMap currentMap;
    private HashMap<String, MyDataEntry> dataEntriesMap;
    private List<Marker> countryMarker;
    
    /**
     * List of countryIds which are colored in the map
     */
    private List<String> setValues;
    
    /**
     * HashMap to match between 2 char and 3 char country id
     * key --> 3 char
     * value --> 2 char
     */
    private HashMap<String, String> countryIdTrans;
    
    /**
     * Maximal value of the displayed entries
     */
    private float maxValue = 0;
    
    public MyUnfoldingMap() {
    	super();
    	this.setSize(900, 600);
	}

    @Override
	public void setup() {  //check size of map
        size(900, 600);
        smooth();
        
        map1 = new UnfoldingMap(this, P2D);
        //map2 = new UnfoldingMap(this, new Google.GoogleMapProvider());
        
        currentMap = map1;

        currentMap.zoomLevel(1);
        currentMap.setZoomRange(2, 4);
        currentMap.setBackgroundColor(140);
        MapUtils.createDefaultEventDispatcher(this, currentMap);
        
      //Load country polygons
        List<Feature> countries = GeoJSONReader.loadData(this,"countries.geo.json");
        countryMarker = MapUtils.createSimpleMarkers(countries);
        currentMap.addMarkers(countryMarker);
        resetMarkers();
        
        dataEntriesMap = loadCountriesFromCSV("countries.csv");
        setValues = new ArrayList<String>();
        
        /*
        //TEST
        HashMap<String, Double> test = new HashMap<String, Double>();
        test.put("AR", 7.45974);
        test.put("US", 19.395875);
        test.put("DE", 4.3595);
        update(test);
        */

    }
    
    public UnfoldingMap getMap() {
    	return currentMap;
    }
    
    @Override
	public void draw() {
        //switchProvider();
        currentMap.draw();
    }

    /**
     * Shades countrys dependent on their relative frequency of tweets
     */
    public void shadeCountries() {
        for (Marker m : countryMarker) {
            String countryId = m.getId();
            String id = countryIdTrans.get(countryId);

            MyDataEntry dataEntry = dataEntriesMap.get(id);
            
            //Strokes
            m.setStrokeColor(color(241, 241, 241, 50));
            m.setStrokeWeight(1);
            
            if (dataEntry != null && dataEntry.getValue() != -1) {
                //Take value as brightness
                Double transparency = dataEntry.getValue();
                float transpa = Float.parseFloat(transparency.toString());
                float t = map(transpa, 0, maxValue, 50, 255);
                
                m.setColor(color(38,192,38, t));
   
            } else {
                // value doesn't exist
                m.setColor(color(173,173,173,50));
            }

        }
    }


    /**
     * Updates new values to be visualized on the map.
     * 
     * @param changedEntries
     *            String array containing country id and new value of it
     */
    public void update(HashMap<String, Double> changedEntries) {

        if (!setValues.isEmpty()) {
            for (String id : setValues) {
                
                MyDataEntry edit = dataEntriesMap.get(id);
                
                edit.setValue(-1);
                dataEntriesMap.put(id, edit);
            }
            setValues.clear();
        }

        for(Entry<String, Double> e: changedEntries.entrySet()) {
            
            MyDataEntry newEntry = dataEntriesMap.get(e.getKey());
            if(newEntry != null) {
                newEntry.setValue(e.getValue());
                dataEntriesMap.put(e.getKey(), newEntry);
                setValues.add(e.getKey());
            }

            if(Float.parseFloat(e.getValue().toString()) > maxValue) {
                maxValue = Float.parseFloat(e.getValue().toString());
            }
        }
        shadeCountries();
    }    
//    /**
//     * Switches provider of the map
//     * By pressing '1' an '2'
//     */
//    public void switchProvider() {
//        if(key == '1') {
//            currentMap = map1;
//        }
//        else if (key == '2') {
//            currentMap = map2;
//        }
//    }
    
    
/**
 * Loads 2 and 3 char countryIds in HashMap for further use
 * @param file countryIds as .csv
 * @return HashMap containing countryIds
 */
    private HashMap<String, MyDataEntry> loadCountriesFromCSV(String file) {
        dataEntriesMap = new HashMap<String, MyDataEntry>();
        countryIdTrans = new HashMap<String, String>();
        
        String[] rows = loadStrings(file);
        for (String row : rows) {
            // Reads country name and countryID from CSV row
            String[] column = row.split(";");
            if (column.length >= 3) {
                MyDataEntry dataEntry = new MyDataEntry();
                dataEntry.setCountryName(column[0]);
                dataEntry.setCountryId3Chars(column[1]);
                dataEntry.setCountryId2Chars(column[2]);
                dataEntriesMap.put(column[2], dataEntry);
                
                countryIdTrans.put(column[1], column[2]);
            }
        }

        return dataEntriesMap;
    }

    /**
     * Resets all colored markers
     */
    public void resetMarkers() {
        for(Marker m : countryMarker) {
            m.setColor(color(173,173,173,50));
            m.setStrokeColor(color(241, 241, 241, 50));
            m.setStrokeWeight(1);
        }
    }
    

}
