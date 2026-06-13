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
import karpiuk.ivan.miniagent.domain.context.ContextStrategy
import karpiuk.ivan.miniagent.domain.context.ContextStrategyManager
import karpiuk.ivan.miniagent.domain.context.ContextStrategyType
import karpiuk.ivan.miniagent.domain.context.FactsStrategy
import karpiuk.ivan.miniagent.domain.context.NoCompressionStrategy
import karpiuk.ivan.miniagent.domain.context.SlidingWindowStrategy
import karpiuk.ivan.miniagent.domain.context.SummarizationStrategy
import karpiuk.ivan.miniagent.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
        @ApplicationScope
        fun provideApplicationScope(): CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.Default)

        @Provides
        @Singleton
        fun provideStrategyMap(
            none: NoCompressionStrategy,
            summarization: SummarizationStrategy,
            slidingWindow: SlidingWindowStrategy,
            facts: FactsStrategy,
        ): Map<ContextStrategyType, ContextStrategy> = mapOf(
            ContextStrategyType.NONE to none,
            ContextStrategyType.SUMMARIZATION to summarization,
            ContextStrategyType.SLIDING_WINDOW to slidingWindow,
            ContextStrategyType.FACTS to facts,
        )

        @Provides
        @Singleton
        fun provideAgent(
            repository: ChatRepository,
            llmClient: LlmClient,
            @ApplicationScope scope: CoroutineScope,
            strategyManager: ContextStrategyManager,
            strategies: @JvmSuppressWildcards Map<ContextStrategyType, ContextStrategy>,
        ): Agent = Agent(repository, llmClient, scope, strategyManager, strategies)
    }
}
