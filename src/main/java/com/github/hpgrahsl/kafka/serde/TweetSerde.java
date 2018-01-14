package com.github.hpgrahsl.kafka.serde;

import com.github.hpgrahsl.kafka.model.Tweet;

public class TweetSerde extends WrapperSerde<Tweet> {

    public TweetSerde() {
        super(new JsonSerializer<>(), new JsonDeserializer<>(Tweet.class));
    }

}
