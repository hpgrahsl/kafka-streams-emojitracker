package com.github.hpgrahsl.kafka.model;

public class Tweet {

    public long Id;
    public long CreatedAt;
    public String Text;
    public String Lang;

    @Override
    public String toString() {
        return "Tweet{" +
            "Id=" + Id +
            ", CreatedAt=" + CreatedAt +
            ", Text='" + Text + '\'' +
            ", Lang='" + Lang + '\'' +
            '}';
    }
}
