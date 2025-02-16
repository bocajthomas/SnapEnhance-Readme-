package me.rhunk.snapenhance.core.ui.menu.impl

import android.view.View
import android.widget.FrameLayout
import me.rhunk.snapenhance.common.ui.OverlayType
import me.rhunk.snapenhance.core.ui.menu.AbstractMenu
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.ktx.getId
import me.rhunk.snapenhance.core.util.ktx.getIdentifier

class SettingsMenu : AbstractMenu() {
    private val hovaHeaderSearchIconId by lazy {
        context.resources.getId("hova_header_search_icon")
    }

    private val ngsChatLabel by lazy { context.resources.run { getString(getIdentifier("ngs_chat_label", "string")) } }
    private val ngsCameraLabelCamera by lazy { context.resources.run { getString(getIdentifier("ngs_camera_label_camera", "string")) } }
    private val ngsCommunityLabelStories by lazy { context.resources.run { getString(getIdentifier("ngs_community_label_stories", "string")) } }
    private val ngsDiscoverTitle by lazy { context.resources.run { getString(getIdentifier("ngs_discover_title", "string")) } }
    private val ngsMapLabel by lazy { context.resources.run { getString(getIdentifier("ngs_map_label", "string")) } }
    private val ngsSpotlightTitle by lazy { context.resources.run { getString(getIdentifier("ngs_spotlight_title", "string")) } }

    override fun init() {
        val getFriendFeedLabel = context.config.userInterface.customPageLabels.friendFeedLabel.get()
        val getCameraLabel = context.config.userInterface.customPageLabels.cameraLabel.get()
        val getStoriesLabel = context.config.userInterface.customPageLabels.storiesLabel.get()
        val getDiscoverLabel = context.config.userInterface.customPageLabels.discoverLabel.get()
        val getMapLabel = context.config.userInterface.customPageLabels.mapLabel.get()
        val getSpotlightLabel = context.config.userInterface.customPageLabels.spotlightLabel.get()

        val friendFeedLabel = if (getFriendFeedLabel.isNotEmpty()) { getFriendFeedLabel } else { "SE Extended" }
        val cameraLabel = if (getCameraLabel.isNotEmpty()) { getCameraLabel } else { "Camera" }
        val storiesLabel = if (getStoriesLabel.isNotEmpty()) { getStoriesLabel } else { "Stories" }
        val discoverLabel = if (getDiscoverLabel.isNotEmpty()) { getDiscoverLabel } else { "Discover" }
        val mapLabel = if (getMapLabel.isNotEmpty()) { getMapLabel } else { "Map" }
        val spotlightLabel = if (getSpotlightLabel.isNotEmpty()) { getSpotlightLabel } else { "Spotlight" }


        context.androidContext.classLoader.loadClass("com.snap.ui.view.SnapFontTextView").hook("setText", HookStage.BEFORE) { param ->
            val view = param.thisObject<View>()
            if ((view.parent as? FrameLayout)?.findViewById<View>(hovaHeaderSearchIconId) != null) {
                view.post {
                    view.setOnClickListener {
                        context.bridgeClient.openOverlay(OverlayType.SETTINGS)
                    }
                }
                if (param.argNullable<String>(0) == ngsChatLabel) {
                    param.setArg(0, friendFeedLabel)
                }
                if (param.argNullable<String>(0) == ngsCameraLabelCamera) {
                    param.setArg(0, cameraLabel)
                }
                if (param.argNullable<String>(0) == ngsCommunityLabelStories) {
                    param.setArg(0, storiesLabel)
                }
                if (param.argNullable<String>(0) == ngsDiscoverTitle) {
                    param.setArg(0, discoverLabel)
                }
                if (param.argNullable<String>(0) == ngsMapLabel) {
                    param.setArg(0, mapLabel)
                }
                if (param.argNullable<String>(0) == ngsSpotlightTitle) {
                    param.setArg(0, spotlightLabel)
                }
            }
        }
    }
}