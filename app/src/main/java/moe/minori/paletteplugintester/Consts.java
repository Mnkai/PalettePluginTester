package moe.minori.paletteplugintester;

/**
 * Created by minori on 15. 8. 19.
 */
public class Consts {

    /**
     * Palette external broadcasting API name
     */
    public final static String EXTERNAL_BROADCAST_API = "palette.twitter.externalBroadcast";

    // From client
    /**
     * External API, write tweet on current active account / Extra: DATA (String), REPLY_TO (LONG - nullable)
     */
    public final static String REQ_WRITE_TWEET = "palette.twitter.externalBroadcast.tweet.write";

    /**
     * External API, Query is Palette API available?
     */
    public final static String API_AVAIL_QUERY = "palette.twitter.externalBroadcast.available.query";

    // From support module
    /**
     * External API, notify on new tweet on timeline and mention / Extra: FROMTEXT, FROMTEXTSCREEN, TEXT, REPLY_TO cf. {@link #NEW_STATUS_NONLIB}
     */
    public final static String NOTIFY_NEW_STATUS = "palette.twitter.externalBroadcast.tweet.streaming";

    /**
     * External API, notify that Palette API is available
     */
    public final static String API_AVAIL_NOTIFY_POS = "palette.twitter.externalBroadcast.available.notify.positive";

    /**
     * External API, notify that Palette API is not available
     */
    public final static String API_AVAIL_NOTIFY_NEG = "palette.twitter.externalBroadcast.available.notify.negative";


}
