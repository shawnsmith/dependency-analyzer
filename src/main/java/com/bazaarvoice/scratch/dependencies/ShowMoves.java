package com.bazaarvoice.scratch.dependencies;

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
import java.util.Map;
import java.util.Set;

public class ShowMoves {

    @SuppressWarnings({"AccessStaticViaInstance"})
    private static Options createOptions() {
        Options options = new Options();
        options.addOption("?", false, "Show this message");
        options.addOption(OptionBuilder.withLongOpt("root").hasArg().isRequired().withDescription("Source root, eg. /svnwork/prr/trunk/working").create());
        options.addOption(OptionBuilder.withLongOpt("moves").hasArg().isRequired().withDescription("File listing class moves, organized by Maven artifact").create());
        options.addOption(OptionBuilder.withLongOpt("package").hasArg().withDescription("Restrict analysis to classes under the specified package").create());
        options.addOption(OptionBuilder.withLongOpt("group").hasArg().withDescription("Restrict analysis to Maven modules with the specified group prefix").create());
        options.addOption(OptionBuilder.withLongOpt("svn").withDescription("Display the moves as a set of \"svn mv\" commands").create());
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
        File rootDirectory = new File(cmd.getOptionValue("root"));
        File movesFile = new File(cmd.getOptionValue("moves"));
        String packageFilter = cmd.getOptionValue("package", "");
        String groupPrefix = cmd.getOptionValue("group", "");

        // scan all the pom.xml files
        Modules modules = new Modules();
        modules.scan(rootDirectory, groupPrefix);

        // load the moves file
        ClassLocations moves = ClassLocations.parseFile(movesFile, groupPrefix);

        // scan the compiled classes of all the maven targets
        ClassScanner classScanner = new ClassScanner(packageFilter);
        classScanner.scan(modules.getAllModules());
        ClassLocations locations = classScanner.getLocations();

        // log warnings about the move file
        for (Map.Entry<ClassName, ModuleName> entry : moves.getLocations()) {
            ClassName className = entry.getKey();
            ModuleName moduleName = entry.getValue();
            if (locations.getModule(className) == null) {
                System.err.println("Warning: move of unknown class will be ignored: " + className);
            }
            if (modules.getModule(moduleName) == null) {
                System.err.println("Warning: move to unknown module will be ignored: " + moduleName);
            }
        }

        // ignore moves that have no effect (maybe they're left over from a previous round of moves)
        ClassLocations effectiveMoves = moves.difference(locations);

        // write out the moves
        PrintWriter out = new PrintWriter(System.out);
        if (cmd.hasOption("svn")) {
            // as a set of "svn mv" operations
            Set<File> createdDirs = Sets.newHashSet();
            for (ModuleName moduleName : Utils.sorted(effectiveMoves.getAllModules())) {
                Module module = modules.getModule(moduleName);
                if (module != null) {
                    for (ClassName className : Utils.sorted(effectiveMoves.getClasses(moduleName))) {
                        Module previousModule = modules.getModule(locations.getModule(className));
                        if (previousModule != null) {
                            printMove(out, className, previousModule, module, rootDirectory, createdDirs);
                        }
                    }
                }
            }

        } else {
            // simple text file format
            effectiveMoves.writeTo(out, groupPrefix);
        }
        out.flush();
    }

    private static void printMove(PrintWriter out, ClassName className, Module src, Module dest, File root, Set<File> createdDirs) {
        File srcFile = className.getLocation(src.getDirectory());
        File destFile = className.getLocation(dest.getDirectory());
        File destDirectory = destFile.getParentFile();
        String srcFilePath = Utils.getRelativePath(srcFile, root);
        String destDirectoryPath = Utils.getRelativePath(destDirectory, root);
        if (createdDirs.add(destDirectory)) {
            out.println("[ -d " + destDirectoryPath + " ] || svn mkdir --parents " + destDirectoryPath);
        }
        out.println("svn mv " + srcFilePath + " " + destDirectoryPath + "/");
    }
}
