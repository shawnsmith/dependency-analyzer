package com.bazaarvoice.scratch.dependencies;

import org.apache.commons.lang3.StringUtils;
import org.jdom.Attribute;
import org.jdom.Element;

public class SpringExtractor extends XmlExtractor {

    public SpringExtractor(String packageFilter) {
        super(packageFilter);
    }

    @Override
    protected void visitElement(Element element) {
        if ("import".equals(element.getName())) {
            Attribute resource = element.getAttribute("resource");
            if (resource != null && resource.getValue().startsWith("classpath:")) {
                addFile(StringUtils.removeStart(resource.getValue(), "classpath:"));
            }
        }
        super.visitElement(element);
    }

    @Override
    protected void visitAttribute(Attribute attribute) {
        if ("class".equals(attribute.getName())) {
            addClass(attribute.getValue());
        }
    }
}
