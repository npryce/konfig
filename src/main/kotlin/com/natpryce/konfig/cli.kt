package com.natpryce.konfig

import com.sun.xml.internal.ws.assembler.MetroConfigName
import java.util.*


data class CommandLineOption(
        val configKey: Key<*>,
        val long: String,
        val short: String?)
{
    val configName: String get() = configKey.name
}


fun parseArgs(args: Array<String>, vararg defs: CommandLineOption): Pair<Configuration, List<String>> {
    val files = ArrayList<String>()
    val config = HashMap<String, String>()
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

            config[configNameFor(configNameByOpt, opt)] = args[i]
        }

        when {
            arg.startsWith("--") -> {
                val bareOpt = arg.substring(2)
                if (bareOpt.contains('=')) {
                    config[configNameFor(longOpts, bareOpt.substringBefore('='))] = bareOpt.substringAfter('=')
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

    return Pair(ConfigurationMap(config), files)
}

