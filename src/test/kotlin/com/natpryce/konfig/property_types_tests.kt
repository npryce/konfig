package com.natpryce.konfig

import com.natpryce.hamkrest.*
import com.natpryce.hamkrest.assertion.assertThat
import org.junit.Test
import java.net.URI

private fun <T> location(parser: (PropertyLocation, String) -> T) =
        PropertyLocation(Key("passed-property-key", parser), Location("source-location"), "property-key-in-source")

private fun <T> assertParse(parser: (PropertyLocation,String) -> T, vararg successful: Pair<String, T>) {
    for ((orig, expected) in successful) {
        val actual = parser(location(parser), orig)
        assertThat(describe(orig), actual, equalTo(expected))
    }
}


private inline fun <reified T> assertThrowsMisconfiguration(noinline parse: (PropertyLocation, String) -> T, vararg bad_inputs: String) {
    val propertyTypeName = T::class.simpleName!!

    for (bad_input in bad_inputs) {
        assertThat(describe(bad_input), { parse(location(parse), bad_input) },
                throws<Misconfiguration>(has(Throwable::message, present(
                        containsSubstring(bad_input) and
                        containsSubstring(propertyTypeName) and
                        containsSubstring("property-key-in-source") and
                        containsSubstring("source-location")
                ))))
    }
}


class ParsingValues {
    @Test
    fun ints() {
        assertParse(intType,
                "1234" to 1234,
                "0" to 0,
                "-123" to -123)
    }

    @Test
    fun bad_ints() {
        assertThrowsMisconfiguration(intType,
                "zzz",
                "123x",
                "")
    }

    @Test
    fun longs() {
        assertParse(longType,
                "1234" to 1234L,
                "0" to 0L,
                "-123" to -123L)
    }

    @Test
    fun bad_longs() {
        assertThrowsMisconfiguration(longType,
                "zzz",
                "123x",
                "")
    }

    @Test
    fun doubles() {
        assertParse(doubleType,
                "1234" to 1234.0,
                "12.4" to 12.4,
                "0" to 0.0,
                "-10" to -10.0,
                "-12.25" to -12.25)
    }

    @Test
    fun bad_doubles() {
        assertThrowsMisconfiguration(doubleType,
                "zzz",
                "123x",
                "")
    }

    @Test
    fun boolean() {
        assertParse(booleanType,
                "true" to true,
                "TRUE" to true,
                "True" to true,
                "false" to false,
                "False" to false,
                "no" to false,
                "yes" to false,
                "zzz" to false,
                "123x" to false,
                "" to false)
    }

    @Test
    fun uris() {
        assertParse(uriType,
                "http://example.com" to URI("http://example.com"),
                "foo/bar" to URI("foo/bar"))
    }

    @Test
    fun bad_uris() {
        assertThrowsMisconfiguration(uriType,
                ":/{}!")
    }
}

class ParsingEnumeratedValues {
    val enumType = enumType("foo" to 1, "bar" to 2, "baz" to 3)

    @Test
    fun parse_enums() {
        assertParse(enumType,
                "foo" to 1,
                "bar" to 2,
                "baz" to 3)
    }

    @Test
    fun invalid_enum() {
        assertThrowsMisconfiguration(enumType, "xxx")
    }
}

class ParsingLists {
    @Test
    fun parse_lists_of_other_types() {
        assertParse(listType(intType),
                "1,2,3" to listOf(1, 2, 3),
                "2, 3, 4" to listOf(2, 3, 4),
                "4,  5,6" to listOf(4, 5, 6)
        )
    }

    @Test
    fun with_custom_separator() {
        assertParse(listType(intType, separator=Regex(":")),
                "1:2:3" to listOf(1, 2, 3)
        )
    }
}