package karpiuk.ivan.miniagent.domain.context

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextStrategyManager @Inject constructor() {
    private val _activeType = MutableStateFlow(ContextStrategyType.NONE)
    val activeType: StateFlow<ContextStrategyType> = _activeType.asStateFlow()

    fun setStrategy(type: ContextStrategyType) {
        _activeType.value = type
    }
}
