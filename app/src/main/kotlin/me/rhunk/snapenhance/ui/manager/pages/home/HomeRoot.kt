package me.rhunk.snapenhance.ui.manager.pages.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.R
import me.rhunk.snapenhance.common.BuildConfig
import me.rhunk.snapenhance.common.Constants
import me.rhunk.snapenhance.common.action.EnumAction
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableState
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableStateList
import me.rhunk.snapenhance.storage.getQuickTiles
import me.rhunk.snapenhance.storage.setQuickTiles
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.manager.data.Updater
import me.rhunk.snapenhance.ui.util.ActivityLauncherHelper

class HomeRoot : Routes.Route() {
    companion object {
        val cardMargin = 10.dp
    }

    private lateinit var activityLauncherHelper: ActivityLauncherHelper

    private fun launchActionIntent(action: EnumAction) {
        val intent = context.androidContext.packageManager.getLaunchIntentForPackage(Constants.SNAPCHAT_PACKAGE_NAME)
        intent?.putExtra(EnumAction.ACTION_PARAMETER, action.key)
        context.androidContext.startActivity(intent)
    }

    private val cards by lazy {
        mapOf(
            ("Friend Tracker" to Icons.Default.PersonSearch) to {
                routes.friendTracker.navigateReset()
            },
            ("Logger History" to Icons.Default.History) to {
                routes.loggerHistory.navigateReset()
            },
        ).toMutableMap().apply {
            EnumAction.entries.forEach { action ->
                this[context.translation["actions.${action.key}.name"] to action.icon] = {
                    launchActionIntent(action)
                }
            }
        }
    }

    override val init: () -> Unit = {
        activityLauncherHelper = ActivityLauncherHelper(context.activity !!)
    }

    override val topBarActions: @Composable (RowScope.() -> Unit) = {
        IconButton(onClick = {
            routes.homeLogs.navigate()
        }) {
            Icon(Icons.Filled.BugReport, contentDescription = null)
        }
        IconButton(onClick = {
            routes.settings.navigate()
        }) {
            Icon(Icons.Filled.Settings, contentDescription = null)
        }
    }

    @Composable
    fun LinkIcon(
        modifier: Modifier = Modifier,
        size: Dp = 32.dp,
        imageVector: ImageVector,
        dataArray: IntArray
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(size)
                .then(modifier)
                .clickable {
                    context.activity?.startActivity(Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse(
                            dataArray
                                .map { it.toChar() }
                                .joinToString("")
                                .reversed()
                        )
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }
        )
    }


    @OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
    override val content: @Composable (NavBackStackEntry) -> Unit = {
        val avenirNextFontFamily = remember {
            FontFamily(
                Font(R.font.avenir_next_medium, FontWeight.Medium)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(ScrollState(0))
        ) {
            Text(
                text = remember {
                    intArrayOf(
                        101, 99, 110, 97, 104, 110, 69, 112, 97, 110, 83
                    ).map { it.toChar() }.joinToString("").reversed()
                },
                fontSize = 30.sp,
                fontFamily = avenirNextFontFamily,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Text(
                text = "v" + BuildConfig.VERSION_NAME + " \u00b7 by rhunk",
                fontSize = 12.sp,
                fontFamily = avenirNextFontFamily,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    15.dp, Alignment.CenterHorizontally
                ), modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 10.dp)
            ) {
                LinkIcon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_github),
                    dataArray = intArrayOf(
                        101, 99, 110, 97, 104, 110, 69, 112, 97, 110, 83, 47, 107, 110,
                        117, 104, 114, 47, 109, 111, 99, 46, 98, 117, 104, 116, 105,
                        103, 47, 58, 115, 112, 116, 116, 104
                    )
                )

                LinkIcon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_telegram),
                    dataArray = intArrayOf(
                        101, 99, 110, 97, 104, 110, 101, 112, 97, 110, 115, 47, 101,
                        109, 46, 116, 47, 47, 58, 115, 112, 116, 116, 104
                    )
                )

                LinkIcon(
                    size = 36.dp,
                    modifier = Modifier.offset(y = (-2).dp),
                    imageVector = Icons.AutoMirrored.Default.Help,
                    dataArray = intArrayOf(
                        105, 107, 105, 119, 47, 101, 99, 110, 97, 104, 110, 69, 112, 97,
                        110, 83, 47, 107, 110, 117, 104, 114, 47, 109, 111, 99, 46, 98,
                        117, 104, 116, 105, 103, 47, 47, 58, 115, 112, 116, 116, 104
                    )
                )
            }

            val selectedTiles = rememberAsyncMutableStateList(defaultValue = listOf()) {
                context.database.getQuickTiles()
            }

            val latestUpdate by rememberAsyncMutableState(defaultValue = null) {
                if (!BuildConfig.DEBUG) Updater.checkForLatestRelease() else null
            }

            if (latestUpdate != null) {
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedCard(
                    modifier = Modifier
                        .padding(all = cardMargin)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = 15.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = translation["update_title"],
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                fontSize = 12.sp, text = translation.format(
                                    "update_content",
                                    "version" to (latestUpdate?.versionName ?: "unknown")
                                ), lineHeight = 20.sp
                            )
                        }
                        Button(onClick = {
                            context.activity?.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse(latestUpdate?.releaseUrl)
                            })
                        }, modifier = Modifier.height(40.dp)) {
                            Text(text = translation["update_button"])
                        }
                    }
                }
            }

            var showQuickActionsMenu by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 10.dp, top = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Quick Actions", fontSize = 20.sp, modifier = Modifier.weight(1f))
                Box {
                    IconButton(
                        onClick = { showQuickActionsMenu = !showQuickActionsMenu },
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = showQuickActionsMenu,
                        onDismissRequest = { showQuickActionsMenu = false }
                    ) {
                        cards.forEach { (card, _) ->
                            fun toggle(state: Boolean? = null) {
                                if (state?.let { !it } ?: selectedTiles.contains(card.first)) {
                                    selectedTiles.remove(card.first)
                                } else {
                                    selectedTiles.add(0, card.first)
                                }
                                context.coroutineScope.launch {
                                    context.database.setQuickTiles(selectedTiles)
                                }
                            }

                            DropdownMenuItem(onClick = { toggle() }, text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(all = 5.dp)
                                ) {
                                    Checkbox(
                                        checked = selectedTiles.contains(card.first),
                                        onCheckedChange = {
                                            toggle(it)
                                        }
                                    )
                                    Text(text = card.first)
                                }
                            })
                        }
                    }
                }
            }

            FlowRow(
                modifier = Modifier
                    .padding(all = cardMargin)
                    .fillMaxWidth(),
                maxItemsInEachRow = 3,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                val tileHeight = LocalDensity.current.run {
                    remember { (context.androidContext.resources.displayMetrics.widthPixels / 3).toDp() - cardMargin / 2 }
                }

                remember(selectedTiles.size, context.translation.loadedLocale) { selectedTiles.mapNotNull {
                    cards.entries.find { entry -> entry.key.first == it }
                } }.forEach { (card, action) ->
                    ElevatedCard(
                        modifier = Modifier
                            .height(tileHeight)
                            .weight(1f)
                            .clickable { action() }
                            .padding(all = 6.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(all = 5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            Icon(
                                imageVector = card.second, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(50.dp)
                            )
                            Text(
                                text = card.first,
                                lineHeight = 16.sp,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
