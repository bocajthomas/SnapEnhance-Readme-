package me.rhunk.snapenhance.ui.manager.pages.theming

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rhunk.snapenhance.common.data.*
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableState
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableStateList
import me.rhunk.snapenhance.common.ui.transparentTextFieldColors
import me.rhunk.snapenhance.storage.*
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.util.AlertDialogs
import me.rhunk.snapenhance.ui.util.CircularAlphaTile
import me.rhunk.snapenhance.ui.util.Dialog

class EditThemeSection: Routes.Route() {
    private var saveCallback by mutableStateOf<(() -> Unit)?>(null)
    private var addEntryCallback by mutableStateOf<(key: String, initialColor: Int) -> Unit>({ _, _ -> })
    private var deleteCallback by mutableStateOf<(() -> Unit)?>(null)
    private var themeColors = mutableStateListOf<ThemeColorEntry>()

    private val alertDialogs by lazy {
        AlertDialogs(context.translation)
    }

    override val topBarActions: @Composable (RowScope.() -> Unit) = {
        var deleteConfirmationDialog by remember { mutableStateOf(false) }

        if (deleteConfirmationDialog) {
            Dialog(onDismissRequest = {
                deleteConfirmationDialog = false
            }) {
                alertDialogs.ConfirmDialog(
                    title = "Delete Theme",
                    message = "Are you sure you want to delete this theme?",
                    onConfirm = {
                        deleteCallback?.invoke()
                        deleteConfirmationDialog = false
                    },
                    onDismiss = {
                        deleteConfirmationDialog = false
                    }
                )
            }
        }

        deleteCallback?.let {
            IconButton(onClick = {
                deleteConfirmationDialog = true
            }) {
                Icon(Icons.Default.Delete, contentDescription = null)
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    override val floatingActionButton: @Composable () -> Unit = {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            var addAttributeDialog by remember { mutableStateOf(false) }
            val attributesTranslation = remember { context.translation.getCategory("theming_attributes") }

            if (addAttributeDialog) {
                AlertDialog(
                    title = { Text("Select an attribute to add") },
                    onDismissRequest = {
                        addAttributeDialog = false
                    },
                    confirmButton = {},
                    text = {
                        var filter by remember { mutableStateOf("") }
                        val attributes = rememberAsyncMutableStateList(defaultValue = listOf(), keys = arrayOf(filter)) {
                            AvailableThemingAttributes[ThemingAttributeType.COLOR]?.filter { key ->
                                themeColors.none { it.key == key } && (key.contains(filter, ignoreCase = true) || attributesTranslation.getOrNull(key)?.contains(filter, ignoreCase = true) == true)
                            } ?: emptyList()
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxHeight(0.7f)
                                .fillMaxWidth(),
                        ) {
                            stickyHeader {
                                TextField(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp),
                                    value = filter,
                                    onValueChange = { filter = it },
                                    label = { Text("Search") },
                                    colors = transparentTextFieldColors().copy(
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceBright
                                    )
                                )
                            }
                            item {
                                if (attributes.isEmpty()) {
                                    Text("No attributes")
                                }
                            }
                            items(attributes) { attribute ->
                                Card(
                                    modifier = Modifier.padding(5.dp).fillMaxWidth(),
                                    onClick = {
                                        addEntryCallback(attribute, Color.White.toArgb())
                                        addAttributeDialog = false
                                    }
                                ) {
                                    val attributeTranslation = remember(attribute) {
                                        attributesTranslation.getOrNull(attribute)
                                    }

                                    Column(
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        Text(attributeTranslation ?: attribute, lineHeight = 15.sp)
                                        attributeTranslation?.let {
                                            Text(attribute, fontWeight = FontWeight.Light, fontSize = 10.sp, lineHeight = 15.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }

            FloatingActionButton(onClick = {
                addAttributeDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = null)
            }

            saveCallback?.let {
                FloatingActionButton(onClick = {
                    it()
                }) {
                    Icon(Icons.Default.Save, contentDescription = null)
                }
            }
        }
    }

    override val content: @Composable (NavBackStackEntry) -> Unit = {
        val coroutineScope = rememberCoroutineScope()
        val currentThemeId = remember { it.arguments?.getString("theme_id")?.toIntOrNull() }

        LaunchedEffect(Unit) {
            themeColors.clear()
        }

        var themeName by remember { mutableStateOf("") }
        var themeDescription by remember { mutableStateOf("") }
        var themeVersion by remember { mutableStateOf("1.0.0") }
        var themeAuthor by remember { mutableStateOf("") }
        var themeUpdateUrl by remember { mutableStateOf("") }

        val themeInfo by rememberAsyncMutableState(defaultValue = null) {
            currentThemeId?.let { themeId ->
                context.database.getThemeInfo(themeId)?.also { theme ->
                    themeName = theme.name
                    themeDescription = theme.description ?: ""
                    theme.version?.let { themeVersion = it }
                    themeAuthor = theme.author ?: ""
                    themeUpdateUrl = theme.updateUrl ?: ""
                }
            }
        }

        val lazyListState = rememberLazyListState()

        rememberAsyncMutableState(defaultValue = DatabaseThemeContent(), keys = arrayOf(themeInfo)) {
            currentThemeId?.let {
                context.database.getThemeContent(it)?.also { content ->
                    themeColors.clear()
                    themeColors.addAll(content.colors)
                    withContext(Dispatchers.Main) {
                        lazyListState.scrollToItem(themeColors.size)
                    }
                }
            } ?: DatabaseThemeContent()
        }

        if (themeName.isNotBlank()) {
            saveCallback = {
                coroutineScope.launch(Dispatchers.IO) {
                    val theme = DatabaseTheme(
                        id = currentThemeId ?: -1,
                        enabled = themeInfo?.enabled ?: false,
                        name = themeName,
                        description = themeDescription,
                        version = themeVersion,
                        author = themeAuthor,
                        updateUrl = themeUpdateUrl
                    )
                    val themeId = context.database.addOrUpdateTheme(theme, currentThemeId)
                    context.database.setThemeContent(themeId, DatabaseThemeContent(
                        colors = themeColors
                    ))
                    withContext(Dispatchers.Main) {
                        routes.theming.navigateReload()
                    }
                }
            }
        } else {
            saveCallback = null
        }

        LaunchedEffect(Unit) {
            deleteCallback = null
            if (currentThemeId != null) {
                deleteCallback = {
                    coroutineScope.launch(Dispatchers.IO) {
                        context.database.deleteTheme(currentThemeId)
                        withContext(Dispatchers.Main) {
                            routes.theming.navigateReload()
                        }
                    }
                }
            }
            addEntryCallback = { key, initialColor ->
                coroutineScope.launch(Dispatchers.Main) {
                    themeColors.add(ThemeColorEntry(key, initialColor))
                    delay(100)
                    lazyListState.scrollToItem(themeColors.size)
                }
            }
        }

        var moreOptionsExpanded by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val focusRequester = remember { FocusRequester() }

                TextField(
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    value = themeName,
                    onValueChange = { themeName = it },
                    label = { Text("Theme Name") },
                    colors = transparentTextFieldColors(),
                    singleLine = true,
                )
                LaunchedEffect(Unit) {
                    if (currentThemeId == null) {
                        delay(200)
                        focusRequester.requestFocus()
                    }
                }
                IconButton(
                    modifier = Modifier.padding(4.dp),
                    onClick = {
                        moreOptionsExpanded = !moreOptionsExpanded
                    }
                ) {
                    Icon(if (moreOptionsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)
                }
            }

            if (moreOptionsExpanded) {
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    value = themeDescription,
                    onValueChange = { themeDescription = it },
                    label = { Text("Description") },
                    colors = transparentTextFieldColors()
                )
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    value = themeVersion,
                    onValueChange = { themeVersion = it },
                    label = { Text("Version") },
                    colors = transparentTextFieldColors()
                )
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    value = themeAuthor,
                    onValueChange = { themeAuthor = it },
                    label = { Text("Author") },
                    colors = transparentTextFieldColors()
                )
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    value = themeUpdateUrl,
                    onValueChange = { themeUpdateUrl = it },
                    label = { Text("Update URL") },
                    colors = transparentTextFieldColors()
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                state = lazyListState,
                contentPadding = PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                reverseLayout = true,
            ) {
                item {
                    Spacer(modifier = Modifier.height(150.dp))
                }
                items(themeColors) { colorEntry ->
                    var showEditColorDialog by remember { mutableStateOf(false) }
                    var currentColor by remember { mutableIntStateOf(colorEntry.value) }

                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth(),
                        onClick = {
                            showEditColorDialog = true
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(4.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Colorize, contentDescription = null, modifier = Modifier.padding(8.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                val translation = remember(colorEntry.key) { context.translation.getOrNull("theming_attributes.${colorEntry.key}") }
                                Text(text = translation ?: colorEntry.key, overflow = TextOverflow.Ellipsis, maxLines = 1, lineHeight = 15.sp)
                                translation?.let {
                                    Text(text = colorEntry.key, fontSize = 10.sp, fontWeight = FontWeight.Light, overflow = TextOverflow.Ellipsis, maxLines = 1, lineHeight = 15.sp)
                                }
                            }
                            CircularAlphaTile(selectedColor = Color(currentColor))
                        }
                    }

                    if (showEditColorDialog) {
                        Dialog(onDismissRequest = { showEditColorDialog = false }) {
                            alertDialogs.ColorPickerDialog(
                                initialColor = Color(currentColor),
                                setProperty = {
                                    if (it == null) {
                                        themeColors.remove(colorEntry)
                                        return@ColorPickerDialog
                                    }
                                    currentColor = it.toArgb()
                                    colorEntry.value = currentColor
                                },
                                dismiss = {
                                    showEditColorDialog = false
                                }
                            )
                        }
                    }
                }
                item {
                    if (themeColors.isEmpty()) {
                        Text("No colors added yet", modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp), fontWeight = FontWeight.Light, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}