package me.rhunk.snapenhance.ui.manager.pages

import android.net.Uri
import android.text.format.Formatter
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.common.ui.AsyncUpdateDispatcher
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableState
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableStateList
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.util.ActivityLauncherHelper
import me.rhunk.snapenhance.ui.util.openFile
import java.text.DateFormat

class FileImportsRoot: Routes.Route() {
    private lateinit var activityLauncherHelper: ActivityLauncherHelper
    private val reloadDispatcher = AsyncUpdateDispatcher()

    override val init: () -> Unit = {
        activityLauncherHelper = ActivityLauncherHelper(context.activity!!)
    }

    override val floatingActionButton: @Composable () -> Unit = {
        val coroutineScope = rememberCoroutineScope()
        Row {
            ExtendedFloatingActionButton(
                icon = {
                    Icon(Icons.Default.Upload, contentDescription = null)
                },
                text = {
                    Text(translation["import_file_button"])
                },
                onClick = {
                context.coroutineScope.launch {
                    activityLauncherHelper.openFile { filePath ->
                        val fileUri = Uri.parse(filePath)
                        runCatching {
                            DocumentFile.fromSingleUri(context.activity!!, fileUri)?.let { file ->
                                if (!file.exists()) {
                                    context.shortToast(translation["file_not_found"])
                                    return@openFile
                                }
                                context.fileHandleManager.importFile(file.name!!) {
                                    context.androidContext.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                                        inputStream.copyTo(this)
                                    }
                                }
                            }
                        }.onFailure {
                            context.log.error("Failed to import file", it)
                            context.shortToast(translation.format("file_import_failed", "error" to it.message.toString()))
                        }.onSuccess {
                            context.shortToast(translation["file_imported"])
                            coroutineScope.launch {
                                reloadDispatcher.dispatch()
                            }
                        }
                    }
                }
            })
        }
    }

    override val content: @Composable (NavBackStackEntry) -> Unit = {
        val files = rememberAsyncMutableStateList(defaultValue = listOf(), updateDispatcher = reloadDispatcher) {
            context.fileHandleManager.getStoredFiles()
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            item {
                if (files.isEmpty()) {
                    Text(
                        text = translation["no_files_hint"],
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Light
                    )
                }
            }
            items(files, key = { it }) { file ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    val fileInfo by rememberAsyncMutableState(defaultValue = null) {
                        context.fileHandleManager.getFileInfo(file.name)
                    }
                    Row(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.padding(5.dp))
                        Column(
                            modifier = Modifier.weight(1f).padding(8.dp),
                        ) {
                            Text(text = file.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 20.sp)
                            fileInfo?.let { (size, lastModified) ->
                                Text(text = "${Formatter.formatFileSize(context.androidContext, size)} - ${DateFormat.getDateTimeInstance().format(lastModified)}", lineHeight = 15.sp)
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            IconButton(onClick = {
                                context.coroutineScope.launch {
                                    if (context.fileHandleManager.deleteFile(file.name)) {
                                        files.remove(file)
                                    } else {
                                        context.shortToast(translation["file_delete_failed"])
                                    }
                                }
                            }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null)
                            }
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}