package com.bazaarvoice.scratch.dependencies;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class ClassDependencies {

    private final SetMultimap<ClassName, ClassName> _classToReferencedClassesMap = HashMultimap.create();

    public Set<ClassName> getAllClasses() {
        return Collections.unmodifiableSet(_classToReferencedClassesMap.keySet());
    }

    public Set<ClassName> getReferencedClasses(ClassName className) {
        return Collections.unmodifiableSet(_classToReferencedClassesMap.get(className));
    }

    public void add(ClassName className, Collection<ClassName> referencedClasses) {
        _classToReferencedClassesMap.putAll(className, referencedClasses);
        _classToReferencedClassesMap.remove(className, className);
    }

    public Set<ClassName> getTransitiveClosure(Collection<ClassName> classNames) {
        Set<ClassName> transitiveClosure = Sets.newHashSet();
        for (ClassName className : classNames) {
            collectTransitiveClosure(className, transitiveClosure);
        }
        return transitiveClosure;
    }

    private void collectTransitiveClosure(ClassName className, Set<ClassName> transitiveClosure) {
        if (transitiveClosure.add(className)) {
            for (ClassName referencedClass : _classToReferencedClassesMap.get(className)) {
                collectTransitiveClosure(referencedClass, transitiveClosure);
            }
        }
    }
}
