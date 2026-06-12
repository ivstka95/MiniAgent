package karpiuk.ivan.miniagent.ui.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import karpiuk.ivan.miniagent.domain.model.Chat
import karpiuk.ivan.miniagent.domain.repository.ChatRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val repository: ChatRepository,
) : ViewModel() {

    val chats: StateFlow<List<Chat>> = repository.observeChats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _newChatEventChannel = Channel<String>(Channel.BUFFERED)
    val newChatEvent: Flow<String> = _newChatEventChannel.receiveAsFlow()

    fun createChat() {
        viewModelScope.launch {
            val chat = repository.createChat("New chat")
            _newChatEventChannel.send(chat.id)
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            repository.deleteChat(chatId)
        }
    }
}
