package com.gezimos.katapult.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RemoveDone
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gezimos.katapult.MainViewModel
import com.gezimos.katapult.R
import com.gezimos.katapult.util.AppLoader
import com.gezimos.katapult.MainActivity
import com.gezimos.katapult.util.DeviceHelper
import com.gezimos.katapult.util.EinkHelper
import com.gezimos.katapult.util.PrefsManager
import com.gezimos.katapult.util.IconUtility
import kotlin.math.abs

private val ClockFormats = listOf("system", "HH:mm", "hh:mm a", "h:mm a", "h:mm")
private val DateFormats = listOf("system", "EEE, MMM d", "EEEE, MMMM d", "MMMM d, yyyy", "M/d/yyyy", "yyyy-MM-dd")

private fun nextOf(options: List<String>, current: String): String {
    val i = options.indexOf(current)
    return options[(i + 1) % options.size]
}

private fun formatLabel(pattern: String): String =
    if (pattern == "system") "System"
    else java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault()).format(java.util.Date())

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val prefs = viewModel.prefs
    val isMudita = remember { DeviceHelper.isMuditaKompakt() }
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (_: Exception) { "" }
    }

    var notificationIndicators by remember { mutableStateOf(prefs.notificationIndicators) }
    var clockFormat by remember { mutableStateOf(prefs.clockFormat) }
    var dateFormat by remember { mutableStateOf(prefs.dateFormat) }
    var showBattery by remember { mutableStateOf(prefs.showBattery) }
    var roundedIcons by remember { mutableStateOf(prefs.roundedIcons) }
    var darkMode by remember { mutableStateOf(prefs.darkMode) }
    var hideStatusBar by remember { mutableStateOf(prefs.hideStatusBar) }
    var einkRefreshOnHome by remember { mutableStateOf(prefs.einkRefreshOnHome) }
    var einkHelperMode by remember { mutableIntStateOf(prefs.einkHelperMode) }

    var homeExtraRow by remember { mutableStateOf(prefs.homeExtraRow) }
    var disableMusicWidget by remember { mutableStateOf(prefs.disableMusicWidget) }
    var infiniteScroll by remember { mutableStateOf(prefs.infiniteScroll) }
    var showKatapultIcon by remember { mutableStateOf(prefs.showKatapultIcon) }
    var hideAppNames by remember { mutableStateOf(prefs.hideAppNames) }
    var hideArrowButtons by remember { mutableStateOf(prefs.hideArrowButtons) }
    var disableHomeEditing by remember { mutableStateOf(prefs.disableHomeEditing) }
    var hideAllAppsButton by remember { mutableStateOf(prefs.hideAllAppsButton) }
    var swipeUpAllApps by remember { mutableStateOf(prefs.swipeUpAllApps) }
    var homeIslands by remember { mutableStateOf(prefs.homeIslands) }
    var verticalAppGestures by remember { mutableStateOf(prefs.verticalAppGestures) }
    var showNotificationsReadMe by remember { mutableStateOf(false) }
    var showEinkReadMe by remember { mutableStateOf(false) }
    var showGesturesReadMe by remember { mutableStateOf(false) }

    // Poll notification listener permission
    // Poll notification listener permission to sync toggle state
    LaunchedEffect(context) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            val hasPermission = NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.packageName)
            if (notificationIndicators != hasPermission) {
                notificationIndicators = hasPermission
                prefs.notificationIndicators = hasPermission
            }
        }
    }

    var currentPage by remember { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(1) }
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    var selectedCategory by rememberSaveable { mutableStateOf<Int?>(null) }

    BackHandler(enabled = selectedCategory != null) { selectedCategory = null }

    LaunchedEffect(pageCount) {
        if (currentPage >= pageCount) currentPage = (pageCount - 1).coerceAtLeast(0)
    }
    LaunchedEffect(selectedCategory) { currentPage = 0 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalSurface.current)
            .statusBarsPadding()
            .navigationBarsPadding()
            .then(
                if (selectedCategory == null) Modifier.padding(6.dp)
                else Modifier.padding(16.dp)
            ),
    ) {
        if (selectedCategory != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectedCategory != null) {
                Text(
                    text = "‹",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = LatoFamily,
                    color = LocalInk.current,
                    modifier = Modifier
                        .clickable { selectedCategory = null }
                        .padding(end = 12.dp),
                )
            }
            Text(
                text = selectedCategory?.let { stringResource(it) }
                    ?: stringResource(R.string.settings),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.weight(1f),
            )
            if (selectedCategory != null && pageCount > 1) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    for (i in 0 until pageCount) {
                        Box(
                            Modifier
                                .padding(horizontal = 2.dp)
                                .size(6.dp)
                                .then(
                                    if (i == currentPage) Modifier.background(LocalInk.current, CircleShape)
                                    else Modifier.border(1.5.dp, LocalInk.current, CircleShape)
                                )
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        }

        val headerIndices = mutableSetOf<Int>()
        val sectionTitles = mutableListOf<Int>()
        val sectionStarts = mutableListOf<Int>()
        val rows = buildList<@Composable () -> Unit> {
            fun addHeader(titleRes: Int) {
                sectionTitles += titleRes
                sectionStarts += size
                headerIndices += size
                add { SettingsSectionHeader(stringResource(titleRes)) }
            }
            // --- Appearance ---
            addHeader(R.string.section_appearance)
            add {
                SettingsCycleRow(
                    title = stringResource(R.string.icon_shape),
                    description = if (roundedIcons) stringResource(R.string.icon_shape_rounded) else stringResource(R.string.icon_shape_circle),
                    onClick = {
                        roundedIcons = !roundedIcons
                        prefs.roundedIcons = roundedIcons
                        viewModel.roundedIcons = roundedIcons
                    },
                )
            }
            add {
                SettingsToggleRow(
                    title = stringResource(R.string.dark_mode),
                    description = stringResource(R.string.dark_mode_desc),
                    checked = darkMode,
                    onCheckedChange = {
                        darkMode = it
                        prefs.darkMode = it
                        viewModel.darkMode = it
                        viewModel.applyStatusBar(context)
                    },
                )
            }
            add {
                SettingsToggleRow(
                    title = stringResource(R.string.hide_status_bar),
                    description = stringResource(R.string.hide_status_bar_desc),
                    checked = hideStatusBar,
                    onCheckedChange = {
                        hideStatusBar = it
                        prefs.hideStatusBar = it
                        viewModel.applyStatusBar(context)
                    },
                )
            }

            // --- Home Screen ---
            addHeader(R.string.section_home)
            add {
                SettingsCycleRow(
                    title = stringResource(R.string.clock_format),
                    description = formatLabel(clockFormat),
                    onClick = {
                        clockFormat = nextOf(ClockFormats, clockFormat)
                        prefs.clockFormat = clockFormat
                        viewModel.updateClock()
                    },
                )
            }
            add {
                SettingsCycleRow(
                    title = stringResource(R.string.date_format),
                    description = formatLabel(dateFormat),
                    onClick = {
                        dateFormat = nextOf(DateFormats, dateFormat)
                        prefs.dateFormat = dateFormat
                        viewModel.updateClock()
                    },
                )
            }
            add {
                SettingsToggleRow(
                    title = stringResource(R.string.show_battery),
                    description = stringResource(R.string.show_battery_desc),
                    checked = showBattery,
                    onCheckedChange = {
                        showBattery = it
                        prefs.showBattery = it
                    },
                )
            }
            add {
                SettingsToggleRow(
                    title = stringResource(R.string.extra_dock_row),
                    description = stringResource(R.string.extra_dock_row_desc),
                    checked = homeExtraRow,
                    onCheckedChange = {
                        homeExtraRow = it
                        prefs.homeExtraRow = it
                    },
                )
            }
            add {
                SettingsToggleRow(
                    title = stringResource(R.string.home_islands),
                    description = stringResource(R.string.home_islands_desc),
                    checked = homeIslands,
                    onCheckedChange = {
                        homeIslands = it
                        prefs.homeIslands = it
                    },
                )
            }
            add {
                SettingsToggleRow(
                    title = stringResource(R.string.disable_music_widget),
                    description = stringResource(R.string.disable_music_widget_desc),
                    checked = disableMusicWidget,
                    onCheckedChange = {
                        disableMusicWidget = it
                        prefs.disableMusicWidget = it
                    },
                )
            }
            add {
                SettingsToggleRow(
                    title = stringResource(R.string.hide_app_names),
                    description = stringResource(R.string.hide_app_names_desc),
                    checked = hideAppNames,
                    onCheckedChange = {
                        hideAppNames = it
                        prefs.hideAppNames = it
                    },
                )
            }
            add {
                SettingsToggleRow(
                    title = stringResource(R.string.disable_home_editing),
                    description = stringResource(R.string.disable_home_editing_desc),
                    checked = disableHomeEditing,
                    onCheckedChange = {
                        disableHomeEditing = it
                        prefs.disableHomeEditing = it
                    },
                )
            }

            // --- All Apps ---
            addHeader(R.string.section_all_apps)
            add {
                SettingsToggleRow(
                    title = stringResource(R.string.show_katapult_icon),
                    description = stringResource(R.string.show_katapult_icon_desc),
                    checked = showKatapultIcon,
                    onCheckedChange = {
                        showKatapultIcon = it
                        prefs.showKatapultIcon = it
                        viewModel.loadApps()
                    },
                )
            }
            add {
                SettingsToggleRow(
                    title = stringResource(R.string.hide_all_apps_button),
                    description = stringResource(R.string.hide_all_apps_button_desc),
                    checked = hideAllAppsButton,
                    onCheckedChange = {
                        hideAllAppsButton = it
                        prefs.hideAllAppsButton = it
                    },
                )
            }
            add {
                SettingsToggleRow(
                    title = stringResource(R.string.infinite_scroll),
                    description = stringResource(R.string.infinite_scroll_desc),
                    checked = infiniteScroll,
                    onCheckedChange = {
                        infiniteScroll = it
                        prefs.infiniteScroll = it
                    },
                )
            }
            add {
                SettingsToggleRow(
                    title = stringResource(R.string.hide_arrow_buttons),
                    description = stringResource(R.string.hide_arrow_buttons_desc),
                    checked = hideArrowButtons,
                    onCheckedChange = {
                        hideArrowButtons = it
                        prefs.hideArrowButtons = it
                    },
                )
            }

            // --- Gestures ---
            addHeader(R.string.section_gestures)
            add {
                SettingsActionRow(
                    title = stringResource(R.string.lockscreen_readme),
                    description = stringResource(R.string.gestures_readme_desc),
                    onClick = { showGesturesReadMe = true },
                )
            }
            add {
                SettingsToggleRow(
                    title = stringResource(R.string.swipe_up_all_apps),
                    description = stringResource(R.string.swipe_up_all_apps_desc),
                    checked = swipeUpAllApps,
                    onCheckedChange = {
                        swipeUpAllApps = it
                        prefs.swipeUpAllApps = it
                    },
                )
            }
            add {
                SettingsCycleRow(
                    title = stringResource(R.string.app_gesture_direction),
                    description = if (verticalAppGestures) stringResource(R.string.gesture_vertical)
                        else stringResource(R.string.gesture_horizontal),
                    onClick = {
                        verticalAppGestures = !verticalAppGestures
                        prefs.verticalAppGestures = verticalAppGestures
                    },
                )
            }
            add {
                SettingsActionRow(
                    title = stringResource(R.string.enable_tap_to_sleep),
                    description = stringResource(R.string.enable_tap_to_sleep_desc),
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                )
            }

            // --- Notifications ---
            addHeader(R.string.section_notifications)
            add {
                SettingsActionRow(
                    title = stringResource(R.string.lockscreen_readme),
                    description = stringResource(R.string.notifications_readme_desc),
                    onClick = { showNotificationsReadMe = true },
                )
            }
            add {
                SettingsToggleRow(
                    title = stringResource(R.string.notification_indicators),
                    description = stringResource(R.string.notification_indicators_desc),
                    checked = notificationIndicators,
                    onCheckedChange = { requested ->
                        val hasPermission = NotificationManagerCompat.getEnabledListenerPackages(context)
                            .contains(context.packageName)
                        if (requested) {
                            if (hasPermission) {
                                notificationIndicators = true
                                prefs.notificationIndicators = true
                            } else {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                } catch (_: Exception) {
                                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    })
                                }
                            }
                        } else {
                            try {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            } catch (_: Exception) {}
                        }
                    },
                )
            }
            add {
                SettingsActionRow(
                    title = stringResource(R.string.app_notifications),
                    description = stringResource(R.string.app_notifications_desc),
                    onClick = {
                        try {
                            context.startActivity(Intent().apply {
                                component = android.content.ComponentName(
                                    "com.android.settings",
                                    "com.android.settings.Settings\$NotificationAppListActivity"
                                )
                            })
                        } catch (_: Exception) {}
                    },
                )
            }
            add {
                SettingsActionRow(
                    title = stringResource(R.string.notification_log),
                    description = stringResource(R.string.notification_log_desc),
                    onClick = {
                        try {
                            context.startActivity(Intent().apply {
                                component = android.content.ComponentName(
                                    "com.android.settings",
                                    "com.android.settings.Settings\$NotificationStationActivity"
                                )
                            })
                        } catch (_: Exception) {}
                    },
                )
            }

            // --- E-Ink (Mudita only) ---
            if (isMudita) {
                addHeader(R.string.section_eink)
                add {
                    SettingsActionRow(
                        title = stringResource(R.string.lockscreen_readme),
                        description = stringResource(R.string.eink_readme_desc),
                        onClick = { showEinkReadMe = true },
                    )
                }
                add {
                    SettingsCycleRow(
                        title = stringResource(R.string.eink_auto_mode),
                        description = EinkHelper.modeName(einkHelperMode),
                        onClick = {
                            val next = EinkHelper.nextMode(einkHelperMode)
                            einkHelperMode = next
                            prefs.einkHelperMode = next
                            (context as? MainActivity)?.setMeinkMode(next)
                        },
                    )
                }
                add {
                    SettingsToggleRow(
                        title = stringResource(R.string.eink_refresh),
                        description = stringResource(R.string.eink_refresh_desc),
                        checked = einkRefreshOnHome,
                        onCheckedChange = {
                            einkRefreshOnHome = it
                            prefs.einkRefreshOnHome = it
                        },
                    )
                }
            }

            // --- System ---
            addHeader(R.string.section_system)
            add {
                SettingsActionRow(
                    title = stringResource(R.string.set_default_launcher),
                    description = stringResource(R.string.set_default_launcher_desc),
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                    },
                )
            }
            add {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = LatoFamily,
                            color = LocalInk.current,
                        )
                        Text(
                            text = versionName,
                            fontSize = 14.sp,
                            fontFamily = LatoFamily,
                            color = LocalInk.current,
                        )
                    }
                }
            }
        }

        if (selectedCategory == null) {
            SettingsCategoryGrid(
                onSelect = { titleRes -> selectedCategory = titleRes },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        } else {
            val sel = sectionTitles.indexOf(selectedCategory).coerceIn(0, (sectionStarts.size - 1).coerceAtLeast(0))
            val start = sectionStarts[sel] + 1
            val end = sectionStarts.getOrElse(sel + 1) { rows.size }
            val activeRows = rows.subList(start, end)
            SubcomposeLayout(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(sel) {
                        detectVerticalDragGestures(
                            onDragStart = { dragAccumulator = 0f },
                            onDragEnd = {
                                if (abs(dragAccumulator) > 80f && pageCount > 1) {
                                    val delta = if (dragAccumulator < 0) 1 else -1
                                    currentPage = (currentPage + delta).coerceIn(0, pageCount - 1)
                                }
                            },
                            onVerticalDrag = { _, amount -> dragAccumulator += amount },
                        )
                    },
            ) { constraints ->
                val childConstraints = Constraints(maxWidth = constraints.maxWidth)
                val rowPlaceables = activeRows.mapIndexed { i, content ->
                    subcompose(i) { content() }.map { it.measure(childConstraints) }
                }
                val rowHeights = rowPlaceables.map { ps -> ps.sumOf { it.height } }

                val groups = mutableListOf<IntRange>()
                var groupStart = 0
                var accH = 0
                for (i in rowHeights.indices) {
                    if (accH + rowHeights[i] > constraints.maxHeight && i > groupStart) {
                        groups.add(groupStart until i)
                        groupStart = i
                        accH = rowHeights[i]
                    } else {
                        accH += rowHeights[i]
                    }
                }
                if (groupStart < rowHeights.size) groups.add(groupStart until rowHeights.size)
                if (groups.isEmpty()) groups.add(0 until 0)

                if (pageCount != groups.size) pageCount = groups.size

                val page = currentPage.coerceIn(0, groups.size - 1)
                val visible = groups[page]

                layout(constraints.maxWidth, constraints.maxHeight) {
                    var y = 0
                    for (i in visible) {
                        for (p in rowPlaceables[i]) {
                            p.place(0, y)
                            y += p.height
                        }
                    }
                }
            }
        }
    }





    if (showNotificationsReadMe) {
        BottomSheet(onDismiss = { showNotificationsReadMe = false }) {
            Text(
                text = stringResource(R.string.lockscreen_readme),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(
                text = stringResource(R.string.notifications_readme_control),
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(
                text = stringResource(R.string.notifications_readme_debug),
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
            )
        }
    }



    if (showEinkReadMe) {
        BottomSheet(onDismiss = { showEinkReadMe = false }) {
            Text(
                text = stringResource(R.string.lockscreen_readme),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(
                text = stringResource(R.string.eink_readme_intro),
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(
                text = stringResource(R.string.eink_readme_modes),
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
            )
        }
    }

    if (showGesturesReadMe) {
        BottomSheet(onDismiss = { showGesturesReadMe = false }) {
            Text(
                text = stringResource(R.string.lockscreen_readme),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(
                text = stringResource(R.string.gestures_readme_nav),
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(
                text = stringResource(R.string.gestures_readme_apps),
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
            )
        }
    }


}

private data class SettingsCategoryInfo(
    val sectionTitleRes: Int,
    val cardTitleRes: Int,
    val subtitleRes: Int,
    val icon: ImageVector,
)

private val settingsCategories = listOf(
    SettingsCategoryInfo(R.string.section_appearance, R.string.section_appearance, R.string.cat_appearance_sub, Icons.Rounded.Palette),
    SettingsCategoryInfo(R.string.section_home, R.string.cat_home, R.string.cat_home_sub, Icons.Rounded.Home),
    SettingsCategoryInfo(R.string.section_all_apps, R.string.section_all_apps, R.string.cat_all_apps_sub, Icons.Rounded.Apps),
    SettingsCategoryInfo(R.string.section_gestures, R.string.section_gestures, R.string.cat_gestures_sub, Icons.Rounded.Gesture),
    SettingsCategoryInfo(R.string.section_notifications, R.string.section_notifications, R.string.cat_notifications_sub, Icons.Rounded.Notifications),
    SettingsCategoryInfo(R.string.section_eink, R.string.section_eink, R.string.cat_eink_sub, Icons.Rounded.Contrast),

    SettingsCategoryInfo(R.string.section_system, R.string.section_system, R.string.cat_system_sub, Icons.Rounded.Tune),
    SettingsCategoryInfo(R.string.section_about, R.string.section_about, R.string.cat_about_sub, Icons.Rounded.Info),
)

@Composable
private fun SettingsCategoryGrid(
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        settingsCategories.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                pair.forEach { info ->
                    SettingsCategoryCard(
                        info = info,
                        onClick = { onSelect(info.sectionTitleRes) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(4.dp),
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SettingsCategoryCard(
    info: SettingsCategoryInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = LocalInk.current
    Column(
        modifier = modifier
            .border(2.dp, ink, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = info.icon,
            contentDescription = null,
            tint = ink,
            modifier = Modifier
                .align(Alignment.End)
                .size(30.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(info.cardTitleRes),
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = LatoFamily,
            color = ink,
            maxLines = 2,
        )
        Text(
            text = stringResource(info.subtitleRes),
            fontSize = 15.sp,
            fontFamily = LatoFamily,
            color = ink,
            maxLines = 2,
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 3.dp),
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = LatoFamily,
            color = LocalInk.current,
        )
        Spacer(Modifier.height(3.dp))
        Box(Modifier.fillMaxWidth().height(2.dp).background(LocalInk.current))
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = LocalInk.current,
            )
            Text(
                text = description,
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
            )
        }
        Spacer(Modifier.width(16.dp))
        // Toggle: pill track with sliding dot
        val trackShape = RoundedCornerShape(12.dp)
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 22.dp)
                .then(
                    if (checked) Modifier.background(LocalInk.current, trackShape)
                    else Modifier.border(2.dp, LocalInk.current, trackShape)
                )
                .clickable { onCheckedChange(!checked) },
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(14.dp)
                    .background(
                        if (checked) LocalSurface.current else LocalInk.current,
                        CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun SettingsButtonRow(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = LocalInk.current,
            )
            Text(
                text = description,
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
            )
        }
        Spacer(Modifier.width(16.dp))
        val trackShape = RoundedCornerShape(10.dp)
        Box(
            modifier = Modifier
                .border(2.5.dp, LocalInk.current, trackShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = buttonText,
                fontSize = 12.sp,
                fontFamily = LatoFamily,
                fontWeight = FontWeight.Bold,
                color = LocalInk.current,
            )
        }
    }
}

@Composable
fun SettingsActionRow(
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = LocalInk.current,
            )
            Text(
                text = description,
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
            )
        }
        Text(
            text = "\u203A",
            fontSize = 24.sp,
            color = LocalInk.current,
        )
    }
}

@Composable
private fun SettingsCycleRow(
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = LocalInk.current,
            )
            Text(
                text = description,
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
            )
        }
        val trackShape = RoundedCornerShape(10.dp)
        Box(
            modifier = Modifier
                .border(2.5.dp, LocalInk.current, trackShape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = description,
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                fontWeight = FontWeight.Bold,
                color = LocalInk.current,
            )
        }
    }
}
