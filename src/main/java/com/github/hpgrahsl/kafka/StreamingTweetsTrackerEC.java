package com.github.hpgrahsl.kafka;

import com.github.hpgrahsl.kafka.config.KStreamsProperties;
import com.github.hpgrahsl.kafka.emoji.EmojiUtils;
import com.github.hpgrahsl.kafka.model.EmojiCount;
import com.github.hpgrahsl.kafka.model.TopEmojis;
import com.github.hpgrahsl.kafka.model.Tweet;
import com.github.hpgrahsl.kafka.serde.EmojiCountSerde;
import com.github.hpgrahsl.kafka.serde.TopNSerdeEC;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Serialized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Properties;

@SpringBootApplication
@EnableConfigurationProperties(KStreamsProperties.class)
public class StreamingTweetsTrackerEC {

    @Bean
    public Topology kStreamsTopology(KStreamsProperties config) {

        Serde<String> stringSerde = Serdes.String();
        EmojiCountSerde countSerde = new EmojiCountSerde();
        TopNSerdeEC topNSerde = new TopNSerdeEC(config.getEmojiCountTopN());

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, Tweet> tweets = builder.stream(config.getTweetsTopic());

        KTable<String, Long> emojiCounts =
            tweets.map((id, t) -> new KeyValue<>(id, EmojiUtils.extractEmojisAsString(t.Text)))
                .filterNot((id, emojis) -> emojis.isEmpty())
                .flatMapValues(emojis -> emojis)
                .map((id, emoji) -> new KeyValue<>(emoji, ""))
                .groupByKey(Serialized.with(stringSerde, stringSerde))
                .count(Materialized.as(config.getStateStoreEmojiCounts()));

        emojiCounts.toStream()
            .map((e, cnt) -> new KeyValue<>("topN", new EmojiCount(e, cnt)))
            .groupByKey(Serialized.with(stringSerde, countSerde))
            .aggregate(
                () -> new TopEmojis(config.getEmojiCountTopN()),
                (aggKey, value, aggregate) -> {
                    aggregate.add(value);
                    return aggregate;
                },
                Materialized.as(config.getStateStoreEmojisTopN()).withValueSerde((Serde) topNSerde)
            );

        return builder.build();

    }

    @Bean
    public KafkaStreams kafkaStreams(KStreamsProperties config) {
        Properties props = new Properties();
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, config.getConsumerAutoOffsetReset());
        props.put(StreamsConfig.APPLICATION_SERVER_CONFIG, config.getApplicationServer());
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, config.getApplicationId());
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, config.getDefaultKeySerde());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, config.getDefaultValueSerde());
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, config.getProcessingGuarantee());
        props.put(StreamsConfig.STATE_DIR_CONFIG, config.getStateStoreDirectory());
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, config.getCacheMaxBytesBuffer());
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, config.getCommitIntervalMs());
        props.put(CommonClientConfigs.METADATA_MAX_AGE_CONFIG, config.getMetadataMaxAgeMs());
        return new KafkaStreams(kStreamsTopology(config), props);
    }

    @Component
    public static class KafkaStreamsBootstrap implements CommandLineRunner {

        @Autowired
        KafkaStreams kafkaStreams;

        @Override
        public void run(String... args) throws Exception {
            kafkaStreams.cleanUp();
            kafkaStreams.start();
            Runtime.getRuntime().addShutdownHook(new Thread(kafkaStreams::close));
        }
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(StreamingTweetsTrackerEC.class);
    }

}
