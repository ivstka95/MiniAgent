package karpiuk.ivan.miniagent.domain.agent

data class AgentResponse(
    val replyText: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val thinkingTokens: Int = 0,
)
