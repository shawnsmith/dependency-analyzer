package com.bazaarvoice.scratch.dependencies;

import com.google.common.base.Predicate;
import org.jdom.Attribute;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;

public class TapestryExtractor extends AbstractXmlExtractor {

    public TapestryExtractor(Predicate<ClassName> packageFilter) {
        super(packageFilter);
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
            addClass(attribute.getValue());
        }
    }
}
