package ai.idraak.stepskyscraper.core.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

private val BOLD = Regex("<b>(.*?)</b>")

/**
 * Turns copy with `<b>…</b>` markers (escaped in strings.xml) into an AnnotatedString where the bold part
 * also takes the stronger ink colour, as in the design ("Floor 5 in **800 steps**").
 */
fun boldText(raw: String, boldColor: Color? = null): AnnotatedString = buildAnnotatedString {
    val style = SpanStyle(fontWeight = FontWeight.Bold, color = boldColor ?: Color.Unspecified)
    var cursor = 0
    BOLD.findAll(raw).forEach { match ->
        append(raw.substring(cursor, match.range.first))
        withStyle(style) { append(match.groupValues[1]) }
        cursor = match.range.last + 1
    }
    append(raw.substring(cursor))
}
