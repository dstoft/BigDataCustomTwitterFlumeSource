package com.group2.flumetwittersource;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDrivenSource;
import org.apache.flume.channel.ChannelProcessor;
import org.apache.flume.conf.Configurable;
import org.apache.flume.event.EventBuilder;
import org.apache.flume.source.AbstractSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.json.DataObjectFactory;

import java.util.HashMap;
import java.util.Map;

// http://davidiscoding.com/real-time-twitter-sentiment-analysis-pt-2-ingesting-twitter-stream-with-flume-and-kafka
public class TwitterSource extends AbstractSource implements EventDrivenSource, Configurable {

    private final static Logger logger = LoggerFactory.getLogger(TwitterSource.class);

    private String consumerKey;
    private String consumerSecret;
    private String accessToken;
    private String accessTokenSecret;

    private String[] keywords;

    private TwitterStream twitterStream;

    @Override
    public void configure(Context context) {
        consumerKey = context.getString(TwitterSourceConstants.CONSUMER_KEY_KEY);
        consumerSecret = context.getString(TwitterSourceConstants.CONSUMER_SECRET_KEY);
        accessToken = context.getString(TwitterSourceConstants.ACCESS_TOKEN_KEY);
        accessTokenSecret = context.getString(TwitterSourceConstants.ACCESS_TOKEN_SECRET_KEY);

        TwitterQueryParameterParser tqpp = new TwitterQueryParameterParser();
        String keywordString = context.getString(TwitterSourceConstants.KEYWORDS_KEY, "");
        keywords = tqpp.getKeywords(keywordString);

        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(consumerKey)
                .setOAuthConsumerSecret(consumerSecret)
                .setOAuthAccessToken(accessToken)
                .setOAuthAccessTokenSecret(accessTokenSecret)
                .setJSONStoreEnabled(true);

        twitterStream = new TwitterStreamFactory(cb.build()).getInstance();
    }

    @Override
    public void start() {
        final ChannelProcessor channel = getChannelProcessor();
        final Map<String, String> headers = new HashMap<String, String>();

        StatusListener listener = new StatusListener() {

            @Override
            public void onStatus(Status status) {
                System.out.println(status.getUser().getName() + " : " + status.getText());
                logger.debug("Tweet arrived");

                headers.put("timestamp", String.valueOf(status.getCreatedAt().getTime()));
                Event event = EventBuilder.withBody(
                        DataObjectFactory.getRawJSON(status).getBytes(), headers);

                channel.processEvent(event);
            }

            public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
            }

            public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
            }

            public void onScrubGeo(long arg0, long arg1) {
            }

            public void onStallWarning(StallWarning arg0) {
            }

            public void onException(Exception ex) {
                logger.error("Error while listening to Twitter stream.", ex);
            }
        };

        logger.debug("Setting up Twitter sample stream using consumer key" + consumerKey +
                " and access token " + accessToken);

        twitterStream.addListener(listener);

        if (keywords.length == 0) {
            logger.debug("Starting up Twitter sampling...");
            twitterStream.sample();

        } else {
            logger.debug("Starting up Twitter filtering...");

            FilterQuery query = new FilterQuery();
            if (keywords.length > 0) query.track(keywords);

            twitterStream.filter(query);
        }

        super.start();
    }

    @Override
    public void stop() {
        logger.debug("Shutting down Twitter sample stream...");
        twitterStream.shutdown();
        super.stop();
    }
}
