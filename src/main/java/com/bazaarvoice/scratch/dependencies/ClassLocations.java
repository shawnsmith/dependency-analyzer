package com.bazaarvoice.scratch.dependencies;

import com.google.common.base.Charsets;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class ClassLocations {

    private final Map<ClassName, ModuleName> _classToModuleMap = Maps.newHashMap();
    private final SetMultimap<ModuleName, ClassName> _moduleToClassMap = HashMultimap.create();

    public static ClassLocations parseFile(File file, String defaultGroupId) throws IOException {
        ClassLocations locations = new ClassLocations();
        ModuleName moduleName = null;
        for (String line : Files.readLines(file, Charsets.UTF_8)) {
            String string = line.trim();
            if (string.isEmpty() || string.startsWith("#")) {
                continue;
            }
            if (!Character.isWhitespace(line.charAt(0))) {
                moduleName = ModuleName.parseDescriptor(string, defaultGroupId);
            } else {
                if (moduleName == null) {
                    throw new IllegalStateException("Class must be preceeded with a maven artifact name: " + string);
                }
                ClassName className = new ClassName(string).getOuterClassName();
                locations.add(className, moduleName);
            }
        }
        return locations;
    }

    public ClassLocations() {
    }

    public Set<ClassName> getAllClasses() {
        return Collections.unmodifiableSet(_classToModuleMap.keySet());
    }

    public ModuleName getModule(ClassName className) {
        return _classToModuleMap.get(className);
    }

    public Set<ClassName> getClasses(ModuleName moduleName) {
        return Collections.unmodifiableSet(_moduleToClassMap.get(moduleName));
    }

    public Set<Map.Entry<ClassName, ModuleName>> getLocations() {
        return Collections.unmodifiableSet(_classToModuleMap.entrySet());
    }

    public void add(ClassName className, ModuleName moduleName) {
        ModuleName previous = _classToModuleMap.put(className, moduleName);
        if (previous != moduleName) {
            if (previous != null) {
                System.err.println(" warning, " + className + " is in both " + previous + " and " + moduleName);
                _moduleToClassMap.remove(moduleName, className);
            }
            _moduleToClassMap.put(moduleName, className);
        }
    }

    public void move(ClassName className, ModuleName moduleName) {
        ModuleName previous = _classToModuleMap.put(className, moduleName);
        if (previous != moduleName) {
            if (previous != null) {
                _moduleToClassMap.remove(previous, className);
            }
            _moduleToClassMap.put(moduleName, className);
        }
    }

    public void moveAll(ClassLocations moves) {
        for (Map.Entry<ClassName, ModuleName> entry : moves.getLocations()) {
            move(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Returns all classes in this object that don't have identical locations in the specified locations object.
     */
    public ClassLocations difference(ClassLocations locations) {
        ClassLocations diff = new ClassLocations();
        for (Map.Entry<ClassName, ModuleName> entry : _classToModuleMap.entrySet()) {
            ClassName className = entry.getKey();
            ModuleName moduleName = entry.getValue();
            if (!moduleName.equals(locations.getModule(className))) {
                diff.add(className, moduleName);
            }
        }
        return diff;
    }

    public void writeTo(File file, String defaultGroupId) throws IOException {
        PrintWriter out = new PrintWriter(Files.newWriter(file, Charsets.UTF_8));
        try {
            writeTo(out, defaultGroupId);
        } finally {
            out.close();
        }
    }

    public void writeTo(PrintWriter out, String defaultGroupId) {
        for (ModuleName moduleName : Utils.sorted(_moduleToClassMap.keySet())) {
            out.println(moduleName.toString(defaultGroupId));
            for (ClassName className : Utils.sorted(_moduleToClassMap.get(moduleName))) {
                out.println("    " + className);
            }
        }
    }
}
