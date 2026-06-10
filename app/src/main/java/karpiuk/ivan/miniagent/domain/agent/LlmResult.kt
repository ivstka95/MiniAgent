package karpiuk.ivan.miniagent.domain.agent

data class LlmResult(
    val assistantText: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val thinkingTokens: Int = 0,
)
