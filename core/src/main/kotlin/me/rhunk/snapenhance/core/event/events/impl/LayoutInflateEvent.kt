package me.rhunk.snapenhance.core.event.events.impl

import android.view.View
import android.view.ViewGroup
import me.rhunk.snapenhance.core.event.events.AbstractHookEvent

class LayoutInflateEvent(
    val layoutId: Int,
    val parent: ViewGroup?,
    val view: View?
) : AbstractHookEvent()