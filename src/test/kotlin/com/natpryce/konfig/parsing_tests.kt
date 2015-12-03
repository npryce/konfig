package com.natpryce.konfig

import com.natpryce.hamkrest.*
import com.natpryce.hamkrest.assertion.assertThat
import org.junit.Test

inline fun <reified T : Throwable> throws(exceptionCriteria: Matcher<T>? = null): Matcher<() -> Unit> {
    val exceptionName = T::class.qualifiedName

    return object : Matcher.Primitive<() -> Unit>() {
        override fun invoke(actual: () -> Unit): MatchResult =
                try {
                    actual()
                    MatchResult.Mismatch("did not throw")
                } catch (e: T) {
                    exceptionCriteria?.invoke(e) ?: MatchResult.Match
                }

        override fun description() = "throws $exceptionName that ${describe(exceptionCriteria)}"
    }
}

class Parsing {
    @Test
    fun parse_ints() {
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
    fun parse_longs() {
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
    fun parse_doubles() {
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
    fun parse_boolean() {
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

    private fun <T> assertParse(parser: (String) -> T, vararg successful: Pair<String, T>) {
        for ((orig, parsed) in successful) {
            assertThat(orig, parser(orig), equalTo(parsed))
        }

    }

    private inline fun <reified T> assertThrowsMisconfiguration(crossinline parser: (String) -> T, vararg bad_inputs: String) {
        for (bad_input in bad_inputs) {
            assertThat(describe(bad_input), { parser(bad_input) },
                    throws<Misconfiguration>(has(Throwable::message, present(
                            containsSubstring(bad_input) and containsSubstring(T::class.simpleName!!)
                    ))))
        }
    }
}