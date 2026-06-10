package karpiuk.ivan.miniagent.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import karpiuk.ivan.miniagent.BuildConfig
import karpiuk.ivan.miniagent.data.remote.AnthropicApi
import karpiuk.ivan.miniagent.data.remote.AnthropicHeaderInterceptor
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHeaderInterceptor(): AnthropicHeaderInterceptor =
        AnthropicHeaderInterceptor(BuildConfig.ANTHROPIC_API_KEY)

    @Provides
    @Singleton
    fun provideOkHttpClient(interceptor: AnthropicHeaderInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder()
            .baseUrl("https://api.anthropic.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideAnthropicApi(retrofit: Retrofit): AnthropicApi =
        retrofit.create(AnthropicApi::class.java)
}
