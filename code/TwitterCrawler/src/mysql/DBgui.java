package mysql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.logging.Logger;

import twitter4j.User;
import mysql.result.Account;
import mysql.result.Category;
import mysql.result.Location;
import mysql.result.Retweets;
import mysql.result.Tweets;
import mysql.result.TweetsAndRetweets;

/**
 * class to modify the database restricted
 * 
 * @author Holger Ebhart
 * @version 1.0
 * 
 */
public class DBgui extends DBConnection implements DBIgui {

    /**
     * configure the connection to the database
     * 
     * @param accessData
     *            the access data to the specified mysql-database as AccessData
     * @param logger
     *            a global logger for the whole program as Logger
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    public DBgui(AccessData accessData, Logger logger)
            throws InstantiationException, IllegalAccessException,
            ClassNotFoundException {
        super(accessData, logger);
    }

    @Override
    public Category[] getCategories() {
        String sqlCommand = "SELECT * FROM category;";

        ResultSet res = null;
        Statement stmt = null;
        try {
            stmt = c.createStatement();
            res = stmt.executeQuery(sqlCommand);
        } catch (SQLException e) {
            sqlExceptionLog(e, stmt);
            return new Category[0];
        }

        List<Category> list = new ArrayList<Category>();
        try {
            while (res.next()) {
                list.add(new Category(res.getInt("Id"), res.getString("Name"),
                        new Category(res.getInt("ParentId"), "", null)));
            }
        } catch (SQLException e) {
            logger.warning("Couldn't read sql result: \n" + e.getMessage());
            return new Category[0];
        } finally {
            if (res != null) {
                try {
                    res.close();
                } catch (SQLException e) {
                    sqlExceptionLog(e);
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    sqlExceptionLog(e);
                }
            }
        }

        Category[] ret = new Category[list.size()];
        int i = 0;
        for (Category c : list) {
            // TODO with inner join
            // parent relationship
            for (Category p : list) {
                if (c.getParent().getId() == p.getId()) {
                    c.setParent(p);
                    break;
                }
            }
            ret[i++] = c;
        }
        return ret;
    }

    @Override
    public Location[] getLocations() {
        String sqlCommand = "SELECT * FROM location;";

        ResultSet res = null;
        Statement stmt = null;
        try {
            stmt = c.createStatement();
            res = stmt.executeQuery(sqlCommand);
        } catch (SQLException e) {
            sqlExceptionLog(e, stmt);
            return new Location[0];
        }

        List<Location> list = new ArrayList<Location>();
        try {
            while (res.next()) {
                list.add(new Location(res.getInt("Id"), res.getString("Name"),
                        res.getString("Code"), new Location(res
                                .getInt("ParentId"), null, null, null)));
            }
        } catch (SQLException e) {
            logger.warning("Couldn't read sql result: \n" + e.getMessage());
            return new Location[0];
        } finally {
            if (res != null) {
                try {
                    res.close();
                } catch (SQLException e) {
                    sqlExceptionLog(e);
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    sqlExceptionLog(e);
                }
            }
        }

        Location[] ret = new Location[list.size()];
        int i = 0;
        for (Location l : list) {
            // TODO with inner join
            // parent relationship
            for (Location p : list) {
                if (l.getParent().getId() == p.getId()) {
                    l.setParent(p);
                    break;
                }
            }
            ret[i++] = l;
        }
        return ret;
    }

    @Override
    public Date[] getDates() {
        String sqlCommand = "SELECT Id,Day FROM day;";

        ResultSet res = null;
        Statement stmt = null;
        try {
            stmt = c.createStatement();
            res = stmt.executeQuery(sqlCommand);
        } catch (SQLException e) {
            sqlExceptionLog(e, stmt);
            return new Date[0];
        }

        Stack<Date> sd = new Stack<Date>();
        Stack<Integer> si = new Stack<Integer>();
        // int max = 0;
        try {
            while (res.next()) {
                sd.push(res.getDate("Day"));
                int temp = res.getInt("Id");
                si.push(temp);
            }
        } catch (SQLException e) {
            logger.warning("Couldn't read sql result: \n" + e.getMessage());
            return new Date[0];
        } finally {
            if (res != null) {
                try {
                    res.close();
                } catch (SQLException e) {
                    sqlExceptionLog(e);
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    sqlExceptionLog(e);
                }
            }
        }

        Date[] ret = new Date[sd.size()];
        while (!sd.isEmpty()) {
            ret[si.pop() - 1] = sd.pop();
        }

        return ret;
    }

    @Override
    public int getAccountId(String accountName) {

        PreparedStatement stmt = null;
        ResultSet res = null;
        try {
            stmt = c.prepareStatement("SELECT Id FROM accounts WHERE AccountName = ? LIMIT 1;");
            stmt.setString(1, accountName);
            res = stmt.executeQuery();
        } catch (SQLException e) {
            sqlExceptionLog(e, stmt);
            return -1;
        }

        int ret = -1;
        try {
            res.next();
            ret = res.getInt("Id");
        } catch (SQLException e) {
            ret = -1;
        } finally {
            if (res != null) {
                try {
                    res.close();
                } catch (SQLException e) {
                    sqlExceptionLog(e);
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    sqlExceptionLog(e);
                }
            }
        }
        return ret;
    }

    @Override
    public Account[] getAccounts(String search) {

        // prevent SQL-injection
        PreparedStatement stmt = null;
        ResultSet res = null;
        try {
            stmt = c.prepareStatement("SELECT Id, TwitterAccountId, AccountName,Verified, Follower, URL, LocationId FROM accounts WHERE AccountName LIKE ? LIMIT 50;");
            stmt.setString(1, "%" + search + "%");
            res = stmt.executeQuery();
        } catch (SQLException e) {
            sqlExceptionLog(e, stmt);
        }

        if (res == null)
            return new Account[0];

        Stack<Account> st = new Stack<Account>();
        try {
            while (res.next()) {
                st.push(new Account(res.getInt("Id"), res
                        .getLong("TwitterAccountId"), res
                        .getString("AccountName"), res.getBoolean("Verified"),
                        res.getString("URL"), res.getInt("Follower"), res
                                .getInt("LocationId")));
            }
        } catch (SQLException e) {
            logger.warning("Couldn't read sql result: \n" + e.getMessage());
            return new Account[0];
        } finally {
            if (res != null) {
                try {
                    res.close();
                } catch (SQLException e) {
                    sqlExceptionLog(e);
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    sqlExceptionLog(e);
                }
            }
        }

        Account[] ret = new Account[st.size()];
        for (int i = 0; i < st.size(); i++) {
            ret[i] = st.pop();
        }
        return ret;
    }

    @Override
    public boolean setCategory(int accountId, int categoryId) {

        // prevent SQL-injection
        PreparedStatement stmt = null;
        boolean ret = false;
        try {
            stmt = c.prepareStatement("INSERT IGNORE INTO accountCategory (AccountId, CategoryId) VALUES (?, ?);");
            stmt.setInt(1, accountId);
            stmt.setInt(2, categoryId);
            ret = stmt.executeUpdate() >= 0 ? true : false;
        } catch (SQLException e) {
            sqlExceptionLog(e, stmt);
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    sqlExceptionLog(e);
                }
            }
        }

        return ret;
    }

    @Override
    public boolean setLocation(int accountId, int locationId, boolean active) {

        // prevent SQL-injection
        PreparedStatement stmt = null;
        boolean ret = false;
        try {
            stmt = c.prepareStatement("UPDATE accounts SET LocationId = ? WHERE Id = ?;");
            stmt.setInt(1, accountId);
            stmt.setInt(2, locationId);
            ret = stmt.executeUpdate() >= 0 ? true : false;
        } catch (SQLException e) {
            sqlExceptionLog(e, stmt);
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    sqlExceptionLog(e);
                }
            }
        }

        return ret;
    }

    @Override
    public boolean addAccount(User user, int locationId) {
        // prevent SQL-injection
        PreparedStatement stmt = null;
        boolean ret = false;
        try {
            stmt = c.prepareStatement("INSERT IGNORE INTO accounts (TwitterAccountId, AccountName, Verified, Follower, LocationId, URL, Categorized) VALUES (?, ?, "
                    + (user.isVerified() ? "1" : "0") + ", ?, ?, ?, 0);");
            stmt.setLong(1, user.getId());
            stmt.setString(2, user.getScreenName());
            stmt.setInt(3, user.getFollowersCount());
            stmt.setInt(4, locationId);
            stmt.setString(5,
                    user.getURL().replace("\\", "/").replace("\"", "\"\""));
            ret = stmt.executeUpdate() >= 0 ? true : false;
        } catch (SQLException e) {
            sqlExceptionLog(e, stmt);
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    sqlExceptionLog(e);
                }
            }
        }

        return ret;
    }

    @Override
    public TweetsAndRetweets getSumOfData(int[] categoryIDs, int[] locationIDs,
            int[] accountIDs) throws IllegalArgumentException, SQLException {
        return getSummedData(categoryIDs, locationIDs, accountIDs, false);
    }

    @Override
    public TweetsAndRetweets getSumOfDataWithDates(int[] categoryIDs,
            int[] locationIDs, int[] accountIDs)
            throws IllegalArgumentException, SQLException {
        return getSummedData(categoryIDs, locationIDs, accountIDs, true);

    }

    private TweetsAndRetweets getSummedData(int[] categoryIDs,
            int[] locationIDs, int[] accountIDs, boolean byDate)
            throws SQLException {

        if (categoryIDs == null || categoryIDs.length < 1
                || locationIDs == null || locationIDs.length < 1) {
            throw new IllegalArgumentException();
        }
        Statement stmt = createBasicStatement(categoryIDs, locationIDs,
                accountIDs);

        TweetsAndRetweets ret = new TweetsAndRetweets();
        ret.tweets = getTweetSum(stmt, byDate);
        ret.retweets = getRetweetSum(stmt, byDate);

        return ret;
    }

    private List<Tweets> getTweetSum(Statement stmt, boolean byDate) {

        String a = "SELECT SUM(Counter), Day FROM final JOIN tweets ON final.val=tweets.AccountId JOIN day ON tweets.DayId=Day.Id GROUP BY DayId;";
        String b = "SELECT SUM(Counter) FROM final JOIN tweets ON final.val=tweets.AccountId;";

        ResultSet res = null;
        try {
            stmt.executeBatch();
            res = stmt.executeQuery(byDate ? a : b);
        } catch (SQLException e) {
            sqlExceptionLog(e, stmt);
        }

        if (res == null)
            return new ArrayList<Tweets>();

        List<Tweets> ret = new ArrayList<Tweets>();
        try {
            res.next();
            ret.add(new Tweets((byDate ? res.getDate("Day") : null), res
                    .getInt(1)));
        } catch (SQLException e) {
            logger.warning("Couldn't read sql result: \n" + e.getMessage());
            return new ArrayList<Tweets>();
        } finally {
            if (res != null) {
                try {
                    res.close();
                } catch (SQLException e) {
                    sqlExceptionLog(e);
                }
            }
            // if (stmt != null) {
            // try {
            // stmt.close();
            // } catch (SQLException e) {
            // sqlExceptionLog(e);
            // }
            // }
        }

        return ret;
    }

    private List<Retweets> getRetweetSum(Statement stmt, boolean byDate) {

        String a = "SELECT SUM(Counter), LocationId, Day FROM final JOIN retweets ON final.val=retweets.AccountId JOIN day ON retweets.DayId=Day.Id GROUP BY LocationId, DayId;";
        String b = "SELECT SUM(Counter), LocationId FROM final JOIN retweets ON final.val=retweets.AccountId GROUP BY LocationId;";

        ResultSet res = null;
        try {
            // stmt.executeBatch();
            res = stmt.executeQuery(byDate ? a : b);
        } catch (SQLException e) {
            sqlExceptionLog(e, stmt);
        }

        if (res == null)
            return new ArrayList<Retweets>();

        List<Retweets> ret = new ArrayList<Retweets>();
        try {
            while (res.next()) {
                ret.add(new Retweets((byDate ? res.getDate("Day") : null),
                        res.getInt(1), res.getInt("LocationId")));
            }
        } catch (SQLException e) {
            logger.warning("Couldn't read sql result: \n" + e.getMessage());
            return new ArrayList<Retweets>();
        } finally {
            if (res != null) {
                try {
                    res.close();
                } catch (SQLException e) {
                    sqlExceptionLog(e);
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    sqlExceptionLog(e);
                }
            }
        }

        return ret;
    }

    @Override
    public Account[] getAllData(int[] categoryIDs, int[] locationIDs,
            int[] accountIDs) throws IllegalArgumentException, SQLException {
        return getDataPerAccount(categoryIDs, locationIDs, accountIDs, false);
    }

    @Override
    public Account[] getAllDataWithDates(int[] categoryIDs, int[] locationIDs,
            int[] accountIDs) throws IllegalArgumentException, SQLException {
        return getDataPerAccount(categoryIDs, locationIDs, accountIDs, true);
    }

    private Account[] getDataPerAccount(int[] categoryIDs, int[] locationIDs,
            int[] accountIDs, boolean byDates) throws IllegalArgumentException,
            SQLException {
        if (categoryIDs == null || categoryIDs.length < 1
                || locationIDs == null || locationIDs.length < 1) {
            throw new IllegalArgumentException();
        }
        Statement stmt = createBasicStatement(categoryIDs, locationIDs,
                accountIDs);

        Account[] ret = getTweetSumPerAccount(stmt, byDates);
        List<Account> temp = getRetweetSumPerAccount(stmt, byDates);

        // match Account lists
        for (int i = 0; i < ret.length; i++) {
            for (int j = 0; j < temp.size(); j++) {
                if (temp.get(j).getId() == ret[i].getId()) {
                    // match
                    ret[i].setRetweets(temp.get(j).getRetweets());
                    temp.remove(j);
                    j = temp.size();
                }
            }
        }

        return ret;
    }

    private Account[] getTweetSumPerAccount(Statement stmt, boolean byDate) {

        String a = "SELECT Counter, AccountName, tweets.AccountId, Day FROM final JOIN tweets ON final.val=tweets.AccountId JOIN day ON tweets.DayId=Day.Id JOIN accounts ON final.val=accounts.Id;";
        String b = "SELECT SUM(Counter),AccountName, tweets.AccountId FROM final JOIN tweets ON final.val=tweets.AccountId JOIN accounts ON final.val=accounts.Id GROUP BY AccountId;";

        ResultSet res = null;
        try {
            stmt.executeBatch();
            res = stmt.executeQuery(byDate ? a : b);
        } catch (SQLException e) {
            sqlExceptionLog(e, stmt);
        }

        if (res == null)
            return new Account[0];

        List<Account> accounts = new ArrayList<Account>();
        try {
            while (res.next()) {
                if (byDate) {
                    // add account and tweet
                    if (!accounts.contains(res.getInt(3))) {
                        accounts.add(new Account(res.getInt(3), res
                                .getString(2), new Tweets(null, res.getInt(1))));
                    } else {
                        // add tweets to account
                        Account ac = null;
                        Iterator<Account> it = accounts.iterator();
                        while (it.hasNext() && ac == null) {
                            Account temp = it.next();
                            if (temp.getId() == res.getInt(3)) {
                                ac = temp;
                            }
                        }
                        ac.addTweet(new Tweets(res.getDate("Day"), res
                                .getInt(1)));
                    }
                } else {
                    accounts.add(new Account(res.getInt(3), res.getString(2),
                            new Tweets(null, res.getInt(1))));
                }
            }
        } catch (SQLException e) {
            logger.warning("Couldn't read sql result: \n" + e.getMessage());
            return new Account[0];
        } finally {
            if (res != null) {
                try {
                    res.close();
                } catch (SQLException e) {
                    sqlExceptionLog(e);
                }
            }
            // if (stmt != null) {
            // try {
            // stmt.close();
            // } catch (SQLException e) {
            // sqlExceptionLog(e);
            // }
            // }
        }

        Account[] ret = new Account[accounts.size()];
        int i = 0;
        for (Account account : accounts) {
            ret[i++] = account;
        }

        return ret;
    }

    private List<Account> getRetweetSumPerAccount(Statement stmt, boolean byDate) {

        String a = "SELECT Counter, retweets.LocationId, AccountId, Day FROM final JOIN retweets ON final.val=retweets.AccountId JOIN day ON retweets.DayId=Day.Id;";
        String b = "SELECT SUM(Counter),retweets.LocationId, AccountId FROM final JOIN retweets ON final.val=retweets.AccountId GROUP BY LocationId, AccountId;";

        ResultSet res = null;
        try {
            // stmt.executeBatch();
            res = stmt.executeQuery(byDate ? a : b);
        } catch (SQLException e) {
            sqlExceptionLog(e, stmt);
        }

        if (res == null)
            return new ArrayList<Account>();

        List<Account> ret = new ArrayList<Account>();
        try {
            while (res.next()) {

                if (!ret.contains(res.getInt(3))) {
                    // add account and retweet
                    ret.add(new Account(res.getInt(3), res.getString(2),
                            new Retweets((byDate ? res.getDate(4) : null), res
                                    .getInt(1), res.getInt(2))));
                } else {
                    // add retweets to account
                    Account ac = null;
                    Iterator<Account> it = ret.iterator();
                    while (it.hasNext() && ac == null) {
                        Account temp = it.next();
                        if (temp.getId() == res.getInt(3)) {
                            ac = temp;
                        }
                    }
                    ac.addRetweet(new Retweets((byDate ? res.getDate("Day")
                            : null), res.getInt(1), res.getInt(2)));
                }

            }
        } catch (SQLException e) {
            logger.warning("Couldn't read sql result: \n" + e.getMessage());
            return new ArrayList<Account>();
        } finally {
            if (res != null) {
                try {
                    res.close();
                } catch (SQLException e) {
                    sqlExceptionLog(e);
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    sqlExceptionLog(e);
                }
            }
        }

        return ret;
    }

    private Statement createBasicStatement(int[] categoryIDs,
            int[] locationIDs, int[] accountIDs) throws SQLException {

        Statement stmt = c.createStatement();

        stmt.addBatch("CREATE TEMPORARY TABLE IF NOT EXISTS temp1 (val int);");
        stmt.addBatch("CREATE TEMPORARY TABLE IF NOT EXISTS temp2 (val int);");
        stmt.addBatch("CREATE TEMPORARY TABLE IF NOT EXISTS final (val int);");
        stmt.addBatch("ALTER TABLE final ADD Constraint u UNIQUE (val);");

        String c1 = " INSERT INTO temp1 (val) SELECT AccountId FROM accountCategory WHERE CategoryId="
                + categoryIDs[0];

        for (int i = 1; i < categoryIDs.length; i++) {
            c1 += " OR CategoryId=" + categoryIDs[i];
        }
        c1 += ";";
        stmt.addBatch(c1);

        String c2 = "INSERT INTO temp2 (val) SELECT Id FROM accounts WHERE LocationId="
                + locationIDs[0];

        for (int i = 1; i < locationIDs.length; i++) {
            c2 += " OR LocationId=" + locationIDs[i];
        }
        c2 += ";";
        stmt.addBatch(c2);

        stmt.addBatch("INSERT INTO final (val) SELECT temp2.val FROM temp2 JOIN temp1 ON temp1.val=temp2.val;");

        if (accountIDs.length > 0) {
            // add accounts
            String c3 = "INSERT IGNORE INTO final (val) VALUES ("
                    + accountIDs[0] + ")";
            for (int i = 1; i < accountIDs.length; i++) {
                c3 += ", (" + accountIDs[i] + ")";

            }
            c3 += ";";
            stmt.addBatch(c3);
        }

        return stmt;
    }

    private void sqlExceptionLog(SQLException e) {
        logger.warning("SQL-Exception: SQL-Status: " + e.getSQLState()
                + "\n Message: " + e.getMessage());
    }

    private void sqlExceptionLog(SQLException e, Statement statement) {
        logger.warning("Couldn't execute sql query! SQL-Status: "
                + e.getSQLState() + "\n Message: " + e.getMessage()
                + "\n SQL-Query: " + statement.toString() + "\n");
    }

}
