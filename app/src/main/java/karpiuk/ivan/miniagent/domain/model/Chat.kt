package karpiuk.ivan.miniagent.domain.model

data class Chat(
    val id: String,
    val title: String,
    val createdAt: Long,
    val summary: String? = null,
    val summaryCoversCount: Int = 0,
)
