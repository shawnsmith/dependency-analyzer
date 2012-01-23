package com.bazaarvoice.scratch.dependencies;

import org.jdom.Attribute;

public class TapestryExtractor extends XmlExtractor {

    public TapestryExtractor(String packageFilter) {
        super(packageFilter);
    }

    @Override
    protected void visitAttribute(Attribute attribute) {
        if ("class".equals(attribute.getName()) || "type".equals(attribute.getName())) {
            addClass(attribute.getValue());
        }
    }
}
