package karpiuk.ivan.miniagent.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import karpiuk.ivan.miniagent.domain.agent.Agent
import karpiuk.ivan.miniagent.domain.model.Message
import karpiuk.ivan.miniagent.domain.repository.ChatRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val chatId: String,
    val chatTitle: String = "Chat",
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val agent: Agent,
    private val repository: ChatRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val chatId: String = checkNotNull(savedStateHandle["chatId"])

    private val _inputText = MutableStateFlow("")
    private val _isSending = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ChatUiState> = combine(
        repository.observeChats().map { chats -> chats.find { it.id == chatId }?.title ?: "Chat" },
        repository.observeMessages(chatId),
        _inputText,
        _isSending,
        _error,
    ) { title, messages, input, isSending, error ->
        ChatUiState(
            chatId = chatId,
            chatTitle = title,
            messages = messages,
            inputText = input,
            isSending = isSending,
            error = error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState(chatId = chatId))

    fun updateInput(text: String) { _inputText.value = text }

    fun sendMessage() {
        val input = _inputText.value.trim()
        if (input.isBlank() || _isSending.value) return
        viewModelScope.launch {
            _isSending.value = true
            _inputText.value = ""
            try {
                agent.sendMessage(chatId, input)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isSending.value = false
            }
        }
    }

    fun clearError() { _error.value = null }
}
