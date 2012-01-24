package com.bazaarvoice.scratch.dependencies;

import org.jdom.Attribute;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;

public class TapestryExtractor extends AbstractXmlExtractor {

    private final ClassCollector _classes;

    public TapestryExtractor(ClassCollector classes) {
        _classes = classes;
    }

    @Override
    protected InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        if ("http://jakarta.apache.org/tapestry/dtd/Tapestry_3_0.dtd".equals(systemId)) {
            return Utils.newClassPathInputSource("org/apache/tapestry/parse/Tapestry_3_0.dtd", systemId);
        } else if ("http://jakarta.apache.org/tapestry/dtd/Script_3_0.dtd".equals(systemId)) {
            return Utils.newClassPathInputSource("org/apache/tapestry/script/Script_3_0.dtd", systemId);
        }
        return super.resolveEntity(publicId, systemId);
    }

    @Override
    protected void visitAttribute(Attribute attribute) {
        if ("class".equals(attribute.getName()) || "type".equals(attribute.getName())) {
            _classes.addClass(attribute.getValue());
        }
    }
}
