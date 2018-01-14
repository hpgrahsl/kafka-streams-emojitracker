package com.github.hpgrahsl.kafka.controller;

import com.github.hpgrahsl.kafka.config.KStreamsProperties;
import com.github.hpgrahsl.kafka.model.EmojiCount;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;

import java.util.Collections;
import java.util.Properties;
import java.util.Set;

@RestController
@RequestMapping("interactive/queries/")
@CrossOrigin(origins = "*")
public class StateStoreController {

    private final StateStoreRouter router;

    private final ConnectableFlux<ReceiverRecord<String, Long>> connectableSource;
    private final Flux<ReceiverRecord<String,Long>> emojiCountsFlux;

    private static final Logger LOGGER = LoggerFactory.getLogger(StateStoreController.class);

    public StateStoreController(KStreamsProperties config, StateStoreRouter router) {

        this.router = router;

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, config.getConsumerClientId());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, config.getConsumerGroupId());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, config.getConsumerAutoOffsetReset());

        ReceiverOptions<String,Long> receiverOptions = ReceiverOptions.<String,Long>create(props)
                                    .subscription(Collections.singleton(config.getChangelogEmojiCounts()));

        connectableSource = KafkaReceiver.create(receiverOptions).receive()
                        .doOnSubscribe(subscription -> LOGGER.info("reactively subscribed to {} topic",
                            config.getChangelogEmojiCounts()))
                        .publish();
        emojiCountsFlux = connectableSource.autoConnect();
        emojiCountsFlux.subscribe();
    }


    @GetMapping("local/emojis")
    public Flux<EmojiCount> getEmojisLocal() {
        return router.queryLocalEmojiCounts();
    }

    @GetMapping("emojis")
    public Flux<EmojiCount> getEmojis() {
        return router.queryAllEmojiCounts();
    }

    @GetMapping("emojis/{code}")
    public Mono<ResponseEntity<EmojiCount>> getEmoji(@PathVariable String code) {
        return router.querySingleEmojiCount(code);
    }

    @GetMapping("emojis/stats/topN")
    public Mono<Set<EmojiCount>> getEmojisTopN() {
        return router.queryEmojiCountsTopN();
    }

    @GetMapping(path = "emojis/updates/notify", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<EmojiCount> consumeEmojiCountsStream() {
        return emojiCountsFlux.map(rr -> new EmojiCount(rr.key(),rr.value()))
                    .doOnNext(ec -> LOGGER.debug("SSE to client -> " + ec.toString()));
    }

}
