package com.example.roomie.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.roomie.data.FakeRoomieRepository
import com.example.roomie.ui.chores.ChoresScreen
import com.example.roomie.ui.chores.ChoreEditorScreen
import com.example.roomie.ui.components.RoomieBottomBar
import com.example.roomie.ui.components.RoomieTab
import com.example.roomie.ui.home.HomeScreen
import com.example.roomie.ui.notes.NoteBoardScreen
import com.example.roomie.ui.onboarding.AddRoommatesScreen
import com.example.roomie.ui.onboarding.PickBuddyScreen
import com.example.roomie.ui.onboarding.SetUpHouseScreen
import com.example.roomie.ui.onboarding.SetupChoiceScreen
import com.example.roomie.ui.onboarding.SignInScreen
import com.example.roomie.ui.profile.MeScreen
import com.example.roomie.ui.shopping.ShoppingScreen
import com.example.roomie.ui.theme.Background

private object Routes {
    const val SIGN_IN = "signin"
    const val BUDDY = "buddy?join={join}"
    const val EDIT_BUDDY = "buddy/edit"
    const val SETUP = "setup?join={join}"
    const val ROOMMATES = "roommates"
    const val HOUSE = "house"
    const val HOME = "home"
    const val CHORES = "chores"
    const val NEW_CHORE = "chores/new"
    const val EDIT_CHORE = "chores/edit/{id}"
    const val NOTES = "notes?compose={compose}"
    const val SHOPPING = "shopping"
    const val ME = "me"

    fun buddy(join: Boolean) = "buddy?join=$join"
    fun setup(join: Boolean) = "setup?join=$join"
    fun notes(compose: Boolean = false) = "notes?compose=$compose"
    fun editChore(id: String) = "chores/edit/$id"
}

private const val COMPOSE_HANDLED = "composeHandled"

private val TAB_ROUTES = mapOf(
    Routes.HOME to RoomieTab.Home,
    Routes.CHORES to RoomieTab.Chores,
    Routes.NOTES to RoomieTab.Notes,
    Routes.SHOPPING to RoomieTab.Shopping,
)

@Composable
fun RoomieApp() {
    val nav = rememberNavController()
    val start = remember {
        val session = FakeRoomieRepository.state.value.session
        when {
            session == null -> Routes.SIGN_IN
            !session.hasHouse -> Routes.SETUP
            else -> Routes.HOME
        }
    }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = route in TAB_ROUTES.keys || route == Routes.ME

    Scaffold(
        containerColor = Background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBar) RoomieBottomBar(selected = TAB_ROUTES[route], onSelect = { nav.openTab(it) })
        },
    ) { padding ->
        NavHost(nav, startDestination = start, modifier = Modifier.padding(padding)) {
            composable(Routes.SIGN_IN) {
                SignInScreen(onSignedIn = { hasHouse, join ->
                    if (hasHouse) nav.resetTo(Routes.HOME) else nav.navigate(Routes.buddy(join))
                })
            }
            composable(Routes.BUDDY, arguments = listOf(navArgument("join") { type = NavType.BoolType; defaultValue = false })) {
                val join = it.arguments?.getBoolean("join") ?: false
                PickBuddyScreen(editing = false, onNext = { nav.navigate(Routes.setup(join)) }, onBack = { nav.popBackStack() })
            }
            composable(Routes.EDIT_BUDDY) {
                PickBuddyScreen(editing = true, onNext = { nav.popBackStack() }, onBack = { nav.popBackStack() })
            }
            composable(Routes.SETUP, arguments = listOf(navArgument("join") { type = NavType.BoolType; defaultValue = false })) {
                SetupChoiceScreen(
                    preferJoin = it.arguments?.getBoolean("join") ?: false,
                    onStartHouse = { nav.navigate(Routes.ROOMMATES) },
                    onJoined = { nav.resetTo(Routes.HOME) },
                    onBack = if (nav.previousBackStackEntry != null) ({ nav.popBackStack() }) else null,
                )
            }
            composable(Routes.ROOMMATES) {
                AddRoommatesScreen(onNext = { nav.navigate(Routes.HOUSE) }, onBack = { nav.popBackStack() })
            }
            composable(Routes.HOUSE) {
                SetUpHouseScreen(onEnter = { nav.resetTo(Routes.HOME) }, onBack = { nav.popBackStack() })
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenSettings = { nav.navigate(Routes.ME) { launchSingleTop = true } },
                    onOpenBoard = { nav.openTab(RoomieTab.Notes) },
                    onAddNote = { nav.openTab(RoomieTab.Notes, Routes.notes(compose = true)) },
                    onOpenChores = { nav.openTab(RoomieTab.Chores) },
                )
            }
            composable(Routes.CHORES) {
                ChoresScreen(
                    onOpenSettings = { nav.navigate(Routes.ME) { launchSingleTop = true } },
                    onAddChore = { nav.navigate(Routes.NEW_CHORE) },
                    onEditChore = { id -> nav.navigate(Routes.editChore(id)) },
                )
            }
            composable(Routes.NEW_CHORE) {
                ChoreEditorScreen(choreId = null, onDone = { nav.popBackStack() })
            }
            composable(Routes.EDIT_CHORE, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                ChoreEditorScreen(choreId = it.arguments?.getString("id"), onDone = { nav.popBackStack() })
            }
            composable(Routes.NOTES, arguments = listOf(navArgument("compose") { type = NavType.BoolType; defaultValue = false })) {
                // The handled flag lives in savedStateHandle so it survives the tab being saved and
                // restored; otherwise coming back to Notes would reopen the composer.
                NoteBoardScreen(
                    startComposing = (it.arguments?.getBoolean("compose") ?: false) &&
                        it.savedStateHandle.get<Boolean>(COMPOSE_HANDLED) != true,
                    onComposeHandled = { it.savedStateHandle[COMPOSE_HANDLED] = true },
                    onOpenSettings = { nav.navigate(Routes.ME) { launchSingleTop = true } },
                )
            }
            composable(Routes.SHOPPING) {
                ShoppingScreen(onOpenSettings = { nav.navigate(Routes.ME) { launchSingleTop = true } })
            }
            composable(Routes.ME) {
                MeScreen(
                    onChangePic = { nav.navigate(Routes.EDIT_BUDDY) },
                    onSignedOut = { nav.resetTo(Routes.SIGN_IN) },
                    onLeftHouse = { nav.resetTo(Routes.setup(false)) },
                )
            }
        }
    }
}

private fun NavHostController.openTab(tab: RoomieTab, route: String? = null) {
    // Me sits on top of whichever tab opened it; close it so it isn't saved as part of that tab.
    if (currentDestination?.route == Routes.ME) popBackStack()
    if (tab == RoomieTab.Home) {
        // Home is the root and never leaves the stack, so just pop back to it. Navigating to it with
        // restoreState would bring back whatever tab was saved when popping up to Home.
        popBackStack(Routes.HOME, inclusive = false, saveState = true)
        return
    }
    val target = route ?: when (tab) {
        RoomieTab.Home -> Routes.HOME
        RoomieTab.Chores -> Routes.CHORES
        RoomieTab.Notes -> Routes.notes()
        RoomieTab.Shopping -> Routes.SHOPPING
    }
    navigate(target) {
        // Tabs sit on top of Home, so Back from any tab returns Home and Back from Home exits.
        popUpTo(Routes.HOME) { saveState = route == null }
        launchSingleTop = true
        restoreState = route == null
    }
}

/** Clears the back stack, used when crossing between onboarding and the house. */
private fun NavHostController.resetTo(route: String) {
    navigate(route) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}
