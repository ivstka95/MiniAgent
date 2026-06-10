package karpiuk.ivan.miniagent.ui.chats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import karpiuk.ivan.miniagent.domain.model.Chat

@Composable
fun ChatListDrawer(
    chats: List<Chat>,
    onNewChat: () -> Unit,
    onSelectChat: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxHeight()) {
        TextButton(
            onClick = onNewChat,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("New chat")
        }
        HorizontalDivider()
        LazyColumn {
            items(chats, key = { it.id }) { chat ->
                ListItem(
                    headlineContent = { Text(chat.title) },
                    modifier = Modifier.clickable { onSelectChat(chat.id) },
                )
            }
        }
    }
}
