package com.hyundaiapp.cruisebreaker.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
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
 * LineBreak.WordBreak.Phrase도 한글에는 듣지 않아서, 줄이 바뀔 수 있는 글자에만
 * 단어 안 글자 사이에 WORD JOINER(U+2060, 보이지 않음)를 넣어 띄어쓰기에서만 줄이 바뀌게 한다.
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
    val shown = if (softWrap && maxLines > 1) keepWordsTogether(text) else text
    androidx.compose.material3.Text(
        shown,
        if (shown == text) modifier else modifier.semantics { this.text = AnnotatedString(text) },
        color, fontSize, fontStyle, fontWeight, fontFamily, letterSpacing, textDecoration, textAlign,
        lineHeight, overflow, softWrap, maxLines, minLines, onTextLayout, style,
    )
}

private const val WordJoiner = '⁠'

/** 띄어쓰기·줄바꿈이 아닌 두 글자 사이에 WORD JOINER를 넣는다 */
internal fun keepWordsTogether(text: String): String {
    if (text.length < 2) return text
    return buildString(text.length * 2) {
        text.forEachIndexed { i, c ->
            if (i > 0 && !c.isWhitespace() && !text[i - 1].isWhitespace()) append(WordJoiner)
            append(c)
        }
    }
}
