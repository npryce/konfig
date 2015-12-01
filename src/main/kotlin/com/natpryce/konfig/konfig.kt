package com.natpryce.konfig

import java.util.*

class Misconfiguration(val key: Key<*>, message: String, cause: Exception? = null) : Exception(message, cause)

data class Key<T>(val name: String, val parse: (String) -> T)

val stringType = String::toString
val intType = String::toInt
val doubleType = String::toDouble

interface Config {
    @Throws(Misconfiguration::class)
    fun <T> getOrElse(key: Key<T>, default: (Key<T>) -> T): T

    @Throws(Misconfiguration::class)
    operator fun <T> get(key: Key<T>): T = getOrElse(key) { key -> propertyMissing(key)}

    @Throws(Misconfiguration::class)
    fun <T> getOrElse(key: Key<T>, default: T): T = getOrElse(key) { default }

    open fun propertyMissing(key: Key<*>) = throw Misconfiguration(key, "${key.name} property not found")
}


class ConfigProperties(private val properties: Properties) : Config {
    override fun <T> getOrElse(key: Key<T>, default: (Key<T>) -> T)
            = properties.getProperty(key.name)?.let(key.parse) ?: default(key)
}

class ConfigMap(private val properties: Map<String,String>) : Config {
    constructor(vararg entries: Pair<String,String>) : this(mapOf(*entries))

    override fun <T> getOrElse(key: Key<T>, default: (Key<T>) -> T)
            = properties[key.name]?.let(key.parse) ?: default(key)
}

class EnvironmentVariables(val prefix: String = "", private val lookup: (String) -> String? = System::getenv) : Config {
    override fun <T> getOrElse(key: Key<T>, default: (Key<T>) -> T): T {
        return lookup(toEnvironmentVariable(key))?.let(key.parse)?:default(key)
    }

    override fun propertyMissing(key: Key<*>) = throw Misconfiguration(key, "${toEnvironmentVariable(key)} environment variable not found")

    private fun <T> toEnvironmentVariable(key: Key<T>) = prefix + key.name.toUpperCase().replace('.', '_')
}

class Override(val override: Config, val fallback: Config) : Config {
    override fun <T> getOrElse(key: Key<T>, default: (Key<T>) -> T)
            = override.getOrElse(key) { fallback.getOrElse(key, default) }

}