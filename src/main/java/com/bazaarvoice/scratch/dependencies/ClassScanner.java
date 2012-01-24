package com.bazaarvoice.scratch.dependencies;

import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.InputSupplier;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ClassScanner {

    private final Predicate<ClassName> _packageFilter;
    private final ClassLocations _locations = new ClassLocations();
    private final ClassDependencies _dependencies = new ClassDependencies();

    public ClassScanner(Predicate<ClassName> packageFilter) {
        _packageFilter = packageFilter;
    }

    public ClassLocations getLocations() {
        return _locations;
    }

    public ClassDependencies getDependencies() {
        return _dependencies;
    }

    public void scan(Collection<Module> modules) {
        int numThreads = Runtime.getRuntime().availableProcessors() * 2;
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        List<Future> futures = Lists.newArrayList();
        for (final Module module : modules) {
            futures.add(threadPool.submit(new Runnable() {
                @Override
                public void run() {
                    scan(module);
                }
            }));
        }
        threadPool.shutdown();
        for (Future future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
    }

    public void scan(final Module module) {
        System.err.println("scanning " + module.getName() + "...");
        Utils.walkDirectory(new File(module.getDirectory(), "target/classes"), new Utils.FileSink() {
            @Override
            public void accept(String filePath, InputSupplier<? extends InputStream> inputSupplier) throws IOException {
                if (filePath.endsWith(".class")) {
                    scanClass(module, inputSupplier);
                } else {
                    scanFile(module, true, filePath, inputSupplier);
                }
            }
        });
        Utils.walkDirectory(new File(module.getDirectory(), "src/main/resources"), new Utils.FileSink() {
            @Override
            public void accept(String filePath, InputSupplier<? extends InputStream> inputSupplier) throws IOException {
                scanFile(module, true, filePath, inputSupplier);
            }
        });
        Utils.walkDirectory(new File(module.getDirectory(), "src/main/webapp/WEB-INF"), new Utils.FileSink() {
            @Override
            public void accept(String filePath, InputSupplier<? extends InputStream> inputSupplier) throws IOException {
                scanFile(module, false, filePath, inputSupplier);
            }
        });
    }

    private void scanClass(Module module, InputSupplier<? extends InputStream> inputSupplier) throws IOException {
        byte[] classBytes = ByteStreams.toByteArray(inputSupplier);
        ClassReader reader = new ClassReader(classBytes);

        ClassName className = new ClassName(Type.getObjectType(reader.getClassName())).getOuterClassName();
        addLocation(className, module);

        ClassCollector classes = new ClassCollector(_packageFilter);
        new ClassExtractor(classes).visit(reader);
        addDependencies(className, classes.getClassNames());
    }

    private void scanFile(Module module, boolean shared, String filePath, InputSupplier<? extends InputStream> inputSupplier) throws IOException {
        String fileName = Utils.getFileName(filePath);
        ClassCollector classes = new ClassCollector(_packageFilter);
        boolean scanned = false;

        if (fileName.startsWith("applicationContext") && fileName.endsWith(".xml")) {
            new SpringExtractor(classes).visit(inputSupplier, filePath);
            scanned = true;

        } else if (fileName.endsWith(".hbm.xml")) {
            new HibernateExtractor(classes).visit(inputSupplier, filePath);
            scanned = true;

        } else if (fileName.endsWith(".page") || fileName.endsWith(".jwc") || fileName.endsWith(".script")) {
            new TapestryExtractor(classes).visit(inputSupplier, filePath);
            scanned = true;

        } else if ("spring.handlers".equals(fileName)) {
            new SpringHandlersExtractor(classes).visit(inputSupplier);
            scanned = true;
        }

        if (scanned) {
            ClassName className = new ClassName(shared ? filePath : module + ":" + filePath);
            addLocation(className, module);
            addDependencies(className, classes.getClassNames());
        }
    }

    private synchronized void addDependencies(ClassName className, Collection<ClassName> referencedClasses) {
        _dependencies.add(className, referencedClasses);
    }

    private synchronized void addLocation(ClassName className, Module module) {
        _locations.add(className, module.getName());
    }
}
