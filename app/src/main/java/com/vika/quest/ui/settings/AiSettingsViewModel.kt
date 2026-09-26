package com.vika.quest.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vika.quest.ai.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AiSettingsUiState(
    val baseUrl: String,
    val model: String,
    val apiKey: String = "",
    val hasSavedKey: Boolean = false,
    val isTesting: Boolean = false,
    val availableModels: List<String> = emptyList(),
    val message: String? = null,
) {
    val activeProvider: String get() = if (hasSavedKey) "DeepSeek" else "离线测试提供器"
}
class AiSettingsViewModel(private val store: AiSettingsStore, private val provider: AiProvider) : ViewModel() {
    private val initial = store.readPublic(); private val _state = MutableStateFlow(AiSettingsUiState(initial.baseUrl, initial.model, hasSavedKey = initial.hasApiKey)); val state = _state.asStateFlow()
    fun baseUrl(v: String) = _state.update { it.copy(baseUrl = v, message = null) }; fun model(v: String) = _state.update { it.copy(model = v, message = null) }; fun apiKey(v: String) = _state.update { it.copy(apiKey = v, message = null) }
    fun selectModel(value: String) = _state.update { it.copy(model = value, message = null) }
    fun save() { val s = _state.value; runCatching { store.save(s.baseUrl, s.model, s.apiKey.takeIf(String::isNotBlank)) }.onSuccess { val saved = store.readPublic(); _state.update { it.copy(apiKey = "", hasSavedKey = saved.hasApiKey, message = "设置已保存") } }.onFailure { e -> _state.update { it.copy(message = e.message) } } }
    fun test() {
        val s = _state.value
        val settings = if (s.apiKey.isNotBlank()) {
            AiConnectionSettings(s.baseUrl.trim().removeSuffix("/"), s.model.trim(), s.apiKey.trim())
        } else {
            store.readConnectionSettings()?.copy(baseUrl = s.baseUrl.trim().removeSuffix("/"), model = s.model.trim())
        }
        if (settings == null) { _state.update { it.copy(message = "请先输入或保存 API Key") }; return }
        if (!settings.baseUrl.startsWith("https://") || settings.model.isBlank()) { _state.update { it.copy(message = "请检查 HTTPS API 地址和模型名称") }; return }
        _state.update { it.copy(isTesting = true, message = null) }
        viewModelScope.launch {
            val result = provider.testConnection(settings)
            _state.update { it.copy(isTesting = false, message = result.message, availableModels = result.availableModels.distinct().sorted()) }
        }
    }
}
