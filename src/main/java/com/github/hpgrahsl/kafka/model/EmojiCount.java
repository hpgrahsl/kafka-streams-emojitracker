package com.github.hpgrahsl.kafka.model;

public class EmojiCount implements Comparable<EmojiCount> {

    private String emoji;
    private Long count;

    public EmojiCount() { }

    public EmojiCount(String emoji, Long count) {
        this.emoji = emoji;
        this.count = count;
    }

    public String getEmoji() {
        return emoji;
    }

    public Long getCount() {
        return count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EmojiCount that = (EmojiCount) o;

        if (!emoji.equals(that.emoji)) return false;
        return count.equals(that.count);
    }

    @Override
    public int hashCode() {
        int result = emoji.hashCode();
        result = 31 * result + count.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return emoji +" --> "+count;
    }

    @Override
    public int compareTo(EmojiCount o) {
        int cmpCount = o.getCount().compareTo(this.getCount());
        if(cmpCount == 0) {
            return this.getEmoji().compareTo(o.getEmoji());
        }
        return cmpCount;
    }
}
