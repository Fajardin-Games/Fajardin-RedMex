package com.ustadmobile.meshrabiya.testapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConnectWithoutContact
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.ustadmobile.meshrabiya.testapp.theme.MeshAmber
import com.ustadmobile.meshrabiya.testapp.theme.MeshBackground
import com.ustadmobile.meshrabiya.testapp.theme.MeshSurface
import com.ustadmobile.meshrabiya.testapp.theme.MeshText
import com.ustadmobile.meshrabiya.testapp.theme.MeshTextSecondary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ustadmobile.meshrabiya.testapp.appstate.AppUiState
import com.ustadmobile.meshrabiya.pokemon.PokemonRepository
import com.ustadmobile.meshrabiya.testapp.screens.LocalVirtualNodeScreen
import com.ustadmobile.meshrabiya.testapp.screens.MyPokedexScreen
import com.ustadmobile.meshrabiya.testapp.screens.NeighborPokedexScreen
import com.ustadmobile.meshrabiya.testapp.screens.PendingTradesScreen
import com.ustadmobile.meshrabiya.testapp.screens.ReceiveScreen
import com.ustadmobile.meshrabiya.testapp.screens.SelectDestNodeScreen
import com.ustadmobile.meshrabiya.testapp.screens.SendFileScreen
import com.ustadmobile.meshrabiya.testapp.screens.StarterPickerScreen
import com.ustadmobile.meshrabiya.testapp.theme.HttpOverBluetoothTheme
import java.net.URLEncoder
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.android.closestDI
import org.kodein.di.compose.withDI
import org.kodein.di.direct
import org.kodein.di.instance

class VNetTestActivity : ComponentActivity(), DIAware {

    override val di by closestDI()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HttpOverBluetoothTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                ) { MeshrabiyaTestApp(di) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshrabiyaTestApp(di: DI) =
        withDI(di) {
            val navController: NavHostController = rememberNavController()
            var appUiState: AppUiState by remember { mutableStateOf(AppUiState()) }

            var selectedItem: String? by remember { mutableStateOf(null) }

            val snackbarHostState = remember { SnackbarHostState() }

            Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    topBar = {
                        TopAppBar(
                                title = {
                                    Text(
                                        appUiState.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MeshAmber
                                    )
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MeshBackground,
                                    titleContentColor = MeshAmber,
                                    actionIconContentColor = MeshText,
                                ),
                                actions = {
                                    if (appUiState.topBarActionIcon != null &&
                                                    appUiState.onTopBarActionClick != null
                                    ) {
                                        androidx.compose.material3.IconButton(
                                                onClick = appUiState.onTopBarActionClick!!
                                        ) {
                                            Icon(
                                                    imageVector = appUiState.topBarActionIcon!!,
                                                    contentDescription =
                                                            appUiState.topBarActionContentDescription,
                                                    tint = MeshAmber
                                            )
                                        }
                                    }
                                }
                        )
                    },
                    floatingActionButton = {
                        if (appUiState.fabState.visible) {
                            ExtendedFloatingActionButton(
                                    onClick = appUiState.fabState.onClick,
                                    icon = {
                                        appUiState.fabState.icon?.also {
                                            Icon(imageVector = it, contentDescription = null)
                                        }
                                    },
                                    text = { Text(appUiState.fabState.label ?: "") }
                            )
                        }
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MeshSurface,
                            contentColor = MeshText,
                        ) {
                            val navItemColors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MeshAmber,
                                selectedTextColor = MeshAmber,
                                indicatorColor = MeshBackground,
                                unselectedIconColor = MeshTextSecondary,
                                unselectedTextColor = MeshTextSecondary,
                            )

                            NavigationBarItem(
                                    selected = navController.currentDestination?.route == "localvirtualnode",
                                    label = { Text("Este Nodo", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                                    alwaysShowLabel = false,
                                    onClick = { navController.navigate("localvirtualnode") },
                                    colors = navItemColors,
                                    icon = { Icon(imageVector = Icons.Default.PhoneAndroid, contentDescription = "Este Nodo") }
                            )

                            NavigationBarItem(
                                    selected = navController.currentDestination?.route == "recentchats",
                                    label = { Text("Chats", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                                    alwaysShowLabel = false,
                                    onClick = { navController.navigate("recentchats") },
                                    colors = navItemColors,
                                    icon = { Icon(imageVector = Icons.Default.ConnectWithoutContact, contentDescription = "Chats") }
                            )

                            NavigationBarItem(
                                    selected = navController.currentDestination?.route == "neighbornodes",
                                    label = { Text("Vecinos", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                                    alwaysShowLabel = false,
                                    onClick = { navController.navigate("neighbornodes") },
                                    colors = navItemColors,
                                    icon = { Icon(imageVector = Icons.Default.People, contentDescription = "Vecinos") }
                            )

                            NavigationBarItem(
                                    selected = navController.currentDestination?.route == "mypokedex",
                                    label = { Text("Pokédex", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                                    alwaysShowLabel = false,
                                    onClick = { navController.navigate("mypokedex") },
                                    colors = navItemColors,
                                    icon = { Icon(imageVector = Icons.Default.Pets, contentDescription = "Pokédex") }
                            )
                        }
                    }
            ) { contentPadding ->
                // Screen content
                Box(modifier = Modifier.padding(contentPadding)) {
                    AppNavHost(
                            navController = navController,
                            onSetAppUiState = { appUiState = it },
                            snackbarHostState = snackbarHostState,
                    )
                }
            }
        }

@Composable
fun AppNavHost(
        modifier: Modifier = Modifier,
        navController: NavHostController = rememberNavController(),
        startDestination: String = "localvirtualnode",
        onSetAppUiState: (AppUiState) -> Unit = {},
        snackbarHostState: SnackbarHostState,
) {
    val di = org.kodein.di.compose.localDI()
    val effectiveStart = remember(startDestination) {
        val repo = di.direct.instance<PokemonRepository>()
        if (repo.hasAnyPokemon()) startDestination else "starterpicker"
    }

    NavHost(
            modifier = modifier,
            navController = navController,
            startDestination = effectiveStart
    ) {
        composable("localvirtualnode") {
            LocalVirtualNodeScreen(
                    onSetAppUiState = onSetAppUiState,
                    snackbarHostState = snackbarHostState,
            )
        }

        composable("recentchats") {
            com.ustadmobile.meshrabiya.testapp.screens.RecentChatsScreen(
                    onSetAppUiState = onSetAppUiState,
                    onChatClick = { ipAddress -> navController.navigate("chat/$ipAddress") }
            )
        }

        composable("starterpicker") {
            StarterPickerScreen(
                onSelected = { entry ->
                    di.direct.instance<PokemonRepository>().addPokemon(entry)
                    navController.navigate("localvirtualnode") {
                        popUpTo("starterpicker") { inclusive = true }
                    }
                }
            )
        }

        composable("mypokedex") {
            MyPokedexScreen(
                onSetAppUiState = onSetAppUiState,
                onNavigateToTrades = { navController.navigate("pendingtrades") },
            )
        }

        composable("neighborpokedex/{ip}") { backStackEntry ->
            val ip = backStackEntry.arguments?.getString("ip") ?: return@composable
            NeighborPokedexScreen(
                targetIp = ip,
                onSetAppUiState = onSetAppUiState,
            )
        }

        composable("pendingtrades") {
            PendingTradesScreen(onSetAppUiState = onSetAppUiState)
        }

        composable("neighbornodes") {
            com.ustadmobile.meshrabiya.testapp.screens.NeighborNodeListScreen(
                    onSetAppUiState = onSetAppUiState,
                    onNodeClick = { ipAddress -> navController.navigate("chat/$ipAddress") },
                    onPokedexClick = { ipAddress -> navController.navigate("neighborpokedex/$ipAddress") },
            )
        }

        composable("chat/{address}") { backStackEntry ->
            val address = backStackEntry.arguments?.getString("address")
            com.ustadmobile.meshrabiya.testapp.screens.ChatScreen(
                    targetAddress = address,
                    onSetAppUiState = onSetAppUiState
            )
        }

        composable("send") {
            SendFileScreen(
                    onNavigateToSelectReceiveNode = { uri ->
                        navController.navigate(
                                "selectdestnode/${URLEncoder.encode(uri.toString(), "UTF-8")}"
                        )
                    },
                    onSetAppUiState = onSetAppUiState,
            )
        }

        composable("selectdestnode/{sendFileUri}") { backStackEntry ->
            val uriToSend =
                    backStackEntry.arguments?.getString("sendFileUri")
                            ?: throw IllegalArgumentException("No uri to send")
            SelectDestNodeScreen(
                    uriToSend = uriToSend,
                    navigateOnDone = { navController.popBackStack() },
                    onSetAppUiState = onSetAppUiState,
            )
        }

        composable("receive") { ReceiveScreen(onSetAppUiState = onSetAppUiState) }
    }
}
