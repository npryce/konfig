package com.natpryce.unittests.konfig

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.describe
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.present
import com.natpryce.hamkrest.throws
import com.natpryce.konfig.Key
import com.natpryce.konfig.Location
import com.natpryce.konfig.Misconfiguration
import com.natpryce.konfig.PropertyLocation
import com.natpryce.konfig.booleanType
import com.natpryce.konfig.doubleType
import com.natpryce.konfig.enumType
import com.natpryce.konfig.intType
import com.natpryce.konfig.listType
import com.natpryce.konfig.longType
import com.natpryce.konfig.setType
import com.natpryce.konfig.timeZoneIdType
import com.natpryce.konfig.timeZoneType
import com.natpryce.konfig.uriType
import com.natpryce.unittests.konfig.ParsingEnumeratedValues.E.A
import com.natpryce.unittests.konfig.ParsingEnumeratedValues.E.B
import com.natpryce.unittests.konfig.ParsingEnumeratedValues.E.C
import org.junit.Test
import java.net.URI
import java.time.ZoneId
import java.util.TimeZone

private fun <T> location(parser: (PropertyLocation, String) -> T) =
    PropertyLocation(Key("passed-property-key", parser), Location("source-location"), "property-key-in-source")

private fun <T> assertParse(parser: (PropertyLocation, String) -> T, vararg successful: Pair<String, T>) {
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
    @Test
    fun parse_enums_by_name_and_value() {
        val theType = enumType("foo" to 1, "bar" to 2, "baz" to 3)
    
        assertParse(theType,
            "foo" to 1,
            "bar" to 2,
            "baz" to 3)
    
        assertThrowsMisconfiguration(theType, "xxx")
    }
    
    enum class E { A, B, C }
    
    @Test
    fun parse_enums_by_elements_of_an_enum_type() {
        val typeFromVarargs = enumType(A, B, C)
        val typeFromArray = enumType(*E.values())
        val typeFromIterable = enumType(setOf(A, B, C))
        val typeFromJavaClass = enumType(E::class.java)
        
        for (theType in listOf(typeFromVarargs, typeFromArray, typeFromIterable, typeFromJavaClass)) {
            assertParse(theType,
                "A" to A,
                "B" to B,
                "C" to C)
    
            assertThrowsMisconfiguration(theType, "xxx")
            assertThrowsMisconfiguration(theType, "a")
            assertThrowsMisconfiguration(theType, "b")
            assertThrowsMisconfiguration(theType, "c")
        }
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
        assertParse(listType(intType, separator = Regex(":")),
            "1:2:3" to listOf(1, 2, 3)
        )
    }
}

class ParsingSets {
    @Test
    fun parse_sets_of_other_types() {
        assertParse(setType(intType),
            "1,3,2,3" to setOf(1, 2, 3),
            "2, 4, 3, 4" to setOf(2, 3, 4),
            "6, 4,  5,6" to setOf(4, 5, 6)
        )
    }
    
    @Test
    fun with_custom_separator() {
        assertParse(setType(intType, separator = Regex(":")),
            "1:2:3" to setOf(1, 2, 3)
        )
    }
}


class TimeZones {
    @Test
    fun `parse_named_time_zones`() {
        assertParse(timeZoneIdType,
            "Europe/London" to ZoneId.of("Europe/London")
        )
        assertParse(timeZoneType,
            "Europe/London" to TimeZone.getTimeZone("Europe/London")
        )
    }
    
    @Test
    fun `throws_misconfiguration_on_invalid_timezone_does_not_return_UTC`() {
        val badTimezones = arrayOf("Foo/Bar",
            "xxx",
            "X",
            "UTC+123123212313223")
    
        assertThrowsMisconfiguration(timeZoneIdType, *badTimezones)
        assertThrowsMisconfiguration(timeZoneType, *badTimezones)
    }
    
}