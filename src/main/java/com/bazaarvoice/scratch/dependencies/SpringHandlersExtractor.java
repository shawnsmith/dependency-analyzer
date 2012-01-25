package com.bazaarvoice.scratch.dependencies;

import com.google.common.io.Closeables;
import com.google.common.io.InputSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class SpringHandlersExtractor {

    private final ClassCollector _classes;

    public SpringHandlersExtractor(ClassCollector classes) {
        _classes = classes;
    }

    public static boolean handles(String fileName) {
        return "spring.handlers".equals(fileName);
    }

    public void visit(InputSupplier<? extends InputStream> inputSupplier) throws IOException {
        Properties props = new Properties();
        InputStream in = inputSupplier.getInput();
        try {
            props.load(in);
        } finally {
            Closeables.closeQuietly(in);
        }
        for (Object className : props.values()) {
            _classes.addClass((String) className);
        }
    }
}
