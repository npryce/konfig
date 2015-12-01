package com.natpryce.konfig

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import java.util.*


class FromProperties {
    val config = ConfigProperties(Properties().apply {
        setProperty("name", "alice")
        setProperty("x", "1")
        setProperty("y", "2")
    })

    val name = Key("name") { it }
    val x = Key("x", String::toInt)
    val y = Key("y", String::toInt)

    @Test
    fun looks_up_string_properties() {
        assertThat(config[name], equalTo("alice"))
    }

    @Test
    fun looks_up_int_properties() {
        assertThat(config[x], equalTo(1))
        assertThat(config[y], equalTo(2))
    }
}

class FromMap {
    val config = ConfigMap("name" to "alice", "x" to "1", "y" to "2")

    val name = Key("name", String::toString)
    val x = Key("x", String::toInt)
    val y = Key("y", String::toInt)

    @Test
    fun looks_up_string_properties() {
        assertThat(config[name], equalTo("alice"))
    }

    @Test
    fun looks_up_int_properties() {
        assertThat(config[x], equalTo(1))
        assertThat(config[y], equalTo(2))
    }

}

class FromEnvironment {
    @Test
    fun translates_property_name_to_upper_case() {
        val env = mapOf("X" to "1", "Y" to "2")
        val config = EnvironmentVariables(lookup = { varname -> env[varname] })

        val x = Key("x", String::toInt)
        val y = Key("y", String::toInt)

        assertThat(config[x], equalTo(1))
        assertThat(config[y], equalTo(2))
    }

    @Test
    fun translates_dotted_property_name_to_upper_case_and_underscore() {
        val env = mapOf("NAME_FIRST" to "alice", "NAME_LAST" to "band")
        val config = EnvironmentVariables(lookup = { varname -> env[varname] })

        val firstName = Key("name.first", String::toString)
        val lastName = Key("name.last", String::toString)

        assertThat(config[firstName], equalTo("alice"))
        assertThat(config[lastName], equalTo("band"))
    }

    @Test
    fun environment_variables_can_be_prefixed() {
        val env = mapOf("XXX_NAME" to "alice")
        val config = EnvironmentVariables(prefix="XXX_", lookup = { varname -> env[varname] })

        val name = Key("name", String::toString)

        assertThat(config[name], equalTo("alice"))
    }
}

class OverridingAndFallingBack {
    @Test
    fun overrides_default_properties() {
        val defaults = ConfigMap("x" to "x", "y" to "y")
        val overrides = ConfigMap("x" to "XX", "z" to "ZZ")

        val config = Override(overrides, defaults)

        assertThat(config[Key("x", String::toString)], equalTo("XX"))
        assertThat(config[Key("y", String::toString)], equalTo("y"))
        assertThat(config[Key("z", String::toString)], equalTo("ZZ"))
    }
}