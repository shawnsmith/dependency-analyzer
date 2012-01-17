package com.bazaarvoice.scratch.dependencies;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.jdom.Element;
import org.jdom.Namespace;

public class ModuleName implements Comparable<ModuleName> {

    private final String _groupId;
    private final String _artifactId;

    public static ModuleName parseDescriptor(String descriptor) {
        String[] fields = descriptor.split(":", 3);  // ignore version #s after the 2nd :
        if (fields.length < 2) {
            throw new IllegalArgumentException("Illegal maven module descriptor: " + descriptor);
        }
        return new ModuleName(fields[0], fields[1]);
    }

    public static ModuleName parseDescriptor(String descriptor, String defaultGroupId) {
        if (descriptor.indexOf(':') == -1 && defaultGroupId != null) {
            descriptor = defaultGroupId + ":" + descriptor;
        }
        return parseDescriptor(descriptor);
    }

    public ModuleName(String groupId, String artifactId) {
        Preconditions.checkArgument(groupId != null && !groupId.isEmpty());
        Preconditions.checkArgument(artifactId != null && !artifactId.isEmpty());
        _groupId = groupId;
        _artifactId = artifactId;
    }

    public ModuleName(Element xmlElement) {
        Namespace ns = xmlElement.getNamespace();
        _groupId = xmlElement.getChildTextTrim("groupId", ns);
        _artifactId = xmlElement.getChildTextTrim("artifactId", ns);
    }

    public String getGroupId() {
        return _groupId;
    }

    public String getArtifactId() {
        return _artifactId;
    }

    public boolean isMemberOfGroup(String groupPrefix) {
        return Utils.hasPrefix(_groupId, groupPrefix, '.');
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ModuleName)) {
            return false;
        }
        ModuleName name = (ModuleName) o;
        return _groupId.equals(name._groupId) && _artifactId.equals(name._artifactId);

    }

    @Override
    public int compareTo(ModuleName o) {
        return new CompareToBuilder().append(_groupId, o._groupId).append(_artifactId, o._artifactId).toComparison();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(_groupId, _artifactId);
    }

    @Override
    public String toString() {
        return _groupId + ":" + _artifactId;
    }

    public String toString(String defaultGroupId) {
        return _groupId.equals(defaultGroupId) ? _artifactId : toString();
    }
}
