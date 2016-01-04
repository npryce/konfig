package com.natpryce.konfig

import java.io.OutputStream
import java.io.PrintWriter
import java.util.*

data class CommandLineOption(
        val configKey: Key<*>,
        val long: String = configKey.name.replace('.', '-'),
        val short: String? = null,
        val description: String = "set ${configKey.name.replace(".", " ")}",
        val metavar: String = long.toUpperCase())
{
    init {
        if (long.startsWith("-")) throw IllegalArgumentException("long flag must not be specified with leading '-'")
        if (short != null && short.startsWith("-")) throw IllegalArgumentException("short flag must not be specified with leading '-'")
    }

    val longFlag = "--$long"
    val shortFlag = if (short == null) null else "-$short"
}


private data class CommandLineProperty(val flagUsed: String, val value: String)


private class CommandLineConfiguration(allOptions: List<CommandLineOption>,
                                       private val optionsUsed: Map<Key<*>, CommandLineProperty>) : Configuration
{
    private val optionsByKey = allOptions.toMapBy { it.configKey }
    private val location: Location = Location("command-line parameters")

    override fun <T> getOrNull(key: Key<T>): T? {
        val optionUsed = optionsUsed[key]
        return if (optionUsed == null) {
            null
        } else {
            key.parse(optionUsed.value) { PropertyLocation(key, location, optionUsed.flagUsed) }
        }
    }

    override fun location(key: Key<*>) = PropertyLocation(key, location,
            optionsByKey[key]?.longFlag ?: throw IllegalArgumentException("no command-line option defined for key ${key.name}"))

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
        return listOf(location to optionsUsed.values.toMapBy({ it.flagUsed }, { it.value }))
    }
}


fun parseArgs(args: Array<String>,
              vararg options: CommandLineOption,
              helpOutput: OutputStream = System.err,
              helpExit: ()->Nothing = { System.exit(0) as Nothing }):
        Pair<Configuration, List<String>>
{
    if ("--help" in args || "-h" in args) {
        PrintWriter(helpOutput).printHelp(options)
        helpExit()
    }
    else {
        val files = ArrayList<String>()
        val properties = HashMap<Key<*>, CommandLineProperty>()
        val shortOpts: Map<String, CommandLineOption> = options.filter { it.short != null }.toMapBy({ "-${it.short!!}" }, { it })
        val longOpts: Map<String, CommandLineOption> = options.toMapBy({ "--${it.long}" }, { it })

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

        return Pair(CommandLineConfiguration(options.asList(), properties), files)
    }
}

fun PrintWriter.printHelp(options: Array<out CommandLineOption>) {
    val helpOptionLine = "-h, --help" to "show this help message and exit"

    val helpLines = options.map {
        (it.short?.let { s -> "-$s ${it.metavar}, " } ?: "") + "--${it.long}=${it.metavar}" to it.description
    } + helpOptionLine

    val optHelpLength = helpLines.map{it.first.length}.max()!!

    println("Usage: <program> [options] FILE ...")
    println()
    println("Options:")
    for ((opt, description) in helpLines) {
        println("  ${opt.padEnd(optHelpLength)}  $description")
    }
    flush()
}

infix fun Pair<Configuration, List<String>>.overriding(defaults: Configuration) = copy(first = first overriding defaults)


