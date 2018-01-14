# Near Real-Time Emoji Tracking

## Overview
This repository contains a working example of how to build a modern data-centric application to track occurrences of emojis in near real-time, which are found in publicly available tweets. It uses the following main technologies:

- data ingestion: [Apache Kafka Connect](https://kafka.apache.org/documentation/#connect)
- persistence: [Apacha Kafka](https://kafka.apache.org)
- stream processing: [Apacha Kafka Streams](https://kafka.apache.org/documentation/streams/)
- RPC integration layer & reactive WebAPI: [Spring Boot 2.0](https://projects.spring.io/spring-boot/)

You may also want to take a look at the accompanying slides of a recent talk of mine: [Building Stateful Streaming Applications Without a Database](https://speakerdeck.com/hpgrahsl/building-stateful-streaming-apps-without-a-database)

## Usage example:

The following paragraphs give a detailed step-by-step explanation to setup and run the application on your local machine.

#### 1 Launch your Kafka environment:
The example application needs a fully working Kafka environment, ideally on your local machine. If you are into containers and know how to use Docker feel free to make use of pre-built Docker images for Apache Kafka of your choice (e.g. the ones provided by [Confluent](https://hub.docker.com/r/confluentinc/)). For simplicity reasons, it is probably a good idea to launch all Kafka realted processes based on a [convenient CLI](https://docs.confluent.io/current/cli/index.html) that ships with the Open Source version of [Confluent's Platform](https://www.confluent.io/download/) - currently version 4.0.0.

Change to your installation folder (e.g. /usr/local/confluent-4.0.0/) and run 

```bash
bin/confluent start
```

This should successfully launch all Kafka related processes, namely _zookeeper, kafka, schema-registry, kafka-rest and connect_ and may take a few moments before resulting in an **[UP] status** for each of them:

```bash
Starting zookeeper
zookeeper is [UP]
Starting kafka
kafka is [UP]
Starting schema-registry
schema-registry is [UP]
Starting kafka-rest
kafka-rest is [UP]
Starting connect
connect is [UP]
```

_In case you are facing any issues while bringing up the Confluent Platform read through their amazing documentation which hopefully helps you getting fixed any issues :)_

#### 2 Create a Kafka topic to store tweets:

Before being able to ingest live tweets a Kafka topic needs to be created. This can be easily achieved with the command line tools that ship with Kafka. The following command creates a topic called **live-tweets** with _4 partitions_ and a _replication factor of 1._

```bash
bin/kafka-topics --zookeeper localhost:2181 --topic live-tweets --create --replication-factor 1 --partitions 4
```

#### 3 Run a twitter source connector to harvest public live tweets:

There is a [plethora of Kafka connectors](https://www.confluent.io/product/connectors/) available in order to read data from a variety of sources and write data to different sinks. This application uses a [twitter source connector](https://github.com/jcustenborder/kafka-connect-twitter) from the community. In order to make this connector available in your local installation you have to copy a folder containing the build artefacts or a [pre-built version](https://github.com/jcustenborder/kafka-connect-twitter/releases/tag/0.2.25) together with its dependencies to a specific folder in your Confluent Platform installation. After unzipping the connector artefact copy the contained folder 

```bash
kafka-connect-twitter-0.2.25/usr/share/kafka-connect/kafka-connect-twitter
```
to 

```bash
/usr/local/confluent-4.0.0/share/java/
```

In order for kafka connect to detect the availability of this newly installed connector simply restart the _connect_ process with the CLI by first running

```bash
bin/confluent stop connect
```

followed by 

```bash
bin/confluent start connect
```

Now the twitter source connector is ready to use. It can be easily configured and managed by means of the [Kafka connect REST API](https://docs.confluent.io/current/connect/restapi.html). First check if the connector is indeed available by sending the following GET request e.g. using CURL, Postman or some other tools:

```bash
curl http://localhost:8083/connector-plugins
```

This should result in JSON array containing all _connectors_ currently available to your Kafka connect installation. Somewhere along the lines you should see the twitter source connector:

```json
[
  ...,
  {
    "class": "com.github.jcustenborder.kafka.connect.twitter.TwitterSourceConnector",
    "type": "source",
    "version": "0.2.25"
  }
  ...,
]
```

 We can run the connector to track a subset of live tweets related to a few key words of our choice (see **filter.keywords** entry below) based on the following JSON configuration. Simply insert your _OAuth tokens/secrets_ which you get by creating a Twitter application in your account. This must be created first in order to get access to the Twitter API. Send the JSON configuration as a POST request to the endpoint e.g. using CURL or Postman:

```json
{ "name": "twitter_source_01",
  "config": {
    "connector.class": "com.github.jcustenborder.kafka.connect.twitter.TwitterSourceConnector",
    "twitter.oauth.accessToken": "...",
    "twitter.oauth.consumerSecret": "...",
    "twitter.oauth.consumerKey": "...",
    "twitter.oauth.accessTokenSecret": "...",
	"kafka.status.topic": "live-tweets",
	"process.deletes": false,
	"value.converter": "org.apache.kafka.connect.json.JsonConverter",
	"value.converter.schemas.enable": false, 
    "key.converter": "org.apache.kafka.connect.json.JsonConverter",
    "key.converter.schemas.enable": false,
    "filter.keywords": "meltdown,spectre,intel,amd,arm,sparc,exploit,cpu,vulnerability,attack,security"
    }
}
```

This should result in a _HTTP status 201 created_ response.

#### 4 Check data ingestion
By means of the Kafka command line tools it's easy to check if tweets are flowing into the topic. Running the following in your Confluent Platform folder

```bash
bin/kafka-console-consumer --bootstrap-server localhost:9092 --topic live-tweets --from-beginning
```

should consume all the tweets in the **live-tweets** topic and write them directly to _stdout_ as they come in. The JSON structure of the tweets based on the source connector is pretty verbose. The example application deserialzes only the following 4 fields while actually only making use of the Text field in order to extract any emojis during the stream processing:

```json
{
    "CreatedAt": 1515751050000,
    "Id": 951755003055820800,
    "Text": "Google details how it protected services like Gmail from Spectre https://t.co/jyuEixDaQq #metabloks",
    "Lang": "en"
}
```

#### 5 Launch Spring Boot 2.0 emoji tracker
Everything is setup now to start the stream processing application. Just build the maven project by running:

```bash
mvn clean package
```

then run the application from the command line using:

```bash
java -jar -Dserver.port=8881 -Dkstreams.tweetsTopic=live-tweets target/kafka-streams-emojitracker-0.1-SNAPSHOT.jar
```

#### 6 Interactively query the kstreams application state stores
After the application successfully started you can perform REST calls against it to query for current emoji counts:

##### query for all emojis tracked so far:

```bash
curl -X GET http://localhost:8881/interactive/queries/emojis/
```

The result is in no particular order and might look like the following based on a sample run:

```json
[
    ...,
    {
        "emoji": "🐾",
        "count": 4
    },
    {
        "emoji": "👇",
        "count": 113
    },
    {
        "emoji": "👉",
        "count": 16
    },
    {
        "emoji": "💀",
        "count": 29
    },
    {
        "emoji": "💋",
        "count": 1
    },
    {
        "emoji": "💖",
        "count": 1
    },
    {
        "emoji": "💥",
        "count": 2
    },
    ...
]
```

_NOTE: the numbers you get will obviously vary!_

##### query for a specific emoji tracked so far:
When using CURL you need to specify the emoji by means of its URL escape code. Thus, it's more convenient to query with Postman or your browser as this allow to directly put the emoji into the URL then.

http://localhost:8881/interactive/queries/emojis/👇

```bash
curl -X GET http://localhost:8881/interactive/queries/emojis/%F0%9F%91%87 
```

{
    "emoji": "👇",
    "count": 113
}

_NOTE: the numbers you get will obviously vary!_

##### query for the top N emojis tracked so far:

```bash
curl -X GET http://localhost:8881/interactive/queries/emojis/stats/topN
```

```json
[
    {
        "emoji": "👇",
        "count": 113
    },
    {
        "emoji": "😭",
        "count": 100
    },
    {
        "emoji": "➡",
        "count": 81
    },
    {
        "emoji": "✨",
        "count": 80
    },
    {
        "emoji": "⚡",
        "count": 79
    },
    {
        "emoji": "🌎",
        "count": 77
    },
    {
        "emoji": "😂",
        "count": 64
    },
    {
        "emoji": "💀",
        "count": 29
    },
    {
        "emoji": "❤",
        "count": 21
    },
    {
        "emoji": "🔥",
        "count": 17
    },
    ...
]
```
_NOTE: the numbers you get will obviously vary!_

#### 7 Optional: Run multiple instance of the kstreams application
In case you want to run multiple instances to experiment with scalability and fault-tolerance of kstreams just launch the application multiple times. **Beware to use different _server.port_ and _live.demo.instance.id_ settings for each further instance**

e.g. start a 2nd instance like so:

```bash
java -jar -Dserver.port=8882 -Dlive.demo.instance.id=2 -Dkstreams.tweetsTopic=live-tweets target/kafka-streams-emojitracker-0.1-SNAPSHOT.jar
```

Now you can query any of the two instances to get the emoji count results!

### Have fun tracking emojis in near real-time based on public tweets... 😊

---

_NOTE: A rough prototype of this code was originally written for the talk "Building Stateful Streaming Applications without a Database" which I presented at [Netconomy's](https://www.netconomy.net) 4th Innovation Day on 12th of January 2018._

---
