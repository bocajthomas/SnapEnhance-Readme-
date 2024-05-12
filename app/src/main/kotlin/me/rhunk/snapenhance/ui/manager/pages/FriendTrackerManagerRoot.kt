package me.rhunk.snapenhance.ui.manager.pages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rhunk.snapenhance.common.bridge.wrapper.TrackerLog
import me.rhunk.snapenhance.common.data.*
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableState
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableStateList
import me.rhunk.snapenhance.common.ui.rememberAsyncUpdateDispatcher
import me.rhunk.snapenhance.common.util.snap.BitmojiSelfie
import me.rhunk.snapenhance.storage.*
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.manager.pages.social.AddFriendDialog
import me.rhunk.snapenhance.ui.util.coil.BitmojiImage
import me.rhunk.snapenhance.ui.util.pagerTabIndicatorOffset
import java.text.DateFormat


@OptIn(ExperimentalFoundationApi::class)
class FriendTrackerManagerRoot : Routes.Route() {
    enum class FilterType {
        CONVERSATION, USERNAME, EVENT
    }

    private val titles = listOf("Logs", "Rules")
    private var currentPage by mutableIntStateOf(0)
    private var showAddRulePopup by mutableStateOf(false)

    override val floatingActionButton: @Composable () -> Unit = {
        if (currentPage == 1) {
            ExtendedFloatingActionButton(
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Rule") },
                expanded = true,
                text = { Text("Add Rule") },
                onClick = { showAddRulePopup = true }
            )
        }
        if (showAddRulePopup) {
            EditRuleDialog(onDismiss = { showAddRulePopup = false })
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun LogsTab() {
        val coroutineScope = rememberCoroutineScope()

        val logs = remember { mutableStateListOf<TrackerLog>() }
        var lastTimestamp by remember { mutableLongStateOf(Long.MAX_VALUE) }
        var filterType by remember { mutableStateOf(FilterType.USERNAME) }

        var filter by remember { mutableStateOf("") }
        var searchTimeoutJob by remember { mutableStateOf<Job?>(null) }

        suspend fun loadNewLogs() {
            withContext(Dispatchers.IO) {
                logs.addAll(context.messageLogger.getLogs(lastTimestamp, filter = {
                    when (filterType) {
                        FilterType.USERNAME -> it.username.contains(filter, ignoreCase = true)
                        FilterType.CONVERSATION -> it.conversationTitle?.contains(filter, ignoreCase = true) == true || (it.username == filter && !it.isGroup)
                        FilterType.EVENT -> it.eventType.contains(filter, ignoreCase = true)
                    }
                }).apply {
                    lastTimestamp = minOfOrNull { it.timestamp } ?: lastTimestamp
                })
            }
        }

        suspend fun resetAndLoadLogs() {
            logs.clear()
            lastTimestamp = Long.MAX_VALUE
            loadNewLogs()
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                var showAutoComplete by remember { mutableStateOf(false) }
                var dropDownExpanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = showAutoComplete,
                    onExpandedChange = { showAutoComplete = it },
                ) {
                    TextField(
                        value = filter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .padding(8.dp),
                        onValueChange = {
                            filter = it
                            coroutineScope.launch {
                                searchTimeoutJob?.cancel()
                                searchTimeoutJob = coroutineScope.launch {
                                    delay(200)
                                    showAutoComplete = true
                                    resetAndLoadLogs()
                                }
                            }
                        },
                        placeholder = { Text("Search") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        maxLines = 1,
                        leadingIcon = {
                            ExposedDropdownMenuBox(
                                expanded = dropDownExpanded,
                                onExpandedChange = { dropDownExpanded = it },
                            ) {
                                ElevatedCard(
                                    modifier = Modifier
                                        .menuAnchor()
                                        .padding(2.dp)
                                ) {
                                    Text(filterType.name, modifier = Modifier.padding(8.dp))
                                }
                                DropdownMenu(expanded = dropDownExpanded, onDismissRequest = {
                                    dropDownExpanded = false
                                }) {
                                    FilterType.entries.forEach { type ->
                                        DropdownMenuItem(onClick = {
                                            filter = ""
                                            filterType = type
                                            dropDownExpanded = false
                                            coroutineScope.launch {
                                                resetAndLoadLogs()
                                            }
                                        }, text = {
                                            Text(type.name)
                                        })
                                    }
                                }
                            }
                        },
                        trailingIcon = {
                            if (filter != "") {
                                IconButton(onClick = {
                                    filter = ""
                                    coroutineScope.launch {
                                        resetAndLoadLogs()
                                    }
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }

                            DropdownMenu(
                                expanded = showAutoComplete,
                                onDismissRequest = {
                                    showAutoComplete = false
                                },
                                properties = PopupProperties(focusable = false),
                            ) {
                                val suggestedEntries = remember(filter) {
                                    mutableStateListOf<String>()
                                }

                                LaunchedEffect(filter) {
                                    launch(Dispatchers.IO) {
                                        suggestedEntries.addAll(when (filterType) {
                                            FilterType.USERNAME -> context.messageLogger.findUsername(filter)
                                            FilterType.CONVERSATION -> context.messageLogger.findConversation(filter) + context.messageLogger.findUsername(filter)
                                            FilterType.EVENT -> TrackerEventType.entries.filter { it.name.contains(filter, ignoreCase = true) }.map { it.key }
                                        }.take(5))
                                    }
                                }

                                suggestedEntries.forEach { entry ->
                                    DropdownMenuItem(onClick = {
                                        filter = entry
                                        coroutineScope.launch {
                                            resetAndLoadLogs()
                                        }
                                        showAutoComplete = false
                                    }, text = {
                                        Text(entry)
                                    })
                                }
                            }
                        },
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                item {
                    if (logs.isEmpty()) {
                        Text("No logs found", modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Light)
                    }
                }
                items(logs, key = { it.userId + it.id }) { log ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(5.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var databaseFriend by remember { mutableStateOf<MessagingFriendInfo?>(null) }

                            LaunchedEffect(Unit) {
                                launch(Dispatchers.IO) {
                                    databaseFriend = context.database.getFriendInfo(log.userId)
                                }
                            }
                            BitmojiImage(
                                modifier = Modifier.padding(10.dp),
                                size = 70,
                                context = context,
                                url = databaseFriend?.takeIf { it.bitmojiId != null }?.let {
                                    BitmojiSelfie.getBitmojiSelfie(it.selfieId, it.bitmojiId, BitmojiSelfie.BitmojiSelfieType.NEW_THREE_D)
                                },
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                            ) {
                                Text(databaseFriend?.displayName?.let {
                                    "$it (${log.username})"
                                } ?: log.username)
                                Text("${log.eventType} in ${log.conversationTitle}", fontSize = 15.sp, fontWeight = FontWeight.Light)
                                Text(
                                    DateFormat.getDateTimeInstance().format(log.timestamp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Light
                                )
                            }

                            OutlinedIconButton(
                                onClick = {
                                    context.messageLogger.deleteTrackerLog(log.id)
                                    logs.remove(log)
                                }
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete")
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    LaunchedEffect(lastTimestamp) {
                        loadNewLogs()
                    }
                }
            }
        }

    }

    @OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
    @Composable
    private fun EditRuleDialog(
        currentRuleId: Int? = null,
        onDismiss: () -> Unit = {}
    ) {
        val events = rememberAsyncMutableStateList(defaultValue = emptyList()) {
            currentRuleId?.let { ruleId ->
                context.database.getTrackerEvents(ruleId)
            } ?: emptyList()
        }
        var currentScopeType by remember { mutableStateOf(TrackerScopeType.BLACKLIST) }
        val scopes = rememberAsyncMutableStateList(defaultValue = emptyList()) {
            currentRuleId?.let { ruleId ->
                context.database.getRuleTrackerScopes(ruleId).also {
                    currentScopeType = if (it.isEmpty()) {
                        TrackerScopeType.WHITELIST
                    } else {
                        it.values.first()
                    }
                }.map { it.key }
            } ?: emptyList()
        }
        val ruleName = rememberAsyncMutableState(defaultValue = "", keys = arrayOf(currentRuleId)) {
            currentRuleId?.let { ruleId ->
                context.database.getTrackerRule(ruleId)?.name ?: "Custom Rule"
            } ?: "Custom Rule"
        }

        fun saveRule() {
            runCatching {
                val ruleId = currentRuleId ?: context.database.newTrackerRule()
                events.forEach { event ->
                    context.database.addOrUpdateTrackerRuleEvent(
                        event.id.takeIf { it > -1 },
                        ruleId,
                        event.eventType,
                        event.params,
                        event.actions
                    )
                }
                context.database.setTrackerRuleName(ruleId, ruleName.value.trim())
                context.database.setRuleTrackerScopes(ruleId, currentScopeType, scopes)
            }.onFailure {
                context.log.error("Failed to save rule", it)
                context.shortToast("Failed to save rule. Please check logs for more details.")
            }
        }

        @Composable
        fun ActionCheckbox(
            text: String,
            checked: MutableState<Boolean>,
            onChanged: (Boolean) -> Unit = {}
        ) {
            Row(
                modifier = Modifier.clickable {
                    checked.value = !checked.value
                    onChanged(checked.value)
                },
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    modifier = Modifier.size(30.dp),
                    checked = checked.value,
                    onCheckedChange = {
                        checked.value = it
                        onChanged(it)
                    }
                )
                Text(text, fontSize = 12.sp)
            }
        }

        AlertDialog(
            onDismissRequest = {
                onDismiss()
            },
            title = {
                TextField(
                    value = ruleName.value,
                    onValueChange = {
                        ruleName.value = it
                    },
                    singleLine = true,
                    placeholder = {
                        Text(
                            "Rule Name",
                            fontSize = 18.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    textStyle = TextStyle(fontSize = 20.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ){
                        var addFriendDialog by remember { mutableStateOf(null as AddFriendDialog?) }

                        val friendDialogActions = remember {
                            AddFriendDialog.Actions(
                                onFriendState = { friend, state ->
                                    if (state) {
                                        scopes.add(friend.userId)
                                    } else {
                                        scopes.remove(friend.userId)
                                    }
                                },
                                onGroupState = { group, state ->
                                    if (state) {
                                        scopes.add(group.conversationId)
                                    } else {
                                        scopes.remove(group.conversationId)
                                    }
                                },
                                getFriendState = { friend ->
                                    friend.userId in scopes
                                },
                                getGroupState = { group ->
                                    group.conversationId in scopes
                                }
                            )
                        }

                        val isScopesEmpty = scopes.isEmpty()

                        Button(
                            onClick = {
                                currentScopeType = TrackerScopeType.BLACKLIST
                                addFriendDialog = AddFriendDialog(
                                    context,
                                    friendDialogActions
                                )
                            },
                            colors = if (!isScopesEmpty && currentScopeType == TrackerScopeType.BLACKLIST) ButtonDefaults.buttonColors() else ButtonDefaults.elevatedButtonColors()
                        ) {
                            Text("Blacklist" + if (currentScopeType == TrackerScopeType.BLACKLIST && !isScopesEmpty) " (" + scopes.size.toString() + ")" else "")
                        }

                        Button(
                            onClick = {
                                currentScopeType = TrackerScopeType.WHITELIST
                                addFriendDialog = AddFriendDialog(
                                    context,
                                    friendDialogActions
                                )
                            },
                            colors = if (!isScopesEmpty && currentScopeType == TrackerScopeType.WHITELIST) ButtonDefaults.buttonColors() else ButtonDefaults.elevatedButtonColors()
                        ) {
                            Text("Whitelist" + if (currentScopeType == TrackerScopeType.WHITELIST && !isScopesEmpty) " (" + scopes.size.toString() + ")" else "")
                        }

                        addFriendDialog?.Content {
                            addFriendDialog = null
                        }
                    }

                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth(),
                    ) {
                        var currentEventType by remember { mutableStateOf(TrackerEventType.CONVERSATION_ENTER.key) }
                        var checkedActions by remember { mutableStateOf(emptySet<TrackerRuleAction>()) }
                        val showDropdown = remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ExposedDropdownMenuBox(expanded = showDropdown.value, onExpandedChange = { showDropdown.value = it }) {
                                ElevatedButton(
                                    onClick = { showDropdown.value = true },
                                    modifier = Modifier.menuAnchor()
                                ) {
                                    Text(currentEventType)
                                }
                                DropdownMenu(expanded = showDropdown.value, onDismissRequest = { showDropdown.value = false }) {
                                    TrackerEventType.entries.forEach { eventType ->
                                        DropdownMenuItem(onClick = {
                                            currentEventType = eventType.key
                                            showDropdown.value = false
                                        }, text = {
                                            Text(eventType.key)
                                        })
                                    }
                                }
                            }

                            OutlinedButton(onClick = {
                                events.add(TrackerRuleEvent(-1, true, currentEventType, TrackerRuleActionParams(), checkedActions.toList()))
                            }) {
                                Text("Add")
                            }
                        }

                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(2.dp),
                        ) {
                            TrackerRuleAction.entries.forEach { action ->
                                ActionCheckbox(action.name, checked = remember { mutableStateOf(checkedActions.contains(action)) }) {
                                    if (it) {
                                        checkedActions += action
                                    } else {
                                        checkedActions -= action
                                    }
                                }
                            }
                        }
                    }


                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(events) { event ->
                            var collapsed by remember { mutableStateOf(false) }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .clickable {
                                        collapsed = !collapsed
                                    },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(event.eventType)
                                    OutlinedIconButton(onClick = {
                                        if (event.id > -1) {
                                            context.database.deleteTrackerRuleEvent(event.id)
                                        }
                                        events.remove(event)
                                    }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete")
                                    }
                                }
                                if (collapsed) {
                                    Text(event.actions.joinToString(", ") { it.name }, fontSize = 10.sp, fontWeight = FontWeight.Light)
                                    ActionCheckbox(text = "Only inside conversation", checked = remember { mutableStateOf(event.params.onlyInsideConversation) }, onChanged = { event.params.onlyInsideConversation = it })
                                    ActionCheckbox(text = "Only outside conversation", checked = remember { mutableStateOf(event.params.onlyOutsideConversation) }, onChanged = { event.params.onlyOutsideConversation = it })
                                    ActionCheckbox(text = "Only when app active", checked = remember { mutableStateOf(event.params.onlyWhenAppActive) }, onChanged = { event.params.onlyWhenAppActive = it })
                                    ActionCheckbox(text = "Only when app inactive", checked = remember { mutableStateOf(event.params.onlyWhenAppInactive) }, onChanged = { event.params.onlyWhenAppInactive = it })
                                    ActionCheckbox(text = "No push notification when active", checked = remember { mutableStateOf(event.params.noPushNotificationWhenAppActive) }, onChanged = { event.params.noPushNotificationWhenAppActive = it })
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    saveRule()
                    onDismiss()
                }) {
                    Text("Save")
                }
            },

            dismissButton = {
                currentRuleId?.let { ruleId ->
                    Button(onClick = {
                        context.database.deleteTrackerRule(ruleId)
                        onDismiss()
                    }) {
                        Text("Delete")
                    }
                }
            }
        )
    }

    @Composable
    private fun ConfigRulesTab() {
        val updateRules = rememberAsyncUpdateDispatcher()
        val rules = rememberAsyncMutableStateList(defaultValue = listOf(), updateDispatcher = updateRules) {
            context.database.getTrackerRules()
        }
        val coroutineScope = rememberCoroutineScope()

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                item {
                    if (rules.isEmpty()) {
                        Text("No rules found", modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Light)
                    }
                }
                items(rules, key = { it.id }) { rule ->
                    val updateRuleState = rememberAsyncUpdateDispatcher()
                    val ruleName by rememberAsyncMutableState(defaultValue = rule.name, updateDispatcher = updateRuleState) {
                        context.database.getTrackerRule(rule.id)?.name ?: "(empty)"
                    }
                    val eventCount by rememberAsyncMutableState(defaultValue = 0, updateDispatcher = updateRuleState) {
                        context.database.getTrackerEvents(rule.id).size
                    }
                    val scopeCount by rememberAsyncMutableState(defaultValue = 0, updateDispatcher = updateRuleState) {
                        context.database.getRuleTrackerScopes(rule.id).size
                    }

                    var editRuleDialog by remember { mutableStateOf(false) }

                    if (editRuleDialog) {
                        EditRuleDialog(rule.id, onDismiss = {
                            context.database.executeAsync {
                                if (context.database.getTrackerRule(rule.id) == null) {
                                    coroutineScope.launch {
                                        rules.removeIf { it.id == rule.id }
                                    }
                                }
                                editRuleDialog = false
                                coroutineScope.launch {
                                    updateRuleState.dispatch()
                                }
                            }
                        })
                    }

                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editRuleDialog = true }
                            .padding(5.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(ruleName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text(buildString {
                                    append(eventCount)
                                    append(" events")
                                    if (scopeCount > 0) {
                                        append(", ")
                                        append(scopeCount)
                                        append(" scopes")
                                    }
                                }, fontSize = 13.sp, fontWeight = FontWeight.Light)
                            }

                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                val scopesBitmoji = rememberAsyncMutableStateList(defaultValue = emptyList(), updateDispatcher = updateRuleState) {
                                    context.database.getRuleTrackerScopes(rule.id, limit = 10).mapNotNull {
                                        context.database.getFriendInfo(it.key)?.let { friend ->
                                            friend.selfieId to friend.bitmojiId
                                        }
                                    }.take(4)
                                }

                                scopesBitmoji.forEachIndexed { index, friend ->
                                    Box(
                                        modifier = Modifier
                                            .offset(x = (-index * 20).dp + (scopesBitmoji.size * 14).dp)
                                    ) {
                                        BitmojiImage(
                                            size = 50,
                                            modifier = Modifier
                                                .border(
                                                    BorderStroke(1.dp, Color.White),
                                                    CircleShape
                                                )
                                                .background(Color.White, CircleShape)
                                                .clip(CircleShape),
                                            context = context,
                                            url = BitmojiSelfie.getBitmojiSelfie(friend.first, friend.second, BitmojiSelfie.BitmojiSelfieType.NEW_THREE_D),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddRulePopup) {
            DisposableEffect(Unit) {
                onDispose {
                    coroutineScope.launch {
                        updateRules.dispatch()
                    }
                }
            }
        }
    }


    @OptIn(ExperimentalFoundationApi::class)
    override val content: @Composable (NavBackStackEntry) -> Unit = {
        val coroutineScope = rememberCoroutineScope()
        val pagerState = rememberPagerState { titles.size }
        currentPage = pagerState.currentPage

        Column {
            TabRow(selectedTabIndex = pagerState.currentPage, indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.pagerTabIndicatorOffset(
                        pagerState = pagerState,
                        tabPositions = tabPositions
                    )
                )
            }) {
                titles.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            Text(
                                text = title,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }

            HorizontalPager(
                modifier = Modifier.weight(1f),
                state = pagerState
            ) { page ->
                when (page) {
                    0 -> LogsTab()
                    1 -> ConfigRulesTab()
                }
            }
        }
    }
}