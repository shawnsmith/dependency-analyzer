package com.bazaarvoice.scratch.dependencies;

import org.objectweb.asm.Type;

/**
 * Type-safe wrapper around a Java class name.
 */
public class ClassName implements Comparable<ClassName> {

    private final String _className;

    public ClassName(Type type) {
        this(type.getClassName());
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

    @Override
    public String toString() {
        return _className;
    }
}
