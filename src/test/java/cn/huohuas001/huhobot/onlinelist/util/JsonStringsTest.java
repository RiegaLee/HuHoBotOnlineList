package cn.huohuas001.huhobot.onlinelist.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonStringsTest {
    @Test
    void quotesControlCharactersAndChineseText() {
        assertEquals("\"在线\\n\\\"列表\\\"\\\\\"", JsonStrings.quote("在线\n\"列表\"\\"));
    }
}
