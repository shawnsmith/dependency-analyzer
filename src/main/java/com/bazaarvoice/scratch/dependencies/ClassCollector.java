package com.bazaarvoice.scratch.dependencies;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import org.objectweb.asm.Type;

import java.util.Set;

public class ClassCollector {

    private final Predicate<ClassName> _packageFilter;
    private final Set<ClassName> _classNames = Sets.newHashSet();

    protected ClassCollector(Predicate<ClassName> packageFilter) {
        _packageFilter = packageFilter;
    }

    public Set<ClassName> getClassNames() {
        return _classNames;
    }

    public void addClass(String string) {
        ClassName className = new ClassName(string).getOuterClassName();
        if (_packageFilter.apply(className)) {
            _classNames.add(className);
        }
    }

    public void addType(Type type) {
        switch (type.getSort()) {
            case Type.OBJECT:
                ClassName className = new ClassName(type).getOuterClassName();
                if (_packageFilter.apply(className)) {
                    _classNames.add(className);
                }
                break;
            case Type.ARRAY:
                addType(type.getElementType());
                break;
            case Type.METHOD:
                addType(type.getReturnType());
                for (Type argumentType : type.getArgumentTypes()) {
                    addType(argumentType);
                }
                break;
        }
    }

    public void addFile(String string) {
        _classNames.add(new ClassName(string));
    }

    @Override
    public String toString() {
        return Utils.sorted(_classNames).toString();
    }
}
