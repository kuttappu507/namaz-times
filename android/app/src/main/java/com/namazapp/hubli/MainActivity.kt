package com.namazapp.hubli

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.namazapp.hubli.notifications.NotificationHelper
import com.namazapp.hubli.ui.AppViewModel
import com.namazapp.hubli.ui.components.BackgroundWash
import com.namazapp.hubli.ui.components.LocalPageVisible
import com.namazapp.hubli.ui.components.pressScale
import com.namazapp.hubli.ui.monthly.MonthlyScreen
import com.namazapp.hubli.ui.settings.SettingsScreen
import com.namazapp.hubli.ui.splash.SplashScreen
import com.namazapp.hubli.ui.theme.AppTheme
import com.namazapp.hubli.ui.theme.Emerald40
import com.namazapp.hubli.ui.theme.Emerald60
import com.namazapp.hubli.ui.theme.Gold60
import com.namazapp.hubli.ui.theme.NamazAppTheme
import com.namazapp.hubli.ui.today.TodayScreen
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // System splash (icon on dark green) → immediately hands over to our Compose splash.
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            NamazAppTheme(themeMode = settings.themeMode) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Root(viewModel)
                }
            }
        }
    }
}

/** Shows the animated splash once per process, then cross-fades into the app. */
@Composable
private fun Root(viewModel: AppViewModel) {
    var splashDone by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = splashDone,
            enter = fadeIn(tween(500)) + scaleIn(initialScale = 0.96f, animationSpec = tween(600, easing = EaseOutCubic)),
            exit = fadeOut(),
        ) {
            NamazApp(viewModel)
        }
        AnimatedVisibility(visible = !splashDone, enter = fadeIn(), exit = fadeOut(tween(300))) {
            SplashScreen(onFinished = { splashDone = true })
        }
    }
}

/* ------------------------------------------------------------------------------------------ */

private sealed class Screen(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    data object Today : Screen("today", R.string.nav_today, Icons.Filled.WbTwilight, Icons.Outlined.WbTwilight)
    data object Monthly : Screen("monthly", R.string.nav_monthly, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
    data object Settings : Screen("settings", R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings)

    companion object {
        val all = listOf(Today, Monthly, Settings)
    }
}

@Composable
private fun NamazApp(viewModel: AppViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val todayState by viewModel.todayState.collectAsStateWithLifecycle()
    val monthlyState by viewModel.monthlyState.collectAsStateWithLifecycle()

    // ---- Permission state, refreshed every time the app returns to the foreground ----
    var notificationGranted by remember { mutableStateOf(NotificationHelper.hasPermission(context)) }
    var exactAlarmAllowed by remember { mutableStateOf(viewModel.canScheduleExactAlarms()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationGranted = NotificationHelper.hasPermission(context)
                exactAlarmAllowed = viewModel.canScheduleExactAlarms()
                viewModel.rescheduleAlarms()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationGranted = granted
        if (granted) viewModel.rescheduleAlarms()
    }
    val requestNotificationPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openAppNotificationSettings(context)
        }
    }
    val openExactAlarmSettings = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
            )
        }
    }

    // Ask for the notification permission once on first launch (Android 13+).
    LaunchedEffect(Unit) {
        if (!notificationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /*
     * Navigation = a pager that keeps all three tabs alive. Switching tabs only *translates*
     * already-laid-out content (no screen is created, destroyed, faded or re-composited), and
     * the nav indicator is driven by the very same scroll offset – so tap-to-switch and swipe
     * both move as one piece, at the display's native frame rate.
     */
    val pagerState = rememberPagerState(pageCount = { Screen.all.size })
    val scope = rememberCoroutineScope()
    val navigateTo: (Int) -> Unit = { index ->
        if (index != pagerState.targetPage) {
            scope.launch {
                pagerState.animateScrollToPage(
                    page = index,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 340f),
                )
            }
        }
    }
    BackHandler(enabled = pagerState.currentPage != 0) { navigateTo(0) }

    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // Space reserved under content for the floating nav bar
    val contentPadding = PaddingValues(top = statusTop, bottom = navBottom + 92.dp)
    val dimColor = MaterialTheme.colorScheme.background

    Box(Modifier.fillMaxSize()) {
        // Soft colour wash behind everything (cached – no per-frame cost)
        BackgroundWash(
            modifier = Modifier.fillMaxSize(),
            primary = Emerald60,
            secondary = Gold60,
            primaryAlpha = 0.16f,
            secondaryAlpha = if (AppTheme.extended.isDark) 0.10f else 0.12f,
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = Screen.all.size - 1, // keep every tab composed
            key = { Screen.all[it].route },
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                snapAnimationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 340f),
            ),
        ) { page ->
            // Parked pages pause their ambient effects and hold their entrance until shown.
            val visible by remember {
                derivedStateOf { pagerState.distanceTo(page) < 1f }
            }
            CompositionLocalProvider(LocalPageVisible provides visible) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Subtle depth: the page leaving/entering is scaled down a touch.
                            // Read here (draw phase) so scrolling never recomposes the page.
                            val d = pagerState.distanceTo(page).coerceIn(0f, 1f)
                            val s = 1f - 0.05f * d
                            scaleX = s
                            scaleY = s
                        },
                ) {
                    when (Screen.all[page]) {
                        Screen.Today -> TodayScreen(
                            state = todayState,
                            contentPadding = contentPadding,
                            notificationsBlocked = !notificationGranted,
                            exactAlarmsBlocked = !exactAlarmAllowed,
                            onFixNotifications = requestNotificationPermission,
                            onFixExactAlarms = openExactAlarmSettings,
                        )
                        Screen.Monthly -> MonthlyScreen(
                            state = monthlyState,
                            contentPadding = contentPadding,
                            onPrevious = viewModel::previousMonth,
                            onNext = viewModel::nextMonth,
                            onToday = viewModel::jumpToCurrentMonth,
                        )
                        Screen.Settings -> SettingsScreen(
                            settings = settings,
                            contentPadding = contentPadding,
                            notificationPermissionGranted = notificationGranted,
                            exactAlarmAllowed = exactAlarmAllowed,
                            onRequestNotificationPermission = requestNotificationPermission,
                            onOpenExactAlarmSettings = openExactAlarmSettings,
                            onMadhab = viewModel::setMadhab,
                            onUse24Hour = viewModel::setUse24Hour,
                            onThemeMode = viewModel::setThemeMode,
                            onNotificationsEnabled = viewModel::setNotificationsEnabled,
                            onNotify = viewModel::setNotify,
                            onReminderMinutes = viewModel::setReminderMinutes,
                            onVibrate = viewModel::setVibrate,
                            onHijriAdjustment = viewModel::setHijriAdjustment,
                        )
                    }
                    // Dims the page in proportion to how far off-centre it is. A flat rect
                    // with layer alpha – no off-screen buffer, unlike fading the page itself.
                    Box(
                        Modifier
                            .matchParentSize()
                            .graphicsLayer { alpha = 0.55f * pagerState.distanceTo(page).coerceIn(0f, 1f) }
                            .background(dimColor),
                    )
                }
            }
        }

        FloatingNavBar(
            pagerState = pagerState,
            onSelect = navigateTo,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navBottom + 14.dp),
        )
    }
}

/** How far [page] is from the viewport centre, in pages (0 = fully shown, 1 = fully off). */
private fun PagerState.distanceTo(page: Int): Float =
    ((currentPage - page) + currentPageOffsetFraction).absoluteValue

/* ------------------------------------------------------------------------------------------ */

/**
 * Pill-shaped floating navigation bar.
 *
 * Every tab has the same fixed width and a single highlight indicator slides between them.
 * The indicator's position is derived from the pager's scroll offset inside a `graphicsLayer`
 * block, so it tracks the page pixel-for-pixel without ever recomposing or re-measuring.
 */
@Composable
private fun FloatingNavBar(
    pagerState: PagerState,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ext = AppTheme.extended
    val tabWidth = 96.dp
    val tabHeight = 54.dp
    val selectedIndex = pagerState.targetPage

    Box(
        modifier
            .shadow(
                elevation = 22.dp,
                shape = CircleShape,
                ambientColor = Emerald40.copy(alpha = if (ext.isDark) 0.45f else 0.25f),
                spotColor = Emerald40.copy(alpha = if (ext.isDark) 0.55f else 0.35f),
            )
            .clip(CircleShape)
            .background(ext.navGlass)
            .border(1.dp, if (ext.isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.9f), CircleShape)
            .padding(5.dp),
    ) {
        // Sliding indicator (drawn under the items), bound to the pager position
        Box(
            Modifier
                .graphicsLayer {
                    translationX = (pagerState.currentPage + pagerState.currentPageOffsetFraction) * tabWidth.toPx()
                }
                .size(tabWidth, tabHeight)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)),
                    ),
                ),
        )
        Row {
            Screen.all.forEachIndexed { index, screen ->
                NavItem(
                    screen = screen,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    modifier = Modifier.size(tabWidth, tabHeight),
                )
            }
        }
    }
}

@Composable
private fun NavItem(screen: Screen, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val fg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(240), label = "navFg",
    )
    val iconScale = animateFloatAsState(
        if (selected) 1.08f else 1f,
        spring(dampingRatio = 0.6f, stiffness = 500f), label = "navIconScale",
    )
    Column(
        modifier
            .clip(CircleShape)
            .selectable(selected = selected, onClick = onClick, interactionSource = interaction, indication = null)
            .pressScale(interaction, 0.92f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Crossfade(targetState = selected, animationSpec = tween(200), label = "navIcon") { isSelected ->
            Icon(
                if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                contentDescription = stringResource(screen.labelRes),
                tint = fg,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer { scaleX = iconScale.value; scaleY = iconScale.value },
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            stringResource(screen.labelRes),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.3.sp),
            color = fg,
            maxLines = 1,
        )
    }
}

private fun openAppNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}
