package me.rhunk.snapenhance.ui.setup.screens.impl

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.ui.setup.screens.SetupScreen
import me.rhunk.snapenhance.ui.util.AlertDialogs

class MappingsScreen : SetupScreen() {
    @Composable
    override fun Content() {
        val coroutineScope = rememberCoroutineScope()
        var infoText by remember { mutableStateOf(null as String?) }
        var isGenerating by remember { mutableStateOf(false) }

        if (infoText != null) {
            fun dismiss() {
                infoText = null
                goNext()
            }

            Dialog(onDismissRequest = { dismiss() }) {
                remember { AlertDialogs(context.translation) }.InfoDialog(title = infoText!!) {
                    dismiss()
                }
            }
        }

        LaunchedEffect(Unit) {
            coroutineScope.launch(Dispatchers.IO) {
                if (isGenerating) return@launch
                isGenerating = true
                runCatching {
                    if (context.installationSummary.snapchatInfo == null) {
                        throw Exception(context.translation["setup.mappings.generate_failure_no_snapchat"])
                    }
                    val warnings = context.mappings.refresh()

                    if (warnings.isNotEmpty()) {
                        isGenerating = false
                        infoText = "${warnings.size} warning(s) occurred while generating mappings:\n\n${warnings.joinToString("\n")}".also {
                            context.log.warn(it)
                        }
                        return@launch
                    }

                    goNext()
                }.onFailure {
                    isGenerating = false
                    infoText = context.translation["setup.mappings.generate_failure"] + "\n\n" + it.message
                    context.log.error("Failed to generate mappings", it)
                }
            }
        }

        if (isGenerating) {
            DialogText(text = context.translation["setup.mappings.dialog"])
            CircularProgressIndicator(
                modifier = Modifier
                    .padding()
                    .size(50.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}