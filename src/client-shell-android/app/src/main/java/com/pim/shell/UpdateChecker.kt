package com.pim.shell

object UpdateChecker {
    fun isNewer(current: String?, remote: String?): Boolean {
        if (remote.isNullOrBlank()) return false
        if (current.isNullOrBlank()) return true
        fun parseN(v: String): Int? {
            val t = v.trim().trimStart('v', 'V')
            if (t.isEmpty()) return null
            val core = t.split("+", "-").firstOrNull()?.trim() ?: return null
            if (core.isEmpty()) return null
            return core.split(".").lastOrNull()?.toIntOrNull()
        }
        val rn = parseN(remote)
        val cn = parseN(current)
        if (rn != null && cn != null) return rn > cn
        return remote.trim() > current.trim()
    }
}
