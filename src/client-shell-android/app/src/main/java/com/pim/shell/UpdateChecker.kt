package com.pim.shell

object UpdateChecker {
    fun isNewer(current: String?, remote: String?): Boolean {
        if (remote.isNullOrBlank()) return false
        if (current.isNullOrBlank()) return true
        return remote.trim() != current.trim() && remote.trim() > current.trim()
    }
}
