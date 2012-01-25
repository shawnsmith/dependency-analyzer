package com.bazaarvoice.scratch.dependencies;

import org.jdom.Element;

public class ServletExtractor extends AbstractXmlExtractor {

    private final ClassCollector _classes;

    public ServletExtractor(ClassCollector classes) {
        _classes = classes;
    }

    public static boolean handles(String fileName) {
        return "web.xml".equals(fileName);
    }

    @Override
    protected void visitElement(Element element) {
        if (element.getName().endsWith("-class")) {
            _classes.addClass(element.getTextTrim());
        } else {
            super.visitElement(element);
        }
    }
}
