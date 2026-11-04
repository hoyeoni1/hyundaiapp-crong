package com.hyundaiapp.cruisebreaker.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class TextWrappingTest {
    @Test fun `한글 단어 안에서만 줄바꿈을 막는다`() {
        assertEquals("한\u2060글 단\u2060어", keepWordsTogether("한글 단어"))
    }
    @Test fun `한글에 붙은 숫자와 영문도 같은 단어로 묶는다`() {
        assertEquals("ID\u2060만 32\u2060분 3\u2060회 판\u2060교\u2060IC", keepWordsTogether("ID만 32분 3회 판교IC"))
        assertEquals("A\u2060형 B\u2060급\u20603", keepWordsTogether("A형 B급3"))
    }
    @Test fun `공백 탭 줄바꿈과 nbsp를 유지한다`() {
        assertEquals("한\u2060글  단\u2060어\t다\u2060음\n줄\u00a0바\u2060꿈", keepWordsTogether("한글  단어\t다음\n줄\u00a0바꿈"))
    }
    @Test fun `빈 문자열과 한 글자는 원문 그대로`() {
        assertEquals("",keepWordsTogether(""))
        assertEquals("한",keepWordsTogether("한"))
    }
    @Test fun `url과 이메일은 기본 줄바꿈에 맡긴다`() {
        for(text in listOf("https://example.com/a-very-long-path", "hoyhoyhoyeoni@gmail.com", "cruisebreaker-terms-2026-11-v1")) assertEquals(text,keepWordsTogether(text))
    }
    @Test fun `서로게이트와 결합 이모지는 분리하지 않는다`() {
        for(text in listOf("\uD83D\uDE97", "\uD83D\uDC69\u200D\uD83D\uDC69\u200D\uD83D\uDC67", "\uD83D\uDC4D\uD83C\uDFFD")) assertEquals(text,keepWordsTogether(text))
        assertEquals("한\u2060글\uD83D\uDE97이\u2060름",keepWordsTogether("한글\uD83D\uDE97이름"))
    }
    @Test fun `분해형 한글과 결합 악센트는 그대로`() {
        for(text in listOf("\u1112\u1161\u11AB", "e\u0301")) assertEquals(text,keepWordsTogether(text))
    }
}
