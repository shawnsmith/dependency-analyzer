package com.bazaarvoice.scratch.dependencies;

import org.apache.commons.lang3.StringUtils;
import org.jdom.Attribute;
import org.jdom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;

public class HibernateExtractor extends AbstractXmlExtractor {

    private final ClassCollector _classes;

    public HibernateExtractor(ClassCollector classes) {
        _classes = classes;
    }

    @Override
    protected InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        if (systemId != null && systemId.startsWith("http://hibernate.sourceforge.net/")) {
            String fileName = StringUtils.substringAfterLast(systemId, "/");
            return Utils.newClassPathInputSource("org/hibernate/" + fileName, systemId);
        }
        return super.resolveEntity(publicId, systemId);
    }

    @Override
    protected void visitElement(Element element) {
        if ("class".equals(element.getName())) {
            _classes.addClass(element.getAttributeValue("name"));
        }
        super.visitElement(element);
    }

    @Override
    protected void visitAttribute(Attribute attribute) {
        if ("class".equals(attribute.getName()) || "type".equals(attribute.getName())) {
            _classes.addClass(attribute.getValue());
        }
    }
}
