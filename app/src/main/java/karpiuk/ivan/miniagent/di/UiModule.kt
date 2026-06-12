package karpiuk.ivan.miniagent.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import karpiuk.ivan.miniagent.R
import karpiuk.ivan.miniagent.ui.chat.BigTextProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Module
@InstallIn(SingletonComponent::class)
object UiModule {

    @Provides
    fun provideBigTextProvider(@ApplicationContext context: Context): BigTextProvider =
        BigTextProvider {
            withContext(Dispatchers.IO) {
                context.resources.openRawResource(R.raw.big_text).use { it.reader().readText() }
            }
        }
}
