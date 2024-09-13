package me.rhunk.snapenhance.ui.manager.pages.theming

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import kotlinx.coroutines.*
import me.rhunk.snapenhance.common.data.RepositoryIndex
import me.rhunk.snapenhance.common.ui.AsyncUpdateDispatcher
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableState
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableStateList
import me.rhunk.snapenhance.storage.getThemeList
import me.rhunk.snapenhance.storage.getRepositories
import me.rhunk.snapenhance.storage.getThemeIdByUpdateUrl
import me.rhunk.snapenhance.ui.util.pullrefresh.PullRefreshIndicator
import me.rhunk.snapenhance.ui.util.pullrefresh.pullRefresh
import me.rhunk.snapenhance.ui.util.pullrefresh.rememberPullRefreshState
import okhttp3.Request


private val cachedRepoIndexes = mutableStateMapOf<String, RepositoryIndex>()
private val cacheReloadDispatcher = AsyncUpdateDispatcher()

@Composable
fun ThemeCatalog(root: ThemingRoot) {
    val context = remember { root.context }
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }

    fun fetchRepoIndexes(): Map<String, RepositoryIndex> {
        val indexes = mutableMapOf<String, RepositoryIndex>()

        context.database.getRepositories().forEach { rootUri ->
            val indexUri = rootUri.toUri().buildUpon().appendPath("index.json").build()

            runCatching {
                root.okHttpClient.newCall(
                    Request.Builder().url(indexUri.toString()).build()
                ).execute().use { response ->
                    if (!response.isSuccessful) {
                        context.log.error("Failed to fetch theme index from $indexUri: ${response.code}")
                        context.shortToast("Failed to fetch index of $indexUri")
                        return@forEach
                    }

                    runCatching {
                        indexes[rootUri] = context.gson.fromJson(response.body.charStream(), RepositoryIndex::class.java)
                    }.onFailure {
                        context.log.error("Failed to parse theme index from $indexUri", it)
                        context.shortToast("Failed to parse index of $indexUri")
                    }
                }
            }.onFailure {
                context.log.error("Failed to fetch theme index from $indexUri", it)
                context.shortToast("Failed to fetch index of $indexUri")
            }
        }

        return indexes
    }

    suspend fun installTheme(themeUri: Uri) {
        root.okHttpClient.newCall(
            Request.Builder().url(themeUri.toString()).build()
        ).execute().use { response ->
            if (!response.isSuccessful) {
                context.log.error("Failed to fetch theme from $themeUri: ${response.code}")
                context.shortToast("Failed to fetch theme from $themeUri")
                return
            }

            val themeContent = response.body.bytes().toString(Charsets.UTF_8)
            root.importTheme(themeContent, themeUri.toString())
        }
    }

    var isRefreshing by remember { mutableStateOf(false) }

    suspend fun refreshCachedIndexes() {
        isRefreshing = true
        coroutineScope {
            launch(Dispatchers.IO) {
                fetchRepoIndexes().let {
                    context.log.verbose("Fetched ${it.size} theme indexes")
                    it.forEach { (t, u) ->
                        context.log.verbose("Fetched theme index from $t with ${u.themes.size} themes")
                    }
                    synchronized(cachedRepoIndexes) {
                        cachedRepoIndexes.clear()
                        cachedRepoIndexes += it
                    }
                    cacheReloadDispatcher.dispatch()
                    delay(600)
                    isRefreshing = false
                }
            }
        }
    }

    val installedThemes = rememberAsyncMutableStateList(defaultValue = listOf(), updateDispatcher = root.localReloadDispatcher, keys = arrayOf(cachedRepoIndexes)) {
        context.database.getThemeList()
    }

    val remoteThemes by rememberAsyncMutableState(defaultValue = listOf(), updateDispatcher = cacheReloadDispatcher, keys = arrayOf(root.searchFilter.value)) {
        cachedRepoIndexes.entries.flatMap {
            it.value.themes.map { theme -> it.key to theme }
        }.let {
            val filter = root.searchFilter.value
            if (filter.isNotBlank()) {
                it.filter { (_, theme) ->
                    theme.name.contains(filter, ignoreCase = true) || theme.description?.contains(filter, ignoreCase = true) == true
                }
            } else it
        }
    }

    LaunchedEffect(Unit) {
        if (cachedRepoIndexes.isNotEmpty()) return@LaunchedEffect
        isRefreshing = true
        coroutineScope.launch {
            refreshCachedIndexes()
        }
    }

    val pullRefreshState = rememberPullRefreshState(isRefreshing, onRefresh = {
        coroutineScope.launch {
            refreshCachedIndexes()
        }
    })

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                if (remoteThemes.isEmpty()) {
                    Text(
                        text = "No themes available",
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Light
                    )
                }
            }
            items(remoteThemes, key = { it.first + it.second.hashCode() }) { (_, themeManifest) ->
                val themeUri = remember {
                    cachedRepoIndexes.entries.find { it.value.themes.contains(themeManifest) }?.key?.toUri()?.buildUpon()?.appendPath(themeManifest.filepath)?.build()
                }

                val hasUpdate by rememberAsyncMutableState(defaultValue = false, keys = arrayOf(themeManifest)) {
                    installedThemes.takeIf { themeUri != null }?.find { it.updateUrl == themeUri.toString() }?.let { installedTheme ->
                        installedTheme.version != themeManifest.version
                    } ?: false
                }

                var isInstalling by rememberAsyncMutableState(defaultValue = false, keys = arrayOf(themeManifest)) {
                    false
                }

                var isInstalled by rememberAsyncMutableState(defaultValue = true, keys = arrayOf(themeManifest)) {
                    context.database.getThemeIdByUpdateUrl(themeUri.toString()) != null
                }

                ElevatedCard(onClick = {
                    //TODO: Show theme details
                }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.padding(16.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = themeManifest.name,
                                maxLines = 1,
                                fontSize = 16.sp,
                                lineHeight = 10.sp,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold,
                            )
                            themeManifest.author?.let {
                                Text(
                                    text = "by $it",
                                    maxLines = 1,
                                    fontSize = 10.sp,
                                    lineHeight = 16.sp,
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = FontWeight.Light,
                                    overflow = TextOverflow.Visible,
                                )
                            }
                            themeManifest.description?.let {
                                Text(
                                    text = it,
                                    fontSize = 12.sp,
                                    maxLines = 3,
                                    lineHeight = 16.sp,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (hasUpdate) {
                                Text(
                                    text = "Version ${themeManifest.version} available",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isInstalling) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Button(
                                    enabled = !isInstalled || hasUpdate,
                                    onClick = {
                                        isInstalling = true
                                        context.coroutineScope.launch {
                                            runCatching {
                                                installTheme(themeUri ?: throw IllegalStateException("Failed to get theme URI"))
                                                isInstalled = true
                                            }.onFailure {
                                                context.log.error("Failed to install theme ${themeManifest.name}", it)
                                                context.shortToast("Failed to install theme ${themeManifest.name}. ${it.message}")
                                            }
                                            isInstalling = false
                                        }
                                    }
                                ) {
                                    if (hasUpdate) {
                                        Text("Update")
                                    } else {
                                        Text(if (isInstalled) "Installed" else "Install")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}