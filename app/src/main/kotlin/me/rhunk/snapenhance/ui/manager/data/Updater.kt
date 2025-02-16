package me.rhunk.snapenhance.ui.manager.data

import com.google.gson.JsonParser
import me.rhunk.snapenhance.common.BuildConfig
import me.rhunk.snapenhance.common.logger.AbstractLogger
import okhttp3.OkHttpClient
import okhttp3.Request


object Updater {
    data class LatestRelease(
        val versionName: String,
        val releaseUrl: String
    )

    private fun fetchLatestRelease() = runCatching {
        val endpoint = Request.Builder().url("https://api.github.com/repos/bocajthomas/SE-Extended/releases").build()
        val response = OkHttpClient().newCall(endpoint).execute()

        if (!response.isSuccessful) throw Throwable("Failed to fetch releases: ${response.code}")

        val releases = JsonParser.parseString(response.body.string()).asJsonArray.also {
            if (it.size() == 0) throw Throwable("No releases found")
        }

        val latestRelease = releases.get(0).asJsonObject
        val latestVersion = latestRelease.getAsJsonPrimitive("tag_name").asString
        if (latestVersion.removePrefix("v") == BuildConfig.VERSION_NAME) return@runCatching null

        LatestRelease(
            versionName = latestVersion,
            releaseUrl = endpoint.url.toString().replace("api.", "").replace("repos/", "")
        )
    }.onFailure {
        AbstractLogger.directError("Failed to fetch latest release", it)
    }.getOrNull()

    private fun fetchLatestDebugCI() = runCatching {
        val actionRuns = OkHttpClient().newCall(Request.Builder().url("https://api.github.com/repos/bocajthomas/SE-Extended/actions/runs?event=workflow_dispatch&branch=dev").build()).execute().use {
            if (!it.isSuccessful) throw Throwable("Failed to fetch CI runs: ${it.code}")
            JsonParser.parseString(it.body.string()).asJsonObject
        }
        val debugRuns = actionRuns.getAsJsonArray("workflow_runs")?.mapNotNull { it.asJsonObject }?.filter { run ->
            run.get("conclusion")?.takeIf { it.isJsonPrimitive }?.asString == "success" && run.getAsJsonPrimitive("path")?.asString == ".github/workflows/debug.yml"
        } ?: throw Throwable("No debug CI runs found")

        val latestRun = debugRuns.firstOrNull() ?: throw Throwable("No debug CI runs found")
        val headSha = latestRun.getAsJsonPrimitive("head_sha")?.asString ?: throw Throwable("No head sha found")

        if (headSha == BuildConfig.GIT_HASH) return@runCatching null

        LatestRelease(
            versionName = headSha.substring(0, headSha.length.coerceAtMost(7)) + "-debug",
            releaseUrl = latestRun.getAsJsonPrimitive("html_url")?.asString?.replace("github.com", "nightly.link") ?: return@runCatching null
        )
    }.onFailure {
        AbstractLogger.directError("Failed to fetch latest debug CI", it)
    }.getOrNull()

    val latestRelease by lazy {
        if (BuildConfig.DEBUG) fetchLatestDebugCI() else fetchLatestRelease()
    }
}
