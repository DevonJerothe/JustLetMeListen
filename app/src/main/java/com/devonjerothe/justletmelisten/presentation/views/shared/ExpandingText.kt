package com.devonjerothe.justletmelisten.presentation.views.shared

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

@Composable
fun ExpandingText(
    text: String,
    modifier: Modifier = Modifier,
    minLines: Int = 5
) {
    var isExpanded by remember { mutableStateOf(false) }
    var canCollapse by remember { mutableStateOf(false) }
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }

    val textStyle = MaterialTheme.colorScheme.primary

    val annotatedText = remember(text, isExpanded, canCollapse) {
        buildAnnotatedString {
            if (isExpanded) {
                append(text)
            } else {
                if (canCollapse) {
                    val lastCharIndex = textLayout?.getLineEnd(minLines - 1) ?: 0
                    val trimmedText = text.substring(0, lastCharIndex).dropLast(20).trim()
                    append(trimmedText)
                    withStyle(style = SpanStyle(color = textStyle)) {
                        append("... Show More")
                    }
                } else {
                    append(text)
                }
            }
        }
    }

    Column(modifier = modifier
            .clickable { isExpanded = !isExpanded }
            .animateContentSize(animationSpec = tween(durationMillis = 300))
    ) {
        Text(
            text = annotatedText,
            style = LocalTextStyle.current,
            maxLines = if (isExpanded) Int.MAX_VALUE else minLines,
            onTextLayout = {
                if (!canCollapse) {
                    canCollapse = it.hasVisualOverflow
                }
                textLayout = it
            }
        )
    }
}
