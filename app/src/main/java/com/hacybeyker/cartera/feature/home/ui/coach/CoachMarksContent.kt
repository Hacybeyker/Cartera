package com.hacybeyker.cartera.feature.home.ui.coach

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.sizes
import com.hacybeyker.cartera.core.designsystem.spacing
import com.hacybeyker.cartera.feature.home.domain.model.CoachMark
import kotlin.math.roundToInt

private const val SCRIM_ALPHA = 0.6f

/**
 * Capa de los coach marks sobre Home: oscurece todo salvo un hueco sobre [targetBounds] (en coordenadas de la raíz)
 * y muestra el texto del paso con Siguiente (Entendido en el último) y Omitir. Ningún toque llega a lo de abajo.
 * Mientras el elemento no tiene límites (por ejemplo, Inicio todavía carga), no hay hueco y el texto va al centro.
 */
@Composable
fun CoachMarksContent(
    mark: CoachMark,
    targetBounds: Rect?,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val inset = with(density) { MaterialTheme.spacing.xs.toPx() }
    val corner = with(density) { MaterialTheme.sizes.highlightCorner.toPx() }
    val stroke = with(density) { MaterialTheme.sizes.highlightStroke.toPx() }
    val hole = targetBounds?.translate(-origin)?.inflate(inset)
    val scrim = MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA)
    val outline = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(CoachMarksTestTags.LAYER)
            .onGloballyPositioned { origin = it.positionInRoot() }
            .pointerInput(Unit) { consumeAllTouches() }
            .drawBehind { drawScrim(hole, CornerRadius(corner), scrim, outline, stroke) }
    ) {
        StepPlacement(hole = hole) {
            StepCard(mark = mark, onNext = onNext, onSkip = onSkip)
        }
    }
}

private suspend fun PointerInputScope.consumeAllTouches() {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent().changes.forEach { it.consume() }
        }
    }
}

private fun DrawScope.drawScrim(hole: Rect?, corner: CornerRadius, scrim: Color, outline: Color, stroke: Float) {
    val path = Path().apply {
        fillType = PathFillType.EvenOdd
        addRect(Rect(Offset.Zero, size))
        hole?.let { addRoundRect(RoundRect(it, corner)) }
    }
    drawPath(path = path, color = scrim)
    hole?.let {
        drawRoundRect(
            color = outline,
            topLeft = it.topLeft,
            size = it.size,
            cornerRadius = corner,
            style = Stroke(width = stroke)
        )
    }
}

/** El texto va debajo del hueco si este está en la mitad de arriba; si no, encima. */
@Composable
private fun StepPlacement(hole: Rect?, card: @Composable () -> Unit) {
    val gutter = MaterialTheme.spacing.screen
    val gap = MaterialTheme.spacing.md
    Layout(content = card, modifier = Modifier.fillMaxSize()) { measurables, constraints ->
        val gutterPx = gutter.roundToPx()
        val gapPx = gap.roundToPx()
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val placeable = measurables.first().measure(
            Constraints(maxWidth = (width - 2 * gutterPx).coerceAtLeast(0), maxHeight = height)
        )
        layout(width, height) {
            val y = when {
                hole == null -> (height - placeable.height) / 2
                hole.center.y < height / 2f -> hole.bottom.roundToInt() + gapPx
                else -> hole.top.roundToInt() - gapPx - placeable.height
            }
            placeable.place(gutterPx, y.coerceIn(0, (height - placeable.height).coerceAtLeast(0)))
        }
    }
}

@Composable
private fun StepCard(mark: CoachMark, onNext: () -> Unit, onSkip: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().testTag(CoachMarksTestTags.step(mark.number))) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
        ) {
            Text(
                text = stringResource(R.string.coach_step_count, mark.number, CoachMark.entries.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(text = stringResource(mark.title), style = MaterialTheme.typography.titleMedium)
            Text(text = stringResource(mark.body), style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm, Alignment.End)
            ) {
                TextButton(onClick = onSkip, modifier = Modifier.testTag(CoachMarksTestTags.SKIP)) {
                    Text(text = stringResource(R.string.coach_skip))
                }
                Button(onClick = onNext, modifier = Modifier.testTag(CoachMarksTestTags.NEXT)) {
                    Text(text = stringResource(if (mark.isLast) R.string.coach_done else R.string.coach_next))
                }
            }
        }
    }
}

@get:StringRes
private val CoachMark.title: Int
    get() = when (this) {
        CoachMark.TRANSFER -> R.string.coach_transfer_title
        CoachMark.MARKET_TAB -> R.string.coach_market_title
        CoachMark.MENU -> R.string.coach_menu_title
    }

@get:StringRes
private val CoachMark.body: Int
    get() = when (this) {
        CoachMark.TRANSFER -> R.string.coach_transfer_body
        CoachMark.MARKET_TAB -> R.string.coach_market_body
        CoachMark.MENU -> R.string.coach_menu_body
    }
