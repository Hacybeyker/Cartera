package com.hacybeyker.cartera.feature.onboarding.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.designsystem.sizes
import com.hacybeyker.cartera.core.designsystem.spacing
import kotlinx.coroutines.launch

private data class OnboardingPage(val icon: ImageVector, @StringRes val title: Int, @StringRes val body: Int)

private val Pages = listOf(
    OnboardingPage(Icons.Filled.Home, R.string.onboarding_accounts_title, R.string.onboarding_accounts_body),
    OnboardingPage(
        Icons.AutoMirrored.Filled.List,
        R.string.onboarding_movements_title,
        R.string.onboarding_movements_body
    ),
    OnboardingPage(
        Icons.AutoMirrored.Filled.Send,
        R.string.onboarding_transfers_title,
        R.string.onboarding_transfers_body
    )
)

// Sin testTags a propósito: estas pantallas suelen venir de terceros y se prueban por texto.
@Composable
fun OnboardingContent(onAction: (OnboardingAction) -> Unit, modifier: Modifier = Modifier, initialPage: Int = 0) {
    val pagerState = rememberPagerState(initialPage = initialPage) { Pages.size }
    val scope = rememberCoroutineScope()
    BackHandler(enabled = pagerState.currentPage > 0) {
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.screen),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg)
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
            PageContent(page = Pages[index])
        }
        PageIndicator(pagerState = pagerState)
        OnboardingButtons(
            isLastPage = pagerState.currentPage == Pages.lastIndex,
            onAction = onAction,
            onNext = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }
        )
    }
}

@Composable
private fun PageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = page.icon,
            contentDescription = null,
            modifier = Modifier.size(MaterialTheme.sizes.illustration),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(page.title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(text = stringResource(page.body), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PageIndicator(pagerState: PagerState) {
    val description = stringResource(R.string.onboarding_page_indicator, pagerState.currentPage + 1, Pages.size)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm, Alignment.CenterHorizontally)
    ) {
        repeat(Pages.size) { index ->
            val color = if (index == pagerState.currentPage) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
            Box(
                modifier = Modifier
                    .size(MaterialTheme.sizes.pageIndicator)
                    .background(color = color, shape = CircleShape)
            )
        }
    }
}

@Composable
private fun OnboardingButtons(isLastPage: Boolean, onAction: (OnboardingAction) -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = { onAction(OnboardingAction.SkipClicked) }) {
            Text(text = stringResource(R.string.onboarding_skip))
        }
        if (isLastPage) {
            Button(onClick = { onAction(OnboardingAction.StartClicked) }) {
                Text(text = stringResource(R.string.onboarding_start))
            }
        } else {
            Button(onClick = onNext) {
                Text(text = stringResource(R.string.onboarding_next))
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun OnboardingFirstPagePreview() {
    CarteraTheme {
        OnboardingContent(onAction = {})
    }
}

@PreviewLightDark
@Composable
private fun OnboardingLastPagePreview() {
    CarteraTheme {
        OnboardingContent(onAction = {}, initialPage = Pages.lastIndex)
    }
}
