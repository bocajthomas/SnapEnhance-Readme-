package me.rhunk.snapenhance.ui.manager.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.common.data.RepositoryIndex
import me.rhunk.snapenhance.common.ui.AsyncUpdateDispatcher
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableStateList
import me.rhunk.snapenhance.common.util.ktx.copyToClipboard
import me.rhunk.snapenhance.common.util.ktx.getUrlFromClipboard
import me.rhunk.snapenhance.storage.addRepo
import me.rhunk.snapenhance.storage.getRepositories
import me.rhunk.snapenhance.storage.removeRepo
import me.rhunk.snapenhance.ui.manager.Routes
import okhttp3.OkHttpClient

class ManageReposSection: Routes.Route() {
    private val updateDispatcher = AsyncUpdateDispatcher()
    private val okHttpClient by lazy { OkHttpClient() }

    override val floatingActionButton: @Composable () -> Unit = {
        var showAddDialog by remember { mutableStateOf(false) }
        ExtendedFloatingActionButton(onClick = {
            showAddDialog = true
        }) {
            Text("Add Repository")
        }

        if (showAddDialog) {
            val coroutineScope = rememberCoroutineScope { Dispatchers.IO }

            suspend fun addRepo(url: String) {
                var modifiedUrl = url;

                if (url.startsWith("https://github.com/")) {
                    val splitUrl = modifiedUrl.removePrefix("https://github.com/").split("/")
                    val repoName = splitUrl[0] + "/" + splitUrl[1]
                    // fetch default branch
                    okHttpClient.newCall(
                        okhttp3.Request.Builder().url("https://api.github.com/repos/$repoName").build()
                    ).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw Exception("Failed to fetch default branch: ${response.code}")
                        }
                        val json = response.body.string()
                        val defaultBranch = context.gson.fromJson(json, Map::class.java)["default_branch"] as String
                        context.log.info("Default branch for $repoName is $defaultBranch")
                        modifiedUrl = "https://raw.githubusercontent.com/$repoName/$defaultBranch/"
                    }
                }

                val indexUri = modifiedUrl.toUri().buildUpon().appendPath("index.json").build()
                okHttpClient.newCall(
                    okhttp3.Request.Builder().url(indexUri.toString()).build()
                ).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw Exception("Failed to fetch index from $indexUri: ${response.code}")
                    }
                    runCatching {
                        val repoIndex = context.gson.fromJson(response.body.charStream(), RepositoryIndex::class.java).also {
                            context.log.info("repository index: $it")
                        }

                        context.database.addRepo(modifiedUrl)
                        context.shortToast("Repository added successfully! $repoIndex")
                        showAddDialog = false
                        updateDispatcher.dispatch()
                    }.onFailure {
                        throw Exception("Failed to parse index from $indexUri")
                    }
                }
            }

            var url by remember { mutableStateOf("") }
            var loading by remember { mutableStateOf(false) }

            AlertDialog(onDismissRequest = {
                showAddDialog = false
            }, title = {
                Text("Add Repository URL")
            }, text = {
                val focusRequester = remember { FocusRequester() }
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onGloballyPositioned {
                            focusRequester.requestFocus()
                        },
                    value = url,
                    onValueChange = {
                        url = it
                    }, label = {
                        Text("Repository URL")
                    }
                )
                LaunchedEffect(Unit) {
                    context.androidContext.getUrlFromClipboard()?.let {
                        url = it
                    }
                }
            }, confirmButton = {
                Button(
                    enabled = !loading,
                    onClick = {
                        loading = true;
                        coroutineScope.launch {
                            runCatching {
                                addRepo(url)
                            }.onFailure {
                                context.log.error("Failed to add repository", it)
                                context.shortToast("Failed to add repository: ${it.message}")
                            }
                            loading = false
                        }
                    }
                ) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text("Add")
                    }
                }
            })
        }
    }

    override val content: @Composable (NavBackStackEntry) -> Unit = {
        val coroutineScope = rememberCoroutineScope()
        val repositories = rememberAsyncMutableStateList(defaultValue = listOf(), updateDispatcher = updateDispatcher) {
            context.database.getRepositories()
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
        ) {
            item {
                if (repositories.isEmpty()) {
                    Text("No repositories added", modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(), fontSize = 15.sp, fontWeight = FontWeight.Light, textAlign = TextAlign.Center)
                }
            }
            items(repositories) { url ->
                ElevatedCard(onClick = {
                    context.androidContext.copyToClipboard(url)
                }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null)
                        Text(text = url, modifier = Modifier.weight(1f), overflow = TextOverflow.Ellipsis, maxLines = 4, fontSize = 15.sp, lineHeight = 15.sp)
                        Button(
                            onClick = {
                                context.database.removeRepo(url)
                                coroutineScope.launch {
                                    updateDispatcher.dispatch()
                                }
                            }
                        ) {
                            Text("Remove")
                        }
                    }
                }
            }
        }
    }
}