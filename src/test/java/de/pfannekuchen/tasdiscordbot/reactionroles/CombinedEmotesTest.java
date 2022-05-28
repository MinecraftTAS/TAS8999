package de.pfannekuchen.tasdiscordbot.reactionroles;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CombinedEmotesTest {

	@Test
	void testContstruction() {
		EmoteWrapper comEmote = new EmoteWrapper(":heart:");
		assertNotNull(comEmote);
	}

}
