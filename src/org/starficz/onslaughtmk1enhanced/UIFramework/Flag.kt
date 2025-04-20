package org.starficz.onslaughtmk1enhanced.UIFramework

data class Flag(var isEnabled: Boolean = true) {
    var isFiltered: Boolean
        get() = !isEnabled
        set(filtered) { isEnabled = !filtered }
}