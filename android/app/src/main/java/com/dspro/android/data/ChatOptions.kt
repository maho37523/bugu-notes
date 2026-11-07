package com.dspro.android.data

/** Capability ranges verified against official docs; unknown models keep the server default. */
fun supportedThinkingEfforts(provider: String, model: String, advertised: List<String>? = null): List<ThinkingEffort> {
    val values = if (provider == "chatgpt" && advertised != null) advertised else when {
        provider == "deepseek" && model in setOf("deepseek-flash", "deepseek-v4-pro") -> listOf("none", "low", "high", "max")
        provider in setOf("openai", "chatgpt") && model in setOf("gpt-6.1-sol", "gpt-6-astra") -> listOf("low", "medium", "high", "xhigh", "max")
        provider in setOf("openai", "chatgpt") && (model == "gpt-5.4-mini" || model == "gpt-5.4-mini-2026-03-17") -> listOf("none", "low", "medium", "high", "xhigh")
        provider == "anthropic" && model in setOf("claude-sonnet-5-5", "claude-opus-5-5", "claude-haiku-5-5") -> listOf("low", "medium", "high", "xhigh", "max")
        provider == "aliyun" && model in setOf("qwen3.8-max", "qwen3.8-max-0902", "qwen3.8-flash", "qwen3.8-2.4t-a95b", "qwen3.8-27b") -> listOf("none", "low", "medium", "xhigh")
        else -> emptyList()
    }
    return listOf(ThinkingEffort.Auto) + values.mapNotNull { value -> ThinkingEffort.entries.find { it != ThinkingEffort.Auto && it.apiValue == value } }.distinct()
}

fun effectiveThinkingEffort(config: ApiConfig, advertised: List<String>? = null): ThinkingEffort =
    config.effort.takeIf { it in supportedThinkingEfforts(config.provider, config.model, advertised) } ?: ThinkingEffort.Auto

fun providerLabel(provider: String): String = when (provider) {
    "deepseek" -> "DeepSeek"; "openai" -> "OpenAI API"; "chatgpt" -> "ChatGPT 订阅"
    "anthropic" -> "Claude API"; "aliyun" -> "阿里云百炼"; else -> "自定义 API"
}

fun suggestedChatModels(provider: String): List<String> = when (provider) {
    "deepseek" -> listOf("deepseek-flash", "deepseek-v4-pro")
    "openai" -> listOf("gpt-4o-mini", "gpt-5.4-mini", "gpt-6.1-sol")
    "anthropic" -> listOf("claude-sonnet-5-5", "claude-opus-5-5", "claude-haiku-5-5")
    "aliyun" -> listOf("qwen-plus", "qwen3.8-max", "qwen3.8-flash", "qwen3.7-plus")
    else -> emptyList()
}

data class ComposerModel(val id: String, val label: String, val effort: ThinkingEffort, val supportedEfforts: List<ThinkingEffort>)
data class ComposerConnection(val provider: String, val label: String, val model: String, val models: List<ComposerModel>)
