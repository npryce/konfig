package com.natpryce.konfig

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import com.natpryce.hamkrest.throws
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream


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
    fun reports_location_as_the_long_option() {
        val (config) = parseArgs(arrayOf("--opt-x=10", "-y", "20"),
                CommandLineOption(optX),
                CommandLineOption(optY, short = "y"))

        assertThat(config.location(optX).nameInLocation, equalTo("--opt-x"))
        assertThat(config.location(optY).nameInLocation, equalTo("--opt-y"))
    }

    @Test
    fun lists_options_set_from_command_line() {
        val (config) = parseArgs(arrayOf("--opt-x=10", "-y", "20"),
                CommandLineOption(optX),
                CommandLineOption(optY, short = "y"))

        assertThat(config.list().single().second, equalTo(mapOf(
                "--opt-x" to "10",
                "-y" to "20"
        )))
    }

    private class Help : Exception()

    @Test
    fun reports_usage_if_passed_long_help_option() {
        val helpOutputBytes = ByteArrayOutputStream()

        expectThrown<Help> {
            parseArgs(arrayOf("--opt-x=10", "-y", "20", "--help"),
                    CommandLineOption(optX),
                    CommandLineOption(optY, short = "y"),
                    helpOutput = helpOutputBytes,
                    helpExit = { throw Help() })
        }

        assertEquals(
"""Usage: <program> [options] FILE ...

Options:
  --opt-x=OPT-X            set opt x
  -y OPT-Y, --opt-y=OPT-Y  set opt y
  -h, --help               show this help message and exit
""", helpOutputBytes.toString())

    }

    @Test
    fun can_specify_program_name_and_arg_metavar() {
        val helpOutputBytes = ByteArrayOutputStream()

        expectThrown<Help> {
            parseArgs(arrayOf("--help"),
                    CommandLineOption(optX),
                    programName = "the-program",
                    argMetavar = "ARG",
                    helpOutput = helpOutputBytes,
                    helpExit = { throw Help() })
        }

        assertEquals("""Usage: the-program [options] ARG ...""", helpOutputBytes.toString().lines().first())
    }

    @Test
    fun reports_usage_if_passed_short_help_option() {
        val helpOutputBytes = ByteArrayOutputStream()

        expectThrown<Help> {
            parseArgs(arrayOf("-h"),
                    CommandLineOption(optX),
                    helpOutput = helpOutputBytes,
                    helpExit = { throw Help() })
        }
    }

    @Test
    fun can_specify_description_of_command_line_option() {
        val helpOutputBytes = ByteArrayOutputStream()

        expectThrown<Help> {
            parseArgs(arrayOf("--help"),
                    CommandLineOption(optX, description = "a descriptive description"),
                    helpOutput = helpOutputBytes,
                    helpExit = { throw Help() })
        }

        assertThat(helpOutputBytes.toString(), containsSubstring(" --opt-x=OPT-X  a descriptive description\n"))
    }

    @Test
    fun can_specify_metavar_of_command_line_option() {
        val helpOutputBytes = ByteArrayOutputStream()

        expectThrown<Help> {
            parseArgs(arrayOf("--help"),
                    CommandLineOption(optX, short="x", metavar = "THE_X", description = "set the x"),
                    helpOutput = helpOutputBytes,
                    helpExit = { throw Help() })
        }

        assertThat(helpOutputBytes.toString(), containsSubstring(" -x THE_X, --opt-x=THE_X  set the x\n"))
    }

    @Test
    fun reports_searched_location_when_property_not_defined() {
        val (config) = parseArgs(emptyArray(),
                CommandLineOption(optX, long="xxx", short="y"))

        try {
            config[optX]
        }
        catch (expected: Misconfiguration) {
            assertThat(expected.message, present(containsSubstring("--xxx")))
            assertThat(expected.message, present(containsSubstring("-y")))
        }
    }

    @Test
    fun reports_searched_location_when_only_long_option() {
        val (config) = parseArgs(emptyArray(),
                CommandLineOption(optX))

        assertThat(config.searchPath(optX).map { it.nameInLocation }, equalTo(listOf("--opt-x")))
    }

    @Test
    fun reports_searched_location_when_long_and_short_option() {
        val (config) = parseArgs(emptyArray(),
                CommandLineOption(optX, short="x"))

        assertThat(config.searchPath(optX).map { it.nameInLocation }, equalTo(listOf("-x", "--opt-x")))
    }

    @Test
    fun reports_searched_location_when_custom_long_and_short_option() {
        val (config) = parseArgs(emptyArray(),
                CommandLineOption(optX, long="the-x", short="x"))

        assertThat(config.searchPath(optX).map { it.nameInLocation }, equalTo(listOf("-x", "--the-x")))
    }
}
