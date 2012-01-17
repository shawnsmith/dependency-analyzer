package com.bazaarvoice.scratch.dependencies;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.apache.commons.io.filefilter.NameFileFilter;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Modules {

    private final Map<ModuleName, Module> _moduleMap = Maps.newHashMap();
    private final Map<List<ModuleName>, Boolean> _visibilityCache = Maps.newHashMap();

    public Collection<Module> getAllModules() {
        return Collections.unmodifiableCollection(_moduleMap.values());
    }

    public Module getModule(ModuleName moduleName) {
        return _moduleMap.get(moduleName);
    }

    public void scan(File root, String groupFilter) {
        // scan all the pom.xml files
        for (File pomFile : Utils.findFiles(root, new NameFileFilter("pom.xml"))) {
            Module module = Module.parseXml(pomFile);
            if (module.getName().isMemberOfGroup(groupFilter)) {
                _moduleMap.put(module.getName(), module);
            }
        }
    }

    public boolean isVisibleTo(ModuleName src, ModuleName dest) {
        Preconditions.checkNotNull(src);
        Preconditions.checkNotNull(dest);
        if (src.equals(dest)) {
            return true;
        }
        List<ModuleName> cacheKey = newPair(src, dest);
        Boolean result = _visibilityCache.get(cacheKey);
        if (result == null) {
            result = false;
            Module srcModule = _moduleMap.get(src);
            if (srcModule != null) {
                for (ModuleName dependencyName : srcModule.getDependencies()) {
                    if (isVisibleTo(dependencyName, dest)) {
                        result = true;
                        break;
                    }
                }
            }
            _visibilityCache.put(cacheKey, result);
        }
        return result;
    }

    private <T> List<T> newPair(T a, T b) {
        return Arrays.asList(a, b);
    }
}
