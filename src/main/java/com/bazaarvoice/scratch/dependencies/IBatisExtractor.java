package com.bazaarvoice.scratch.dependencies;

import org.apache.commons.lang3.StringUtils;
import org.jdom.Attribute;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;

public class IBatisExtractor extends AbstractXmlExtractor {

    private final ClassCollector _classes;

    public IBatisExtractor(ClassCollector classes) {
        _classes = classes;
    }

    public static boolean handles(String fileName) {
        return fileName.endsWith(".ibatis.xml");
    }

    @Override
    protected InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        if (systemId != null && systemId.startsWith("http://ibatis.apache.org/dtd/")) {
            String fileName = StringUtils.substringAfterLast(systemId, "/");
            return Utils.newClassPathInputSource("com/ibatis/sqlmap/engine/builder/xml/" + fileName, systemId);
        }
        return super.resolveEntity(publicId, systemId);
    }

    @Override
    protected void visitAttribute(Attribute attribute) {
        String attributeName = attribute.getName();
        if ("parameterClass".equals(attributeName) || "resultClass".equals(attributeName)) {
            _classes.addClass(attribute.getValue());
        }
    }
}
