package me.rhunk.snapenhance.core.features.impl

import android.widget.TextView
import me.rhunk.snapenhance.core.event.events.impl.AddViewEvent
import me.rhunk.snapenhance.core.features.Feature

class Debug : Feature("Debug") {
    override fun init() {
        if (!context.isDeveloper) return
        context.event.subscribe(AddViewEvent::class) { event ->
            event.view.post {
                val viewText = event.view.takeIf { it is TextView }?.let { (it as TextView).text } ?: ""
                event.view.contentDescription = "0x" + (event.view.id.takeIf { it > 0 }?.toString(16) ?: "") + " " + viewText
            }
        }
    }
}