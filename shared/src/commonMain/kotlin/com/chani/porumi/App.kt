package com.chani.porumi

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.key.Keyer
import coil3.request.crossfade
import com.chani.porumi.data.isSupabaseConfigured
import com.chani.porumi.feature.AppState
import com.chani.porumi.feature.MainViewModel
import com.chani.porumi.feature.pet.FirstPetScreen
import com.chani.porumi.feature.splash.SplashScreen
import kotlinx.coroutines.delay
import com.chani.porumi.feature.analyze.AnalyzeScreen
import com.chani.porumi.feature.auth.AuthScreen
import com.chani.porumi.feature.detail.DetailScreen
import com.chani.porumi.feature.home.HomeScreen
import com.chani.porumi.feature.notifications.NotificationsScreen
import com.chani.porumi.feature.legal.LegalDoc
import com.chani.porumi.feature.legal.LegalScreen
import com.chani.porumi.feature.pet.PetManageScreen
import com.chani.porumi.feature.settings.AccountScreen
import com.chani.porumi.feature.settings.LicensesScreen
import com.chani.porumi.feature.settings.MyScreen
import com.chani.porumi.feature.settings.NotificationSettingsScreen
import com.chani.porumi.feature.settings.SupportScreen
import com.chani.porumi.feature.report.ReportScreen
import com.chani.porumi.ui.components.PorumiTabBar
import com.chani.porumi.ui.components.PrimaryButton
import com.chani.porumi.ui.components.TabItem
import com.chani.porumi.ui.theme.PorumiTheme
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.coil.coil3
import io.github.jan.supabase.storage.StorageItem
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import com.chani.porumi.ui.components.AdaptiveWidth
import com.chani.porumi.ui.components.centeredMaxSize

@Serializable data object HomeRoute
@Serializable data object ReportRoute
@Serializable data object RecordRoute
@Serializable data object MyRoute
@Serializable data object AccountRoute
@Serializable data class PetManageRoute(val openAdd: Boolean = false)
@Serializable data object NotificationRoute
@Serializable data object SupportRoute
@Serializable data object LicensesRoute
@Serializable data object NotificationsRoute
@Serializable data class DetailRoute(val id: String)
@Serializable data class LegalRoute(val doc: String)

@Composable
fun App() {
    PorumiTheme {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            if (!isSupabaseConfigured) {
                ConfigMissingScreen()
            } else {
                Root()
            }
        }
    }
}

@OptIn(SupabaseExperimental::class)
@Composable
private fun Root(viewModel: MainViewModel = koinViewModel()) {
    val supabase = koinInject<SupabaseClient>()
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(supabase.coil3)
                // Storage 경로를 메모리 캐시 키로 사용
                add(Keyer<StorageItem> { data, _ -> "${data.bucketId}/${data.path}" })
            }
            .crossfade(true)
            .build()
    }

    val appState by viewModel.appState.collectAsStateWithLifecycle()

    // 스플래시는 최소 SPLASH_MIN_MS 동안 보여주고, 로딩이 더 걸리면 끝날 때까지 유지
    var minTimeElapsed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(SPLASH_MIN_MS)
        minTimeElapsed = true
    }
    val screen: AppState? = if (!minTimeElapsed || appState == AppState.Loading) null else appState

    Crossfade(targetState = screen, animationSpec = tween(durationMillis = 400)) { state ->
        RootContent(state, onRetry = viewModel::load)
    }
}

private const val SPLASH_MIN_MS = 1400L

@Composable
private fun RootContent(state: AppState?, onRetry: () -> Unit) {
    when (state) {
        null, AppState.Loading -> SplashScreen()
        AppState.SignedOut -> AuthScreen()
        AppState.NeedsFirstPet -> FirstPetScreen()
        AppState.LoadFailed -> Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("정보를 불러오지 못했어요")
            PrimaryButton(onClick = onRetry) { Text("다시 시도") }
        }
        AppState.Ready -> MainScaffold()
    }
}

private data class Tab(val item: TabItem, val route: Any)

// 탭 아이콘은 둥근 채움 아이콘, 선택 여부는 색과 말랑한 배경으로 구분
private val tabs = listOf(
    Tab(TabItem("홈", Icons.Rounded.Home), HomeRoute),
    Tab(TabItem("리포트", Icons.Rounded.Insights), ReportRoute),
    Tab(TabItem("기록", Icons.Rounded.PhotoCamera), RecordRoute),
    Tab(TabItem("마이", Icons.Rounded.Person), MyRoute),
)

@Composable
private fun MainScaffold() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    // 하단 탭은 최상위 4개 화면에서만 표시
    val selectedTab = tabs.indexOfFirst { destination?.hasRoute(it.route::class) == true }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (selectedTab >= 0) {
                PorumiTabBar(
                    items = tabs.map { it.item },
                    selectedIndex = selectedTab,
                    onSelect = { navController.navigateTab(tabs[it].route) },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            // 상태바 인셋은 여기서 소비 → 하위 화면의 내비게이션 바가 중복으로 띄우지 않음
            // 폴더블 펼침·태블릿·가로 모드: 내용 폭을 제한하고 가운데 정렬 (모든 하위 화면에 적용)
            modifier = Modifier.padding(padding).consumeWindowInsets(padding).centeredMaxSize(AdaptiveWidth.Content),
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    onPhotoClick = { navController.navigate(DetailRoute(it)) },
                    onWeekClick = { navController.navigateTab(ReportRoute) },
                    onAnalyzeClick = { navController.navigateTab(RecordRoute) },
                    onAddPet = { navController.navigate(PetManageRoute(openAdd = true)) },
                    onNotifications = { navController.navigate(NotificationsRoute) },
                )
            }
            composable<NotificationsRoute> { NotificationsScreen(onBack = { navController.popBackStack() }) }
            composable<ReportRoute> {
                ReportScreen(
                    onPhotoClick = { navController.navigate(DetailRoute(it)) },
                    onAddPet = { navController.navigate(PetManageRoute(openAdd = true)) },
                )
            }
            composable<RecordRoute> {
                AnalyzeScreen(
                    onGoHome = { navController.navigateTab(HomeRoute) },
                    onAddPet = { navController.navigate(PetManageRoute(openAdd = true)) },
                )
            }
            composable<MyRoute> {
                MyScreen(
                    onAccount = { navController.navigate(AccountRoute) },
                    onPets = { navController.navigate(PetManageRoute()) },
                    onAddPet = { navController.navigate(PetManageRoute(openAdd = true)) },
                    onNotifications = { navController.navigate(NotificationRoute) },
                    onSupport = { navController.navigate(SupportRoute) },
                    onLicenses = { navController.navigate(LicensesRoute) },
                    onOpenLegal = { navController.navigate(LegalRoute(it.name)) },
                )
            }
            composable<AccountRoute> { AccountScreen(onBack = { navController.popBackStack() }) }
            composable<PetManageRoute> { entry ->
                PetManageScreen(
                    onBack = { navController.popBackStack() },
                    openAddDialog = entry.toRoute<PetManageRoute>().openAdd,
                )
            }
            composable<NotificationRoute> { NotificationSettingsScreen(onBack = { navController.popBackStack() }) }
            composable<SupportRoute> { SupportScreen(onBack = { navController.popBackStack() }) }
            composable<LicensesRoute> { LicensesScreen(onBack = { navController.popBackStack() }) }
            composable<LegalRoute> { entry ->
                LegalScreen(
                    doc = LegalDoc.valueOf(entry.toRoute<LegalRoute>().doc),
                    onBack = { navController.popBackStack() },
                )
            }
            composable<DetailRoute> { entry ->
                DetailScreen(
                    analysisId = entry.toRoute<DetailRoute>().id,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun NavHostController.navigateTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun ConfigMissingScreen() {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text("Supabase 설정이 필요해요", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "프로젝트 루트의 local.properties 에 아래 두 줄을 추가한 뒤 다시 빌드해 주세요.\n\n" +
                "supabase.url=https://<project-ref>.supabase.co\n" +
                "supabase.key=<publishable 또는 anon key>",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
