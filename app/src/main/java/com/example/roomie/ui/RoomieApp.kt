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
import com.example.roomie.ui.chores.ChoreDetailScreen
import com.example.roomie.ui.chores.ChoresScreen
import com.example.roomie.ui.chores.EditRotationScreen
import com.example.roomie.ui.chores.ManageRotationsScreen
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
    const val BUDDY = "buddy"
    const val EDIT_BUDDY = "buddy/edit"
    const val SETUP = "setup"
    const val ROOMMATES = "roommates"
    const val HOUSE = "house"
    const val HOME = "home"
    const val CHORES = "chores"
    const val CHORE = "chores/chore/{id}"
    const val ROTATIONS = "chores/rotations"
    const val ROTATION = "chores/rotations/{id}"
    const val NOTES = "notes?compose={compose}"
    const val SHOPPING = "shopping"
    const val ME = "me"

    fun notes(compose: Boolean = false) = "notes?compose=$compose"
    fun chore(id: String) = "chores/chore/$id"
    fun rotation(id: String) = "chores/rotations/$id"
}

private const val COMPOSE_HANDLED = "composeHandled"

private val TAB_ROUTES = mapOf(
    Routes.HOME to RoomieTab.Home,
    Routes.CHORES to RoomieTab.Chores,
    Routes.CHORE to RoomieTab.Chores,
    Routes.ROTATIONS to RoomieTab.Chores,
    Routes.ROTATION to RoomieTab.Chores,
    Routes.NOTES to RoomieTab.Notes,
    Routes.SHOPPING to RoomieTab.Shopping,
    Routes.ME to RoomieTab.Profile,
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
    val showBar = route in TAB_ROUTES.keys

    Scaffold(
        containerColor = Background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBar) RoomieBottomBar(selected = TAB_ROUTES[route], onSelect = { nav.openTab(it) })
        },
    ) { padding ->
        NavHost(nav, startDestination = start, modifier = Modifier.padding(padding)) {
            composable(Routes.SIGN_IN) {
                SignInScreen(onSignedIn = { hasHouse ->
                    if (hasHouse) nav.resetTo(Routes.HOME) else nav.navigate(Routes.BUDDY)
                })
            }
            composable(Routes.BUDDY) {
                PickBuddyScreen(editing = false, onNext = { nav.navigate(Routes.SETUP) }, onBack = { nav.popBackStack() })
            }
            composable(Routes.EDIT_BUDDY) {
                PickBuddyScreen(editing = true, onNext = { nav.popBackStack() }, onBack = { nav.popBackStack() })
            }
            composable(Routes.SETUP) {
                SetupChoiceScreen(
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
                    onOpenSettings = { nav.openTab(RoomieTab.Profile) },
                    onOpenBoard = { nav.openTab(RoomieTab.Notes) },
                    onOpenChores = { nav.openTab(RoomieTab.Chores) },
                    onOpenChore = { id -> nav.navigate(Routes.chore(id)) },
                )
            }
            composable(Routes.CHORES) {
                ChoresScreen(
                    onOpenSettings = { nav.openTab(RoomieTab.Profile) },
                    onOpenChore = { id -> nav.navigate(Routes.chore(id)) },
                    onManageRotations = { nav.navigate(Routes.ROTATIONS) },
                )
            }
            composable(Routes.CHORE, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                ChoreDetailScreen(
                    choreId = it.arguments?.getString("id").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onViewRotation = { id -> nav.navigate(Routes.rotation(id)) },
                )
            }
            composable(Routes.ROTATIONS) {
                ManageRotationsScreen(onBack = { nav.popBackStack() }, onOpenRotation = { id -> nav.navigate(Routes.rotation(id)) })
            }
            composable(Routes.ROTATION, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                EditRotationScreen(rotationId = it.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() })
            }
            composable(Routes.NOTES, arguments = listOf(navArgument("compose") { type = NavType.BoolType; defaultValue = false })) {
                // The handled flag lives in savedStateHandle so it survives the tab being saved and
                // restored; otherwise coming back to Notes would reopen the composer.
                NoteBoardScreen(
                    startComposing = (it.arguments?.getBoolean("compose") ?: false) &&
                        it.savedStateHandle.get<Boolean>(COMPOSE_HANDLED) != true,
                    onComposeHandled = { it.savedStateHandle[COMPOSE_HANDLED] = true },
                    onOpenSettings = { nav.openTab(RoomieTab.Profile) },
                )
            }
            composable(Routes.SHOPPING) {
                ShoppingScreen(onOpenSettings = { nav.openTab(RoomieTab.Profile) })
            }
            composable(Routes.ME) {
                MeScreen(
                    onChangePic = { nav.navigate(Routes.EDIT_BUDDY) },
                    onSignedOut = { nav.resetTo(Routes.SIGN_IN) },
                    onLeftHouse = { nav.resetTo(Routes.SETUP) },
                )
            }
        }
    }
}

private fun NavHostController.openTab(tab: RoomieTab, route: String? = null) {
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
        RoomieTab.Profile -> Routes.ME
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
