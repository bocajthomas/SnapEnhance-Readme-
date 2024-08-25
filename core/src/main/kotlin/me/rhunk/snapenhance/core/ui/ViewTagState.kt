package me.rhunk.snapenhance.core.ui

import android.view.View
import kotlin.random.Random

fun randomTag() = Random.nextInt(0x7000000, 0x7FFFFFFF)

class ViewTagState {
    private val tag = randomTag()

    operator fun get(view: View) = hasState(view)

    private fun hasState(view: View): Boolean {
        if (view.getTag(tag) != null) return true
        view.setTag(tag, true)
        return false
    }

    fun removeState(view: View) {
        view.setTag(tag, null)
    }
}