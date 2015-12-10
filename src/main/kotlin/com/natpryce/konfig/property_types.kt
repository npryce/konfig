package com.natpryce.konfig

import java.net.URISyntaxException
import java.net.URI
import kotlin.text.Regex

/**
 * A parser for string properties (the identity function)
 */
val stringType = String::toString

/**
 * Wraps a [parse] function and translates [NumberFormatException]s into [Misconfiguration] exceptions.
 */
inline fun <reified T, reified X : Exception> propertyType(crossinline parse: (String) -> T): (String) -> T {
    return { s ->
        try {
            parse(s)
        } catch (e: Exception) {
            when (e) {
                is X -> throw Misconfiguration("invalid ${T::class.simpleName ?: "value"}: $s", e)
                else -> throw e
            }
        }
    }
}

/**
 * Wraps a [parse] function and translates [NumberFormatException]s into [Misconfiguration] exceptions.
 */
inline fun <reified T> numericPropertyType(crossinline parse: (String) -> T) =
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
val booleanType = String::toBoolean

/**
 * The type of URI properties
 */
val uriType = propertyType<URI, URISyntaxException>(::URI)


private val defaultSeparator = Regex(",\\s*")

fun <T> listType(elementType: (String) -> T, separator: Regex = defaultSeparator): (String) -> List<T> = { s ->
    s.split(separator).map(elementType)
}