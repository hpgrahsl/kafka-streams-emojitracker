package com.github.hpgrahsl.kafka.serde;

import com.github.hpgrahsl.kafka.model.EmojiCount;

public class EmojiCountSerde extends WrapperSerde<EmojiCount> {

    public EmojiCountSerde() {
        super(new JsonSerializer<>(), new JsonDeserializer<>(EmojiCount.class));
    }

}
