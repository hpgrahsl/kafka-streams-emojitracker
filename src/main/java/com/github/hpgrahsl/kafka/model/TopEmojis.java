package com.github.hpgrahsl.kafka.model;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

public class TopEmojis implements Iterable<EmojiCount> {

    public static final int DEFAULT_LIMIT = 25;

    private final Map<String,EmojiCount> emojis = new HashMap<>();

    private final TreeSet<EmojiCount> topN = new TreeSet<>(
            Comparator.comparingLong(EmojiCount::getCount).reversed()
                    .thenComparing(Comparator.comparing(EmojiCount::getEmoji)));

    private final int limit;

    public TopEmojis() {
        this.limit = DEFAULT_LIMIT;
    }

    public TopEmojis(int limit) {
        this.limit = limit;
    }

    public void add(final EmojiCount ec) {
        if(emojis.containsKey(ec.getEmoji())) {
            topN.remove(emojis.remove(ec.getEmoji()));
        }
        topN.add(ec);
        emojis.put(ec.getEmoji(),ec);
        if(topN.size() > limit) {
            final EmojiCount lowest = topN.last();
            topN.remove(emojis.remove(lowest.getEmoji()));
        }
    }

    public void remove(final EmojiCount ec) {
        topN.remove(ec);
        emojis.remove(ec.getEmoji());
    }

    public TreeSet<EmojiCount> getTopN() {
        return topN;
    }

    @Override
    public Iterator<EmojiCount> iterator() {
        return topN.iterator();
    }

    @Override
    public String toString() {
        return "TopEmojis{" +
                "topN=" + topN +
                '}';
    }
}
