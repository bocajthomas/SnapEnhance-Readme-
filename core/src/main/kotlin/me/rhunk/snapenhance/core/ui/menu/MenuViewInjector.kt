package me.rhunk.snapenhance.core.ui.menu

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import me.rhunk.snapenhance.core.event.events.impl.AddViewEvent
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.features.impl.COFOverride
import me.rhunk.snapenhance.core.ui.menu.impl.*
import me.rhunk.snapenhance.core.util.ktx.getIdentifier
import kotlin.reflect.KClass

@SuppressLint("DiscouragedApi")
class MenuViewInjector : Feature("MenuViewInjector") {
    private val menuMap by lazy {
        arrayOf(
            SettingsMenu(),
            NewChatActionMenu(),
            OperaContextActionMenu(),
            OperaViewerIcons(),
            FriendFeedInfoMenu(),
            ChatActionMenu(),
        ).associateBy {
            it.context = context
            it.menuViewInjector = this
            it::class
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T: AbstractMenu> menu(menuClass: KClass<T>): T? {
        return menuMap[menuClass] as? T
    }

    override fun init() {
        onNextActivityCreate(defer = true) {
            menuMap.forEach { it.value.init() }

            val chatActionMenu = context.resources.getIdentifier("chat_action_menu", "id")
            val hasV2ActionMenu = { context.feature(COFOverride::class).hasActionMenuV2 }

            context.event.subscribe(AddViewEvent::class) { event ->
                menuMap.forEach { it.value.onViewAdded(event) }
            }

            context.event.subscribe(AddViewEvent::class) { event ->
                val originalAddView: (View) -> Unit = {
                    event.adapter.invokeOriginal(arrayOf(it, -1,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        ))
                    )
                }

                val viewGroup: ViewGroup = event.parent
                val childView: View = event.view

                if (viewGroup !is LinearLayout && childView.id == chatActionMenu && context.isDeveloper) {
                    event.view = LinearLayout(childView.context).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(
                            (menuMap[NewChatActionMenu::class]!! as NewChatActionMenu).createDebugInfoView(childView.context)
                        )
                        addView(event.view)
                    }
                }

                if (childView.javaClass.name.endsWith("ChatActionMenuComponent") && hasV2ActionMenu()) {
                    (menuMap[NewChatActionMenu::class]!! as NewChatActionMenu).handle(event)
                    return@subscribe
                }

                if (viewGroup.javaClass.name.endsWith("ActionMenuChatItemContainer") && !hasV2ActionMenu()) {
                    if (viewGroup.parent == null || viewGroup.parent.parent == null) return@subscribe
                    menuMap[ChatActionMenu::class]!!.inject(viewGroup, childView, originalAddView)
                    return@subscribe
                }
            }
        }
    }
}