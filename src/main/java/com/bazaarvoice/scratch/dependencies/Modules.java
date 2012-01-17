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
    private final Map<List<ModuleName>, Boolean> _dependencyCache = Maps.newHashMap();

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

    public boolean isDependentOf(ModuleName descendentName, ModuleName ancestorName) {
        Preconditions.checkNotNull(descendentName);
        Preconditions.checkNotNull(ancestorName);
        if (descendentName.equals(ancestorName)) {
            return true;
        }
        List<ModuleName> cacheKey = newPair(descendentName, ancestorName);
        Boolean result = _dependencyCache.get(cacheKey);
        if (result == null) {
            result = false;
            Module descendent = _moduleMap.get(descendentName);
            if (descendent != null) {
                for (ModuleName dependencyName : descendent.getDependencies()) {
                    if (isDependentOf(dependencyName, ancestorName)) {
                        result = true;
                        break;
                    }
                }
            }
            _dependencyCache.put(cacheKey, result);
        }
        return result;
    }

    private <T> List<T> newPair(T a, T b) {
        //noinspection unchecked
        return Arrays.asList(a, b);
    }
}
