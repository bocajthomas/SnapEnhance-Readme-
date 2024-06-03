package me.rhunk.snapenhance.ui.manager.pages.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.R
import me.rhunk.snapenhance.action.EnumQuickActions
import me.rhunk.snapenhance.common.BuildConfig
import me.rhunk.snapenhance.common.Constants
import me.rhunk.snapenhance.common.action.EnumAction
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableState
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableStateList
import me.rhunk.snapenhance.core.ui.Snapenhance
import me.rhunk.snapenhance.storage.getQuickTiles
import me.rhunk.snapenhance.storage.setQuickTiles
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.manager.data.Updater
import me.rhunk.snapenhance.ui.util.ActivityLauncherHelper
import java.text.DateFormat

class HomeRootSection : Routes.Route() {
    companion object {
        val cardMargin = 10.dp
    }

    private lateinit var activityLauncherHelper: ActivityLauncherHelper

    private fun launchActionIntent(action: EnumAction) {
        val intent = context.androidContext.packageManager.getLaunchIntentForPackage(
            Constants.SNAPCHAT_PACKAGE_NAME
        )
        intent?.putExtra(EnumAction.ACTION_PARAMETER, action.key)
        context.androidContext.startActivity(intent)
    }

    private val cards by lazy {
        EnumQuickActions.entries.map {
            (context.translation["actions.${it.key}.name"] to it.icon) to it.action
        }.associate {
            it.first to it.second
        }.toMutableMap().apply {
            EnumAction.entries.forEach { action ->
                this[context.translation["actions.${action.key}.name"] to action.icon] = {
                    launchActionIntent(action)
                }
            }
        }
    }

    @Composable
    private fun InfoCard(
        content: @Composable ColumnScope.() -> Unit,
    ) {
        OutlinedCard(
            modifier = Modifier
                .padding(all = cardMargin)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 15.dp)
            ) {
                content()
            }
        }
    }

    @Composable
    fun ExternalLinkIcon(
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
                            dataArray.reversed().map { (-it xor BuildConfig.APPLICATION_ID.hashCode()).toChar() }.joinToString("")
                        )
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }
        )
    }


    override val init: () -> Unit = {
        activityLauncherHelper = ActivityLauncherHelper(context.activity!!)
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


    @OptIn(ExperimentalLayoutApi::class)
    override val content: @Composable (NavBackStackEntry) -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Icon(
                imageVector = Snapenhance, contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 8.dp)
                    .align(Alignment.CenterHorizontally),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = translation.format(
                    "version_title",
                    "versionName" to BuildConfig.VERSION_NAME
                ),
                fontSize = 12.sp,
                fontFamily = remember {
                    FontFamily(
                        Font(R.font.avenir_next_medium, FontWeight.Medium)
                    )
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    15.dp, Alignment.CenterHorizontally
                ), modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 10.dp)
            ) {
                ExternalLinkIcon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_telegram),
                    // https://t.me/snapenhance
                    dataArray = intArrayOf(
                        0xe4f8b47, 0xe4f8b41, 0xe4f8b4e, 0xe4f8b43, 0xe4f8b4c, 0xe4f8b4e, 0xe4f8b47,
                        0xe4f8b54, 0xe4f8b43, 0xe4f8b4e, 0xe4f8b51, 0xe4f8b0d, 0xe4f8b47, 0xe4f8b4f,
                        0xe4f8b0e, 0xe4f8b58, 0xe4f8b0d, 0xe4f8b0d, 0xe4f8b1a, 0xe4f8b51, 0xe4f8b54,
                        0xe4f8b58, 0xe4f8b58, 0xe4f8b4c
                    )
                )

                ExternalLinkIcon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_github),
                    // https://github.com/rhunk/SnapEnhance
                    dataArray = intArrayOf(
                        0xe4f8b47, 0xe4f8b41, 0xe4f8b4e, 0xe4f8b43, 0xe4f8b4c, 0xe4f8b4e, 0xe4f8b67,
                        0xe4f8b54, 0xe4f8b43, 0xe4f8b4e, 0xe4f8b71, 0xe4f8b0d, 0xe4f8b49, 0xe4f8b4e,
                        0xe4f8b57, 0xe4f8b4c, 0xe4f8b52, 0xe4f8b0d, 0xe4f8b4f, 0xe4f8b4d, 0xe4f8b41,
                        0xe4f8b0e, 0xe4f8b42, 0xe4f8b57, 0xe4f8b4c, 0xe4f8b58, 0xe4f8b4b, 0xe4f8b45,
                        0xe4f8b0d, 0xe4f8b0d, 0xe4f8b1a, 0xe4f8b51, 0xe4f8b54, 0xe4f8b58, 0xe4f8b58,
                        0xe4f8b4c
                    )
                )

                ExternalLinkIcon(
                    size = 36.dp,
                    modifier = Modifier.offset(y = (-2).dp),
                    imageVector = Icons.AutoMirrored.Default.Help,
                    // https://github.com/rhunk/SnapEnhance/wiki
                    dataArray = intArrayOf(
                        0xe4f8b4b, 0xe4f8b49, 0xe4f8b4b, 0xe4f8b55, 0xe4f8b0d, 0xe4f8b47, 0xe4f8b41,
                        0xe4f8b4e, 0xe4f8b43, 0xe4f8b4c, 0xe4f8b4e, 0xe4f8b67, 0xe4f8b54, 0xe4f8b43,
                        0xe4f8b4e, 0xe4f8b71, 0xe4f8b0d, 0xe4f8b49, 0xe4f8b4e, 0xe4f8b57, 0xe4f8b4c,
                        0xe4f8b52, 0xe4f8b0d, 0xe4f8b4f, 0xe4f8b4d, 0xe4f8b41, 0xe4f8b0e, 0xe4f8b42,
                        0xe4f8b57, 0xe4f8b4c, 0xe4f8b58, 0xe4f8b4b, 0xe4f8b45, 0xe4f8b0d, 0xe4f8b0d,
                        0xe4f8b1a, 0xe4f8b51, 0xe4f8b54, 0xe4f8b58, 0xe4f8b58, 0xe4f8b4c
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
                Spacer(modifier = Modifier.height(10.dp))
                InfoCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
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

            if (BuildConfig.DEBUG) {
                Spacer(modifier = Modifier.height(10.dp))
                InfoCard {
                    Text(
                        text = translation["debug_build_summary_title"],
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    val buildSummary = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Light
                            )
                        ) {
                            append(
                                remember {
                                    translation.format(
                                        "debug_build_summary_content",
                                        "versionName" to BuildConfig.VERSION_NAME,
                                        "versionCode" to BuildConfig.VERSION_CODE.toString(),
                                    )
                                }
                            )
                            append(" - ")
                        }
                        pushStringAnnotation(
                            tag = "git_hash",
                            annotation = BuildConfig.GIT_HASH
                        )
                        withStyle(
                            style = SpanStyle(
                                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            append(BuildConfig.GIT_HASH.substring(0, 7))
                        }
                        pop()
                    }
                    ClickableText(
                        text = buildSummary,
                        onClick = { offset ->
                            buildSummary.getStringAnnotations(
                                tag = "git_hash", start = offset, end = offset
                            )
                                .firstOrNull()?.let {
                                    context.activity?.startActivity(
                                        Intent(Intent.ACTION_VIEW).apply {
                                            data = Uri.parse(
                                                "https://github.com/rhunk/SnapEnhance/commit/${it.item}"
                                            )
                                        })
                                }
                        }
                    )
                    Text(
                        fontSize = 12.sp,
                        text = remember {
                            translation.format(
                                "debug_build_summary_date",
                                "date" to DateFormat.getDateTimeInstance()
                                    .format(BuildConfig.BUILD_TIMESTAMP),
                                "days" to ((System.currentTimeMillis() - BuildConfig.BUILD_TIMESTAMP) / 86400000).toInt()
                                    .toString()
                            )
                        },
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Light
                    )
                }
            }

            var showQuickActionsMenu by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 10.dp, top = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    translation["quick_actions_title"], fontSize = 20.sp,
                    modifier = Modifier.weight(1f)
                )
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

                remember(selectedTiles.size, context.translation.loadedLocale) {
                    selectedTiles.mapNotNull {
                        cards.entries.find { entry -> entry.key.first == it }
                    }
                }.forEach { (card, action) ->
                    ElevatedCard(
                        modifier = Modifier
                            .height(tileHeight)
                            .weight(1f)
                            .clickable { action(routes) }
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
                                fontSize = 14.sp,
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
