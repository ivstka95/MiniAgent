package karpiuk.ivan.miniagent.data.remote

import okhttp3.Interceptor
import okhttp3.Response

class AnthropicHeaderInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("content-type", "application/json")
            .build()
        return chain.proceed(request)
    }
}
