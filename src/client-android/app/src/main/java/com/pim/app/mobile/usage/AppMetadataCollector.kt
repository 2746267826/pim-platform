package com.pim.app.mobile.usage

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.pim.app.data.MobileAppMetadataEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppMetadataCollector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun collectForPackages(
        packageNames: Set<String>,
        collectedAtUtc: Long = System.currentTimeMillis()
    ): List<MobileAppMetadataEntity> {
        if (packageNames.isEmpty()) {
            return emptyList()
        }

        val packageManager = context.packageManager
        return packageNames
            .asSequence()
            .filter { it.isNotBlank() }
            .distinct()
            .mapNotNull { packageName ->
                collectPackage(packageManager, packageName, collectedAtUtc)
            }
            .toList()
    }

    fun collectInstalledApps(
        collectedAtUtc: Long = System.currentTimeMillis()
    ): List<MobileAppMetadataEntity> {
        val packageManager = context.packageManager
        return installedPackages(packageManager)
            .asSequence()
            .map { it.packageName }
            .filter { it.isNotBlank() }
            .distinct()
            .mapNotNull { packageName ->
                collectPackage(packageManager, packageName, collectedAtUtc)
            }
            .toList()
    }

    private fun collectPackage(
        packageManager: PackageManager,
        packageName: String,
        collectedAtUtc: Long
    ): MobileAppMetadataEntity? {
        return try {
            val packageInfo = packageInfo(packageManager, packageName)
            val appInfo = applicationInfo(packageManager, packageName)
            val label = appInfo.loadLabel(packageManager)?.toString().orEmpty().ifBlank { packageName }
            val installerPackageName = installerPackageName(packageManager, packageName)
            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val category = appCategory(appInfo)
            val versionCode = versionCode(packageInfo)

            MobileAppMetadataEntity(
                packageName = packageName,
                label = label,
                versionName = packageInfo.versionName,
                versionCode = versionCode,
                firstInstallTimeUtc = packageInfo.firstInstallTime,
                lastUpdateTimeUtc = packageInfo.lastUpdateTime,
                isSystemApp = isSystemApp,
                category = category,
                installerPackageName = installerPackageName,
                collectedAtUtc = collectedAtUtc,
                rawJson = JSONObject()
                    .put("packageName", packageName)
                    .put("label", label)
                    .put("versionName", packageInfo.versionName ?: JSONObject.NULL)
                    .put("versionCode", versionCode)
                    .put("firstInstallTimeUtc", packageInfo.firstInstallTime)
                    .put("lastUpdateTimeUtc", packageInfo.lastUpdateTime)
                    .put("isSystemApp", isSystemApp)
                    .put("category", category ?: JSONObject.NULL)
                    .put("installerPackageName", installerPackageName ?: JSONObject.NULL)
                    .put("collectedAtUtc", collectedAtUtc)
                    .toString()
            )
        } catch (_: PackageManager.NameNotFoundException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun installedPackages(packageManager: PackageManager): List<PackageInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledPackages(0)
        }
    }

    private fun packageInfo(packageManager: PackageManager, packageName: String): PackageInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }
    }

    private fun applicationInfo(packageManager: PackageManager, packageName: String): ApplicationInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getApplicationInfo(packageName, 0)
        }
    }

    private fun installerPackageName(packageManager: PackageManager, packageName: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                packageManager.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(packageName)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun appCategory(appInfo: ApplicationInfo): Int? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appInfo.category
        } else {
            null
        }
    }

    private fun versionCode(packageInfo: PackageInfo): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    }
}
