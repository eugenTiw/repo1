package com.eugentiw.fernziel.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eugentiw.fernziel.R

/** Abschnittsüberschrift in Kapitälchen-Anmutung, optional mit Zusatz rechts. */
@Composable
fun SectionHeader(text: String, trailing: String? = null) {
    val c = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 28.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text.uppercase(),
            modifier = Modifier.weight(1f),
            style = AppType.overline,
            color = c.textSecondary,
        )
        if (trailing != null) {
            Text(trailing, style = AppType.overline, color = c.textSecondary)
        }
    }
}

/** Ruhige Fläche mit abgerundeten Ecken, ohne Schatten. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LocalAppColors.current
    val shape = RoundedCornerShape(16.dp)
    var m = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(c.surface)
    if (onClick != null) {
        m = m.clickable(onClick = onClick)
    }
    Column(m, content = content)
}

/** Haarlinie zwischen Zeilen einer Fläche. */
@Composable
fun Hairline(startInset: Dp = 16.dp) {
    val c = LocalAppColors.current
    Box(
        Modifier
            .padding(start = startInset)
            .fillMaxWidth()
            .height(1.dp)
            .background(c.separator)
    )
}

enum class Glyph {
    PLUS, MINUS, CLOSE, CHECK, CHECK_CIRCLE, TARGET,
    EYE, EYE_OFF, CHEVRON_LEFT, CHEVRON_RIGHT, CHEVRON_DOWN, CALENDAR,
}

private fun checkPath(w: Float, h: Float): Path {
    val p = Path()
    p.moveTo(w * 0.30f, h * 0.52f)
    p.lineTo(w * 0.45f, h * 0.66f)
    p.lineTo(w * 0.71f, h * 0.37f)
    return p
}

/** Zeichnet die wenigen Symbole der App selbst, ohne zusätzliche Symbolbibliothek. */
fun DrawScope.drawGlyph(glyph: Glyph, color: Color) {
    val w = size.width
    val h = size.height
    val sw = w * 0.1f
    val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (glyph) {
        Glyph.PLUS -> {
            drawLine(color, Offset(w * 0.5f, h * 0.12f), Offset(w * 0.5f, h * 0.88f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(color, Offset(w * 0.12f, h * 0.5f), Offset(w * 0.88f, h * 0.5f), strokeWidth = sw, cap = StrokeCap.Round)
        }
        Glyph.MINUS -> {
            drawLine(color, Offset(w * 0.12f, h * 0.5f), Offset(w * 0.88f, h * 0.5f), strokeWidth = sw, cap = StrokeCap.Round)
        }
        Glyph.CLOSE -> {
            drawLine(color, Offset(w * 0.18f, h * 0.18f), Offset(w * 0.82f, h * 0.82f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(color, Offset(w * 0.82f, h * 0.18f), Offset(w * 0.18f, h * 0.82f), strokeWidth = sw, cap = StrokeCap.Round)
        }
        Glyph.CHECK -> {
            val p = Path()
            p.moveTo(w * 0.14f, h * 0.54f)
            p.lineTo(w * 0.40f, h * 0.78f)
            p.lineTo(w * 0.86f, h * 0.24f)
            drawPath(p, color, style = Stroke(width = w * 0.14f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        Glyph.CHECK_CIRCLE -> {
            drawCircle(color, radius = w / 2f - sw / 2f, style = stroke)
            drawPath(checkPath(w, h), color, style = stroke)
        }
        Glyph.EYE, Glyph.EYE_OFF -> {
            val thin = Stroke(width = w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val p = Path()
            p.moveTo(w * 0.05f, h * 0.5f)
            p.cubicTo(w * 0.27f, h * 0.16f, w * 0.73f, h * 0.16f, w * 0.95f, h * 0.5f)
            p.cubicTo(w * 0.73f, h * 0.84f, w * 0.27f, h * 0.84f, w * 0.05f, h * 0.5f)
            p.close()
            drawPath(p, color, style = thin)
            drawCircle(color, radius = w * 0.14f, style = thin)
            if (glyph == Glyph.EYE_OFF) {
                drawLine(color, Offset(w * 0.14f, h * 0.10f), Offset(w * 0.86f, h * 0.90f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
            }
        }
        Glyph.CALENDAR -> {
            val line = w * 0.08f
            drawRoundRect(
                color,
                topLeft = Offset(w * 0.10f, h * 0.18f),
                size = Size(w * 0.80f, h * 0.72f),
                cornerRadius = CornerRadius(w * 0.14f, w * 0.14f),
                style = Stroke(width = line, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawLine(color, Offset(w * 0.10f, h * 0.40f), Offset(w * 0.90f, h * 0.40f), strokeWidth = line)
            drawLine(color, Offset(w * 0.32f, h * 0.07f), Offset(w * 0.32f, h * 0.26f), strokeWidth = line, cap = StrokeCap.Round)
            drawLine(color, Offset(w * 0.68f, h * 0.07f), Offset(w * 0.68f, h * 0.26f), strokeWidth = line, cap = StrokeCap.Round)
        }
        Glyph.CHEVRON_LEFT -> {
            val p = Path()
            p.moveTo(w * 0.64f, h * 0.12f)
            p.lineTo(w * 0.30f, h * 0.5f)
            p.lineTo(w * 0.64f, h * 0.88f)
            drawPath(p, color, style = Stroke(width = w * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        Glyph.CHEVRON_RIGHT -> {
            val p = Path()
            p.moveTo(w * 0.36f, h * 0.12f)
            p.lineTo(w * 0.70f, h * 0.5f)
            p.lineTo(w * 0.36f, h * 0.88f)
            drawPath(p, color, style = Stroke(width = w * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        Glyph.CHEVRON_DOWN -> {
            val p = Path()
            p.moveTo(w * 0.12f, h * 0.36f)
            p.lineTo(w * 0.5f, h * 0.70f)
            p.lineTo(w * 0.88f, h * 0.36f)
            drawPath(p, color, style = Stroke(width = w * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        Glyph.TARGET -> {
            drawCircle(color, radius = w / 2f - sw / 2f, style = stroke)
            drawCircle(color, radius = w * 0.16f)
        }
    }
}

/** Runde Schaltfläche mit Symbol und 44-dp-Tippfläche. */
@Composable
fun GlyphButton(
    glyph: Glyph,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glyphSize: Dp = 20.dp,
) {
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(glyphSize)) { drawGlyph(glyph, tint) }
    }
}

/** Kreis zum Abhaken: leer mit feinem Rand oder gefüllt mit Haken. */
@Composable
fun CheckMark(done: Boolean) {
    val c = LocalAppColors.current
    val fill = c.accent
    val check = c.onAccent
    val ring = c.textTertiary
    Canvas(Modifier.size(24.dp)) {
        if (done) {
            drawCircle(fill)
            drawPath(
                checkPath(size.width, size.height),
                check,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        } else {
            val sw = 1.75.dp.toPx()
            drawCircle(ring, radius = size.minDimension / 2f - sw / 2f, style = Stroke(width = sw))
        }
    }
}

/**
 * Eine abhakbare Aufgabe. Erledigtes wird durchgestrichen und bleibt in der Liste.
 * Mit [onRemove] erscheint rechts ein kleines Kreuz zum Entfernen.
 */
@Composable
fun TaskRow(
    title: String,
    subtitle: String?,
    done: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    onRemove: (() -> Unit)? = null,
    removeLabel: String = "",
) {
    val c = LocalAppColors.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .toggleable(
                    value = done,
                    enabled = enabled,
                    role = Role.Checkbox,
                    onValueChange = { onToggle() },
                )
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CheckMark(done)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = AppType.body,
                    color = if (done || !enabled) c.textSecondary else c.text,
                    textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = AppType.caption,
                        color = c.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (onRemove != null) {
            GlyphButton(
                glyph = Glyph.CLOSE,
                label = removeLabel,
                tint = c.textTertiary,
                onClick = onRemove,
                glyphSize = 12.dp,
            )
        }
    }
}

/** Farbpunkt mit Beschriftung für Legenden. */
@Composable
fun LegendItem(color: Color, label: String) {
    val c = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(10.dp)) { drawCircle(color) }
        Spacer(Modifier.width(6.dp))
        Text(label, style = AppType.caption, color = c.textSecondary)
    }
}

/** Schlichte Textschaltfläche. */
@Composable
fun TextAction(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    bold: Boolean = false,
    align: TextAlign = TextAlign.Start,
) {
    val c = LocalAppColors.current
    Text(
        text,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        style = AppType.body,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        color = if (enabled) color else c.textTertiary,
        textAlign = align,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Kleine Kapsel-Schaltfläche, z. B. „Heute einplanen". */
@Composable
fun Pill(text: String, active: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) c.accentSoft else c.fill)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        style = AppType.caption,
        fontWeight = FontWeight.Medium,
        color = if (active) c.accent else c.text,
        maxLines = 1,
    )
}

/** Auswahl-Chip, z. B. für den Lebensbereich. */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) c.accent else c.surface)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        style = AppType.subhead,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        color = if (selected) c.onAccent else c.text,
        maxLines = 1,
    )
}

/** Segmentierte Auswahl mit ruhigem Hintergrund. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val c = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(c.fill)
            .padding(3.dp)
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) c.fillSelected else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(index) })
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = AppType.subhead,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) c.text else c.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Zähler mit Minus und Plus. */
@Composable
fun Stepper(value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    val c = LocalAppColors.current
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(c.fill),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphButton(
            glyph = Glyph.MINUS,
            label = stringResource(R.string.less),
            tint = if (value > min) c.text else c.textTertiary,
            onClick = { onChange(value - 1) },
            enabled = value > min,
            glyphSize = 14.dp,
        )
        Box(
            Modifier
                .width(1.dp)
                .height(20.dp)
                .background(c.separator)
        )
        GlyphButton(
            glyph = Glyph.PLUS,
            label = stringResource(R.string.more),
            tint = if (value < max) c.text else c.textTertiary,
            onClick = { onChange(value + 1) },
            enabled = value < max,
            glyphSize = 14.dp,
        )
    }
}

enum class DayState { NONE, PLANNED, DONE }

/** Wochentag einer Wochenhandlung: leer, eingeplant (Ring) oder erledigt (gefüllt). */
@Composable
fun DayChip(
    label: String,
    description: String,
    dayState: DayState,
    isToday: Boolean,
    dimmed: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val c = LocalAppColors.current
    val fillColor = if (dayState == DayState.DONE) c.accent else Color.Transparent
    val ringColor = when {
        dayState == DayState.PLANNED -> c.accent
        dayState == DayState.NONE && isToday -> c.textTertiary
        else -> Color.Transparent
    }
    val textColor = when (dayState) {
        DayState.DONE -> c.onAccent
        DayState.PLANNED -> c.accent
        DayState.NONE -> if (dimmed) c.textTertiary else if (isToday) c.text else c.textSecondary
    }
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(fillColor)
            .border(1.5.dp, ringColor, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = AppType.subhead,
            fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
        )
    }
}

/** Schlichtes Textfeld ohne Rahmen, für Zeilen innerhalb einer Fläche. */
@Composable
fun PlainField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
) {
    val c = LocalAppColors.current
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = AppType.body.copy(color = c.text),
        placeholder = { Text(placeholder, style = AppType.body, color = c.textTertiary) },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = c.accent,
        ),
    )
}

/** Leerer Zustand mit Einladung, das erste Ziel anzulegen. */
@Composable
fun EmptyState(onAddGoal: () -> Unit) {
    val c = LocalAppColors.current
    Panel {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp)) {
            Text(stringResource(R.string.empty_title), style = AppType.headline, color = c.text)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.empty_text), style = AppType.subhead, color = c.textSecondary)
            Spacer(Modifier.height(8.dp))
            TextAction(
                text = stringResource(R.string.add_first_goal),
                color = c.accent,
                onClick = onAddGoal,
                modifier = Modifier.padding(start = 0.dp),
                bold = true,
            )
        }
    }
}
