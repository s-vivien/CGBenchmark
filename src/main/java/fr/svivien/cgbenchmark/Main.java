package fr.svivien.cgbenchmark;

import org.apache.commons.cli.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Main {

    private static final Log LOG = LogFactory.getLog(Main.class);

    public static void main(String[] args) {
        Options options = new Options();

        options.addOption("h", false, "Print the help.")
                .addOption("c", true, "Required. JSON configuration file.");

        CommandLine cmd = null;
        try {
            cmd = new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            LOG.fatal("CGBenchmark has encountered a problem while parsing command line", e);
            System.exit(1);
        }

        // Need help ?
        if (cmd.hasOption("h") || !cmd.hasOption("c")) {
            new HelpFormatter().printHelp("-c <json config file path>", options);
            System.exit(0);
        }

        String cfgPath = cmd.getOptionValue("c");
        CGBenchmark cgBenchmark = new CGBenchmark(cfgPath);
        cgBenchmark.launch();
    }
}