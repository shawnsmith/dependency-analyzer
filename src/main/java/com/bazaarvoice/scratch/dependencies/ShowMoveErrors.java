package com.bazaarvoice.scratch.dependencies;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class ShowMoveErrors {

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
            new HelpFormatter().printHelp("java " + ShowMoves.class.getName(), options);
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
        ClassDependencies dependencies = classScanner.getDependencies();

        // apply the moves file
        locations.moveAll(moves);

        // find modules that reference classes they don't have access to
        Map<ModuleName, ListMultimap<ClassName, ClassName>> brokenMap = Maps.newHashMap();
        for (Map.Entry<ClassName, ModuleName> entry : locations.getLocations()) {
            ClassName className = entry.getKey();
            ModuleName moduleName = entry.getValue();
            Set<ClassName> referencedClasses = dependencies.getReferencedClasses(className);

            ListMultimap<ClassName, ClassName> moduleBrokenMap = null;
            for (ClassName referencedClass : referencedClasses) {
                ModuleName referencedModule = locations.getModule(referencedClass);
                if (referencedModule != null && !modules.isVisibleTo(moduleName, referencedModule)) {
                    if (moduleBrokenMap == null) {
                        moduleBrokenMap = brokenMap.get(moduleName);
                        if (moduleBrokenMap == null) {
                            brokenMap.put(moduleName, moduleBrokenMap = ArrayListMultimap.create());
                        }
                    }
                    moduleBrokenMap.put(className, referencedClass);
                }
            }
        }

        // report broken dependencies
        System.out.println();
        for (ModuleName moduleName : Utils.sorted(brokenMap.keySet())) {
            ListMultimap<ClassName, ClassName> missingMap = brokenMap.get(moduleName);

            System.out.println();
            System.out.println(moduleName.toString(groupPrefix));

            for (ClassName className : Utils.sorted(missingMap.keySet())) {
                System.out.println("  " + className);
                for (ClassName referencedClass : Utils.sorted(missingMap.get(className))) {
                    ModuleName referencedModule = locations.getModule(referencedClass);
                    System.out.println("    " + referencedClass + " (" + referencedModule.toString(groupPrefix) + ")");
                }
            }
        }
    }
}
