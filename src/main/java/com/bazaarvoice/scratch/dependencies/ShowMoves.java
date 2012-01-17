package com.bazaarvoice.scratch.dependencies;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class ShowMoves {

    @SuppressWarnings({"AccessStaticViaInstance"})
    private static Options createOptions() {
        Options options = new Options();
        options.addOption("?", false, "Show this message");
        options.addOption(OptionBuilder.withLongOpt("root").hasArg().isRequired().withDescription("Source root, eg. /svnwork/prr/trunk/working").create());
        options.addOption(OptionBuilder.withLongOpt("moves").hasArg().isRequired().withDescription("File listing class moves, organized by Maven artifact").create());
        options.addOption(OptionBuilder.withLongOpt("package").hasArg().withDescription("Restrict analysis to classes under the specified package").create());
        options.addOption(OptionBuilder.withLongOpt("group").hasArg().withDescription("Restrict analysis to Maven modules with the specified group prefix").create());
        return options;
    }

    public static void main(String[] args) throws ParseException, IOException {
        Options options = createOptions();
        CommandLineParser parser = new GnuParser();
        CommandLine cmd = parser.parse(options, args);
        if (cmd.hasOption("?")) {
            new HelpFormatter().printHelp("java " + ShowMoveErrors.class.getName(), options);
            System.exit(1);
        }
        File rootFile = new File(cmd.getOptionValue("root"));
        File movesFile = new File(cmd.getOptionValue("moves"));
        String packageFilter = cmd.getOptionValue("package", "");
        String groupPrefix = cmd.getOptionValue("group", "");

        // scan all the pom.xml files
        Modules modules = new Modules();
        modules.scan(rootFile, groupPrefix);

        // load the moves file
        ClassLocations moves = ClassLocations.parseFile(movesFile, groupPrefix);

        // scan the compiled classes of all the maven targets
        ClassScanner classScanner = new ClassScanner(packageFilter);
        classScanner.scan(modules.getAllModules());
        ClassLocations locations = classScanner.getLocations();

        // ignore moves that have no effect (maybe they're left over from a previous round of moves)
        ClassLocations effectiveMoves = moves.difference(locations);

        // write out the moves
        PrintWriter out = new PrintWriter(System.out);
        effectiveMoves.writeTo(out, groupPrefix);
        out.flush();
    }
}
