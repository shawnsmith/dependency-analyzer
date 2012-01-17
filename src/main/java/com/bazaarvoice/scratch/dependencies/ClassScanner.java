package com.bazaarvoice.scratch.dependencies;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import java.io.File;
import java.util.Collection;
import java.util.Set;
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
        final File classesDir = new File(module.getDirectory(), "target/classes");
        if (!classesDir.isDirectory()) {
            return;
        }
        System.err.println("scanning " + module.getName() + "...");
        Utils.findClassFiles(classesDir, new Function<byte[], Void>() {
            @Override
            public Void apply(byte[] classBytes) {
                ClassReader reader = new ClassReader(classBytes);
                ClassName className = new ClassName(Type.getObjectType(reader.getClassName())).getOuterClassName();
                synchronized(_locations) {
                    _locations.add(className, module.getName());
                }

                Set<ClassName> referencedClasses = new ClassExtractor(_packageFilter).visit(reader).getClassNames();
                synchronized (_dependencies) {
                    _dependencies.add(className, referencedClasses);
                }
                return null;
            }
        });
    }
}
