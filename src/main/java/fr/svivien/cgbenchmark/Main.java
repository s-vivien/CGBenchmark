package fr.svivien.cgbenchmark;

import org.apache.commons.cli.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

public class Main {

    private static final Log LOG = LogFactory.getLog(Main.class);

    public static void main(String[] args) {
        Options options = new Options();

        options.addOption("h", false, "Print the help.")
                .addOption("c", true, "Required. JSON configuration file.")
                .addOption("l", false, "Optional. Enable logs saving in current folder for each game.");

        CommandLine cmd = null;
        try {
            cmd = new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            LOG.fatal("CGBenchmark has encountered a problem while parsing command line", e);
            System.exit(1);
        }

        // Need help ?
        if (cmd.hasOption("h") || !cmd.hasOption("c")) {
            new HelpFormatter().printHelp("-c <json config file path> [-l]", options);
            System.exit(0);
        }

        String cfgPath = cmd.getOptionValue("c");
        boolean saveLogs = cmd.hasOption('l');
        CGBenchmark cgBenchmark = new CGBenchmark(cfgPath, saveLogs);
        pausable(cgBenchmark).start();
        cgBenchmark.launch();
    }

    private static Thread pausable(CGBenchmark cgBenchmark) {
        final Thread pauseThread = new Thread(() -> {
            while (true) {
                System.err.println("Press ENTER to pause ...");
                waitForEnter();
                System.err.println("Consumers will pause after ongoing requests are completed");
                cgBenchmark.pause();

                System.err.println("Press ENTER to resume ...");
                waitForEnter();
                System.err.println("Resuming benchmark ...");
                cgBenchmark.resume();
            }
        }, "Wait for pause");
        pauseThread.setDaemon(true);
        return pauseThread;
    }

    private static void waitForEnter() {
        try {
            System.in.read();
            while (System.in.available() > 0) {
                System.in.read();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}