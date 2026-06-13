package karpiuk.ivan.miniagent.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import karpiuk.ivan.miniagent.domain.agent.Agent
import karpiuk.ivan.miniagent.domain.model.Message
import karpiuk.ivan.miniagent.domain.model.Role
import karpiuk.ivan.miniagent.domain.context.ContextStrategyManager
import karpiuk.ivan.miniagent.domain.context.ContextStrategyType
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

fun interface BigTextProvider {
    suspend fun load(): String
}

data class ChatUiState(
    val chatId: String,
    val chatTitle: String = "Chat",
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val error: String? = null,
    val tokenStatsDisplay: String? = null,
    val activeStrategyType: ContextStrategyType = ContextStrategyType.NONE,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val agent: Agent,
    private val repository: ChatRepository,
    savedStateHandle: SavedStateHandle,
    private val bigTextProvider: BigTextProvider,
    private val strategyManager: ContextStrategyManager,
) : ViewModel() {

    private val chatId: String = checkNotNull(savedStateHandle["chatId"])

    private val _inputText = MutableStateFlow("")
    private val _isSending = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    private data class CoreState(
        val title: String,
        val messages: List<Message>,
        val input: String,
        val isSending: Boolean,
        val error: String?,
    )

    val uiState = combine(
        combine(
            repository.observeChats().map { chats -> chats.find { it.id == chatId }?.title ?: "Chat" },
            repository.observeMessages(chatId),
            _inputText,
            _isSending,
            _error,
        ) { title, messages, input, isSending, error ->
            CoreState(title, messages, input, isSending, error)
        },
        strategyManager.activeType,
    ) { core, strategyType ->
        var inputToks = 0; var outputToks = 0
        core.messages.forEach { m ->
            when (m.role) {
                Role.USER -> inputToks += m.tokenCount ?: 0
                Role.ASSISTANT -> outputToks += m.tokenCount ?: 0
            }
        }
        val tokenStatsDisplay = if (core.messages.isNotEmpty()) {
            val cost = (inputToks * 1.0 + outputToks * 5.0) / 1_000_000
            "Σ ${inputToks + outputToks} tokens · ${String.format(java.util.Locale.US, "\$%.6f", cost)}"
        } else null
        ChatUiState(
            chatId = chatId,
            chatTitle = core.title,
            messages = core.messages,
            inputText = core.input,
            isSending = core.isSending,
            error = core.error,
            tokenStatsDisplay = tokenStatsDisplay,
            activeStrategyType = strategyType,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState(chatId = chatId))

    fun updateInput(text: String) { _inputText.value = text }

    fun sendMessage() {
        val input = _inputText.value.trim()
        if (input.isBlank()) return
        sendWithGuard {
            _inputText.value = ""
            agent.sendMessage(chatId, input)
        }
    }

    fun sendBigText() = sendWithGuard {
        agent.sendMessage(chatId, bigTextProvider.load())
    }

    fun clearError() { _error.value = null }

    fun setStrategy(type: ContextStrategyType) {
        strategyManager.setStrategy(type)
    }

    private fun sendWithGuard(block: suspend () -> Unit) {
        if (_isSending.value) return
        viewModelScope.launch {
            _isSending.value = true
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isSending.value = false
            }
        }
    }
}
