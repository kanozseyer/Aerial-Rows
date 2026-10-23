package com.conreo.couchytv.core.model

/** Stable application identity. Component/profile fields become populated as discovery is upgraded. */
data class AppId(
    val packageName: String,
    val componentName: String? = null,
    val userSerial: Long? = null,
) {
    init {
        require(packageName.isNotBlank()) { "AppId packageName must not be blank" }
    }

    val stableKey: String = buildString {
        append(packageName)
        componentName?.let { append('|').append(it) }
        userSerial?.let { append('|').append(it) }
    }
}

@JvmInline
value class RowId(val value: String) {
    init {
        require(value.isNotBlank()) { "RowId must not be blank" }
    }
}

sealed interface HomeItemId {
    val stableKey: String

    data class App(val appId: AppId) : HomeItemId {
        override val stableKey: String = "app:${appId.stableKey}"
    }

    data class TvProgram(val providerPackage: String, val programKey: String) : HomeItemId {
        init {
            require(providerPackage.isNotBlank()) { "TV program provider must not be blank" }
            require(programKey.isNotBlank()) { "TV program key must not be blank" }
        }

        override val stableKey: String = "tv:$providerPackage:$programKey"
    }

    data class InternalAction(val actionId: String) : HomeItemId {
        init {
            require(actionId.isNotBlank()) { "Internal action ID must not be blank" }
        }

        override val stableKey: String = "action:$actionId"
    }
}
