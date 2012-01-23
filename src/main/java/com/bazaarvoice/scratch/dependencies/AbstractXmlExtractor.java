package com.bazaarvoice.scratch.dependencies;

import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import com.google.common.io.InputSupplier;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

public abstract class AbstractXmlExtractor {

    private final Predicate<ClassName> _packageFilter;
    private final Set<ClassName> _classNames = Sets.newHashSet();

    protected AbstractXmlExtractor(Predicate<ClassName> packageFilter) {
        _packageFilter = packageFilter;
    }

    public Set<ClassName> getClassNames() {
        return _classNames;
    }

    public AbstractXmlExtractor visit(InputSupplier<? extends InputStream> inputSupplier, String systemId) {
        visitDocument(parseXmlFile(inputSupplier, systemId));
        return this;
    }

    private Document parseXmlFile(InputSupplier<? extends InputStream> inputSupplier, String systemId) {
        SAXBuilder saxBuilder = new SAXBuilder();
        // set an explicit entity resolver that should prevent the xml parser from making http requests to download DTD
        saxBuilder.setEntityResolver(new EntityResolver() {
            @Override
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                return AbstractXmlExtractor.this.resolveEntity(publicId, systemId);
            }
        });
        try {
            return saxBuilder.build(inputSupplier.getInput(), systemId);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    protected InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        throw new UnsupportedOperationException(systemId);  // don't want the default behavior of making an http request to the system id
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
        List<Element> children = (List<Element>) element.getChildren();
        for (Element child : children) {
            visitElement(child);
        }
    }

    protected void addClass(String string) {
        ClassName className = new ClassName(string).getOuterClassName();
        if (_packageFilter.apply(className)) {
            _classNames.add(className);
        }
    }

    protected void addFile(String string) {
        _classNames.add(new ClassName(string));
    }
}
