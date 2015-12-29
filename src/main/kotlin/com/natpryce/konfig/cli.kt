package com.natpryce.konfig

import java.util.*


data class CommandLineOption(
        val configKey: Key<*>,
        val long: String = configKey.name.replace('.', '-'),
        val short: String? = null)
{
    val configName: String get() = configKey.name
}


private data class CommandLineProperty(val flagUsed: String, val value: String)

private class CommandLineConfiguration (private val options: Map<String, CommandLineProperty>) :
        Configuration
{
    private val location: Location = Location("command-line parameters")

    override fun <T> getOrNull(key: Key<T>): T? =
            options[key.name]?.value?.let { stringValue -> key.parse(stringValue) { location(key) } }

    override fun location(key: Key<*>): PropertyLocation {
        return PropertyLocation(key, location, options[key.name]?.flagUsed?:throw IllegalArgumentException("property not defined"))
    }

    override fun list(): List<Pair<Location, Map<String, String>>> {
        return listOf(location to options.values.toMapBy({it.flagUsed}, {it.value}))
    }
}


fun parseArgs(args: Array<String>, vararg defs: CommandLineOption): Pair<Configuration, List<String>> {
    val files = ArrayList<String>()
    val properties = HashMap<String, CommandLineProperty>()
    val shortOpts: Map<String, CommandLineOption> = defs.filter { it.short != null }.toMapBy({ "-${it.short!!}" }, { it })
    val longOpts: Map<String, CommandLineOption> = defs.toMapBy({ "--${it.long}" }, { it })

    var i = 0;
    while (i < args.size) {
        val arg = args[i]

        fun Map<String, CommandLineOption>.configNameFor(opt: String) =
                this[opt]?.configName ?: throw Misconfiguration("unrecognised command-line option $arg")

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

    return Pair(CommandLineConfiguration(properties), files)
}

infix fun Pair<Configuration, List<String>>.overriding(defaults: Configuration) = copy(first = first overriding defaults)
