package me.rhunk.snapenhance.core.ui.menu

import android.view.View
import android.view.ViewGroup
import me.rhunk.snapenhance.core.ModContext
import me.rhunk.snapenhance.core.event.events.impl.AddViewEvent

abstract class AbstractMenu {
    lateinit var menuViewInjector: MenuViewInjector
    lateinit var context: ModContext

    open fun inject(parent: ViewGroup, view: View, viewConsumer: (View) -> Unit) {}
    open fun onViewAdded(event: AddViewEvent) {}

    open fun init() {}
}