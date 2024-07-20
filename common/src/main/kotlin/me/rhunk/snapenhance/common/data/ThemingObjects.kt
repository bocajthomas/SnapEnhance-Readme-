package me.rhunk.snapenhance.common.data

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize


@Parcelize
data class ThemeColorEntry(
    @SerializedName("key")
    val key: String,
    @SerializedName("value")
    var value: Int,
): Parcelable

@Parcelize
data class DatabaseThemeContent(
    @SerializedName("colors")
    val colors: List<ThemeColorEntry> = emptyList(),
): Parcelable

data class DatabaseTheme(
    val id: Int,
    val enabled: Boolean,
    val name: String,
    val description: String?,
    val version: String?,
    val author: String?,
    val updateUrl: String?,
) {
    fun toExportedTheme(content: DatabaseThemeContent): ExportedTheme {
        return ExportedTheme(
            name = name,
            description = description,
            version = version,
            author = author,
            content = content,
        )
    }
}

data class ExportedTheme(
    val name: String,
    val description: String?,
    val version: String?,
    val author: String?,
    val content: DatabaseThemeContent,
) {
    fun toDatabaseTheme(id: Int = -1, updateUrl: String? = null, enabled: Boolean = false): DatabaseTheme {
        return DatabaseTheme(
            id = id,
            enabled = enabled,
            name = name,
            description = description,
            version = version,
            author = author,
            updateUrl = updateUrl,
        )
    }
}

data class RepositoryThemeManifest(
    val name: String,
    val author: String?,
    val description: String?,
    val version: String?,
    val filepath: String,
)

data class RepositoryIndex(
    val themes: List<RepositoryThemeManifest> = emptyList(),
)

enum class ThemingAttributeType {
    COLOR
}

val AvailableThemingAttributes = mapOf(
    ThemingAttributeType.COLOR to listOf(
        "sigColorTextPrimary",
        "sigColorBackgroundSurface",
        "sigColorBackgroundMain",
        "actionSheetBackgroundDrawable",
        "actionSheetRoundedBackgroundDrawable",
        "sigColorChatChat",
        "sigColorChatPendingSending",
        "sigColorChatSnapWithSound",
        "sigColorChatSnapWithoutSound",
        "sigExceptionColorCameraGridLines",
        "listDivider",
        "listBackgroundDrawable",
        "sigColorIconPrimary",
        "actionSheetDescriptionTextColor",
        "ringColor",
        "sigColorIconSecondary",
        "itemShapeFillColor",
        "ringStartColor",
        "sigColorLayoutPlaceholder",
        "scButtonColor",
        "recipientPillBackgroundDrawable",
        "boxBackgroundColor",
        "editTextColor",
        "chipBackgroundColor",
        "recipientInputStyle",
        "rangeFillColor",
        "pstsIndicatorColor",
        "pstsTabBackground",
        "pstsDividerColor",
        "tabTextColor",
        "statusBarForeground",
        "statusBarBackground",
        "strokeColor",
        "storyReplayViewRingColor",
        "sigColorButtonPrimary",
        "sigColorBaseAppYellow",
        "sigColorBackgroundSurfaceTranslucent",
        "sigColorStoryRingFriendsFeedStoryRing",
        "sigColorStoryRingDiscoverTabThumbnailStoryRing",
    )
)
