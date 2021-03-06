package mysql.result;

import java.util.ArrayList;
import java.util.List;

/**
 * class to transmit and store the results from the database
 * 
 * @author Holger Ebhart
 */
public class Account extends Result implements Comparable<Account> {

    private String name;
    private String url;
    private long twitterId;
    private int follower;
    private String locationCode;
    private boolean verified;
    private List<Integer> categoryIds;
    private List<Tweets> tweets;
    private List<Retweets> retweets;

    /**
     * create a new object to store a account-data
     * 
     * @param id
     *            the id of the account in the database as int
     * @param twitterId
     *            the id of the twitter account as long
     * @param name
     *            the name of the account as String
     * @param verified
     *            true if this account is verified by Twitter, else false
     * @param url
     *            the official url of the account as String
     * @param follower
     *            the number of followers of the account as int
     * @param locationCode
     *            the location-code of the account as String
     * @param categoryIds
     *            the category-id's of the account as List<Integer>
     * @param tweets
     *            the tweets of the account as List<Tweets>
     * @param retweets
     *            the retweets of the account as List<Retweets>
     */
    public Account(int id, long twitterId, String name, boolean verified,
            String url, int follower, String locationCode,
            List<Integer> categoryIds, List<Tweets> tweets,
            List<Retweets> retweets) {
        super(id);
        this.verified = verified;
        this.twitterId = twitterId;
        this.name = name;
        this.url = url;
        this.follower = follower;
        this.locationCode = locationCode;
        this.categoryIds = categoryIds;
        this.tweets = tweets;
        this.retweets = retweets;
    }

    // /**
    // * create a new object to store a account-data
    // *
    // * @param id
    // * the id of the account in the database as int
    // * @param twitterId
    // * the id of the twitter account as long
    // * @param name
    // * the name of the account as String
    // * @param url
    // * the official url of the account as String
    // * @param follower
    // * the number of followers of the account as int
    // * @param locationCode
    // * the location-code of the account as String
    // * @param categories
    // * the category-id's of the account as List<Integer>
    // */
    // public Account(int id, long twitterId, String name, String url,
    // int follower, String locationCode, List<Integer> categories) {
    // this(id, twitterId, name, false, url, follower, locationCode,
    // categories, new ArrayList<Tweets>(), new ArrayList<Retweets>());
    // }

    /**
     * create a new object to store a account-data
     * 
     * @param id
     *            the id of the account in the database as int
     * @param twitterId
     *            the id of the twitter account as long
     * @param name
     *            the name of the account as String
     * @param verified
     *            true if this account is verified by Twitter, else false
     * @param url
     *            the official url of the account as String
     * @param follower
     *            the number of followers of the account as int
     * @param locationCode
     *            the location-code of the account as String
     */
    public Account(int id, long twitterId, String name, boolean verified,
            String url, int follower, String locationCode) {
        this(id, twitterId, name, verified, url, follower, locationCode,
                new ArrayList<Integer>(), new ArrayList<Tweets>(),
                new ArrayList<Retweets>());
    }

    /**
     * create a new object to store a account-data
     * 
     * @param id
     *            the id of the account in the database as int
     * @param name
     *            the name of the account
     * @param url
     *            the official url of the account as String
     */
    public Account(int id, String name, String url) {
        this(id, 0, name, false, url, 0, "0");
    }

    /**
     * create a new object to store a account-data
     * 
     * @param id
     *            the id of the account in the database as int
     * @param name
     *            the name of the account
     * @param follower
     *            the number of followers of the account as int
     * @param locationCode
     *            the location-code of the account as String
     */
    public Account(int id, String name, int follower, String locationCode) {
        this(id, 0, name, false, null, follower, locationCode);
    }

    /**
     * returns the name of the account
     * 
     * @return the name of the account as String
     */
    public String getName() {
        return name;
    }

    /**
     * returns the url of the account
     * 
     * @return the url of the account as String
     */
    public String getUrl() {
        return url;
    }

    /**
     * returns the id of the twitter account
     * 
     * @return the name of the twitter account as long
     */
    public long getTwitterId() {
        return twitterId;
    }

    /**
     * returns the number of followers of the account
     * 
     * @return the number of followers of the account as int
     */
    public int getFollower() {
        return follower;
    }

    /**
     * returns the location-code of the account
     * 
     * @return the location-code of the account as String
     */
    public String getLocationCode() {
        return locationCode;
    }

    /**
     * returns the category-id's of the account
     * 
     * @return the category-id's of the account as List<Integer>
     */
    public List<Integer> getCategoryIds() {
        return categoryIds;
    }

    /**
     * returns the tweets of the account
     * 
     * @return the tweets of the account as List<Tweets>
     */
    public List<Tweets> getTweets() {
        return tweets;
    }

    /**
     * returns the retweets of the account
     * 
     * @return the retweets of the account as List<Retweet>
     */
    public List<Retweets> getRetweets() {
        return retweets;
    }

    /**
     * returns if the account is verified or not
     * 
     * @return true if the account is verified, else false
     */
    public boolean isVerified() {
        return verified;
    }

    /**
     * set the retweets array from this account
     * 
     * @param retweets
     *            the new array with the actual retweets as Retweets[]
     */
    public void setRetweets(List<Retweets> retweets) {
        this.retweets = retweets;
    }

    /**
     * adds a Tweets-Object to the tweets-list of this account
     * 
     * @param tweet
     *            the sum of tweets to add as Tweets
     */
    public void addTweet(Tweets tweet) {
        tweets.add(tweet);
    }

    /**
     * adds a Retweets-Object to the retweets-list of this account
     * 
     * @param retweet
     *            the sum of retweets to add as Retweets
     */
    public void addRetweet(Retweets retweet) {
        retweets.add(retweet);
    }

    /**
     * adds a category to this account
     * 
     * @param categoryId
     *            the id of the category to add to this account as int
     */
    public void addCategoryId(int categoryId) {
        categoryIds.add(categoryId);
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        boolean equal = false;
        if (o != null && o instanceof Result) {
            equal = ((Result) o).getId() == getId();
        }
        return equal;
    }

    @Override
    public int compareTo(Account a) {
        return Integer.compare(a.getFollower(), follower);
    }
}
