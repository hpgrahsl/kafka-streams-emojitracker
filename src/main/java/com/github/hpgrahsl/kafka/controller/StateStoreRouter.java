package com.github.hpgrahsl.kafka.controller;

import com.github.hpgrahsl.kafka.config.KStreamsProperties;
import com.github.hpgrahsl.kafka.model.EmojiCount;
import com.github.hpgrahsl.kafka.model.TopEmojis;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.state.HostInfo;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.StreamsMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

@Component
public class StateStoreRouter {

    private final KStreamsProperties config;

    private final KafkaStreams kafkaStreams;

    private final HostInfo localSelf;

    private static final Logger LOGGER = LoggerFactory.getLogger(StateStoreRouter.class);

    public StateStoreRouter(KStreamsProperties config, KafkaStreams kafkaStreams) {
        this.config = config;
        this.kafkaStreams = kafkaStreams;
        this.localSelf = new HostInfo(config.getApplicationServer().split(":")[0],
                                Integer.parseInt(config.getApplicationServer().split(":")[1]));

    }

    public Mono<ResponseEntity<EmojiCount>> querySingleEmojiCount(String code) {
        try {
            StreamsMetadata metadata = kafkaStreams.metadataForKey(
                config.getStateStoreEmojiCounts(),
                code, Serdes.String().serializer());

            if(localSelf.equals(metadata.hostInfo())) {
                ReadOnlyKeyValueStore<String,Long> kvStoreEmojiCounts =
                    kafkaStreams.store(config.getStateStoreEmojiCounts(),
                        QueryableStoreTypes.keyValueStore());

                LOGGER.debug("state store for emoji  {}  is locally available",code);
                Long count = kvStoreEmojiCounts.get(code);
                return Mono.just(new ResponseEntity<>(
                            new EmojiCount(code, count != null ? count : 0L), HttpStatus.OK));
            }

            LOGGER.debug("state store for emoji  {}  NOT locally available",code);

            String location = String.format("http://%s:%d/interactive/queries/emojis/%s",
                                metadata.host(),metadata.port(),code);

            LOGGER.debug("redirecting client to {}",location);

            return Mono.just(ResponseEntity.status(HttpStatus.FOUND)
                                .location(URI.create(location)).build());

        } catch (InvalidStateStoreException exc) {
            LOGGER.error(exc.getMessage());
            return Mono.error(exc);
        }
    }

    public Flux<EmojiCount> queryLocalEmojiCounts() {
        try {
            LOGGER.debug("querying local state store for its managed emoji counts");

            ReadOnlyKeyValueStore<String,Long> kvStoreEmojiCounts =
                kafkaStreams.store(config.getStateStoreEmojiCounts(),
                    QueryableStoreTypes.keyValueStore());

            List<EmojiCount> result = new ArrayList<>();
            kvStoreEmojiCounts.all().forEachRemaining(
                entry -> result.add(new EmojiCount(entry.key, entry.value))
            );

            return Flux.fromIterable(result);

        } catch (InvalidStateStoreException exc) {
            LOGGER.error(exc.getMessage());
            return Flux.error(exc);
        }
    }

    public Flux<EmojiCount> queryAllEmojiCounts() {
        Stream<Flux<EmojiCount>> queries =
                        Stream.concat(Stream.of(queryLocalEmojiCounts()),
                            kafkaStreams.allMetadataForStore(config.getStateStoreEmojiCounts()).stream()
                            .filter(metadata -> !localSelf.equals(metadata.hostInfo()))
                            .map(metadata ->
                                WebClient.create("http://"+metadata.host()+":"+metadata.port())
                                    .get().uri("/interactive/queries/local/emojis")
                                    .accept(MediaType.APPLICATION_JSON)
                                    .retrieve().bodyToFlux(EmojiCount.class)
                                    .doOnSubscribe(sub ->
                                        LOGGER.debug("querying http://{}:{} state store for all its remote emojis",
                                                                                metadata.host(),metadata.port())
                                    )
                            ));

        return Flux.merge(Flux.fromStream(queries))
                .doOnSubscribe(sub -> LOGGER.debug("scatter & gather query for the entire app state of all emoji counts"));
    }

    public Mono<Set<EmojiCount>> queryEmojiCountsTopN() {
        try {
            StreamsMetadata metadata = kafkaStreams.metadataForKey(
                config.getStateStoreEmojisTopN(),
                "topN", Serdes.String().serializer());

            if(localSelf.equals(metadata.hostInfo())) {
                LOGGER.debug("state store for top N emojis is locally available");

                ReadOnlyKeyValueStore<String,TopEmojis> kvStoreEmojisTopN =
                    kafkaStreams.store(config.getStateStoreEmojisTopN(),
                        QueryableStoreTypes.keyValueStore());

                return Mono.justOrEmpty(
                        Optional.ofNullable(kvStoreEmojisTopN.get("topN"))
                            .map(te -> te.getTopN()).orElseGet(TreeSet::new)
                );
            }

            LOGGER.debug("state store for top N emojis NOT locally available thus fetching from {}", metadata.hostInfo());

            WebClient webClient = WebClient.create("http://"+metadata.host()+":"+metadata.port());
            return webClient.get().uri("/interactive/queries/emojis/stats/topN")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .flatMap(response -> response.bodyToMono(
                    new ParameterizedTypeReference<TreeSet<EmojiCount>>(){}
                ));

        } catch (InvalidStateStoreException exc) {
            LOGGER.error(exc.getMessage());
            return Mono.error(exc);
        }
    }

}
