package com.bazaarvoice.scratch.dependencies;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

import java.io.File;
import java.util.List;
import java.util.Set;

public class Module {

    private final File _directory;
    private final ModuleName _name;
    private final ModuleName _parent;
    private final Set<ModuleName> _dependencies = Sets.newHashSet();

    public static Module parseXml(File pomFile) {
        File directory = pomFile.getParentFile();

        Document xmlDocument;
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            xmlDocument = saxBuilder.build(pomFile);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

        Element xmlProject = xmlDocument.getRootElement();
        Namespace ns = xmlProject.getNamespace();

        ModuleName name = new ModuleName(xmlProject);

        Element xmlParent = xmlProject.getChild("parent", ns);
        ModuleName parent = xmlParent != null ? new ModuleName(xmlParent) : null;

        Module module = new Module(directory, name, parent);

        Element xmlDependencies = xmlProject.getChild("dependencies", ns);
        if (xmlDependencies != null) {
            //noinspection unchecked
            for (Element xmlDependency  : (List<Element>) xmlDependencies.getChildren("dependency", ns)) {
                if (!"test".equals(xmlDependency.getChildTextTrim("scope", ns))) {
                    module.getDependencies().add(new ModuleName(xmlDependency));
                }
            }
        }

        return module;
    }

    public Module(File directory, ModuleName name, ModuleName parent) {
        _directory = directory;
        _name = name;
        _parent = parent;
    }

    public File getDirectory() {
        return _directory;
    }

    public ModuleName getName() {
        return _name;
    }

    public ModuleName getParent() {
        return _parent;
    }

    public Set<ModuleName> getDependencies() {
        return _dependencies;
    }

    @Override
    public String toString() {
        return _name.toString();
    }
}
