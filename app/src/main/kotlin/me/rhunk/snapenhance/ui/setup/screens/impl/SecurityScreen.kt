package me.rhunk.snapenhance.ui.setup.screens.impl

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.ui.setup.screens.SetupScreen

class SecurityScreen : SetupScreen() {
    @Composable
    override fun Content() {
        Icon(
            imageVector = Icons.Default.WarningAmber,
            contentDescription = null,
            modifier = Modifier.padding(16.dp).size(30.dp),
        )

        DialogText(
            "Since Snapchat has implemented additional security measures against third-party applications such as SnapEnhance, we offer a non-opensource solution that reduces the risk of banning and prevents Snapchat from detecting SnapEnhance. " +
                    "\nPlease note that this solution does not provide a ban bypass or spoofer for anything, and does not take any personal data or communicate with the network." +
                    "\nWe also encourage you to use official signed builds to avoid compromising the security of your account." +
            "\nIf you're having trouble using the solution, or are experiencing crashes, join the Telegram Group for help: https://t.me/snapenhance_chat"
        )

        var denyDialog by remember { mutableStateOf(false) }

        if (denyDialog) {
            AlertDialog(
                onDismissRequest = {
                    denyDialog = false
                },
                text = {
                    Text("Are you sure you don't want to use this solution? You can always change this later in the settings in the SnapEnhance app.")
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

        Column (
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    context.coroutineScope.launch {
                        context.sharedPreferences.edit().putString("sif", "").commit()
                        context.remoteSharedLibraryManager.init()
                    }
                    goNext()
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