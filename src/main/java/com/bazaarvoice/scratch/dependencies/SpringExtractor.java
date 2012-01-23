package com.bazaarvoice.scratch.dependencies;

import com.google.common.base.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Attribute;
import org.jdom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpringExtractor extends AbstractXmlExtractor {

    private static final Pattern OGNL_CLASS = Pattern.compile("@(.*?)@");

    public SpringExtractor(Predicate<ClassName> packageFilter) {
        super(packageFilter);
    }

    @Override
    protected InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        if (systemId != null && systemId.startsWith("http://www.springframework.org/dtd/")) {
            String fileName = StringUtils.substringAfterLast(systemId, "/");
            return Utils.newClassPathInputSource("org/springframework/beans/factory/xml/" + fileName, systemId);
        }
        return super.resolveEntity(publicId, systemId);
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
        String name = attribute.getName();
        String value = attribute.getValue();
        if ("class".equals(name)) {
            addClass(value);
        } else if ("value".equals(name) && value.indexOf('@') != -1) {
            Matcher matcher = OGNL_CLASS.matcher(value);
            while (matcher.find()) {
                addClass(matcher.group(1));
            }
        }
    }
}
