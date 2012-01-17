package com.bazaarvoice.scratch.dependencies;

import com.google.common.io.InputSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipInputSupplier implements InputSupplier<InputStream> {
    private final ZipFile _file;
    private final ZipEntry _entry;

    ZipInputSupplier(ZipFile file, ZipEntry entry) {
        _file = file;
        _entry = entry;
    }

    @Override
    public InputStream getInput() throws IOException {
        return _file.getInputStream(_entry);
    }
}
