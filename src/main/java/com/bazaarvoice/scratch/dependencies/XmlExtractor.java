package com.bazaarvoice.scratch.dependencies;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import com.google.common.io.InputSupplier;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

public abstract class XmlExtractor {

    private final String _packageFilter;
    private final Set<ClassName> _classNames = Sets.newHashSet();

    protected XmlExtractor(String packageFilter) {
        _packageFilter = packageFilter;
    }

    public Set<ClassName> getClassNames() {
        return _classNames;
    }

    public XmlExtractor visit(InputSupplier<? extends InputStream> inputSupplier, String systemId) {
        visitDocument(parseXmlFile(inputSupplier, systemId));
        return this;
    }

    private Document parseXmlFile(InputSupplier<? extends InputStream> inputSupplier, String systemId) {
        SAXBuilder saxBuilder = new SAXBuilder();
        try {
            return saxBuilder.build(inputSupplier.getInput(), systemId);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    protected void visitDocument(Document xmlDocument) {
        visitElement(xmlDocument.getRootElement());
    }

    protected void visitElement(Element element) {
        visitAttributes(element);
        visitChildElements(element);
    }

    protected void visitAttributes(Element element) {
        //noinspection unchecked
        for (Attribute attribute : (List<Attribute>) element.getAttributes()) {
            visitAttribute(attribute);
        }
    }

    protected void visitAttribute(Attribute attribute) {
        // subclasses may override
    }

    protected void visitChildElements(Element element) {
        //noinspection unchecked
        List<Element> elements = (List<Element>) element.getChildren();
        for (Element child : elements) {
            visitElement(child);
        }
    }

    protected void addClass(String string) {
        ClassName className = new ClassName(string).getOuterClassName();
        if (className.isMemberOfPackage(_packageFilter)) {
            _classNames.add(className);
        }
    }

    protected void addFile(String string) {
        _classNames.add(new ClassName(string));
    }
}
