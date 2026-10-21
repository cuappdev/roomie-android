package com.example.roomie.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import com.example.roomie.ui.components.glassSurface
import com.example.roomie.ui.theme.Background
import com.example.roomie.ui.theme.BrandBrown
import com.example.roomie.ui.theme.Divider
import com.example.roomie.ui.theme.RoomieType
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
    titleStyle: TextStyle = RoomieType.onboardingTitle,
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
            Text(title, style = titleStyle, modifier = Modifier.semantics { heading() })
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
fun SignInScreen(onSignedIn: (hasHouse: Boolean) -> Unit, vm: OnboardingViewModel = viewModel()) {
    var netId by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    fun submit() {
        error = vm.signIn(netId, password, onSignedIn)
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
        Image(painterResource(R.drawable.ic_brand_mark), null, Modifier.size(64.dp))
        Spacer(Modifier.height(15.dp))
        Text("Welcome to Roomie", style = RoomieType.welcomeTitle, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(8.dp))
        Text("Sign in with your Cornell NetID", style = RoomieType.input.copy(color = TextSecondary))
        Spacer(Modifier.height(18.dp))
        LabeledField("NetID", labelStyle = RoomieType.onboardingLabel) {
            RoomieTextField(
                value = netId,
                onValueChange = { netId = it.trim(); error = null },
                placeholder = "e.g. lk452",
                suffix = "@cornell.edu",
                suffixStyle = RoomieType.input.copy(color = TextSecondary, lineHeight = 20.sp),
                isError = error != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Next, autoCorrectEnabled = false),
                contentDescription = "Cornell NetID",
            )
        }
        Spacer(Modifier.height(16.dp))
        LabeledField("Password", labelStyle = RoomieType.onboardingLabel) {
            RoomieTextField(
                value = password,
                onValueChange = { password = it; error = null },
                placeholder = "Password",
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { submit() }),
                contentDescription = "Password",
            )
        }
        ErrorText(error, Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(20.dp))
        PrimaryButton("Sign In", ::submit, gradient = true)
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
        title = "Pick Your House Buddy",
        subtitle = "Roommates see it next to your name.",
        onBack = onBack,
        titleStyle = RoomieType.onboardingTitle.copy(fontWeight = FontWeight.Bold, lineHeight = 32.sp),
        bottom = {
            PrimaryButton(if (editing) "Save" else "Next", { vm.pickBuddy(picked); onNext() }, gradient = true)
            if (!editing) {
                // Skipping assigns a random buddy; it can be changed any time from Me.
                TextAction(
                    "Skip for Now",
                    { vm.pickBuddy(Buddy.entries.random()); onNext() },
                    Modifier.align(Alignment.CenterHorizontally).offset(y = (-6).dp),
                    style = RoomieType.linkButton.copy(fontSize = 16.sp, lineHeight = 20.sp),
                )
            }
        },
    ) {
        Column(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BuddyAvatar(picked, 88.dp, contentDescription = "Your buddy: ${picked.label}")
            Text(session.firstName, style = RoomieType.sectionTitle.copy(lineHeight = 20.sp))
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(Buddy.entries.take(3), Buddy.entries.drop(3)).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(19.dp, Alignment.CenterHorizontally)) {
                    row.forEach { buddy -> BuddyOption(buddy, buddy == picked) { picked = buddy } }
                }
            }
        }
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
                    .border(2.dp, if (selected) BrandBrown else Color.Transparent, CircleShape)
                    .padding(6.dp),
            ) {
                BuddyAvatar(buddy, 76.dp)
            }
            // Sibling of the ring so the ring's border never paints over the check.
            if (selected) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-5).dp, y = (-5).dp)
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
            style = RoomieType.rowTitle.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
        )
    }
}

// endregion

// region 3. How are you setting up?

@Composable
fun SetupChoiceScreen(
    onStartHouse: () -> Unit,
    onJoined: () -> Unit,
    onBack: (() -> Unit)?,
    vm: OnboardingViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val session = state.session ?: return
    var join by rememberSaveable { mutableStateOf(false) }
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
            }, gradient = true)
        },
    ) {
        ChoiceCard(
            selected = !join,
            title = "Start a House",
            body = "Name it, then add your roommates by NetID.",
            onSelect = { join = false; error = null },
        )
        ChoiceCard(
            selected = join,
            title = "Join with an Invite Code",
            body = "A roommate already started the house? Enter their code and skip ahead.",
            onSelect = { join = true },
        ) {
            Column(Modifier.padding(start = 36.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel("Invite Code", style = RoomieType.onboardingLabel)
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
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .glassSurface(shape, borderWidth = if (selected) 2.dp else 1.dp)
            .padding(17.dp),
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
                Text(title, style = RoomieType.sectionTitle.copy(lineHeight = 20.sp))
                Text(body, style = RoomieType.onboardingBody)
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
        title = "Exciting!",
        subtitle = "Add your roommates' NetIDs.",
        onBack = onBack,
        bottom = { PrimaryButton("Next", onNext, gradient = true) },
    ) {
        house.members.filter { it.id != me }.forEach { roommate ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .glassSurface(RoundedCornerShape(14.dp))
                    .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BuddyAvatar(roommate.buddy, 40.dp)
                Column(Modifier.weight(1f)) {
                    Text(roommate.firstName, style = RoomieType.sectionTitle.copy(lineHeight = 20.sp))
                    Text(roommate.netId, style = RoomieType.onboardingNote)
                }
                MemberStatusPill(roommate.status, pendingLabel = "Invite sent")
                IconAction(R.drawable.ic_close, "Remove ${roommate.firstName}", { vm.removeRoommate(roommate.id) }, iconSize = 18.dp)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
            LabeledField("Roommate NetID", Modifier.weight(1f), labelStyle = RoomieType.onboardingLabel) {
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
            InlineAddButton(
                ::add,
                height = 50.dp,
                corner = 12.dp,
                enabled = netId.isNotBlank(),
                gradient = true,
                textStyle = RoomieType.primaryButton.copy(lineHeight = 20.sp),
            )
        }
        ErrorText(error)
        Text(
            "They get an email to join and pick their own house buddy. You can keep going. The house updates as they accept.",
            style = RoomieType.onboardingNote,
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
        title = "Set Up Your House",
        subtitle = null,
        onBack = onBack,
        titleStyle = RoomieType.onboardingTitle.copy(letterSpacing = 0.sp),
        bottom = {
            PrimaryButton("Enter Your House", { vm.renameHouse(name); vm.finish(); onEnter() }, enabled = name.isNotBlank(), gradient = true)
        },
    ) {
        LabeledField("House Name", labelStyle = RoomieType.onboardingLabel) {
            RoomieTextField(
                value = name,
                onValueChange = { name = it; vm.renameHouse(it) },
                placeholder = "e.g. The Linden Loft",
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                contentDescription = "House name",
            )
        }
        InviteCodeCard(house.inviteCode, label = "Invite Code", glass = true)
        ShareInviteButton(house.name, house.inviteCode, text = "Share Invite Link", glass = true)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Roommates", style = RoomieType.sectionTitle, modifier = Modifier.padding(bottom = 4.dp))
            house.members.forEach { roommate ->
                RoommateRow(roommate, isMe = roommate.id == me, nameStyle = RoomieType.listTitle.copy(fontSize = 16.sp, lineHeight = 20.sp)) {
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
