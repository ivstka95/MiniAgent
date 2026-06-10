package karpiuk.ivan.miniagent.ui

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import karpiuk.ivan.miniagent.ui.chat.ChatRoute
import karpiuk.ivan.miniagent.ui.chat.HomeScreen
import karpiuk.ivan.miniagent.ui.chats.ChatListDrawer
import karpiuk.ivan.miniagent.ui.chats.ChatsViewModel
import karpiuk.ivan.miniagent.ui.navigation.Chat
import karpiuk.ivan.miniagent.ui.navigation.Home
import kotlinx.coroutines.launch

@Composable
fun ChatAppScreen(
    chatsViewModel: ChatsViewModel = hiltViewModel(),
) {
    val chats by chatsViewModel.chats.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    LaunchedEffect(chatsViewModel) {
        chatsViewModel.newChatEvent.collect { chatId ->
            drawerState.close()
            navController.navigate(Chat(chatId)) {
                popUpTo(Home) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                ChatListDrawer(
                    chats = chats,
                    onNewChat = { chatsViewModel.createChat() },
                    onSelectChat = { chatId ->
                        scope.launch { drawerState.close() }
                        navController.navigate(Chat(chatId)) {
                            popUpTo(Home) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                )
            }
        },
    ) {
        NavHost(navController = navController, startDestination = Home) {
            composable<Home> {
                HomeScreen(onOpenDrawer = { scope.launch { drawerState.open() } })
            }
            composable<Chat> {
                ChatRoute(onOpenDrawer = { scope.launch { drawerState.open() } })
            }
        }
    }
}
