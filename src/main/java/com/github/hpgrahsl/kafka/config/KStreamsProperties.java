package com.github.hpgrahsl.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kstreams")
public class KStreamsProperties {

    private String applicationServer;
    private String applicationId;
    private String bootstrapServers;
    private String defaultKeySerde;
    private String defaultValueSerde;
    private String processingGuarantee;
    private String consumerAutoOffsetReset;
    private String consumerClientId;
    private String consumerGroupId;
    private String stateStoreDirectory;
    private Long commitIntervalMs;
    private Long cacheMaxBytesBuffer;
    private Long metadataMaxAgeMs;
    private String tweetsTopic;
    private String stateStoreEmojiCounts;
    private Integer emojiCountTopN;
    private String stateStoreEmojisTopN;

    private String changelogEmojiCounts;

    public String getApplicationServer() {
        return applicationServer;
    }

    public void setApplicationServer(String applicationServer) {
        this.applicationServer = applicationServer;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public String getDefaultKeySerde() {
        return defaultKeySerde;
    }

    public void setDefaultKeySerde(String defaultKeySerde) {
        this.defaultKeySerde = defaultKeySerde;
    }

    public String getDefaultValueSerde() {
        return defaultValueSerde;
    }

    public void setDefaultValueSerde(String defaultValueSerde) {
        this.defaultValueSerde = defaultValueSerde;
    }

    public String getProcessingGuarantee() {
        return processingGuarantee;
    }

    public void setProcessingGuarantee(String processingGuarantee) {
        this.processingGuarantee = processingGuarantee;
    }

    public String getConsumerAutoOffsetReset() {
        return consumerAutoOffsetReset;
    }

    public void setConsumerAutoOffsetReset(String consumerAutoOffsetReset) {
        this.consumerAutoOffsetReset = consumerAutoOffsetReset;
    }

    public String getConsumerClientId() {
        return consumerClientId;
    }

    public void setConsumerClientId(String consumerClientId) {
        this.consumerClientId = consumerClientId;
    }

    public String getConsumerGroupId() {
        return consumerGroupId;
    }

    public void setConsumerGroupId(String consumerGroupId) {
        this.consumerGroupId = consumerGroupId;
    }

    public String getStateStoreDirectory() {
        return stateStoreDirectory;
    }

    public void setStateStoreDirectory(String stateStoreDirectory) {
        this.stateStoreDirectory = stateStoreDirectory;
    }

    public Long getCommitIntervalMs() {
        return commitIntervalMs;
    }

    public void setCommitIntervalMs(Long commitIntervalMs) {
        this.commitIntervalMs = commitIntervalMs;
    }

    public Long getCacheMaxBytesBuffer() {
        return cacheMaxBytesBuffer;
    }

    public void setCacheMaxBytesBuffer(Long cacheMaxBytesBuffer) {
        this.cacheMaxBytesBuffer = cacheMaxBytesBuffer;
    }

    public Long getMetadataMaxAgeMs() {
        return metadataMaxAgeMs;
    }

    public void setMetadataMaxAgeMs(Long metadataMaxAgeMs) {
        this.metadataMaxAgeMs = metadataMaxAgeMs;
    }

    public String getTweetsTopic() {
        return tweetsTopic;
    }

    public void setTweetsTopic(String tweetsTopic) {
        this.tweetsTopic = tweetsTopic;
    }

    public String getStateStoreEmojiCounts() {
        return stateStoreEmojiCounts;
    }

    public void setStateStoreEmojiCounts(String stateStoreEmojiCounts) {
        this.stateStoreEmojiCounts = stateStoreEmojiCounts;
    }

    public Integer getEmojiCountTopN() {
        return emojiCountTopN;
    }

    public void setEmojiCountTopN(Integer emojiCountTopN) {
        this.emojiCountTopN = emojiCountTopN;
    }

    public String getStateStoreEmojisTopN() {
        return stateStoreEmojisTopN;
    }

    public void setStateStoreEmojisTopN(String stateStoreEmojisTopN) {
        this.stateStoreEmojisTopN = stateStoreEmojisTopN;
    }

    public String getChangelogEmojiCounts() {
        return changelogEmojiCounts;
    }

    public void setChangelogEmojiCounts(String changelogEmojiCounts) {
        this.changelogEmojiCounts = changelogEmojiCounts;
    }

}
