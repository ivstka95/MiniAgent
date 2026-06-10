package karpiuk.ivan.miniagent.data.remote

import karpiuk.ivan.miniagent.data.remote.dto.AnthropicRequestDto
import karpiuk.ivan.miniagent.data.remote.dto.AnthropicResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AnthropicApi {
    @POST("v1/messages")
    suspend fun createMessage(@Body request: AnthropicRequestDto): AnthropicResponseDto
}
