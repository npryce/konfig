package com.natpryce.konfig

import java.net.URI
import java.net.URISyntaxException

/**
 * A parser for string properties (the identity function)
 */
val stringType = propertyType<String, IllegalArgumentException>(String::toString)

/**
 * Wraps a [parse] function and translates [NumberFormatException]s into [Misconfiguration] exceptions.
 */
inline fun <reified T, reified X : Exception> propertyType(crossinline parse: (String) -> T): (PropertyLocation, String) -> T {
    return { location, stringValue ->
        try {
            parse(stringValue)
        } catch (e: Exception) {
            when (e) {
                is X -> {
                    val typeName = T::class.simpleName ?: "value"

                    throw Misconfiguration("${location.source.description} ${location.nameInLocation} - invalid $typeName: $stringValue", e)
                }
                else -> throw e
            }
        }
    }
}

/**
 * Wraps a [parse] function and translates [NumberFormatException]s into [Misconfiguration] exceptions.
 */
inline fun <reified T> numericPropertyType(noinline parse: (String) -> T) =
        propertyType<T, NumberFormatException>(parse)

/**
 * The type of Int properties
 */
val intType = numericPropertyType(String::toInt)

/**
 * The type of Long properties
 */
val longType = numericPropertyType(String::toLong)

/**
 * The type of Double properties
 */
val doubleType = numericPropertyType(String::toDouble)

/**
 * The type of Boolean properties
 */
val booleanType = propertyType<Boolean, IllegalArgumentException>(String::toBoolean)

/**
 * An enumerated list of possible values, each specified by the string value used in configuration files and the
 * value used in the program.
 */
inline fun <reified T> enumType(allowed: Map<String,T>) = propertyType<T, IllegalArgumentException>({str ->
    allowed[str]?:throw IllegalArgumentException("invalid value: $str; must be one of: ${allowed.keys}")
})

inline fun <reified T> enumType(vararg allowed: Pair<String,T>) = enumType(mapOf(*allowed))

inline fun <reified T : Enum<T>> enumType(allowed: Array<T>) = enumType(allowed.toMap { it.name to it })

/**
 * The type of URI properties
 */
val uriType = propertyType<URI, URISyntaxException>(::URI)


private val defaultSeparator = Regex(",\\s*")

fun <T> listType(elementType: (PropertyLocation, String) -> T, separator: Regex = defaultSeparator) =
        { p: PropertyLocation, s: String ->
            s.split(separator).map { elementAsString -> elementType(p, elementAsString) }
        }