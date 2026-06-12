package karpiuk.ivan.miniagent.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object Home

@Serializable
data class Chat(val chatId: String)
