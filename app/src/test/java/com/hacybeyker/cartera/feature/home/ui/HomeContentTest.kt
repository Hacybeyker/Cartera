package com.hacybeyker.cartera.feature.home.ui

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.ui.preview.previewAccounts
import com.hacybeyker.cartera.feature.home.domain.model.CoachMark
import com.hacybeyker.cartera.feature.home.ui.coach.CoachMarksTestTags
import com.hacybeyker.cartera.feature.home.ui.menu.HomeMenuItem
import com.hacybeyker.cartera.feature.home.ui.overview.AccountsTabActions
import com.hacybeyker.cartera.feature.home.ui.overview.OverviewContent
import com.hacybeyker.cartera.feature.home.ui.overview.OverviewTestTags
import com.hacybeyker.cartera.feature.home.ui.overview.OverviewUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class HomeContentTest {
    @get:Rule
    val compose = createComposeRule()

    private val loaded = OverviewUiState(isLoading = false, firstName = "Ana", accounts = previewAccounts)

    @Test
    fun `the coach layer blocks the screen below and sends next and skip`() {
        val actions = mutableListOf<HomeAction>()
        var transfers = 0
        var menuClicks = 0
        compose.setContent {
            CarteraTheme(dynamicColor = false) {
                HomeContent(
                    selectedTab = HomeTab.START,
                    onTabSelected = {},
                    uiState = HomeUiState(coachMark = CoachMark.TRANSFER),
                    onAction = { actions += it }
                ) {
                    OverviewContent(
                        uiState = loaded,
                        onAction = {},
                        accountsActions = AccountsTabActions(onAccountClick = {}, onTransferClick = { transfers++ }),
                        onMenuClick = { menuClicks++ }
                    ) {}
                }
            }
        }

        compose.onNodeWithTag(CoachMarksTestTags.LAYER).assertIsDisplayed()
        compose.onNodeWithTag(CoachMarksTestTags.step(1)).assertIsDisplayed()
        compose.onNodeWithTag(OverviewTestTags.TRANSFER).performClick()
        compose.onNodeWithTag(HomeTestTags.MENU).performClick()
        compose.onNodeWithTag(CoachMarksTestTags.NEXT).performClick()
        compose.onNodeWithTag(CoachMarksTestTags.SKIP).performClick()

        assertEquals(0, transfers)
        assertEquals(0, menuClicks)
        assertEquals(listOf(HomeAction.CoachNextClicked, HomeAction.CoachSkipClicked), actions)
    }

    @Test
    fun `the last step says understood with the same tag`() {
        compose.setContent {
            CarteraTheme(dynamicColor = false) {
                HomeContent(
                    selectedTab = HomeTab.START,
                    onTabSelected = {},
                    uiState = HomeUiState(coachMark = CoachMark.MENU)
                ) {}
            }
        }

        compose.onNodeWithTag(CoachMarksTestTags.step(3)).assertIsDisplayed()
        compose.onNodeWithTag(CoachMarksTestTags.NEXT).assertTextEquals("Entendido")
    }

    @Test
    fun `each menu item is reported`() {
        val items = mutableListOf<HomeMenuItem>()
        compose.setContent {
            CarteraTheme(dynamicColor = false) {
                HomeContent(
                    selectedTab = HomeTab.START,
                    onTabSelected = {},
                    drawerState = rememberDrawerState(DrawerValue.Open),
                    onMenuItemClick = { items += it }
                ) {
                    OverviewContent(
                        uiState = loaded,
                        onAction = {},
                        accountsActions = AccountsTabActions(onAccountClick = {}, onTransferClick = {}),
                        onMenuClick = {}
                    ) {}
                }
            }
        }

        HomeMenuItem.entries.forEach { compose.onNodeWithTag(it.testTag).performClick() }

        assertEquals(HomeMenuItem.entries, items)
    }

    @Test
    fun `the menu button is reported without coach marks`() {
        var menuClicks = 0
        compose.setContent {
            CarteraTheme(dynamicColor = false) {
                HomeContent(selectedTab = HomeTab.START, onTabSelected = {}) {
                    OverviewContent(
                        uiState = loaded,
                        onAction = {},
                        accountsActions = AccountsTabActions(onAccountClick = {}, onTransferClick = {}),
                        onMenuClick = { menuClicks++ }
                    ) {}
                }
            }
        }

        compose.onNodeWithTag(HomeTestTags.MENU).performClick()

        assertEquals(1, menuClicks)
    }
}
