package com.github.hpgrahsl.kafka.serde;

import com.github.hpgrahsl.kafka.model.EmojiCount;
import com.github.hpgrahsl.kafka.model.TopEmojis;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

public class TopNSerdeEC implements Serde<TopEmojis> {

    private final int limit;

    public TopNSerdeEC() {
        this.limit = TopEmojis.DEFAULT_LIMIT;
    }

    public TopNSerdeEC(int limit) {
        this.limit = limit;
    }

    @Override
    public Serializer<TopEmojis> serializer() {
        return new Serializer<TopEmojis>() {

            @Override
            public void configure(final Map<String, ?> map, final boolean b) {
            }

            @Override
            public byte[] serialize(final String s, final TopEmojis TopEmojis) {
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                final DataOutputStream
                        dataOutputStream =
                        new DataOutputStream(out);
                try {
                    for (EmojiCount ec : TopEmojis) {
                        dataOutputStream.writeUTF(ec.getEmoji());
                        dataOutputStream.writeLong(ec.getCount());
                    }
                    dataOutputStream.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return out.toByteArray();
            }

            @Override
            public void close() {

            }
        };
    }

    @Override
    public Deserializer<TopEmojis> deserializer() {
        return new Deserializer<TopEmojis>() {

            @Override
            public void configure(final Map<String, ?> map, final boolean b) {

            }

            @Override
            public TopEmojis deserialize(final String s, final byte[] bytes) {
                if (bytes == null || bytes.length == 0) {
                    return null;
                }
                final TopEmojis result = new TopEmojis(limit);

                final DataInputStream
                        dataInputStream =
                        new DataInputStream(new ByteArrayInputStream(bytes));

                try {
                    while(dataInputStream.available() > 0) {
                        result.add(new EmojiCount(dataInputStream.readUTF(),
                                dataInputStream.readLong()));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return result;
            }

            @Override
            public void close() {

            }
        };
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {

    }

    @Override
    public void close() {

    }

}
