package com.natpryce.konfig

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.Test


class CommandLineParsing {
    val optX = Key("opt.x", stringType)
    val optY = Key("opt.y", stringType)

    @Test
    fun no_options() {
        val (config, args) = parseArgs(arrayOf("foo", "bar", "baz"))

        val reportedConfig = config.list().single().second
        assertThat(reportedConfig.size, equalTo(0))

        assertThat(args, equalTo(listOf("foo", "bar", "baz")))
    }

    @Test
    fun one_long_option_with_separator() {
        val (config, args) = parseArgs(arrayOf("--opt-x=bar", "baz"),
                CommandLineOption(optX))

        assertThat(config[optX], equalTo("bar"))
        assertThat(args, equalTo(listOf("baz")))
    }

    @Test
    fun one_long_option_as_two_args() {
        val (config, args) = parseArgs(arrayOf("--opt-x", "bar", "baz"),
                CommandLineOption(optX))

        assertThat(config[optX], equalTo("bar"))
        assertThat(args, equalTo(listOf("baz")))
    }

    @Test
    fun one_short_option() {
        val (config, args) = parseArgs(arrayOf("-x", "bar", "baz"),
                CommandLineOption(optX, short = "x"))

        assertThat(config[optX], equalTo("bar"))
        assertThat(args, equalTo(listOf("baz")))
    }

    @Test
    fun unrecognised_long_option() {
        assertThat({ parseArgs(arrayOf("--wtf", "foo", "bar"), CommandLineOption(optX, short = "x", long = "opt-x")) },
                throws<Misconfiguration>())
    }

    @Test
    fun unrecognised_short_option() {
        assertThat({ parseArgs(arrayOf("-a", "foo", "bar"), CommandLineOption(optX, short = "x", long = "opt-x")) },
                throws<Misconfiguration>())
    }

    @Test
    fun defaults() {
        val opts = arrayOf(
                CommandLineOption(optX),
                CommandLineOption(optY))

        val (config, files) = parseArgs(arrayOf("--opt-x=cli-x", "a-file"), *opts) overriding
                ConfigurationMap(
                        "opt.x" to "default-x",
                        "opt.y" to "default-y")

        assertThat(config[optX], equalTo("cli-x"))
        assertThat(config[optY], equalTo("default-y"))
        assertThat(files, equalTo(listOf("a-file")))
    }

    @Test
    fun reports_location_as_the_option_used_on_the_command_line() {
        val (config) = parseArgs(arrayOf("--opt-x=10", "-y", "20"),
                CommandLineOption(optX),
                CommandLineOption(optY, short = "y"))

        assertThat(config.location(optX).nameInLocation, equalTo("--opt-x"))
        assertThat(config.location(optY).nameInLocation, equalTo("-y"))
    }
}
