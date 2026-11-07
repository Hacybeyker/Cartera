package com.hacybeyker.cartera.feature.splash.ui

import com.hacybeyker.cartera.core.designsystem.component.CommonTestTags

object SplashTestTags {
    const val PREFIX = "splash"
    const val SCREEN = "splash_pantalla"
    val LOADING = CommonTestTags.loading(PREFIX)
    val ERROR = CommonTestTags.error(PREFIX)
    val RETRY = CommonTestTags.retry(PREFIX)
}
