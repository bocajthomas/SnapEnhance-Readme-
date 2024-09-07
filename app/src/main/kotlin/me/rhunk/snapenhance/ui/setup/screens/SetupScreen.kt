package me.rhunk.snapenhance.ui.setup.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.rhunk.snapenhance.RemoteSideContext

abstract class SetupScreen {
    lateinit var context: RemoteSideContext
    lateinit var allowNext: (canGoNext: Boolean) -> Unit
    lateinit var goNext: () -> Unit
    lateinit var route: String

    @Composable
    fun DialogText(text: String, modifier: Modifier = Modifier) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(16.dp).then(modifier)
        )
    }
    private fun openLink(link: String) {
        kotlin.runCatching {
            context.activity?.startActivity(Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                data = Uri.parse(link)
            })
        }.onFailure {
            context.log.error("Couldn't open link", it)
            context.shortToast("Couldn't open link. Check SE Extended logs for more details.")
        }
    }

    open fun init() {}
    open fun onLeave() {}

    @Composable
    abstract fun Content()
}