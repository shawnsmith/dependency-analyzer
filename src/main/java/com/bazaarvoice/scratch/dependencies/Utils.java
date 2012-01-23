package com.bazaarvoice.scratch.dependencies;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.InputSource;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Utils {

    private static final List<String> IGNORE_DIRS = Arrays.asList(".svn", "target", "build", "dist");

    public static <T extends Comparable<? super T>> List<T> sorted(Collection<T> col) {
        List<T> list = Lists.newArrayList(col);
        Collections.sort(list);
        return list;
    }

    public static boolean hasPrefix(String string, String prefix, char fieldSeparator) {
        return string.startsWith(prefix) &&
                (prefix.isEmpty() || string.length() == prefix.length() || string.charAt(prefix.length()) == fieldSeparator);
    }

    public static String getFileName(String path) {
        return new File(path).getName();
    }

    public static String getRelativePath(File file, File rootDirectory) {
        String rootPath = rootDirectory.getPath();
        String filePath = file.getPath();
        Preconditions.checkArgument(filePath.startsWith(rootPath));
        return StringUtils.removeStart(filePath.substring(rootPath.length()), File.separator);
    }

    public static List<File> findFiles(File root, FileFilter filter) {
        final List<File> files = Lists.newArrayList();
        walkDirectoryHelper(root, filter, new Function<File, Void>() {
            @Override
            public Void apply(File file) {
                files.add(file);
                return null;
            }
        });
        Collections.sort(files);
        return files;
    }

    interface FileSink {
        void accept(String filePath, InputSupplier<? extends InputStream> inputSupplier) throws IOException;
    }

    public static void walkDirectory(final File root, final FileSink sink) {
        try {
            if (root.isDirectory()) {
                // directory of class files
                walkDirectoryHelper(root, null, new Function<File, Void>() {
                    @Override
                    public Void apply(File file) {
                        String name = getRelativePath(file, root);
                        try {
                            sink.accept(name, Files.newInputStreamSupplier(file));
                            return null;
                        } catch (IOException e) {
                            throw Throwables.propagate(e);
                        }
                    }
                });
            } else if (root.isFile() && "jar".equals(Files.getFileExtension(root.getName()))) {
                // jar of class files
                ZipFile zipFile = new ZipFile(root);
                try {
                    for (Enumeration<? extends ZipEntry> e = zipFile.entries(); e.hasMoreElements();) {
                        ZipEntry zipEntry = e.nextElement();
                        sink.accept(zipEntry.getName(), new ZipInputSupplier(zipFile, zipEntry));
                    }
                } finally {
                    zipFile.close();
                }
            }
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private static void walkDirectoryHelper(File dir, @Nullable FileFilter filter, Function<File, Void> sink) {
        if (!IGNORE_DIRS.contains(dir.getName())) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!file.isHidden()) {
                        if (file.isDirectory()) {
                            walkDirectoryHelper(file, filter, sink);
                        } else if (filter == null || filter.accept(file)) {
                            sink.apply(file);
                        }
                    }
                }
            }
        }
    }

    public static InputSource newClassPathInputSource(String resource, String systemId) {
        Preconditions.checkNotNull(resource);
        Preconditions.checkNotNull(systemId);
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        if (in == null) {
            throw new IllegalArgumentException("Unknown resource for " + systemId + ": " + resource);
        }
        InputSource inputSource = new InputSource(in);
        inputSource.setSystemId(systemId);
        return inputSource;
    }
}
