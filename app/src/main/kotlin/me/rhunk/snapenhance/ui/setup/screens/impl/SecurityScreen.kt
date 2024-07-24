package me.rhunk.snapenhance.ui.setup.screens.impl

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rhunk.snapenhance.ui.setup.screens.SetupScreen

class SecurityScreen : SetupScreen() {
    @SuppressLint("ApplySharedPref")
    @Composable
    override fun Content() {
        Icon(
            imageVector = Icons.Default.WarningAmber,
            contentDescription = null,
            modifier = Modifier
                .padding(16.dp)
                .size(30.dp),
        )

        DialogText(
            "Since Snapchat has implemented additional security measures against third-party applications such as SE Extended, we offer a non-opensource solution that reduces the risk of banning and prevents Snapchat from detecting SE Extended. " +
                    "\nPlease note that this solution does not provide a ban bypass or A spoofer for anything, and does not take any personal data or communicate with the network." +
            "\nIf you're having trouble using the solution, or are experiencing crashes, join the Telegram Group for help: https://t.me/SE_Extended_Chat"
        )

        var denyDialog by remember { mutableStateOf(false) }

        if (denyDialog) {
            AlertDialog(
                onDismissRequest = {
                    denyDialog = false
                },
                text = {
                    Text("Are you sure you don't want to use this solution? You can always change this later in the settings in the SE Extended app.")
                },
                dismissButton = {
                    Button(onClick = {
                        denyDialog = false
                    }) {
                        Text("Go back")
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        context.sharedPreferences.edit().putString("sif", "false").apply()
                        goNext()
                    }) {
                        Text("Yes, I'm sure")
                    }
                }
            )
        }

        var downloadJob by remember { mutableStateOf(null as Job?) }
        var jobError by remember { mutableStateOf(null as Throwable?) }

        if (downloadJob != null) {
            AlertDialog(onDismissRequest = {
                downloadJob?.cancel()
                downloadJob = null
            }, confirmButton = {}, text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (jobError != null) {
                        Text("Failed to download the required files.\n\n${jobError?.message}")
                    } else {
                        Text("Downloading the required files...")
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    }
                }
            })
        }

        fun newDownloadJob() {
            downloadJob?.cancel()
            downloadJob = context.coroutineScope.launch {
                context.sharedPreferences.edit().putString("sif", "").commit()
                runCatching {
                    context.remoteSharedLibraryManager.init()
                }.onFailure {
                    jobError = it
                    context.log.error("Failed to download the required files", it)
                }.onSuccess {
                    downloadJob = null
                    withContext(Dispatchers.Main) {
                        goNext()
                    }
                }
            }
        }

        Column (
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    newDownloadJob()
                }
            ) {
                Text("Accept and continue", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                onClick = {
                    denyDialog = true
                }
            ) {
                Text("I don't want to use this solution")
            }
        }
    }
}