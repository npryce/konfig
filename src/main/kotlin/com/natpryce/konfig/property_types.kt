package com.natpryce.konfig

import java.net.URI
import java.net.URISyntaxException
import kotlin.text.Regex

/**
 * A parser for string properties (the identity function)
 */
val stringType = propertyType<String, IllegalArgumentException>(String::toString)

/**
 * Wraps a [parse] function and translates [NumberFormatException]s into [Misconfiguration] exceptions.
 */
inline fun <reified T, reified X : Exception> propertyType(crossinline parse: (String) -> T): (String, () -> PropertyLocation) -> T {
    return { s, provenanceSupplier ->
        try {
            parse(s)
        } catch (e: Exception) {
            when (e) {
                is X -> {
                    val p = provenanceSupplier()
                    val typeName = T::class.simpleName ?: "value"

                    throw Misconfiguration("${p.source.description} ${p.nameInLocation} - invalid $typeName: $s", e)
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
 * The type of URI properties
 */
val uriType = propertyType<URI, URISyntaxException>(::URI)


private val defaultSeparator = Regex(",\\s*")

fun <T> listType(elementType: (String, () -> PropertyLocation) -> T, separator: Regex = defaultSeparator) = { s: String, p: () -> PropertyLocation ->
    s.split(separator).map { elementType(it, p) }
}