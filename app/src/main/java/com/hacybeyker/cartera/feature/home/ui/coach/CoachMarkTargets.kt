package com.hacybeyker.cartera.feature.home.ui.coach

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import com.hacybeyker.cartera.feature.home.domain.model.CoachMark

/** Dónde está, en coordenadas de la raíz, cada elemento que señalan los coach marks mientras se ve. */
@Stable
class CoachMarkTargets {
    private val targets = mutableStateMapOf<CoachMark, Target>()

    operator fun get(mark: CoachMark): Rect? = targets[mark]?.bounds

    internal fun update(mark: CoachMark, owner: Any, bounds: Rect) {
        if (targets[mark] != Target(owner, bounds)) targets[mark] = Target(owner, bounds)
    }

    // Solo quien lo midió lo borra: si el elemento se vuelve a componer, el nodo nuevo puede medir antes de que el
    // viejo se suelte.
    internal fun remove(mark: CoachMark, owner: Any) {
        if (targets[mark]?.owner === owner) targets.remove(mark)
    }

    private data class Target(val owner: Any, val bounds: Rect)
}

/** Fuera de Home (por ejemplo, en un golden de Inicio solo) no hay nada que medir. */
val LocalCoachMarkTargets = staticCompositionLocalOf<CoachMarkTargets?> { null }

/** Marca este elemento como el que señala [mark]: sus límites se miden con cada cambio de posición. */
fun Modifier.coachMarkTarget(mark: CoachMark): Modifier = this then CoachMarkTargetElement(mark)

private data class CoachMarkTargetElement(val mark: CoachMark) : ModifierNodeElement<CoachMarkTargetNode>() {
    override fun create() = CoachMarkTargetNode(mark)

    override fun update(node: CoachMarkTargetNode) {
        node.mark = mark
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "coachMarkTarget"
        properties["mark"] = mark
    }
}

private class CoachMarkTargetNode(var mark: CoachMark) :
    Modifier.Node(),
    GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode {

    // Dónde se registró la última medida, para borrarla al soltarse el nodo.
    private var targets: CoachMarkTargets? = null

    override fun onDetach() {
        targets?.remove(mark, owner = this)
        targets = null
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val current = currentValueOf(LocalCoachMarkTargets)
        if (current !== targets) targets?.remove(mark, owner = this)
        targets = current
        current?.update(mark, owner = this, bounds = coordinates.boundsInRoot())
    }
}
