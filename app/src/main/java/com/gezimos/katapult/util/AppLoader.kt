package com.gezimos.katapult.util

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import com.gezimos.katapult.R
import com.gezimos.katapult.model.AppModel

object AppLoader {

    private var blacklist: Set<String>? = null

    private fun getBlacklist(context: Context): Set<String> {
        blacklist?.let { return it }
        val packages = mutableSetOf<String>()
        try {
            val parser = context.resources.getXml(R.xml.blacklist)
            while (parser.next() != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == org.xmlpull.v1.XmlPullParser.START_TAG && parser.name == "app") {
                    parser.getAttributeValue(null, "packageName")?.let { packages.add(it) }
                }
            }
        } catch (_: Exception) {}
        blacklist = packages
        return packages
    }

    /**
     * Loads launchable apps across ALL user profiles (main user + managed/work
     * profiles). The stock PackageManager.queryIntentActivities() only ever sees
     * the current user's activities, so work-profile apps never appeared in the
     * drawer. LauncherApps enumerates every profile UserManager exposes.
     */
    fun loadApps(context: Context, showSelf: Boolean = false): List<AppModel> {
        val selfPackage = context.packageName
        val blocked = getBlacklist(context)

        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
        val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager

        val apps = mutableListOf<AppModel>()

        if (launcherApps != null && userManager != null) {
            val mainUser = Process.myUserHandle()
            val profiles: List<UserHandle> = try {
                userManager.userProfiles
            } catch (_: Exception) {
                listOf(mainUser)
            }.ifEmpty { listOf(mainUser) }

            for (profile in profiles) {
                val serial = if (profile == mainUser) {
                    0L
                } else {
                    try { userManager.getSerialNumberForUser(profile) } catch (_: Exception) { 0L }
                }
                val activities = try {
                    launcherApps.getActivityList(null, profile)
                } catch (_: Exception) {
                    emptyList()
                }
                for (info in activities) {
                    val pkg = info.applicationInfo.packageName
                    val isSelf = pkg == selfPackage && serial == 0L
                    if ((isSelf && !showSelf) || pkg in blocked) continue
                    apps.add(
                        AppModel(
                            packageName = pkg,
                            label = info.label?.toString() ?: pkg,
                            activityName = info.componentName.className,
                            userSerial = serial
                        )
                    )
                }
            }
        }

        // Fallback: if LauncherApps yielded nothing (e.g. unusual OEM state),
        // fall back to the legacy single-user query so the drawer is never empty.
        if (apps.isEmpty()) {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = context.packageManager
                .queryIntentActivities(intent, PackageManager.MATCH_ALL)
            for (ri in resolveInfos) {
                val pkg = ri.activityInfo.packageName
                if ((pkg == selfPackage && !showSelf) || pkg in blocked) continue
                apps.add(
                    AppModel(
                        packageName = pkg,
                        label = ri.loadLabel(context.packageManager).toString(),
                        activityName = ri.activityInfo.name,
                        userSerial = 0L
                    )
                )
            }
        }

        // De-duplicate identical (package, activity, user) triples that can appear
        // when the same app is visible via multiple mechanisms.
        return apps
            .distinctBy { "${it.packageName}/${it.activityName}/${it.userSerial}" }
            .sortedBy { it.label.lowercase() }
    }
}
