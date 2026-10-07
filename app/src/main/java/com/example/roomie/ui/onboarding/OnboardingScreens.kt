package com.example.roomie.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roomie.R
import com.example.roomie.data.Buddy
import com.example.roomie.data.MemberStatus
import com.example.roomie.ui.components.BuddyAvatar
import com.example.roomie.ui.components.ErrorText
import com.example.roomie.ui.components.FieldLabel
import com.example.roomie.ui.components.IconAction
import com.example.roomie.ui.components.InlineAddButton
import com.example.roomie.ui.components.InviteCodeCard
import com.example.roomie.ui.components.LabeledField
import com.example.roomie.ui.components.MemberStatusPill
import com.example.roomie.ui.components.PrimaryButton
import com.example.roomie.ui.components.RoomieTextField
import com.example.roomie.ui.components.RoommateRow
import com.example.roomie.ui.components.ShareInviteButton
import com.example.roomie.ui.components.TextAction
import com.example.roomie.ui.theme.Background
import com.example.roomie.ui.theme.Border
import com.example.roomie.ui.theme.BrandBrown
import com.example.roomie.ui.theme.Divider
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.ShadowBrown
import com.example.roomie.ui.theme.Surface
import com.example.roomie.ui.theme.TabIcon
import com.example.roomie.ui.theme.TextSecondary

// region Shared onboarding layout

private const val STEPS = 4

/** Back arrow, 4-step progress, title and a bottom-pinned primary button. */
@Composable
private fun OnboardingScaffold(
    step: Int?,
    title: String,
    subtitle: String?,
    onBack: (() -> Unit)?,
    bottom: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 20.dp, end = 20.dp, top = 7.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(44.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                Box(Modifier.width(34.dp)) {
                    IconAction(R.drawable.ic_back, "Back", onBack, Modifier.offset(x = (-10).dp))
                }
            }
            if (step != null) StepProgress(step, Modifier.weight(1f))
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = if (subtitle == null) RoomieType.onboardingTitle.copy(fontSize = 28.sp, letterSpacing = 0.sp) else RoomieType.onboardingTitle, modifier = Modifier.semantics { heading() })
            if (subtitle != null) Text(subtitle, style = RoomieType.onboardingSubtitle)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
        bottom()
    }
}

@Composable
private fun StepProgress(step: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.semantics {
            contentDescription = "Step $step of $STEPS"
            progressBarRangeInfo = ProgressBarRangeInfo(step.toFloat(), 0f..STEPS.toFloat(), STEPS)
        },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(STEPS) { i ->
            Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(if (i < step) BrandBrown else Divider))
        }
    }
}

// endregion

// region 1. Sign in

@Composable
fun SignInScreen(onSignedIn: (hasHouse: Boolean, wantsToJoin: Boolean) -> Unit, vm: OnboardingViewModel = viewModel()) {
    var netId by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    fun submit(wantsToJoin: Boolean) {
        error = vm.signIn(netId, password) { hasHouse -> onSignedIn(hasHouse, wantsToJoin) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 43.dp, bottom = 20.dp),
    ) {
        Box(
            Modifier
                .size(64.dp)
                .shadow(5.dp, RoundedCornerShape(18.dp), ambientColor = ShadowBrown.copy(alpha = 0.3f), spotColor = ShadowBrown.copy(alpha = 0.3f))
                .background(Surface, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_house_logo), null, Modifier.size(34.dp), tint = Color.Unspecified)
        }
        Spacer(Modifier.height(15.dp))
        Text("Welcome to Roomie", style = RoomieType.welcomeTitle, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(8.dp))
        Text("Sign in with your Cornell NetID", style = RoomieType.input.copy(color = TextSecondary))
        Spacer(Modifier.height(24.dp))
        LabeledField("NetID") {
            RoomieTextField(
                value = netId,
                onValueChange = { netId = it.trim(); error = null },
                placeholder = "e.g. lk452",
                suffix = "@cornell.edu",
                isError = error != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Next, autoCorrectEnabled = false),
                contentDescription = "Cornell NetID",
            )
        }
        Spacer(Modifier.height(16.dp))
        LabeledField("Password") {
            RoomieTextField(
                value = password,
                onValueChange = { password = it; error = null },
                placeholder = "Password",
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { submit(false) }),
                contentDescription = "Password",
            )
        }
        ErrorText(error, Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(20.dp))
        PrimaryButton("Sign in", { submit(false) })
        Spacer(Modifier.height(16.dp))
        Text(
            "Only Cornell accounts can join a house, so you always know who you're living with.",
            style = RoomieType.bodySecondary.copy(lineHeight = 18.85.sp),
        )
        Spacer(Modifier.weight(1f).height(32.dp))
        val shape = RoundedCornerShape(10.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Surface)
                .border(1.dp, Border, shape)
                .clickable(role = Role.Button, onClickLabel = "Sign in and join with an invite code") { submit(true) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Got an invite code?", style = RoomieType.sectionTitle.copy(lineHeight = 19.sp))
                Text("Join your roommates' house directly", style = RoomieType.bodySecondary)
            }
            Icon(painterResource(R.drawable.ic_chevron_right_20), null, Modifier.size(20.dp), tint = Color.Unspecified)
        }
    }
}

// endregion

// region 2. Pick your house buddy

@Composable
fun PickBuddyScreen(
    editing: Boolean,
    onNext: () -> Unit,
    onBack: (() -> Unit)?,
    vm: OnboardingViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val session = state.session ?: return
    var picked by rememberSaveable { mutableStateOf(session.buddy) }

    OnboardingScaffold(
        step = if (editing) null else 1,
        title = "Pick your house buddy",
        subtitle = "Roommates see it next to your name.",
        onBack = onBack,
        bottom = {
            PrimaryButton(if (editing) "Save" else "Next", { vm.pickBuddy(picked); onNext() })
            if (!editing) {
                TextAction("Skip for now", onNext, Modifier.align(Alignment.CenterHorizontally).offset(y = (-6).dp))
            }
        },
    ) {
        Column(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BuddyAvatar(picked, 112.dp, contentDescription = "Your buddy: ${picked.label}")
            Text(session.firstName, style = RoomieType.sectionTitle.copy(fontSize = 17.sp))
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(Buddy.entries.take(3), Buddy.entries.drop(3)).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(19.dp, Alignment.CenterHorizontally)) {
                    row.forEach { buddy -> BuddyOption(buddy, buddy == picked) { picked = buddy } }
                }
            }
        }
        Text(
            "You can change it any time in Me.",
            style = RoomieType.bodySecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun BuddyOption(buddy: Buddy, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .width(104.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(88.dp)) {
            Box(
                Modifier
                    .size(88.dp)
                    .border(3.dp, if (selected) BrandBrown else Color.Transparent, CircleShape)
                    .padding(6.dp),
            ) {
                BuddyAvatar(buddy, 76.dp)
            }
            // Sibling of the ring so the ring's border never paints over the check.
            if (selected) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-2).dp, y = (-2).dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(BrandBrown)
                        .border(2.dp, Background, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_check_small), null, Modifier.size(14.dp), tint = Color.Unspecified)
                }
            }
        }
        Text(
            buddy.label,
            style = RoomieType.rowTitle.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
        )
    }
}

// endregion

// region 3. How are you setting up?

@Composable
fun SetupChoiceScreen(
    preferJoin: Boolean,
    onStartHouse: () -> Unit,
    onJoined: () -> Unit,
    onBack: (() -> Unit)?,
    vm: OnboardingViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val session = state.session ?: return
    var join by rememberSaveable { mutableStateOf(preferJoin) }
    var code by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    OnboardingScaffold(
        step = 2,
        title = "Hi ${session.firstName},",
        subtitle = "how are you setting up?",
        onBack = onBack,
        bottom = {
            PrimaryButton("Next", {
                if (join) {
                    error = vm.joinHouse(code)
                    if (error == null) onJoined()
                } else {
                    vm.startHouse()
                    onStartHouse()
                }
            })
        },
    ) {
        ChoiceCard(
            selected = !join,
            title = "Start a house",
            body = "Name it, then add your roommates by NetID.",
            onSelect = { join = false; error = null },
        )
        ChoiceCard(
            selected = join,
            title = "Join with an invite code",
            body = "A roommate already started the house? Enter their code and skip ahead.",
            onSelect = { join = true },
        ) {
            Column(Modifier.padding(start = 36.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel("Invite code")
                RoomieTextField(
                    value = code,
                    onValueChange = { code = it.uppercase(); join = true; error = null },
                    placeholder = "LOFT-0000",
                    background = Background,
                    textStyle = RoomieType.input.copy(letterSpacing = 0.96.sp),
                    isError = error != null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false, imeAction = ImeAction.Done),
                    contentDescription = "Invite code",
                )
                ErrorText(error)
            }
        }
    }
}

@Composable
private fun ChoiceCard(
    selected: Boolean,
    title: String,
    body: String,
    onSelect: () -> Unit,
    extra: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) BrandBrown else Border, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .padding(top = 3.dp)
                    .size(22.dp)
                    .border(1.dp, if (selected) BrandBrown else TabIcon, CircleShape)
                    .background(Surface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Box(Modifier.size(13.2.dp).background(BrandBrown, CircleShape))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = RoomieType.sectionTitle.copy(fontSize = 17.sp))
                Text(body, style = RoomieType.body.copy(color = TextSecondary, lineHeight = 19.6.sp))
            }
        }
        extra?.invoke()
    }
}

// endregion

// region 4. Add roommates

@Composable
fun AddRoommatesScreen(onNext: () -> Unit, onBack: () -> Unit, vm: OnboardingViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val house = state.house ?: return
    val me = state.session?.userId
    var netId by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    fun add() {
        error = vm.invite(netId)
        if (error == null) netId = ""
    }

    OnboardingScaffold(
        step = 3,
        title = "Amazing!",
        subtitle = "Add your roommates' NetIDs.",
        onBack = onBack,
        bottom = { PrimaryButton("Next", onNext) },
    ) {
        house.members.filter { it.id != me }.forEach { roommate ->
            val shape = RoundedCornerShape(10.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(Surface)
                    .border(1.dp, Divider, shape)
                    .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BuddyAvatar(roommate.buddy, 40.dp)
                Column(Modifier.weight(1f)) {
                    Text(roommate.firstName, style = RoomieType.sectionTitle.copy(fontSize = 15.sp, lineHeight = 18.sp))
                    Text(roommate.netId, style = RoomieType.bodySecondary)
                }
                MemberStatusPill(roommate.status, pendingLabel = "Invite sent")
                IconAction(R.drawable.ic_close, "Remove ${roommate.firstName}", { vm.removeRoommate(roommate.id) }, iconSize = 18.dp)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
            LabeledField("Roommate NetID", Modifier.weight(1f)) {
                RoomieTextField(
                    value = netId,
                    onValueChange = { netId = it.trim(); error = null },
                    placeholder = "e.g. ab123",
                    isError = error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, autoCorrectEnabled = false, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { add() }),
                    contentDescription = "Roommate NetID",
                )
            }
            InlineAddButton(::add, height = 50.dp, corner = 10.dp, enabled = netId.isNotBlank())
        }
        ErrorText(error)
        Text(
            "They get an email to join and pick their own house buddy. You can keep going. The house updates as they accept.",
            style = RoomieType.bodySecondary.copy(lineHeight = 18.2.sp),
        )
    }
}

// endregion

// region 5. Set up your house

@Composable
fun SetUpHouseScreen(onEnter: () -> Unit, onBack: () -> Unit, vm: OnboardingViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val house = state.house ?: return
    val me = state.session?.userId
    var name by rememberSaveable { mutableStateOf(house.name) }

    OnboardingScaffold(
        step = 4,
        title = "Set up your house",
        subtitle = null,
        onBack = onBack,
        bottom = {
            PrimaryButton("Enter your house", { vm.renameHouse(name); vm.finish(); onEnter() }, enabled = name.isNotBlank())
        },
    ) {
        LabeledField("House name") {
            RoomieTextField(
                value = name,
                onValueChange = { name = it; vm.renameHouse(it) },
                placeholder = "e.g. The Linden Loft",
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                contentDescription = "House name",
            )
        }
        InviteCodeCard(house.inviteCode)
        ShareInviteButton(house.name, house.inviteCode)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Roommates", style = RoomieType.sectionTitle, modifier = Modifier.padding(bottom = 4.dp))
            house.members.forEach { roommate ->
                RoommateRow(roommate, isMe = roommate.id == me) {
                    MemberStatusPill(roommate.status)
                }
            }
            if (house.members.none { it.status != MemberStatus.Owner }) {
                Text("No roommates yet. Share the invite code to add them.", style = RoomieType.bodySecondary)
            }
        }
    }
}

// endregion
