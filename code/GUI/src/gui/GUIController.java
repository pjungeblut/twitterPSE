package gui;

import gui.GUIElement;
import gui.GUIElement.UpdateType;
import gui.InfoRunnable;
import gui.Labels;
import gui.SelectionHashList;

import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import mysql.AccessData;
import mysql.DBgui;
import mysql.result.Account;
import mysql.result.Category;
import mysql.result.Location;
import mysql.result.TweetsAndRetweets;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import twitter4j.User;
import unfolding.MyDataEntry;
import util.LoggerUtil;

/**
 * Supercontroller which provides information from db and informs subcontroller
 * on data changes.
 * 
 * @author Maximilian Awiszus and Paul Jungeblut
 * 
 */

public class GUIController extends Application implements Initializable {
	
	private static final int WIDTH = 900;
    private static final int HEIGHT = 600;
    private static final int MIN_WIDTH = 860;
    private static final int MIN_HEIGHT = 650;
    
    private static GUIController instance = null;

    private Category categoryRoot;

    private DBgui db, db2, db3;
    private boolean dontLoad = false;
    private Logger log;
    private Stage stage;
    private AtomicInteger upToDate = new AtomicInteger();



    @FXML
    private Pane paSelectionOfQuery;
    @FXML
    private TextField txtSearch;
    @FXML
    private ListView<String> lstInfo;

    private ArrayList<GUIElement> guiElements = new ArrayList<GUIElement>();

    private SelectionHashList<Location> locations = new SelectionHashList<Location>();
    private SelectionHashList<Account> accounts = new SelectionHashList<Account>();
    private List<Account> dataByAccount = new ArrayList<Account>();
    private TweetsAndRetweets dataByLocationAndDate = new TweetsAndRetweets();

    private HashSet<Integer> selectedCategories = new HashSet<Integer>();
    private HashMap<Integer, Category> categories = new HashMap<Integer, Category>();
    private Thread reloadDataThread = new Thread();

    private ExecutorService listLoaderPool = Executors.newCachedThreadPool();
    private ExecutorService reloadDataPool = Executors.newCachedThreadPool();
    private ExecutorService threadPool = Executors.newCachedThreadPool();

    private String accountSearchText = "";
    private MyDataEntry mapDetailInformation = null;

    private HashMap<Date, HashMap<String, Integer>> totalNumberOfRetweets = null;
    private List<Location> allLocationsInDB = null;

    private Runnable rnbInitDBConnection = new Runnable() {
        @Override
        public void run() {
            boolean success = true;
            String info = Labels.DB_CONNECTING;
            setInfo(info);
            try {
                log = LoggerUtil.getLogger();
            } catch (SecurityException | IOException e1) {
                e1.printStackTrace();
                success = false;
            }
            AccessData accessData = new AccessData("172.22.214.133", "3306",
                    "twitter", "gui", "272b28");
            if (success) {
                try {
                    db = new DBgui(accessData, log);
                    db2 = new DBgui(accessData, log);
                    db3 = new DBgui(accessData, log);
                } catch (SecurityException | InstantiationException
                        | IllegalAccessException | ClassNotFoundException e) {
                    e.printStackTrace();
                    success = false;
                }
                if (success) {
                    try {
                        db.connect();
                        db2.connect();
                        db3.connect();
                    } catch (SQLException e) {
                        success = false;
                    }
                } else {
                    setInfo(Labels.DB_CONNECTING_ERROR, info);
                }
            } else {
                setInfo(Labels.NO_LOGIN_DATA_FOUND_ERROR, info);
            }
            if (success) {
                setInfo(Labels.DB_CONNECTED, info);
                reloadAll();
            }
        }
    };

    private Runnable rnbReloadData = new Runnable() {
        @Override
        public void run() {
            Integer current = upToDate.incrementAndGet();
            List<Integer> selectedLocations = new ArrayList<Integer>();
            for (Location l : locations.getSelected()) {
                selectedLocations.add(l.getId());
            }
            List<Integer> selectedAccounts = new ArrayList<Integer>();
            for (Account a : accounts.getSelected()) {
                selectedAccounts.add(a.getId());
            }
            List<Integer> allSelectedCategories = new ArrayList<Integer>();
            for (Integer id : selectedCategories) {
                allSelectedCategories.add(id);
            }
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    lstInfo.getItems()
                            .removeAll(Labels.DATA_BY_ACCOUNT_LOADING);
                    lstInfo.getItems().removeAll(
                            Labels.DATA_BY_LOCATION_LOADING);
                }
            });
            reloadDataPool.shutdownNow();
            db2.interruptGetAllDataQuery();
            db3.interruptGetSumOfDataQuery();
            if (allSelectedCategories.size() + selectedLocations.size()
                    + selectedAccounts.size() >= 1) {
                reloadDataPool = Executors.newCachedThreadPool();
                try {
                    reloadDataPool
                            .submit(new PPPPRunnable<List<Integer>, List<Integer>, List<Integer>, Integer>(
                                    selectedLocations, selectedAccounts,
                                    allSelectedCategories, current) {
                                @Override
                                public void run(List<Integer> l,
                                        List<Integer> a, List<Integer> c,
                                        Integer current) {
                                    String info = Labels.DATA_BY_ACCOUNT_LOADING;
                                    setInfo(info);
                                    List<Account> temp = db2.getAllData(c, l,
                                            a, false);
                                    if (current == upToDate.get()) {
                                        dataByAccount = temp;
                                        setInfo(Labels.DATA_BY_ACCOUNT_LOADED,
                                                info);
                                        update(UpdateType.TWEET_BY_ACCOUNT);
                                    }
                                }
                            });
                    reloadDataPool
                            .submit(new PPPPRunnable<List<Integer>, List<Integer>, List<Integer>, Integer>(
                                    selectedLocations, selectedAccounts,
                                    allSelectedCategories, current) {
                                @Override
                                public void run(List<Integer> l,
                                        List<Integer> a, List<Integer> c,
                                        Integer current) {
                                    String info = Labels.DATA_BY_LOCATION_LOADING;
                                    setInfo(info);
                                    TweetsAndRetweets temp = db3.getSumOfData(
                                            c, l, a, true);
                                    if (current == upToDate.get()) {
                                        dataByLocationAndDate = temp;
                                        setMapDetailInformation(new MyDataEntry());
                                        setInfo(Labels.DATA_BY_LOCATION_LOADED,
                                                info);
                                        update(UpdateType.TWEET_BY_LOCATION_BY_DATE);
                                    }
                                }
                            });
                } catch (RejectedExecutionException e) {
                    setInfo(Labels.ERROR, 3000);
                }
            } else {
                if (current == upToDate.get()) {
                    dataByAccount = new ArrayList<Account>();
                    dataByLocationAndDate = new TweetsAndRetweets();
                    setInfo(Labels.ERROR_NO_FILTER_SELECTED, 3000);
                    update(UpdateType.TWEET_BY_ACCOUNT);
                    update(UpdateType.TWEET_BY_LOCATION_BY_DATE);
                    setMapDetailInformation(new MyDataEntry());
                }
            }
        }
    };

    private Runnable rnbReloadLocation = new Runnable() {
        @Override
        public void run() {
            String info = Labels.LOCATIONS_LOADING;
            setInfo(info);
            locations.removeAll();
            List<Location> locationList = db.getLocations();
            if (locationList != null) {
                locations.updateAll(locationList);
                update(UpdateType.LOCATION);
                setInfo(Labels.LOCATIONS_LOADED, info);
            } else {
                setInfo(Labels.DB_CONNECTION_ERROR, info);
            }
        }
    };

    private Runnable rnbReloadAccounts = new Runnable() {
        @Override
        public void run() {
            reloadAccounts(true);
        }
    };

    private Runnable rnbReloadCategories = new Runnable() {
        @Override
        public void run() {
            String info = Labels.CATEGORIES_LOADING;
            setInfo(info);
            categoryRoot = db.getCategories();
            if (categoryRoot != null) {
                reloadCategoryHashMap();
                update(UpdateType.CATEGORY);
                setInfo(Labels.CATEGORIES_LOADED, info);
            } else {
                categoryRoot = new Category(0, Labels.ERROR, 0, false);
                update(UpdateType.ERROR);
                setInfo(Labels.DB_CONNECTION_ERROR, info);
            }
        }
    };

    /**
     * Create a GUIController and set the singelton instance.
     */
    public GUIController() {
        super();
        instance = this;
    }

    /**
     * Get an instance of GUIController.
     * 
     * @return instance of GUIController
     */
    public static GUIController getInstance() {
        if (instance == null) {
            System.out.println("Fehler in GUIController getInstance(). "
                    + "Application nicht gestartet. (instance == null).");
        }
        return instance;
    }

    @Override
    public void start(final Stage primaryStage) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
                "GUIView.fxml"));
        fxmlLoader.setController(this);
        Parent parent = null;
        try {
            parent = fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (parent != null) {
            Scene scene = new Scene(parent, WIDTH, HEIGHT);
            scene.getStylesheets().add(
                    getClass().getResource("application.css").toExternalForm());
            stage = primaryStage;
            stage.setTitle(Labels.PSE_TWITTER);
            stage.setMinHeight(MIN_HEIGHT);
            stage.setMinWidth(MIN_WIDTH);
            stage.setScene(scene);
            stage.show();
            scene.getWindow().setOnCloseRequest(
                    new EventHandler<WindowEvent>() {
                        @Override
                        public void handle(WindowEvent event) {
                            event.consume();
                            close();
                        }
                    });
        }
    }

    /**
     * Close the application and disconnect from db, if there has been a
     * connection.
     */
    public void close() {
        update(UpdateType.CLOSE);
        reloadDataPool.shutdownNow();
        listLoaderPool.shutdownNow();
        threadPool.shutdownNow();
        if (db != null && db.isConnected()) {
            setInfo(Labels.DB_CONNECTION_CLOSING);
            db.disconnect();
            setInfo(Labels.DB_CONNECTION_CLOSED, Labels.DB_CONNECTION_CLOSING);
        }
        Platform.exit();
    }

    /**
     * Get whether the GUIController is connected to the database.
     * 
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return db != null && db.isConnected();
    }

    /**
     * Get if the application is ready meaning all locations, categories and
     * accounts are already loaded from db.
     * 
     * @return true if data is loaded, false otherwise
     */
    public boolean isReady() {
        return !locations.get().isEmpty() && !categories.isEmpty()
                && !accounts.get().isEmpty();
    }

    /**
     * Start the application.
     * 
     * @param args
     *            this parameter is not used
     */
    public static void main(String[] args) {
        launch();
    }

    private void reloadAccounts(boolean update) {
        String info = Labels.ACCOUNTS_LOADING;
        setInfo(info);
        db.interruptGetAccountsQuery();
        accounts.removeAll();
        List<Account> accountList = db
                .getAccounts(accountSearchText == null ? "" : accountSearchText);
        if (accountList != null) {
            accounts.updateAll(accountList);
            if (update) {
                update(UpdateType.ACCOUNT);
            }
            setInfo(Labels.ACCOUNTS_LOADED, info);
        } else {
            setInfo(Labels.DB_CONNECTION_ERROR, info);
        }
    }

    private void reloadCategoryHashMap() {
        categories.clear();
        reloadCategoryHashMapRec(categoryRoot);
    }

    private void reloadCategoryHashMapRec(Category category) {
        for (Category child : category.getChilds()) {
            reloadCategoryHashMapRec(child);
        }
        categories.put(category.getId(), category);
    }

    private void reloadData() {
        reloadDataThread = new Thread(rnbReloadData);
        reloadDataThread.start();
    }

    /**
     * Reload accounts, categories and locations parallel from db.
     */
    private void reloadAll() {
        if (!listLoaderPool.isShutdown()) {
            listLoaderPool.execute(rnbReloadAccounts);
            listLoaderPool.execute(rnbReloadCategories);
            listLoaderPool.execute(rnbReloadLocation);
            listLoaderPool.execute(new Runnable() {
                @Override
                public void run() {
                    if (totalNumberOfRetweets == null) {
                        totalNumberOfRetweets = getSumOfRetweetsPerLocation();
                    }
                }
            });
        }
    }

    /**
     * Display information in information list.
     * 
     * @param info
     *            which should be displayed
     */
    public void setInfo(String info) {
        Platform.runLater(new PRunnable<String>(info) {
            @Override
            public void run(String info) {
                lstInfo.getItems().removeAll(info);
                lstInfo.getItems().add(info);
            }
        });
    }

    /**
     * Remove the old information and display the new information for some
     * seconds.
     * 
     * @param info
     *            which should be displayed
     * @param oldInfo
     *            which should be removed
     */
    public void setInfo(String info, String oldInfo) {
        Platform.runLater(new PPRunnable<String, String>(info, oldInfo) {
            @Override
            public void run(String info, String oldInfo) {
                lstInfo.getItems().remove(oldInfo);
                lstInfo.getItems().removeAll(info);
                lstInfo.getItems().add(info);
                Platform.runLater(new InfoRunnable(lstInfo, info));
            }
        });
    }

    /**
     * Show info message for a time period.
     * 
     * @param info
     *            which should be displayed
     * @param timeToShow
     *            time after the message is going to disappears
     */
    public void setInfo(String info, Integer timeToShow) {
        Platform.runLater(new PPRunnable<String, Integer>(info, timeToShow) {
            @Override
            public void run(String info, Integer timeToShow) {
                lstInfo.getItems().add(info);
                Platform.runLater(new InfoRunnable(lstInfo, info, timeToShow));
            }
        });
    }

    /**
     * Get list of all categories
     * 
     * @return list of categories
     */
    public Category getCategoryRoot() {
        return categoryRoot;
    }

    /**
     * Get categories containing text
     * 
     * @param text
     *            which categories should contain
     * @return list of categories containing text
     */
    public Category getCategoryRoot(String text) {
        HashMap<Integer, Category> categories = new HashMap<Integer, Category>();
        Stack<Category> toVisit = new Stack<Category>();
        Category newRoot = new Category(categoryRoot.getId(),
                categoryRoot.getText(), categoryRoot.getParentId(),
                categoryRoot.isUsed(), categoryRoot.getMatchedAccounts());
        HashSet<Integer> foundCategories = new HashSet<Integer>();

        categories.put(categoryRoot.getId(), categoryRoot);
        for (Category category : categoryRoot.getChilds()) {
            toVisit.push(category);
        }

        while (!toVisit.isEmpty()) {
            Category category = toVisit.pop();
            categories.put(
                    category.getId(),
                    new Category(category.getId(), category.getText(), category
                            .getParentId(), category.isUsed(), category
                            .getMatchedAccounts()));
            for (Category child : category.getChilds()) {
                toVisit.push(child);
            }
            if (category.getText().toLowerCase().trim()
                    .contains(text.toLowerCase().trim())) {
                foundCategories.add(category.getId());
            }
        }
        Iterator<Integer> iterator = foundCategories.iterator();
        while (iterator.hasNext()) {
            int categoryID = iterator.next();
            Stack<Integer> pathToRoot = new Stack<Integer>();
            pathToRoot.push(categoryID);
            while (pathToRoot.peek() != newRoot.getId()) {
                pathToRoot
                        .push(categories.get(pathToRoot.peek()).getParentId());
            }
            pathToRoot.pop(); // remove root
            Category nodeToAdd = newRoot;
            while (!pathToRoot.isEmpty()) {
                Category category = categories.get(pathToRoot.pop());
                if (!nodeToAdd.getChilds().contains(category)) {
                    nodeToAdd.addChild(category);
                }
                for (Category child : nodeToAdd.getChilds()) {
                    if (child.equals(category)) {
                        nodeToAdd = child;
                        break;
                    }
                }
            }
        }
        return newRoot;
    }

    /**
     * Creates a category tree containing all Categories given in 'categoryIds'
     * 
     * @param categoryIds
     *            ids of categories
     * @return root of the created tree, null if CategoryIds contains invalid Id
     *         or has length 0
     */
    public Category getCategoryRoot(int[] categoryIds) {

        if (categoryIds == null || categoryIds.length == 0) {
            return null;
        }
        Category root = null;
        Category currentCat = null;
        Boolean first = true;

        // contains all vertex of the tree
        HashMap<Integer, Category> tree = new HashMap<Integer, Category>();

        // iterate over CategoryIds and create tree
        for (int i = 0; i < categoryIds.length; i++) {

            currentCat = getCategory(categoryIds[i]);

            if (currentCat == null) {
                return null;
            }
            // create new category with old values but without childs
            currentCat = new Category(currentCat.getId(), currentCat.getText(),
                    currentCat.getParentId(), currentCat.isUsed(),
                    currentCat.getMatchedAccounts());

            tree.put(currentCat.getId(), currentCat);

            while (currentCat.getId() != 1) {
                // while currenCat is not root
                Category parent = getCategory(currentCat.getParentId());
                if (parent == null) {
                    return null;
                }
                // create new category with old values but without childs
                parent = new Category(parent.getId(), parent.getText(),
                        parent.getParentId(), parent.isUsed(),
                        parent.getMatchedAccounts());

                if (tree.containsKey(parent.getId())) {
                    // look up parentId in the hashMap if found add currentCat
                    // and break
                    tree.get(parent.getId()).addChild(currentCat);
                    break;
                } else {
                    // add currentCat and go on
                    parent.addChild(currentCat);
                    tree.put(parent.getId(), parent);
                    currentCat = parent;
                }
            }
            // hoping that categories form a tree, and root id is still 1 set in
            // the first run to the root the root item
            if (first) {
                root = currentCat;
                first = false;
            }

        }
        return root;
    }

    /**
     * Get accounts containing the text.
     * 
     * @param text
     *            with which should be compared incase-sensitively.
     * @return list of accounts containing text
     */
    public List<Account> getAccounts(String text) {
        if (!accountSearchText.equals(text)) {
            accountSearchText = text;
            reloadAccounts(false);
        }
        return accounts.get();
    }

    /**
     * Get list of all locations.
     * 
     * @return list of locations
     */
    public List<Location> getLocations() {
        return locations.get();
    }

    /**
     * Get locations containing text
     * 
     * @param text
     *            which locations should contain
     * @return list of locations containing text
     */
    public List<Location> getLocations(String text) {
        ArrayList<Location> filteredLocations = new ArrayList<Location>();
        for (Location location : locations.get()) {
            if (location.toString().toLowerCase().trim()
                    .contains(text.toLowerCase().trim())) {
                filteredLocations.add(location);
            }
        }
        return filteredLocations;
    }

    /**
     * Set of an account is selected.
     * 
     * @param id
     *            of account
     * @param selected
     *            is true if account should be selected, false otherwise
     */
    public synchronized void setSelectedAccount(int id, boolean selected) {
        if (accounts.setSelected(id, selected)) {
            interruptReloadData();
            update(UpdateType.ACCOUNT_SELECTION);
            reloadData();
        }
        accounts.printSelected();
    }

    private void interruptReloadData() {
        reloadDataThread.interrupt();
        reloadDataPool.shutdownNow();
        reloadDataPool = Executors.newCachedThreadPool();
    }

    /**
     * Set if all categories in the list are selected or not. Update is called
     * after all categories are (de)selected.
     * 
     * @param ids
     *            of all categories
     * @param selected
     *            true if category should be selected, false otherwise
     */
    public void setSelectedCategory(Set<Integer> ids, boolean selected) {
        synchronized (this) {
            interruptReloadData();
            for (Integer id : ids) {
                setSelectedCategoryInList(id, selected);
            }
            update(UpdateType.CATEGORY_SELECTION);
            reloadData();
        }
    }

    /**
     * Set if a category is selected.
     * 
     * @param id
     *            of category
     * @param selected
     *            is true if category should be selected, false otherwise
     */
    public void setSelectedCategory(int id, boolean selected) {
        synchronized (this) {
            interruptReloadData();
            setSelectedCategoryInList(id, selected);
            update(UpdateType.CATEGORY_SELECTION);
            reloadData();
        }
    }

    private void setSelectedCategoryInList(int id, boolean selected) {
        if (selected) {
            selectedCategories.add(id);
        } else {
            selectedCategories.remove(id);
        }
    }

    /**
     * Set if a location is selected.
     * 
     * @param id
     *            of location
     * @param selected
     *            is true if location should be selected, false otherwise
     */
    public void setSelectedLocation(int id, boolean selected) {
        synchronized (this) {
            interruptReloadData();
            if (locations.setSelected(id, selected)) {
                update(UpdateType.LOCATION_SELECTION);
                reloadData();
            }
        }
    }

    /**
     * Block reloading.
     * 
     * @param dontLoad
     *            is true if reloading should be blocked
     */
    public void setDontLoadFromDB(boolean dontLoad) {
        this.dontLoad = dontLoad;
        update(UpdateType.DONT_LOAD);
    }

    /**
     * Indicates whether reloading is blocked.
     * 
     * @return True if data reloading is blocked, false otherwise
     */
    public boolean isDontLoad() {
        return dontLoad;
    }

    /**
     * Get list of all accounts.
     * 
     * @return a list of all accounts
     */
    public List<Account> getSelectedAccounts() {
        return accounts.getSelected();
    }

    /**
     * Get list of selected categories.
     * 
     * @return selected categories
     */
    public List<Category> getSelectedCategories() {
        List<Category> selectedCategoriesList = new ArrayList<Category>();
        for (Integer categoryID : selectedCategories) {
            selectedCategoriesList.add(categories.get(categoryID));
        }
        return selectedCategoriesList;
    }

    /**
     * Get the primary stage.
     * 
     * @return primary stage
     */
    public Stage getStage() {
        return stage;
    }

    /**
     * Get list of selected locations.
     * 
     * @return selected locations
     */
    public List<Location> getSelectedLocations() {
        return locations.getSelected();
    }

    /**
     * Get data grouped by account.
     * 
     * @return data grouped by account or null
     */
    public List<Account> getDataByAccount() {
        return dataByAccount;
    }

    /**
     * Get data grouped by location and date.
     * 
     * @return data grouped by location or null
     */
    public TweetsAndRetweets getDataByLocationAndDate() {

        return dataByLocationAndDate;
    }

    /**
     * Get the account by id. Only accounts which are cached in the
     * GUIController are available meaning accounts displayed in
     * SelectionOfQuery account list.
     * 
     * @param id
     *            of account
     * @return account or null if no account is found
     */
    public Account getAccount(Integer id) {
        Account a = accounts.getElement(id);
        if (a == null) {
            a = db.getAccount(id);
        }
        return a;
    }

    /**
     * Get the category by id
     * 
     * @param id
     *            of category
     * @return category or null if no category is found
     */
    public Category getCategory(Integer id) {
        return categories.get(id);
    }

    /**
     * Get the location by id
     * 
     * @param id
     *            of location
     * @return location or null if no location is found
     */
    public Location getLocation(Integer id) {
        return locations.getElement(id);
    }

    /**
     * Adds user who's tweets the crawler will be listening.
     * 
     * @param user
     *            the twitter user
     * @param locationID
     *            of location from user
     */
    public void addUserToWatch(User user, int locationID) {
        db.addAccount(user, locationID);
    }

    /**
     * Set the detail information.
     * 
     * @param detailInfo
     *            which should be set
     */
    public void setMapDetailInformation(MyDataEntry detailInfo) {
        mapDetailInformation = detailInfo;
        update(UpdateType.MAP_DETAIL_INFORMATION);
    }

    /**
     * Get the detail information
     * 
     * @return detail information or null if not set
     */
    public MyDataEntry getMapDetailInformation() {
        return mapDetailInformation;
    }

    /**
     * Add a category to an user.
     * 
     * @param accountID
     *            of user
     * @param categoryID
     *            of category
     */
    public void setCategory(int accountID, int categoryID) {
        db.setCategory(accountID, categoryID);
    }

    /**
     * Add a location to an user.
     * 
     * @param accountID
     *            of user
     * @param locationID
     *            of location
     */
    public void setLocation(int accountID, int locationID) {
        db.setLocation(accountID, locationID);
    }

    private void update(UpdateType type) {
        for (GUIElement element : guiElements) {
            try {
                threadPool.execute(new UpdateRunnable(type, element) {
                    @Override
                    public void run() {
                        element.update(type);
                    }
                });
            } catch (RejectedExecutionException e) {
                setInfo("GUI kann nicht aktualisiert werden.");
            }
        }
    }

    /**
     * Add a GUIElement as subscriber
     * 
     * @param element
     *            which will later be notified on update.
     */
    public void subscribe(GUIElement element) {
        guiElements.add(element);
    }

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        update(UpdateType.GUI_STARTED);
        new Thread(rnbInitDBConnection).start();
    }

    /**
     * Get the sum of all retweets per location
     * 
     * @return HashMap with date as key and a HashMap with location code and the
     *         sum of retweets as integer per date as value.
     */
    private HashMap<Date, HashMap<String, Integer>> getSumOfRetweetsPerLocation() {
        return db3.getAllRetweetsPerLocation();
    }

    /**
     * calculates the displayed value per country
     * 
     * given: a category, country, accounts combination, a period of time:
     * 
     * x := number of retweets for that combination in that country in period y
     * := overall number of retweets for that country in period z := overall
     * number of retweets in all contries in that combination/period
     * 
     * output = log_10(x^2 / (y * z))
     * 
     * 
     * @param start
     *            start date of the requested period of time, if null start is
     *            interpreted an LocalDate.MIN
     * @param end
     *            end date of the requested period of time, if null end is
     *            interpreted an LocalDate.MAX
     * @param retweetsPerLocation
     *            number of retweets for each country in a
     *            category/country/accounts combination in the requested period
     *            of time
     * @return the hashmap mapping countries to the number quantifying the
     *         retweet activity in this country or null if retweetsPerLocation
     *         contained invalid countrycode identifier, or scale is not
     *         positive
     */
    public HashMap<String, MyDataEntry> getDisplayValuePerCountry(
            HashMap<String, Integer> retweetsPerLocation, LocalDate start,
            LocalDate end) {
        if (allLocationsInDB == null) {
            allLocationsInDB = getLocations();
        }

        HashMap<String, MyDataEntry> result = new HashMap<String, MyDataEntry>();
        HashMap<String, Integer> hashMapOverallNumber = getOverallNumberOfRetweetsForPeriod(
                start, end);
        if (hashMapOverallNumber == null) {
            return null;
        }

        // calculate overall number of retweets for each country in this special
        // combination
        Set<String> keySet = retweetsPerLocation.keySet();
        int overallCounter = 0;
        for (String key : keySet) {
            overallCounter += retweetsPerLocation.get(key);
        }

        // calculate relative value and iterate over all countries
        // available in the DB not just the countries in the query
        // Iterator iterator = allLocationsInDB.listIterator();
        // System.out.println(allLocationsInDB.size());
        double minValue = Double.POSITIVE_INFINITY;
        for (int i = 0; i < allLocationsInDB.size(); i++) {
            String key = ((Location) allLocationsInDB.get(i)).getLocationCode();

            // catch case if country is not set in the query
            if (!hashMapOverallNumber.containsKey(key)) {
                hashMapOverallNumber.put(key, 0);
            }
            if (!retweetsPerLocation.containsKey(key)) {
                retweetsPerLocation.put(key, 0);
            }

            double relativeValue;
            if (hashMapOverallNumber.get(key) == 0) {
                relativeValue = 0;
            } else {
                relativeValue = ((double) retweetsPerLocation.get(key) * (double) retweetsPerLocation
                        .get(key))
                        / ((double) hashMapOverallNumber.get(key) * (double) overallCounter);
            }

            if (relativeValue > 0) {
                minValue = Math.min(minValue, relativeValue);
            }

            result.put(
                    key,
                    new MyDataEntry(relativeValue, key, hashMapOverallNumber
                            .get(key), retweetsPerLocation.get(key)));
        }

        Iterator<Entry<String, MyDataEntry>> it = result.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, MyDataEntry> entry = it.next();
            entry.getValue().setValue(
                    Math.log10(entry.getValue().getValue() / minValue + 1));
        }

        return result;
    }

    /**
     * calculates the overall number of retweets per country within the
     * requested period of time
     * 
     * @param start
     *            start date, if null start is interpreted an LocalDate.MIN
     * @param end
     *            end date, if null end is interpreted an LocalDate.MAX
     * @return HashMap with countryCode as key and overall number of retweets
     *         within the period of time as value or null if input is invalid
     */
    private HashMap<String, Integer> getOverallNumberOfRetweetsForPeriod(
            LocalDate start, LocalDate end) {
        HashMap<String, Integer> result = new HashMap<String, Integer>();
        while (totalNumberOfRetweets == null) { // wait if totalNumberOfRetweets
                                                // is not loaded yet
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {

            }
        }
        Set<Date> dateSet = totalNumberOfRetweets.keySet();
        if (dateSet == null) {
            return null;
        }
        // check start end
        if (start == null) {
            start = LocalDate.MIN;
        }
        if (end == null) {
            end = LocalDate.MAX;
        }

        // iterate over all dates to sum the number of retweets
        for (Date d : dateSet) {

            LocalDate currentDate = gui.standardMap.Dates.buildLocalDate(d);

            if (currentDate == null) {
                return null;
            }

            if (gui.standardMap.Dates.inRange(start, end, currentDate)) {
                // currentDate is in requested period of time
                HashMap<String, Integer> curMap = totalNumberOfRetweets.get(d);
                Set<String> curKeySet = curMap.keySet();

                // iterate over all countries at this specific date
                for (String s : curKeySet) {
                    if (result.containsKey(s)) {
                        Integer counter = result.get(s);
                        counter = counter.intValue() + curMap.get(s);
                        result.put(s, counter);
                    } else {
                        result.put(s, curMap.get(s));
                    }
                }
            }
        }
        return result;

    }

    private abstract class UpdateRunnable implements Runnable {
        protected UpdateType type;
        protected GUIElement element;

        public UpdateRunnable(UpdateType t, GUIElement e) {
            type = t;
            element = e;
        }
    }
}
