package me.rhunk.snapenhance.ui.manager.pages.home

import android.net.Uri
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.rhunk.snapenhance.LogReader
import me.rhunk.snapenhance.common.logger.LogChannel
import me.rhunk.snapenhance.common.logger.LogLevel
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.util.ActivityLauncherHelper
import me.rhunk.snapenhance.ui.util.pullrefresh.PullRefreshIndicator
import me.rhunk.snapenhance.ui.util.pullrefresh.rememberPullRefreshState
import me.rhunk.snapenhance.ui.util.saveFile

class HomeLogs : Routes.Route() {
    private val logListState by lazy { LazyListState(0) }
    private lateinit var activityLauncherHelper: ActivityLauncherHelper

    override val init: () -> Unit = {
        activityLauncherHelper = ActivityLauncherHelper(context.activity!!)
    }

    override val topBarActions: @Composable (RowScope.() -> Unit) = {
        var showDropDown by remember { mutableStateOf(false) }

        IconButton(onClick = {
            showDropDown = true
        }) {
            Icon(Icons.Filled.MoreVert, contentDescription = null)
        }

        DropdownMenu(
            expanded = showDropDown,
            onDismissRequest = { showDropDown = false },
            modifier = Modifier.align(Alignment.CenterVertically)
        ) {
            DropdownMenuItem(onClick = {
                context.coroutineScope.launch {
                    context.log.clearLogs()
                }
                navigateReload()
                showDropDown = false
            }, text = {
                Text(translation["clear_logs_button"])
            })

            DropdownMenuItem(onClick = {
                activityLauncherHelper.saveFile("SE Extended-logs-${System.currentTimeMillis()}.zip", "application/zip") { uri ->
                    context.coroutineScope.launch {
                        context.shortToast(translation["saving_logs_toast"])
                        context.androidContext.contentResolver.openOutputStream(Uri.parse(uri))?.use {
                            runCatching {
                                context.log.exportLogsToZip(it)
                                context.longToast(translation["saved_logs_success_toast"])
                            }.onFailure {
                                context.longToast(translation["saved_logs_failure_toast"])
                                context.log.error("Failed to save logs to $uri!", it)
                            }
                        }
                    }
                }
                showDropDown = false
            }, text = {
                Text(translation["export_logs_button"])
            })
        }
    }

    override val content: @Composable (NavBackStackEntry) -> Unit = {
        val coroutineScope = rememberCoroutineScope()
        val clipboardManager = LocalClipboardManager.current
        var lineCount by remember { mutableIntStateOf(0) }
        var logReader by remember { mutableStateOf<LogReader?>(null) }
        var isRefreshing by remember { mutableStateOf(false) }

        fun refreshLogs() {
            coroutineScope.launch(Dispatchers.IO) {
                runCatching {
                    logReader = context.log.newReader {
                        lineCount++
                    }
                    lineCount = logReader!!.lineCount
                }.onFailure {
                    context.longToast("Failed to read logs!")
                }
                delay(300)
                isRefreshing = false
                withContext(Dispatchers.Main) {
                    logListState.scrollToItem((logListState.layoutInfo.totalItemsCount - 1).takeIf { it >= 0 } ?: return@withContext)
                }
            }
        }

        val pullRefreshState = rememberPullRefreshState(isRefreshing, onRefresh = {
            refreshLogs()
        })

        LaunchedEffect(Unit) {
            isRefreshing = true
            refreshLogs()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .horizontalScroll(ScrollState(0)),
                state = logListState
            ) {
                item {
                    if (lineCount == 0 && logReader != null) {
                        Text(
                            text = translation["no_logs_hint"],
                            modifier = Modifier.padding(16.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Light
                        )
                    }
                }
                items(lineCount) { index ->
                    val logLine by remember(index) {
                        mutableStateOf(runBlocking(Dispatchers.IO) {
                            logReader?.getLogLine(index)
                        })
                    }
                    var expand by remember { mutableStateOf(false) }

                    logLine?.let { line ->
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
                                        coroutineScope.launch {
                                            clipboardManager.setText(
                                                AnnotatedString(
                                                    line.message
                                                )
                                            )
                                        }
                                    },
                                    onTap = {
                                        expand = !expand
                                    }
                                )
                            }) {
                            Row(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 30.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!expand) {
                                    Icon(
                                        imageVector = when (line.logLevel) {
                                            LogLevel.DEBUG -> Icons.Outlined.BugReport
                                            LogLevel.ERROR, LogLevel.ASSERT -> Icons.Outlined.Report
                                            LogLevel.INFO, LogLevel.VERBOSE -> Icons.Outlined.Info
                                            LogLevel.WARN -> Icons.Outlined.Warning
                                            else -> Icons.Outlined.Info
                                        },
                                        contentDescription = null,
                                    )

                                    Text(
                                        text = LogChannel.fromChannel(line.tag)?.shortName ?: line.tag,
                                        modifier = Modifier.padding(start = 4.dp),
                                        fontWeight = FontWeight.Light,
                                        fontSize = 10.sp,
                                    )

                                    Text(
                                        text = line.dateTime,
                                        modifier = Modifier.padding(start = 4.dp, end = 4.dp),
                                        fontSize = 10.sp
                                    )
                                }

                                Text(
                                    text = line.message.trimIndent(),
                                    fontSize = 10.sp,
                                    maxLines = if (expand) Int.MAX_VALUE else 6,
                                    overflow = if (expand) TextOverflow.Visible else TextOverflow.Ellipsis,
                                    softWrap = !expand,
                                )
                            }
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    override val floatingActionButton: @Composable () -> Unit = {
        val coroutineScope = rememberCoroutineScope()
        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            val firstVisibleItem by remember { derivedStateOf { logListState.firstVisibleItemIndex } }
            val layoutInfo by remember { derivedStateOf { logListState.layoutInfo } }
            FilledIconButton(
                onClick = {
                    coroutineScope.launch {
                        logListState.scrollToItem(0)
                    }
                },
                enabled = firstVisibleItem != 0
            ) {
                Icon(Icons.Filled.KeyboardDoubleArrowUp, contentDescription = null)
            }

            FilledIconButton(
                onClick = {
                    coroutineScope.launch {
                        logListState.scrollToItem((logListState.layoutInfo.totalItemsCount - 1).takeIf { it >= 0 } ?: return@launch)
                    }
                },
                enabled = layoutInfo.visibleItemsInfo.lastOrNull()?.index != layoutInfo.totalItemsCount - 1
            ) {
                Icon(Icons.Filled.KeyboardDoubleArrowDown, contentDescription = null)
            }
        }
    }
}