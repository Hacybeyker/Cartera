package com.hacybeyker.cartera.feature.legacy.data

import com.hacybeyker.cartera.core.ui.marks.VisibleContentMarker
import com.hacybeyker.cartera.legacy.integration.LegacyContentMarker
import javax.inject.Inject

class VisibleContentLegacyMarker @Inject constructor(
    private val markers: Set<@JvmSuppressWildcards VisibleContentMarker>
) : LegacyContentMarker {
    override fun onContentVisible(mark: String) {
        markers.forEach { it.onContentVisible(mark) }
    }
}
