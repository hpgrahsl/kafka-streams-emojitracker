package com.github.hpgrahsl.kafka.emoji;

import com.vdurmont.emoji.EmojiParser;

import java.util.List;
import java.util.stream.Collectors;

public class EmojiUtils extends EmojiParser {

	public static List<String> extractEmojisAsString(String original) {

		return EmojiUtils.getUnicodeCandidates(original)
					.stream().map(uc -> uc.getEmoji().getUnicode())
						.collect(Collectors.toList());

	}

}
