package com.pim.app.v2

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

class AndroidV2TextEncodingTest {
    @Test
    fun activeAndroidV2SourcesDoNotContainMojibakeMarkers() {
        val roots = listOf(
            repoFile("src", "main", "java", "com", "pim", "app", "ui"),
            repoFile("src", "main", "java", "com", "pim", "app", "location"),
            repoFile("src", "main", "java", "com", "pim", "app", "status")
        )
        val markers = listOf(
            "\uFFFD",
            "缁?",
            "閻樿埖",
            "娴犲﹥",
            "鐠佸墽",
            "閹镐胶",
            "閺夊啴",
            "鎵嬫満",
            "鐘舵",
            "璁剧疆",
            "浠婃棩",
            "杞ㄨ抗",
            "鏃ョ▼"
        )
        val offenders = roots
            .flatMap { root -> root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList() }
            .filter { file -> markers.any { marker -> file.readText(Charsets.UTF_8).contains(marker) } }
            .map { it.path }

        assertFalse("Mojibake markers found in active v2 sources: $offenders", offenders.isNotEmpty())
    }

    private fun repoFile(vararg parts: String): File {
        var current: File? = File("").canonicalFile
        while (current != null) {
            val candidate = parts.fold(current) { dir, part -> dir.resolve(part) }
            if (candidate.exists()) return candidate
            current = current.parentFile
        }
        error("Could not find ${parts.joinToString(File.separator)}")
    }
}
