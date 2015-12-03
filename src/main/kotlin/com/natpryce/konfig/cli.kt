package com.natpryce.konfig

import java.util.*


data class CommandLineOption(
        val configKey: Key<*>,
        val long: String,
        val short: String?)


fun parseArgs(args: Array<String>, vararg defs: CommandLineOption): Pair<Configuration, List<String>> {
    val files = ArrayList<String>()
    val config = HashMap<String, String>()

    var i = 0;
    while (i < args.size) {
        val arg = args[i]
        when {
            arg.startsWith("--") -> {
                val bareArg = arg.substring(2)
                if (bareArg.contains('=')) {
                    val optName = bareArg.substringBefore('=')
                    val value = bareArg.substringAfter('=')

                    val def = defs.find { it.long == optName } ?: throw Misconfiguration("unrecognised option $arg")

                    config[def.configKey.name] = value
                }
                else {
                    val def = defs.find { it.long == bareArg } ?: throw Misconfiguration("unrecognised option $arg")

                    i++
                    if (i >= args.size) throw Misconfiguration("no argument for $arg option")

                    config[def.configKey.name] = args[i]
                }
            }
            arg.startsWith("-") -> {
                val bareArg = arg.substring(1)
                val def = defs.find { it.short == bareArg } ?: throw Misconfiguration("unrecognised option $arg")

                i++
                if (i >= args.size) throw Misconfiguration("no argument for $arg option")

                config[def.configKey.name] = args[i]
            }
            else -> {
                files.add(arg)
            }
        }

        i++
    }

    return Pair(ConfigurationMap(config), files)
}