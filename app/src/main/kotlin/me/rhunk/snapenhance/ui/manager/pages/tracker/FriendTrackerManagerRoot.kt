package me.rhunk.snapenhance.ui.manager.pages.tracker

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import me.rhunk.snapenhance.common.data.MessagingFriendInfo
import me.rhunk.snapenhance.common.data.TrackerEventType
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableState
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableStateList
import me.rhunk.snapenhance.common.ui.rememberAsyncUpdateDispatcher
import me.rhunk.snapenhance.common.util.snap.BitmojiSelfie
import me.rhunk.snapenhance.storage.*
import me.rhunk.snapenhance.ui.manager.Routes
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

    override val floatingActionButton: @Composable () -> Unit = {
        if (currentPage == 1) {
            ExtendedFloatingActionButton(
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Rule") },
                expanded = true,
                text = { Text("Add Rule") },
                onClick = { routes.editRule.navigate() }
            )
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
                                .padding(4.dp),
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
                                    .weight(1f),
                            ) {
                                Text(databaseFriend?.displayName?.let {
                                    "$it (${log.username})"
                                } ?: log.username, lineHeight = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${log.eventType} in ${log.conversationTitle}", fontSize = 15.sp, fontWeight = FontWeight.Light, lineHeight = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    DateFormat.getDateTimeInstance().format(log.timestamp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Light,
                                    lineHeight = 15.sp,
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

    @Composable
    private fun ConfigRulesTab() {
        val updateRules = rememberAsyncUpdateDispatcher()
        val rules = rememberAsyncMutableStateList(defaultValue = listOf(), updateDispatcher = updateRules) {
            context.database.getTrackerRulesDesc()
        }

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
                    val ruleName by rememberAsyncMutableState(defaultValue = rule.name) {
                        context.database.getTrackerRule(rule.id)?.name ?: "(empty)"
                    }
                    val eventCount by rememberAsyncMutableState(defaultValue = 0) {
                        context.database.getTrackerEvents(rule.id).size
                    }
                    val scopeCount by rememberAsyncMutableState(defaultValue = 0) {
                        context.database.getRuleTrackerScopes(rule.id).size
                    }
                    var enabled by rememberAsyncMutableState(defaultValue = rule.enabled) {
                        context.database.getTrackerRule(rule.id)?.enabled ?: false
                    }

                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                routes.editRule.navigate {
                                    this["rule_id"] = rule.id.toString()
                                }
                            }
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
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val scopesBitmoji = rememberAsyncMutableStateList(defaultValue = emptyList()) {
                                    context.database.getRuleTrackerScopes(rule.id, limit = 10).mapNotNull {
                                        context.database.getFriendInfo(it.key)?.let { friend ->
                                            friend.selfieId to friend.bitmojiId
                                        }
                                    }.take(3)
                                }

                                Row {
                                    scopesBitmoji.forEachIndexed { index, friend ->
                                        Box(
                                            modifier = Modifier
                                                .offset(x = (-index * 20).dp + (scopesBitmoji.size * 20).dp - 20.dp)
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

                                Box(modifier = Modifier
                                    .padding(start = 5.dp, end = 5.dp)
                                    .height(50.dp)
                                    .width(1.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(5.dp)
                                    )
                                )

                                Switch(
                                    checked = enabled,
                                    onCheckedChange = {
                                        enabled = it
                                        context.database.setTrackerRuleState(rule.id, it)
                                    }
                                )
                            }
                        }
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