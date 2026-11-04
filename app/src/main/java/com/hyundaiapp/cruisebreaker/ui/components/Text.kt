package com.hyundaiapp.cruisebreaker.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

/*
 * 두 앱 공통(차슐랭 Text.kt와 같은 내용). 화면 파일은 material3 Text 대신 이 Text를 import 한다.
 * 안드로이드는 한글을 글자 단위로 줄바꿈해서 "기록/합니다"처럼 단어 중간에서 끊는다.
 * 일반 한글 단어 안에 WORD JOINER를 넣되, URL·이모지·분해형 문자는 원문을 유지한다.
 * URL·이메일은 강제로 묶지 않고 기본 줄바꿈에 맡긴다.
 * 접근성·테스트가 읽는 글자는 원래 문자열 그대로 둔다.
 */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
) {
    val shown = remember(text, softWrap, maxLines) {
        if (softWrap && maxLines > 1) keepWordsTogether(text) else text
    }
    androidx.compose.material3.Text(
        shown,
        if (shown == text) modifier else modifier.semantics { this.text = AnnotatedString(text) },
        color, fontSize, fontStyle, fontWeight, fontFamily, letterSpacing, textDecoration, textAlign,
        lineHeight, overflow, softWrap, maxLines, minLines, onTextLayout, style,
    )
}

private const val WordJoiner = '\u2060'

/** 완성형 한글 음절 사이만 묶는다. 다른 문자와 UTF-16/이모지 결합은 그대로 둔다. */
internal fun keepWordsTogether(text: String): String {
    if (text.length < 2) return text
    return buildString(text.length * 2) {
        var index = 0
        var previous = -1
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            if (previous in 0xAC00..0xD7A3 && codePoint in 0xAC00..0xD7A3) append(WordJoiner)
            appendCodePoint(codePoint)
            previous = codePoint
            index += Character.charCount(codePoint)
        }
    }
}
