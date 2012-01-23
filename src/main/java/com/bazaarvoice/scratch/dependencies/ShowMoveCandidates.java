package com.bazaarvoice.scratch.dependencies;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
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
import java.util.Set;

public class ShowMoveCandidates {

    @SuppressWarnings({"AccessStaticViaInstance"})
    private static Options createOptions() {
        Options options = new Options();
        options.addOption("?", false, "Show this message");
        options.addOption(OptionBuilder.withLongOpt("root").hasArg().isRequired().withDescription("Source root, eg. /svnwork/prr/trunk/working").create());
        options.addOption(OptionBuilder.withLongOpt("src").hasArg().isRequired().withDescription("Source module").create());
        options.addOption(OptionBuilder.withLongOpt("dest").hasArg().isRequired().withDescription("Destination module").create());
        options.addOption(OptionBuilder.withLongOpt("moves").hasArg().withDescription("File listing class moves, organized by Maven artifact").create());
        options.addOption(OptionBuilder.withLongOpt("package").hasArg().withDescription("Restrict analysis to classes under the specified package").create());
        options.addOption(OptionBuilder.withLongOpt("group").hasArg().withDescription("Restrict analysis to Maven modules with the specified group prefix").create());
        return options;
    }

    public static void main(String[] args) throws ParseException, IOException {
        Options options = createOptions();
        CommandLineParser parser = new GnuParser();
        CommandLine cmd = parser.parse(options, args);
        if (cmd.hasOption("?")) {
            new HelpFormatter().printHelp("java " + ShowMoves.class.getName(), options);
            System.exit(1);
        }
        File rootFile = new File(cmd.getOptionValue("root"));
        Predicate<ClassName> packageFilter = new PackagePredicate(cmd.getOptionValue("package", ""));
        String groupPrefix = cmd.getOptionValue("group", "");
        ModuleName src = ModuleName.parseDescriptor(cmd.getOptionValue("src"), groupPrefix);
        ModuleName dest = ModuleName.parseDescriptor(cmd.getOptionValue("dest"), groupPrefix);
        File movesFile = cmd.hasOption("moves") ? new File(cmd.getOptionValue("moves")) : null;

        // scan all the pom.xml files
        Modules modules = new Modules();
        modules.scan(rootFile, groupPrefix);

        if (modules.getModule(src) == null) {
            throw new IllegalArgumentException("Unknown source module: " + src);
        }
        if (modules.getModule(dest) == null) {
            throw new IllegalArgumentException("Unknown destination module: " + dest);
        }

        // load the moves file, if one was provided
        ClassLocations moves = (movesFile != null) ? ClassLocations.parseFile(movesFile, groupPrefix) : null;

        // scan the compiled classes of all the maven targets
        ClassScanner classScanner = new ClassScanner(packageFilter);
        classScanner.scan(modules.getAllModules());
        ClassLocations locations = classScanner.getLocations();
        ClassDependencies dependencies = classScanner.getDependencies();

        // apply the moves file
        if (moves != null) {
            locations.moveAll(moves);
        }

        // get the list of modules that depend on src but not on dest.  these may be affected by moves.
        Set<ClassName> moveBlockers = Sets.newHashSet();
        for (Module module : modules.getAllModules()) {
            if (!src.equals(module.getName()) &&
                    modules.isDependentOf(module.getName(), src) &&
                    !modules.isDependentOf(module.getName(), dest)) {
                for (ClassName className : locations.getClasses(module.getName())) {
                    moveBlockers.add(className);
                }
            }
        }
        moveBlockers = dependencies.getTransitiveClosure(moveBlockers);

        // everything can move from src to dest except for members of moveBlockers
        ClassLocations candidates = new ClassLocations();
        for (ClassName candidate : locations.getClasses(src)) {
            if (!moveBlockers.contains(candidate)) {
                candidates.add(candidate, dest);
            }
        }

        // write out the moves
        PrintWriter out = new PrintWriter(System.out);
        candidates.writeTo(out, groupPrefix);
        out.flush();
    }
}
