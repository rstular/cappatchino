package cappatchino.cli

import cappatchino.patcher.Patcher
import mu.two.KotlinLogging
import org.apache.commons.cli.*
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    logger.info { "Starting cappatchino" }

    val opts = Options().run {
        addOption(Option.builder("t").longOpt("target").hasArg(true).required(true).desc("Path to target JAR file").build())
        addOption(Option.builder("p").longOpt("patches").hasArg(true).required(true).desc("Path to patches JAR file").build())
    }

    val cmd: CommandLine = try {
        DefaultParser().parse(opts, args)
    } catch (e: ParseException) {
        logger.error { e.message }
        HelpFormatter().printHelp("Usage:", opts)
        exitProcess(1)
    }

    val patcher = Patcher(cmd.getOptionValue("t"))
    patcher.patch(cmd.getOptionValue("p"))
}
