package com.bazaarvoice.scratch.dependencies;

import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ClassScanner {

    private final String _packageFilter;
    private final ClassLocations _locations = new ClassLocations();
    private final ClassDependencies _dependencies = new ClassDependencies();

    public ClassScanner(String packageFilter) {
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
        for (final Module module : modules) {
            threadPool.submit(new Runnable() {
                @Override
                public void run() {
                    scan(module);
                }
            });
        }
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            throw Throwables.propagate(e);
        }
    }

    public void scan(final Module module) {
        if (!new File(module.getDirectory(), "target").isDirectory()) {
            return;
        }
        System.err.println("scanning " + module.getName() + "...");
        Utils.walkDirectory(new File(module.getDirectory(), "target/classes"), new Utils.FileSink() {
            @Override
            public void accept(String fileName, InputSupplier<? extends InputStream> inputSupplier) throws IOException {
                String extension = Files.getFileExtension(fileName);
                if ("class".equals(extension)) {
                    scanClass(module, inputSupplier);
                } else if ("xml".equals(extension) && Utils.getFileName(fileName).startsWith("applicationContext")) {
                    scanSpring(module, fileName, inputSupplier);
                }
            }
        });
        Utils.walkDirectory(new File(module.getDirectory(), "src/main/resources"), new Utils.FileSink() {
            @Override
            public void accept(String fileName, InputSupplier<? extends InputStream> inputSupplier) throws IOException {
                String extension = Files.getFileExtension(fileName);
                if ("xml".equals(extension) && Utils.getFileName(fileName).startsWith("applicationContext")) {
                    scanSpring(module, fileName, inputSupplier);
                }
            }
        });
        Utils.walkDirectory(new File(module.getDirectory(), "src/main/webapp/WEB-INF"), new Utils.FileSink() {
            @Override
            public void accept(String fileName, InputSupplier<? extends InputStream> inputSupplier) throws IOException {
                String extension = Files.getFileExtension(fileName);
                if ("xml".equals(extension) && Utils.getFileName(fileName).startsWith("applicationContext")) {
                    scanSpring(module, module + ":" + fileName, inputSupplier);
                }
            }
        });
    }

    private void scanClass(Module module, InputSupplier<? extends InputStream> inputSupplier) throws IOException {
        byte[] classBytes = ByteStreams.toByteArray(inputSupplier);
        ClassReader reader = new ClassReader(classBytes);
        ClassName className = new ClassName(Type.getObjectType(reader.getClassName())).getOuterClassName();
        addLocation(className, module);
        addDependencies(className, new ClassExtractor(_packageFilter).visit(reader).getClassNames());
    }

    private void scanSpring(Module module, String fileName, InputSupplier<? extends InputStream> inputSupplier) {
        ClassName className = new ClassName(fileName);
        addLocation(className, module);
        addDependencies(className, new SpringExtractor(_packageFilter).visit(inputSupplier, fileName).getClassNames());
    }

    private synchronized void addDependencies(ClassName className, Collection<ClassName> referencedClasses) {
        _dependencies.add(className, referencedClasses);
    }

    private synchronized void addLocation(ClassName className, Module module) {
        _locations.add(className, module.getName());
    }
}
