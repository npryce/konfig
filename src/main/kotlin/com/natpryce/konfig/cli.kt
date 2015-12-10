package com.natpryce.konfig

import java.util.*


data class CommandLineOption(
        val configKey: Key<*>,
        val long: String = configKey.name.replace('.', '-'),
        val short: String? = null) {
    val configName: String get() = configKey.name
}


fun parseArgs(args: Array<String>, vararg defs: CommandLineOption): Pair<Configuration, List<String>> {
    val files = ArrayList<String>()
    val properties = HashMap<String, String>()
    val shortOpts = defs.filter { it.short != null }.toMap({ it.short!! }, { it.configName })
    val longOpts = defs.toMap({ it.long }, { it.configName })

    var i = 0;
    while (i < args.size) {
        val arg = args[i]

        fun configNameFor(configNamesByOpt: Map<String, String>, opt: String) =
                configNamesByOpt[opt] ?: throw Misconfiguration("unrecognised command-line option $arg")

        fun storeNextArg(configNameByOpt: Map<String, String>, opt: String) {
            i++
            if (i >= args.size) throw Misconfiguration("no argument for $arg command-line option")

            properties[configNameFor(configNameByOpt, opt)] = args[i]
        }

        when {
            arg.startsWith("--") -> {
                val bareOpt = arg.substring(2)
                if (bareOpt.contains('=')) {
                    properties[configNameFor(longOpts, bareOpt.substringBefore('='))] = bareOpt.substringAfter('=')
                } else {
                    storeNextArg(longOpts, bareOpt)
                }
            }
            arg.startsWith("-") -> {
                storeNextArg(shortOpts, arg.substring(1))
            }
            else -> {
                files.add(arg)
            }
        }

        i++
    }

    return Pair(ConfigurationMap(properties, Location("command-line arguments")), files)
}

infix fun Pair<Configuration, List<String>>.overriding(defaults: Configuration) = copy(first = first overriding defaults)
