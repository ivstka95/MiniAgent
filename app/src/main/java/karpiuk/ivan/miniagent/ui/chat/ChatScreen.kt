package karpiuk.ivan.miniagent.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import karpiuk.ivan.miniagent.domain.context.ContextStrategyType
import karpiuk.ivan.miniagent.domain.model.Message
import karpiuk.ivan.miniagent.domain.model.Role

@Composable
fun ChatRoute(
    onOpenDrawer: () -> Unit,
    onStrategyChange: (ContextStrategyType) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChatScreen(
        state = state,
        onOpenDrawer = onOpenDrawer,
        onInputChange = viewModel::updateInput,
        onSendMessage = viewModel::sendMessage,
        onSendBigText = viewModel::sendBigText,
        onClearError = viewModel::clearError,
        onStrategyChange = viewModel::setStrategy,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    onOpenDrawer: () -> Unit,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onSendBigText: () -> Unit,
    onClearError: () -> Unit,
    onStrategyChange: (ContextStrategyType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(state.error) {
        if (state.error != null) {
            snackbarHostState.showSnackbar(
                message = state.error,
                actionLabel = "Dismiss",
                duration = SnackbarDuration.Indefinite,
            )
            onClearError()
        }
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(state.chatTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Open drawer")
                    }
                },
                actions = {
                    TextButton(
                        onClick = onSendBigText,
                        enabled = !state.isSending,
                    ) {
                        Text("Big")
                    }
                    var strategyMenuExpanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { strategyMenuExpanded = true }) {
                            Text(state.activeStrategyType.name)
                        }
                        DropdownMenu(
                            expanded = strategyMenuExpanded,
                            onDismissRequest = { strategyMenuExpanded = false },
                        ) {
                            ContextStrategyType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name) },
                                    onClick = {
                                        onStrategyChange(type)
                                        strategyMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .fillMaxSize(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(state.messages, key = { it.id }) { message ->
                    MessageItem(message)
                }
            }
            if (state.isSending) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            state.tokenStatsDisplay?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
            MessageInputRow(
                text = state.inputText,
                isSending = state.isSending,
                onTextChange = onInputChange,
                onSend = onSendMessage,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Mini Agent") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Open drawer")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text("Select or create a chat")
        }
    }
}

private const val COLLAPSED_MAX_LINES = 5

@Composable
fun MessageItem(
    message: Message,
    modifier: Modifier = Modifier,
) {
    val isUser = message.role == Role.USER
    var expanded by remember { mutableStateOf(false) }
    var hasOverflow by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            Column(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isUser)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                    )
                    .clickable(enabled = hasOverflow || expanded) { expanded = !expanded }
                    .padding(8.dp),
            ) {
                Text(
                    text = message.content,
                    maxLines = if (expanded) Int.MAX_VALUE else COLLAPSED_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { if (!expanded) hasOverflow = it.hasVisualOverflow },
                )
                if (hasOverflow || expanded) {
                    Text(
                        text = if (expanded) "Show less" else "Show more",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            if (message.tokenCount != null) {
                Text(
                    text = "${message.tokenCount} tok",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun MessageInputRow(
    text: String,
    isSending: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Message") },
            maxLines = 4,
        )
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onSend,
            enabled = text.isNotBlank() && !isSending,
        ) {
            Text("Send")
        }
    }
}
