@file:JvmName("CommandLineOptions")

package com.natpryce.konfig

import java.io.OutputStream
import java.io.PrintWriter
import java.util.ArrayList
import java.util.HashMap
import kotlin.system.exitProcess

data class CommandLineOption(
    val configKey: Key<*>,
    val long: String = configKey.name.replace('.', '-'),
    val short: String? = null,
    val description: String = "set ${configKey.name.replace(".", " ")}",
    val metavar: String = long.toUpperCase()) {
    init {
        if (long.startsWith("-")) throw IllegalArgumentException("long flag must not be specified with leading '-'")
        if (short != null && short.startsWith("-")) throw IllegalArgumentException("short flag must not be specified with leading '-'")
    }

    val longFlag = "--$long"
    val shortFlag = short?.let { "-$it" }
}


private data class CommandLineProperty(val flagUsed: String, val value: String)

private class CommandLineConfiguration(
    allOptions: List<CommandLineOption>,
    private val optionsUsed: Map<Key<*>, CommandLineProperty>

) : Configuration {

    private val optionsByKey = allOptions.associateBy { it.configKey }
    internal val location: Location = Location("command-line parameters")

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

    override fun locationOf(key: Key<*>): PropertyLocation? {
        return optionsUsed[key]?.let { PropertyLocation(key, location, it.flagUsed) }
    }

    override fun list(): List<Pair<Location, Map<String, String>>> {
        return listOf(location to optionsUsed.values.associateBy({ it.flagUsed }, { it.value }))
    }
}


fun parseArgs(args: Array<String>,
              vararg options: CommandLineOption,
              helpOutput: OutputStream = System.err,
              helpExit: () -> Nothing = { exitProcess(0) },
              programName: String = "<program>",
              argMetavar: String = "FILE"):
    Pair<Configuration, List<String>> {
    if ("--help" in args || "-h" in args) {
        PrintWriter(helpOutput).printHelp(programName, argMetavar, options)
        helpExit()
    }

    val files = ArrayList<String>()
    val properties = HashMap<Key<*>, CommandLineProperty>()
    val shortOpts: Map<String, CommandLineOption> = options.filter { it.short != null }.associateBy({ "-${it.short!!}" }, { it })
    val longOpts: Map<String, CommandLineOption> = options.associateBy({ "--${it.long}" }, { it })

    var i = 0
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
                }
                else {
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

    return Pair(CommandLineConfiguration(options.asList(), properties), files)
}

fun PrintWriter.printHelp(programName: String, argMetavar: String, options: Array<out CommandLineOption>) {
    val helpOptionLine = "-h, --help" to "show this help message and exit"

    val helpLines = options.map {
        (it.short?.let { s -> "-$s ${it.metavar}, " } ?: "") + "--${it.long}=${it.metavar}" to it.description
    } + helpOptionLine

    val optHelpLength = helpLines.map { it.first.length }.max()!!

    println("Usage: $programName [options] $argMetavar ...")
    println()
    println("Options:")
    for ((opt, description) in helpLines) {
        println("  ${opt.padEnd(optHelpLength)}  $description")
    }
    flush()
}

infix fun Pair<Configuration, List<String>>.overriding(defaults: Configuration) = copy(first = first overriding defaults)


