package com.bazaarvoice.scratch.dependencies;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;

public class PackagePredicate implements Predicate<ClassName> {

    private final String _packagePrefix;

    public PackagePredicate(String packagePrefix) {
        Preconditions.checkNotNull(packagePrefix);
        _packagePrefix = packagePrefix;
    }

    @Override
    public boolean apply(ClassName className) {
        return className.isMemberOfPackage(_packagePrefix);
    }
}
