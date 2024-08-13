package me.rhunk.snapenhance.ui.manager.pages.social

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.R
import me.rhunk.snapenhance.common.data.MessagingFriendInfo
import me.rhunk.snapenhance.common.data.MessagingGroupInfo
import me.rhunk.snapenhance.common.data.SocialScope
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableState
import me.rhunk.snapenhance.common.util.snap.BitmojiSelfie
import me.rhunk.snapenhance.storage.*
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.util.coil.BitmojiImage
import me.rhunk.snapenhance.ui.util.pagerTabIndicatorOffset

class SocialRootSection : Routes.Route() {
    private var friendList: List<MessagingFriendInfo> by mutableStateOf(emptyList())
    private var groupList: List<MessagingGroupInfo> by mutableStateOf(emptyList())

    private fun updateScopeLists() {
        context.coroutineScope.launch {
            friendList = context.database.getFriends(descOrder = true)
            groupList = context.database.getGroups()
        }
    }

    @Composable
    private fun ScopeList(scope: SocialScope) {
        val remainingHours = remember { context.config.root.streaksReminder.remainingHours.get() }

        LazyColumn(
            modifier = Modifier
                .padding(2.dp)
                .fillMaxWidth()
                .fillMaxHeight(),
            contentPadding = PaddingValues(bottom = 110.dp),
        ) {
            //check if scope list is empty
            val listSize = when (scope) {
                SocialScope.GROUP -> groupList.size
                SocialScope.FRIEND -> friendList.size
            }

            if (listSize == 0) {
                item {
                    Text(
                        text = translation["empty_hint"], modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp), textAlign = TextAlign.Center
                    )
                }
            }

            items(listSize) { index ->
                val id = when (scope) {
                    SocialScope.GROUP -> groupList[index].conversationId
                    SocialScope.FRIEND -> friendList[index].userId
                }

                ElevatedCard(
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth()
                        .height(80.dp)
                        .clickable {
                            routes.manageScope.navigate {
                                put("id", id)
                                put("scope", scope.key)
                            }
                        },
                ) {
                    Row(
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (scope) {
                            SocialScope.GROUP -> {
                                val group = groupList[index]
                                Column(
                                    modifier = Modifier
                                        .padding(7.dp)
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    Text(
                                        text = group.name,
                                        maxLines = 1,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            SocialScope.FRIEND -> {
                                val friend = friendList[index]
                                val streaks by rememberAsyncMutableState(defaultValue = friend.streaks) {
                                    context.database.getFriendStreaks(friend.userId)
                                }

                                BitmojiImage(
                                    context = context,
                                    url = BitmojiSelfie.getBitmojiSelfie(
                                        friend.selfieId,
                                        friend.bitmojiId,
                                        BitmojiSelfie.BitmojiSelfieType.THREE_D
                                    )
                                )
                                Column(
                                    modifier = Modifier
                                        .padding(7.dp)
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    Text(
                                        text = friend.displayName ?: friend.mutableUsername,
                                        maxLines = 1,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = friend.mutableUsername,
                                        maxLines = 1,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Light
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    streaks?.takeIf { it.notify }?.let { streaks ->
                                        Icon(
                                            imageVector = ImageVector.vectorResource(id = R.drawable.streak_icon),
                                            contentDescription = null,
                                            modifier = Modifier.height(40.dp),
                                            tint = if (streaks.isAboutToExpire(remainingHours))
                                                MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = translation.format(
                                                "streaks_expiration_short",
                                                "hours" to (((streaks.expirationTimestamp - System.currentTimeMillis()) / 3600000).toInt().takeIf { it > 0 } ?: 0)
                                                    .toString()
                                            ),
                                            maxLines = 1,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        FilledIconButton(onClick = {
                            routes.messagingPreview.navigate {
                                put("id", id)
                                put("scope", scope.key)
                            }
                        }) {
                            Icon(imageVector = Icons.Filled.RemoveRedEye, contentDescription = null)
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    override val content: @Composable (NavBackStackEntry) -> Unit = {
        val titles = remember {
            listOf(translation["friends_tab"], translation["groups_tab"])
        }
        val coroutineScope = rememberCoroutineScope()
        val pagerState = rememberPagerState { titles.size }
        var addFriendDialog by remember { mutableStateOf(null as AddFriendDialog?) }

        if (addFriendDialog != null) {
            addFriendDialog?.Content {
                addFriendDialog = null
            }
            DisposableEffect(Unit) {
                onDispose {
                    updateScopeLists()
                }
            }
        }

        LaunchedEffect(Unit) {
            updateScopeLists()
        }

        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        addFriendDialog = AddFriendDialog(
                            context,
                            AddFriendDialog.Actions(
                                onFriendState = { friend, state ->
                                    if (state) {
                                        context.bridgeService?.triggerScopeSync(SocialScope.FRIEND, friend.userId)
                                    } else {
                                        context.database.deleteFriend(friend.userId)
                                    }
                                },
                                onGroupState = { group, state ->
                                    if (state) {
                                        context.bridgeService?.triggerScopeSync(SocialScope.GROUP, group.conversationId)
                                    } else {
                                        context.database.deleteGroup(group.conversationId)
                                    }
                                },
                                getFriendState = { friend -> context.database.getFriendInfo(friend.userId) != null },
                                getGroupState = { group -> context.database.getGroupInfo(group.conversationId) != null }
                            ),
                            pinnedIds = (friendList.map { it.userId } + groupList.map { it.conversationId }).reversed(),
                        )
                    },
                    modifier = Modifier.padding(10.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null
                    )
                }
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
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
                    modifier = Modifier.padding(paddingValues),
                    state = pagerState
                ) { page ->
                    when (page) {
                        0 -> ScopeList(SocialScope.FRIEND)
                        1 -> ScopeList(SocialScope.GROUP)
                    }
                }
            }
        }
    }
}