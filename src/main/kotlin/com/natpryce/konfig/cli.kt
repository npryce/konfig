@file:JvmName("CommandLineOptions")
package com.natpryce.konfig

import java.io.OutputStream
import java.io.PrintWriter
import java.util.*

data class CommandLineOption(
        val configKey: Key<*>,
        val long: String = configKey.name.replace('.', '-'),
        val short: String? = null,
        val description: String = "set ${configKey.name.replace(".", " ")}",
        val metavar: String = long.toUpperCase(),
        val required: Boolean = false)
{
    init {
        if (long.startsWith("-")) throw IllegalArgumentException("long flag must not be specified with leading '-'")
        if (short != null && short.startsWith("-")) throw IllegalArgumentException("short flag must not be specified with leading '-'")
    }

    val longFlag = "--$long"
    val shortFlag = short?.let { "-$it" }
}


private data class CommandLineProperty(val flagUsed: String, val value: String)


private class CommandLineConfiguration(allOptions: List<CommandLineOption>,
                                       private val optionsUsed: Map<Key<*>, CommandLineProperty>) : Configuration
{
    private val optionsByKey = allOptions.associateBy { it.configKey }
    private val location: Location = Location("command-line parameters")

    override fun <T> getOrNull(key: Key<T>) = optionsUsed[key]?.let {
        key.parse(PropertyLocation(key, location, it.flagUsed), it.value)
    }

    override fun searchPath(key: Key<*>): List<PropertyLocation> {
        val opt = optionsByKey[key]

        val result = ArrayList<PropertyLocation>()

        if (opt != null) {
            if (opt.shortFlag != null) {
                result.add(PropertyLocation(key, location, opt.shortFlag))
            }
            result.add(PropertyLocation(key, location, opt.longFlag))
        }

        return result
    }

    override fun list(): List<Pair<Location, Map<String, String>>> {
        return listOf(location to optionsUsed.values.associateBy({ it.flagUsed }, { it.value }))
    }
}


fun parseArgs(args: Array<String>,
              vararg options: CommandLineOption,
              helpOutput: OutputStream = System.err,
              helpExit: () -> Nothing = { System.exit(0) as Nothing },
              programName: String = "<program>",
              argMetavar: String = "FILE"):
        Pair<Configuration, List<String>>
{
    if ("--help" in args || "-h" in args) {
        PrintWriter(helpOutput).printHelp(programName, argMetavar, options)
        helpExit()
    }

    val files = ArrayList<String>()
    val properties = HashMap<Key<*>, CommandLineProperty>()
    val shortOpts: Map<String, CommandLineOption> = options.filter { it.short != null }.associateBy({ "-${it.short!!}" }, { it })
    val longOpts: Map<String, CommandLineOption> = options.associateBy({ "--${it.long}" }, { it })

    var i = 0;
    while (i < args.size) {
        val arg = args[i]

        fun Map<String, CommandLineOption>.configNameFor(opt: String) =
                this[opt]?.configKey ?: throw Misconfiguration("unrecognised command-line option $arg")

        fun storeNextArg(configNameByOpt: Map<String, CommandLineOption>, opt: String) {
            i++
            if (i >= args.size) throw Misconfiguration("no argument for $arg command-line option")

            properties[configNameByOpt.configNameFor(opt)] = CommandLineProperty(arg, args[i])
        }

        when {
            arg.startsWith("--") -> {
                if (arg.contains('=')) {
                    val flag = arg.substringBefore('=')
                    val value = arg.substringAfter('=')
                    properties[longOpts.configNameFor(flag)] = CommandLineProperty(flag, value)
                } else {
                    storeNextArg(longOpts, arg)
                }
            }
            arg.startsWith("-") -> {
                storeNextArg(shortOpts, arg)
            }
            else -> {
                files.add(arg)
            }
        }

        i++
    }
    validateRequiredParameters(options, properties)
    return Pair(CommandLineConfiguration(options.asList(), properties), files)
}

private fun validateRequiredParameters(options: Array<out CommandLineOption>,
                                       properties: HashMap<Key<*>, CommandLineProperty>) {
    val missingOptions = options
            .filter { it.required && !properties.containsKey(it.configKey) }
            .map { it.formatCmdOption() }
    if (missingOptions.isNotEmpty()) {
        val optHelpLength = missingOptions.map { it.first.length }.max()!!
        val error = missingOptions
                .joinTo(StringBuilder("The following options are required:").appendln().appendln(),
                        separator = System.lineSeparator()) {
                    val (opt, description) = it
                    "  ${opt.padEnd(optHelpLength)}  $description"
                }
        throw Misconfiguration(error.toString())
    }
}

fun PrintWriter.printHelp(programName: String, argMetavar: String, options: Array<out CommandLineOption>) {
    val helpOptionLine = "-h, --help" to "show this help message and exit"

    val helpLines = options.map { it.formatCmdOption() } + helpOptionLine

    val optHelpLength = helpLines.map { it.first.length }.max()!!

    println("Usage: $programName [options] $argMetavar ...")
    println()
    println("Options:")
    for ((opt, description) in helpLines) {
        println("  ${opt.padEnd(optHelpLength)}  $description")
    }
    flush()
}

private fun CommandLineOption.formatCmdOption() =
        (short?.let { s -> "-$s $metavar, " } ?: "") + "--$long=$metavar" to description

infix fun Pair<Configuration, List<String>>.overriding(defaults: Configuration) = copy(first = first overriding defaults)


