package com.example.ritmo.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ritmo.RitmoViewModel
import com.example.ritmo.data.Song
import com.example.ritmo.ui.components.AddToPlaylistSheet
import com.example.ritmo.ui.components.LocalAddToPlaylist
import com.example.ritmo.ui.components.LocalSongMenu
import com.example.ritmo.ui.components.MiniPlayer
import com.example.ritmo.ui.components.NameDialog
import com.example.ritmo.ui.components.SongOptionsSheet
import com.example.ritmo.ui.screens.AlbumScreen
import com.example.ritmo.ui.screens.ArtistScreen
import com.example.ritmo.ui.screens.FavoritesScreen
import com.example.ritmo.ui.screens.HomeScreen
import com.example.ritmo.ui.screens.LibraryScreen
import com.example.ritmo.ui.screens.NowPlayingScreen
import com.example.ritmo.ui.screens.PlaylistScreen
import com.example.ritmo.ui.screens.SearchScreen
import com.example.ritmo.ui.screens.SettingsScreen
import com.example.ritmo.ui.theme.RitmoTheme
import kotlinx.coroutines.launch

@Composable
fun RitmoRoot(vm: RitmoViewModel = viewModel()) {
    val themeMode by vm.themeMode.collectAsStateWithLifecycle()
    RitmoTheme(themeMode) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            PermissionGate(vm) { AppShell(vm) }
        }
    }
}

// ---------------- Permissão de acesso às músicas ----------------

@Composable
private fun PermissionGate(vm: RitmoViewModel, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val audioPermission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    fun isGranted() =
        ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED

    var granted by remember { mutableStateOf(isGranted()) }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        if (ok && Build.VERSION.SDK_INT >= 33) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    // Se o usuário liberou pelas configurações do sistema, vale ao voltar para o app.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = isGranted() }
    LaunchedEffect(granted) { if (granted) vm.onPermissionGranted() }

    if (granted) {
        content()
    } else {
        PermissionScreen(
            onAllow = { launcher.launch(audioPermission) },
            onOpenSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                )
            },
        )
    }
}

@Composable
private fun PermissionScreen(onAllow: () -> Unit, onOpenSettings: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = colors.primary, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(20.dp))
        Text("Ritmo", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Para tocar suas músicas, o Ritmo precisa de acesso aos arquivos de áudio do aparelho. Nada sai do seu telefone.",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Button(onClick = onAllow) { Text("Permitir acesso às músicas") }
        TextButton(onClick = onOpenSettings) { Text("Abrir configurações do app") }
    }
}

// ---------------- Estrutura principal ----------------

private fun NavController.navigateTop(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun AppShell(vm: RitmoViewModel) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val playerState by vm.playerState.collectAsStateWithLifecycle()
    val favoriteIds by vm.favoriteIds.collectAsStateWithLifecycle()
    val playlists by vm.playlists.collectAsStateWithLifecycle()

    var nowPlayingOpen by rememberSaveable { mutableStateOf(false) }
    var currentTab by rememberSaveable { mutableStateOf(Routes.HOME) }
    var menuSong by remember { mutableStateOf<Song?>(null) }
    var addTarget by remember { mutableStateOf<List<Song>?>(null) }
    var newPlaylistFor by remember { mutableStateOf<List<Song>?>(null) }

    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    LaunchedEffect(route) {
        if (route == Routes.HOME || route == Routes.SEARCH || route == Routes.LIBRARY) currentTab = route
    }

    LaunchedEffect(Unit) {
        vm.messages.collect { message ->
            launch {
                snackbar.currentSnackbarData?.dismiss()
                snackbar.showSnackbar(message)
            }
        }
    }

    val openSongMenu = remember { { song: Song -> menuSong = song } }
    val openAddToPlaylist = remember { { songs: List<Song> -> addTarget = songs } }

    CompositionLocalProvider(
        LocalSongMenu provides openSongMenu,
        LocalAddToPlaylist provides openAddToPlaylist,
    ) {
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    Column {
                        MiniPlayer(vm, playerState, onOpen = { nowPlayingOpen = true })
                        BottomBar(currentTab) { nav.navigateTop(it) }
                    }
                },
            ) { padding ->
                NavHost(
                    navController = nav,
                    startDestination = Routes.HOME,
                    modifier = Modifier.padding(padding),
                    enterTransition = { fadeIn(tween(180)) },
                    exitTransition = { fadeOut(tween(120)) },
                    popEnterTransition = { fadeIn(tween(180)) },
                    popExitTransition = { fadeOut(tween(120)) },
                ) {
                    composable(Routes.HOME) { HomeScreen(vm, nav) }
                    composable(Routes.SEARCH) { SearchScreen(vm, nav) }
                    composable(Routes.LIBRARY) { LibraryScreen(vm, nav) }
                    composable(Routes.FAVORITES) { FavoritesScreen(vm, nav) }
                    composable(Routes.SETTINGS) { SettingsScreen(vm, nav) }
                    composable(
                        Routes.PLAYLIST,
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) { entry -> PlaylistScreen(vm, nav, entry.arguments?.getLong("id") ?: -1L) }
                    composable(
                        Routes.ALBUM,
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) { entry -> AlbumScreen(vm, nav, entry.arguments?.getLong("id") ?: -1L) }
                    composable(
                        Routes.ARTIST,
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) { entry -> ArtistScreen(vm, nav, entry.arguments?.getLong("id") ?: -1L) }
                }
            }

            AnimatedVisibility(
                visible = nowPlayingOpen && playerState.hasQueue,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
            ) {
                NowPlayingScreen(vm, onClose = { nowPlayingOpen = false })
            }
        }
    }

    BackHandler(enabled = nowPlayingOpen) { nowPlayingOpen = false }

    menuSong?.let { song ->
        SongOptionsSheet(
            song = song,
            favorite = song.id in favoriteIds,
            onDismiss = { menuSong = null },
            onPlayNext = { vm.playNext(listOf(song)) },
            onEnqueue = { vm.enqueue(listOf(song)) },
            onAddToPlaylist = { addTarget = listOf(song) },
            onToggleFavorite = { vm.toggleFavorite(song) },
            onGoAlbum = { nav.navigate(Routes.album(song.albumId)) },
            onGoArtist = { nav.navigate(Routes.artist(song.artistId)) },
        )
    }

    addTarget?.let { songs ->
        AddToPlaylistSheet(
            playlists = playlists,
            onDismiss = { addTarget = null },
            onPick = { playlist -> vm.addToPlaylist(playlist.id, playlist.name, songs) },
            onCreate = { newPlaylistFor = songs },
        )
    }

    newPlaylistFor?.let { songs ->
        NameDialog(
            title = "Nova playlist",
            initial = "",
            confirmLabel = "Criar",
            onDismiss = { newPlaylistFor = null },
            onConfirm = { name ->
                newPlaylistFor = null
                vm.createPlaylist(name, songs)
            },
        )
    }
}

@Composable
private fun BottomBar(current: String, onSelect: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val items = listOf(
        Triple(Routes.HOME, "Início", Icons.Rounded.Home),
        Triple(Routes.SEARCH, "Buscar", Icons.Rounded.Search),
        Triple(Routes.LIBRARY, "Biblioteca", Icons.Rounded.LibraryMusic),
    )
    NavigationBar(containerColor = colors.surfaceContainerLow, tonalElevation = 0.dp) {
        items.forEach { (route, label, icon) ->
            NavigationBarItem(
                selected = current == route,
                onClick = { onSelect(route) },
                icon = { Icon(icon, contentDescription = null) },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.primary,
                    selectedTextColor = colors.primary,
                    indicatorColor = colors.primary.copy(alpha = 0.16f),
                    unselectedIconColor = colors.onSurfaceVariant,
                    unselectedTextColor = colors.onSurfaceVariant,
                ),
            )
        }
    }
}
