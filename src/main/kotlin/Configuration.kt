package org.example

data class Configuration(
    val baseUrl: String,
    val timeoutSeconds: Int,
    val enableLogging: Boolean = false
)

interface BaseUrlSet
interface TimeoutSecondsSet

class ConfigurationBuilder<T, R> private constructor(
    var baseUrl: String? = null,
    var timeoutSeconds: Int? = null
) {

    var enableLogging: Boolean = false

    fun withBaseUrl(baseUrl: String): ConfigurationBuilder<BaseUrlSet, R> {
        return ConfigurationBuilder(
            baseUrl,
            timeoutSeconds
        )
    }

    fun withTimeout(timeoutSeconds: Int) : ConfigurationBuilder<T, TimeoutSecondsSet> {
        return ConfigurationBuilder(
            baseUrl,
            timeoutSeconds
        )
    }

    fun withLogging(enableLogging: Boolean) = apply {
        this.enableLogging = enableLogging
    }

    companion object {
        operator fun invoke() = ConfigurationBuilder<Nothing, Nothing>()
    }
}

fun ConfigurationBuilder<BaseUrlSet, TimeoutSecondsSet>.build() : Configuration {
    return Configuration(
        baseUrl!!,
        timeoutSeconds!!,
        enableLogging
    )
}