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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableState
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableStateList
import me.rhunk.snapenhance.common.ui.rememberAsyncUpdateDispatcher
import me.rhunk.snapenhance.common.util.snap.BitmojiSelfie
import me.rhunk.snapenhance.storage.*
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.util.ActivityLauncherHelper
import me.rhunk.snapenhance.ui.util.coil.BitmojiImage
import me.rhunk.snapenhance.ui.util.pagerTabIndicatorOffset


@OptIn(ExperimentalFoundationApi::class)
class FriendTrackerManagerRoot : Routes.Route() {
    enum class FilterType {
        CONVERSATION, USERNAME, EVENT
    }

    private val titles = listOf("Logs", "Rules")
    private var currentPage by mutableIntStateOf(0)
    private lateinit var logDeleteAction : () -> Unit
    private lateinit var exportAction : () -> Unit

    private lateinit var activityLauncherHelper: ActivityLauncherHelper

    override val init: () -> Unit = {
        activityLauncherHelper = ActivityLauncherHelper(context.activity!!)
    }

    override val floatingActionButton: @Composable () -> Unit = {
        when (currentPage) {
            0 -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ExtendedFloatingActionButton(
                        icon = { Icon(Icons.Default.SaveAlt, contentDescription = "Export") },
                        expanded = true,
                        text = { Text("Export") },
                        onClick = {
                            context.coroutineScope.launch { exportAction() }
                        }
                    )
                    ExtendedFloatingActionButton(
                        icon = { Icon(Icons.Default.DeleteOutline, contentDescription = "Delete") },
                        expanded = true,
                        text = { Text("Delete") },
                        onClick = {
                            context.coroutineScope.launch { logDeleteAction() }
                        }
                    )
                }
            }
            1 -> {
                ExtendedFloatingActionButton(
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add Rule") },
                    expanded = true,
                    text = { Text("Add Rule") },
                    onClick = { routes.editRule.navigate() }
                )
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
                    0 -> LogsTab(
                        context = context,
                        activityLauncherHelper = activityLauncherHelper,
                        deleteAction = { logDeleteAction = it },
                        exportAction = { exportAction = it }
                    )
                    1 -> ConfigRulesTab()
                }
            }
        }
    }
}