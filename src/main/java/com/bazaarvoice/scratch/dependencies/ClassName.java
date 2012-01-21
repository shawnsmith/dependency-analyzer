package com.bazaarvoice.scratch.dependencies;

import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.Type;

import java.io.File;

/**
 * Type-safe wrapper around a Java class name.
 */
public class ClassName implements Comparable<ClassName> {

    private final String _className;

    public ClassName(Type type) {
        this(type.getClassName());
    }

    public ClassName(Class clazz) {
        this(clazz.getName());
    }

    public ClassName(String className) {
        _className = className;
    }

    public boolean isMemberOfPackage(String packageName) {
        return Utils.hasPrefix(_className, packageName, '.');
    }

    public ClassName getOuterClassName() {
        int dollar = _className.indexOf('$');
        return (dollar != -1) ? new ClassName(_className.substring(0, dollar)) : this;
    }

    @Override
    public boolean equals(Object o) {
        return this == o ||
                (o instanceof ClassName) && _className.equals(((ClassName) o)._className);
    }

    @Override
    public int hashCode() {
        return _className.hashCode();
    }

    @Override
    public int compareTo(ClassName className) {
        return _className.compareTo(className.toString());
    }

    public File getLocation(File rootDirectory) {
        // reverse the logic in ClassScanner.java, more or less
        if (_className.endsWith(".xml")) {
            if (_className.contains(":")) {
                return findPreferredLocation(rootDirectory, StringUtils.substringAfterLast(_className, ":"), "src/main/webapp/WEB-INF");
            } else {
                return findPreferredLocation(rootDirectory, _className, "src/main/resources", "src/main/java");
            }
        }
        return findPreferredLocation(rootDirectory, _className.replace('.', '/') + ".java", "src/main/java");
    }

    private File findPreferredLocation(File rootDirectory, String filename, String... prefixes) {
        for (String prefix : prefixes) {
            File file = new File(new File(rootDirectory, prefix), filename);
            if (file.isFile()) {
                return file;
            }
        }
        return new File(new File(rootDirectory, prefixes[0]), filename);
    }

    @Override
    public String toString() {
        return _className;
    }
}
