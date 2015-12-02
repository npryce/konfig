package com.natpryce.konfig

import java.io.File
import java.io.InputStream
import java.util.*
import kotlin.reflect.KClass

/**
 * Error thrown when a mandatory property is missing
 */
class Misconfiguration(message: String, cause: Exception? = null) : Exception(message, cause)

/**
 * A key that identifies a named, typed property and can convert a string representation into a value of the type.
 *
 * Define keys as constants which can be used to look up properties.  For example:
 *
 * ~~~~~~~~
 * val RETRY_COUNT = Key("connection.retrycount", intType)
 *
 * ...
 *
 * val retryCount = config\[RETRY_COUNT\]
 * ~~~~~~~~
 */
data class Key<T>(val name: String, val parse: (String) -> T)

/**
 * A parser for string properties
 */
val stringType = String::toString

/**
 * A parser for Int properties
 */
val intType = String::toInt

/**
 * A parser for Double properties
 */
val doubleType = String::toDouble

/**
 * Looks up configuration properties.
 */
interface Configuration {
    /**
     * Look up a property value identified by [key], or throw [Misconfiguration] if there is no definition of the
     * property.  Implementations can override [missingPropertyMessage] to provide a more detailed error message
     * for the exception.
     */
    @Throws(Misconfiguration::class)
    operator fun <T> get(key: Key<T>): T = getOrElse(key) { key -> throw Misconfiguration(missingPropertyMessage(key)) }

    /**
     * Look up a property value identified by [key], or return [default] with the key if there is no definition of the
     * property defined.
     */
    @Throws(Misconfiguration::class)
    fun <T> getOrElse(key: Key<T>, default: T): T = getOrElse(key) { default }

    /**
     * Look up a property value identified by [key], or call [default] with the key if there is no definition of the
     * property defined.
     */
    @Throws(Misconfiguration::class)
    fun <T> getOrElse(key: Key<T>, default: (Key<T>) -> T): T

    /**
     * The message used for the [Misconfiguration] exception thrown by [get] when there is no property defined
     * for [key].
     */
    open fun <T> missingPropertyMessage(key: Key<T>) = "${key.name} property not found"
}

/**
 * Configuration stored in a [Properties] object.
 */
class ConfigurationProperties(private val properties: Properties) : Configuration {
    override fun <T> getOrElse(key: Key<T>, default: (Key<T>) -> T)
            = properties.getProperty(key.name)?.let(key.parse) ?: default(key)

    companion object {
        /**
         * Returns the system properties as a Config object.
         */
        fun systemProperties() = ConfigurationProperties(System.getProperties())

        /**
         * Load from resources relative to a class
         */
        fun fromResource(relativeToClass: Class<*>, resourceName: String) =
                load(relativeToClass.getResourceAsStream(resourceName)) {
                    "resource $resourceName not found"
                }

        /**
         * Load from resource within the same classloader that loaded the Konfig library (probably the
         * system classloader)
         */
        fun fromResource(resourceName: String) =
                load(ConfigurationProperties::class.java.classLoader.getResourceAsStream(resourceName)) {
                    "resource $resourceName not found"
                }

        /**
         * Load from file
         */
        fun fromFile(file: File) = load(if (file.exists()) file.inputStream() else null) {
            "file $file does not exist"
        }

        private fun load(input: InputStream?, errorMessageFn: () -> String) =
                (input ?: throw Misconfiguration(errorMessageFn())).use {
                    ConfigurationProperties(Properties().apply { load(input) })
                }
    }
}


/**
 * Configuration stored in a map.
 */
class ConfigurationMap(private val properties: Map<String, String>) : Configuration {
    /**
     * A convenience method for creating a [Configuration] as an inline expression.
     */
    constructor(vararg entries: Pair<String, String>) : this(mapOf(*entries))

    override fun <T> getOrElse(key: Key<T>, default: (Key<T>) -> T) =
            properties[key.name]?.let(key.parse) ?: default(key)
}

/**
 * Configuration looked up in the environment variables of the process.
 *
 * Key names are translated from lower-case and periods convention to environment variable names with upper-case and
 * underscore convention, with an optional prefix.
 *
 * E.g. if the EnvironmentVariables instance is initialised a prefix of "APP_", the key name "db.password" will be
 * translated to "APP_DB_PASSWORD".
 *
 */
class EnvironmentVariables(val prefix: String = "", private val lookup: (String) -> String? = System::getenv) : Configuration {
    override fun <T> getOrElse(key: Key<T>, default: (Key<T>) -> T) =
            lookup(toEnvironmentVariable(key))?.let(key.parse) ?: default(key)

    override fun <T> missingPropertyMessage(key: Key<T>) = "${toEnvironmentVariable(key)} environment variable not found"

    private fun <T> toEnvironmentVariable(key: Key<T>) = prefix + key.name.toUpperCase().replace('.', '_')
}

/**
 * Looks up configuration in [override] and, if the property is not defined there, looks it up in [fallback].
 */
class Override(val override: Configuration, val fallback: Configuration) : Configuration {
    override fun <T> getOrElse(key: Key<T>, default: (Key<T>) -> T)
            = override.getOrElse(key) { fallback.getOrElse(key, default) }

}

infix fun Configuration.overriding(defaults: Configuration) = Override(this, defaults)

/**
 * Represents a subset of a larger set of configuration properties.
 *
 * The [namePrefix] and a "." separator is prepended to keys looked up in this configuration, and the keys
 * are then looked up in [configuration].
 *
 * For example, if initialised with a [namePrefix] of "db", a look up with a key named "password" would be
 * delegated to [configuration] as a look up for "db.password".
 */
class Subset(val namePrefix: String, private val configuration: Configuration) : Configuration {
    override fun <T> getOrElse(key: Key<T>, default: (Key<T>) -> T) =
            configuration.getOrElse(key.copy(name = namePrefix + "." + key.name), default)
}