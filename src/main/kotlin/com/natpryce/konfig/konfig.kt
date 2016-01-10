package com.natpryce.konfig

import java.io.File
import java.io.InputStream
import java.net.URI
import java.net.URL
import java.util.*

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
data class Key<out T>(val name: String, val parse: (PropertyLocation, String) -> T) {
    fun getOrNullBy(lookup: (String)->Pair<PropertyLocation,String?>) : T? {
        val (propertyLocation,stringValue) = lookup(name)
        return stringValue?.let{parse(propertyLocation, stringValue)}
    }
}


/**
 * Describes the location of configuration information.  A location may have a [uri] or may not, because it is
 * compiled into the application or obtained from ephemeral data, such as the process environment or command-line
 * parameters,
 */
data class Location(val description: String, val uri: URI? = null) {
    constructor(file: File) : this(file.absolutePath, file.toURI())

    constructor(uri: URI) : this(uri.toString(), uri)

    companion object {
        /**
         * Describes the location of configuration data that is compiled into the application, as resources
         * or code that creates a [Configuration] object.
         */
        val INTRINSIC = Location("intrinsic")
    }
}

/**
 * Represents the location of a value looked up by a key.
 */
data class PropertyLocation(val key: Key<*>, val source: Location, val nameInLocation: String) {
    val description: String get() = "$nameInLocation in ${source.description}"
}


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
     * property.
     */
    fun <T> getOrElse(key: Key<T>, default: T): T = getOrElse(key) { default }

    /**
     * Look up a property value identified by [key], or return `null` if there is no definition of the
     * property.
     */
    fun <T> getOrNull(key: Key<T>): T?

    /**
     * Look up a property value identified by [key], or call [default] with the key if there is no definition of the
     * property.
     */
    fun <T> getOrElse(key: Key<T>, default: (Key<T>) -> T): T = getOrNull(key) ?: default(key)

    fun contains(key: Key<*>) = getOrNull(key) != null

    /**
     * Report the locations that will be searched for a configuration property, in priority order.  The value used
     * is taken from the first location in the list that contains a mapping for the key.
     */
    fun searchPath(key: Key<*>): List<PropertyLocation>

    fun list(): List<Pair<Location, Map<String, String>>>

}

/**
 * The message used for the [Misconfiguration] exception thrown by [get] when there is no property defined
 * for [key].
 */
fun Configuration.missingPropertyMessage(key: Key<*>) =
        "${key.name} property not found; searched:\n${searchPath(key).description}"

val List<PropertyLocation>.description: String
    get() = map { " - ${it.description}" }.joinToString(separator = "\n", postfix = "\n")


abstract class  LocatedConfiguration : Configuration {
    abstract val location: Location

    /**
     * An implementation that works for a [Configuration] that is loaded from single source, and must be
     * overridden if the [Configuration] searches in multiple sources.
     */
    override fun searchPath(key: Key<*>): List<PropertyLocation> = listOf(location(key))

    protected fun location(key: Key<*>) = PropertyLocation(key, location, key.name)
}

/**
 * Configuration stored in a [Properties] object.
 */
class ConfigurationProperties(private val properties: Properties, override val location: Location = Location.INTRINSIC) :
        LocatedConfiguration()
{
    override fun <T> getOrNull(key: Key<T>) = key.getOrNullBy {name ->
        PropertyLocation(key, location, name) to properties.getProperty(name)
    }

    override fun contains(key: Key<*>) = properties.getProperty(key.name) != null

    override fun searchPath(key: Key<*>): List<PropertyLocation> {
        return listOf(PropertyLocation(key, location, key.name))
    }

    override fun list(): List<Pair<Location,Map<String, String>>> {
        return listOf(location to properties.stringPropertyNames().toMapBy({ it }, { properties.getProperty(it) }))
    }

    companion object {
        /**
         * Returns the system properties as a Config object.
         */
        fun systemProperties() = ConfigurationProperties(System.getProperties(), Location("system properties"))

        /**
         * Load from resources relative to a class
         */
        fun fromResource(relativeToClass: Class<*>, resourceName: String) =
                loadFromResource(resourceName, relativeToClass.getResource(resourceName))

        /**
         * Load from resource within the system classloader.
         */
        fun fromResource(resourceName: String): ConfigurationProperties {
            val classLoader = ClassLoader.getSystemClassLoader()
            return loadFromResource(resourceName, classLoader.getResource(resourceName))
        }

        private fun loadFromResource(resourceName: String, resourceUrl: URL?): ConfigurationProperties {
            return load(resourceUrl?.openStream(), Location("resource $resourceName", resourceUrl?.toURI())) {
                "resource $resourceName not found"
            }
        }

        /**
         * Load from file
         */
        fun fromFile(file: File) = load(if (file.exists()) file.inputStream() else null, Location(file.absolutePath, file.toURI())) {
            "file $file does not exist"
        }

        private fun load(input: InputStream?, location: Location, errorMessageFn: () -> String) =
                (input ?: throw Misconfiguration(errorMessageFn())).use {
                    ConfigurationProperties(Properties().apply { load(input) }, location)
                }
    }
}


/**
 * Configuration stored in a map.
 */
class ConfigurationMap(private val properties: Map<String, String>, public override val location: Location = Location.INTRINSIC) :
        LocatedConfiguration()
{
    /**
     * A convenience method for creating a [Configuration] as an inline expression.
     */
    constructor(vararg entries: Pair<String, String>, location: Location = Location.INTRINSIC) : this(mapOf(*entries), location)

    override fun <T> getOrNull(key: Key<T>) = key.getOrNullBy { location(key) to properties[key.name] }

    override fun contains(key: Key<*>): Boolean {
        return key.name in properties
    }

    override fun list(): List<Pair<Location, Map<String, String>>> {
        return listOf(location to properties)
    }
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
class EnvironmentVariables(val prefix: String = "",
                           private val lookup: (String) -> String? = System::getenv,
                           private val all: ()->Map<String,String> = System::getenv)
    : Configuration
{
    public val location = Location("environment variables")

    override fun <T> getOrNull(key: Key<T>) = key.getOrNullBy { name ->
        var envvar = toEnvironmentVariable(name)
        PropertyLocation(key, location, envvar) to lookup(envvar)
    }

    override fun searchPath(key: Key<*>): List<PropertyLocation> {
        return listOf(PropertyLocation(key, location, toEnvironmentVariable(key.name)))
    }

    override fun list(): List<Pair<Location, Map<String, String>>> =
            listOf(location to all().filterKeys { it.startsWith(prefix) })

    private fun toEnvironmentVariable(name: String) = prefix + name.toUpperCase().replace('.', '_')
}

/**
 * Looks up configuration in [override] and, if the property is not defined there, looks it up in [fallback].
 */
class Override(val override: Configuration, val fallback: Configuration) : Configuration {
    override fun searchPath(key: Key<*>) = override.searchPath(key) + fallback.searchPath(key)

    override fun <T> getOrNull(key: Key<T>) = override.getOrNull(key) ?: fallback.getOrNull(key)

    override fun list() = override.list() + fallback.list()
}

infix fun Configuration.overriding(defaults: Configuration?) =
        if (defaults == null) this else Override(this, defaults)

/**
 * Represents a subset of a larger set of configuration properties.
 *
 * The [namePrefix] and a "." separator is prepended to keys looked up in this configuration, and the keys
 * are then looked up in [configuration].
 *
 * For example, if initialised with a [namePrefix] of "db", a look up with a key named "password" would be
 * delegated to [configuration] as a look up for "db.password".
 */
class Subset(namePrefix: String, private val configuration: Configuration) : Configuration {
    private val prefix = namePrefix + "."

    override fun <T> getOrNull(key: Key<T>) = configuration.getOrNull(prefixed(key))

    override fun contains(key: Key<*>) = configuration.contains(prefixed(key))

    override fun searchPath(key: Key<*>) = configuration.searchPath(prefixed(key))

    override fun list() = configuration.list().map { it.first to it.second.filterKeys{ k -> k.startsWith(prefix) } }

    private fun <T> prefixed(key: Key<T>) = key.copy(name = prefix + key.name)
}
