package com.bazaarvoice.scratch.dependencies;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import com.google.common.io.InputSupplier;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

public class SpringExtractor {

    private final String _packageFilter;
    private final Set<ClassName> _classNames = Sets.newHashSet();

    public SpringExtractor(String packageFilter) {
        _packageFilter = packageFilter;
    }

    public Set<ClassName> getClassNames() {
        return _classNames;
    }

    public SpringExtractor visit(InputSupplier<? extends InputStream> inputSupplier, String systemId) {
        Document xmlDocument;
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            xmlDocument = saxBuilder.build(inputSupplier.getInput(), systemId);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        visit(xmlDocument.getRootElement());
        return this;
    }

    private void visit(Element element) {
        if ("import".equals(element.getName())) {
            Attribute resource = element.getAttribute("resource");
            if (resource != null && resource.getValue().startsWith("classpath:")) {
                _classNames.add(new ClassName(StringUtils.removeStart(resource.getValue(), "classpath:")));
            }
        }

        //noinspection unchecked
        for (Attribute attribute : (List<Attribute>) element.getAttributes()) {
            if ("class".equals(attribute.getName())) {
                ClassName className = new ClassName(attribute.getValue()).getOuterClassName();
                if (className.isMemberOfPackage(_packageFilter)) {
                    _classNames.add(className);
                }
            }
        }

        //noinspection unchecked
        for (Element child : (List<Element>) element.getChildren()) {
            visit(child);
        }
    }

}
