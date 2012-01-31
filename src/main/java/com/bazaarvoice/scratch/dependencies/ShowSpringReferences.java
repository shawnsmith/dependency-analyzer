package com.bazaarvoice.scratch.dependencies;

import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;

public class ShowSpringReferences {

    @SuppressWarnings({"AccessStaticViaInstance"})
    private static Options createOptions() {
        Options options = new Options();
        options.addOption("?", false, "Show this message");
        options.addOption(OptionBuilder.withLongOpt("artifact").hasArg().isRequired().withDescription("Name of the referring artifact").create());
        options.addOption(OptionBuilder.withLongOpt("package").hasArg().withDescription("Restrict analysis to classes under the specified package").create());
        options.addOption(OptionBuilder.withLongOpt("group").hasArg().withDescription("Restrict analysis to Maven modules with the specified group prefix").create());
        options.addOption(OptionBuilder.withLongOpt("out").hasArg().withDescription("Name of the output file, defaults to stdout.").create());
        return options;
    }

    public static void main(String[] args) throws Exception {
        Options options = createOptions();
        CommandLineParser parser = new GnuParser();
        CommandLine cmd = parser.parse(options, args);
        if (cmd.hasOption("?")) {
            new HelpFormatter().printHelp("java " + ShowSpringReferences.class.getName(), options);
            System.exit(1);
        }
        String groupPrefix = cmd.getOptionValue("group", "");
        final ModuleName moduleName = ModuleName.parseDescriptor(cmd.getOptionValue("artifact"), groupPrefix);
        final Predicate<ClassName> packageFilter = new PackagePredicate(cmd.getOptionValue("package", ""));
        String outFileName = cmd.getOptionValue("out", "-");

        final ClassLocations references = new ClassLocations();

        //noinspection unchecked
        for (String arg : (List<String>) cmd.getArgList()) {
            Utils.walkDirectory(new File(arg), new Utils.FileSink() {
                @Override
                public void accept(String filePath, InputSupplier<? extends InputStream> inputSupplier) throws IOException {
                    if (filePath.contains("/config/") && filePath.endsWith(".xml") && !filePath.endsWith("bundleConfiguration.xml")) {
                        scanFile(references, moduleName, filePath, inputSupplier, packageFilter);
                    }
                }
            });
        }

        // write the output file
        PrintWriter out = outFileName == null || "-".equals(outFileName) ? new PrintWriter(System.out) :
                new PrintWriter(Files.newWriter(new File(outFileName), Charsets.UTF_8));
        references.writeTo(out, groupPrefix);
        out.close();
    }

    private static void scanFile(ClassLocations references, ModuleName moduleName, String filePath,
                                 InputSupplier<? extends InputStream> inputSupplier, Predicate<ClassName> packageFilter) {
        ClassCollector classes = new ClassCollector(packageFilter);
        new SpringExtractor(classes).visit(inputSupplier, filePath);
        for (ClassName className : classes.getClassNames()) {
            references.add(className, moduleName);
        }
    }
}
