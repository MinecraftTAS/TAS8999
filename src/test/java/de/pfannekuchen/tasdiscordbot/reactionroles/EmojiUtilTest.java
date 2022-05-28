package de.pfannekuchen.tasdiscordbot.reactionroles;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import emoji4j.EmojiUtils;

class EmojiUtilTest {

	@Test
	void isEmoteTest() {
		assertTrue(EmojiUtils.isEmoji(":grin:"));
	}

}
