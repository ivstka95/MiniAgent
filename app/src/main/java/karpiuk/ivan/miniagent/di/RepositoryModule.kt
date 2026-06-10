package karpiuk.ivan.miniagent.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import karpiuk.ivan.miniagent.data.repository.ChatRepositoryImpl
import karpiuk.ivan.miniagent.data.repository.LlmClientImpl
import karpiuk.ivan.miniagent.domain.agent.Agent
import karpiuk.ivan.miniagent.domain.agent.LlmClient
import karpiuk.ivan.miniagent.domain.repository.ChatRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindLlmClient(impl: LlmClientImpl): LlmClient

    companion object {
        @Provides
        @Singleton
        fun provideAgent(repository: ChatRepository, llmClient: LlmClient): Agent =
            Agent(repository, llmClient)
    }
}
