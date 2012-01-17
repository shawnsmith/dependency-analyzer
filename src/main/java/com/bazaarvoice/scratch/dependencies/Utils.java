package com.bazaarvoice.scratch.dependencies;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import org.apache.commons.io.filefilter.SuffixFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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

    public static <T> List<T> sorted(Collection<T> col, Comparator<T> comparator) {
        List<T> list = Lists.newArrayList(col);
        Collections.sort(list, comparator);
        return list;
    }

    public static boolean hasPrefix(String string, String prefix, char fieldSeparator) {
        return string.startsWith(prefix) &&
                (prefix.isEmpty() || string.length() == prefix.length() || string.charAt(prefix.length()) == fieldSeparator);
    }

    public static List<File> findFiles(File root, FileFilter filter) {
        final List<File> files = Lists.newArrayList();
        findFiles(root, filter, new Function<File, Void>() {
            @Override
            public Void apply(File file) {
                files.add(file);
                return null;
            }
        });
        Collections.sort(files);
        return files;
    }

    public static void findFiles(File dir, FileFilter filter, Function<File, Void> sink) {
        if (!IGNORE_DIRS.contains(dir.getName())) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!file.isHidden()) {
                        if (file.isDirectory()) {
                            findFiles(file, filter, sink);
                        } else if (filter.accept(file)) {
                            sink.apply(file);
                        }
                    }
                }
            }
        }
    }

    public static void findClassFiles(File root, final Function<byte[], Void> sink) {
        try {
            if (root.isDirectory()) {
                // directory of class files
                findFiles(root, new SuffixFileFilter(".class"), new Function<File, Void>() {
                    @Override
                    public Void apply(File file) {
                        try {
                            sink.apply(Files.toByteArray(file));
                            return null;
                        } catch (IOException e) {
                            throw Throwables.propagate(e);
                        }
                    }
                });
            } else {
                // jar of class files
                ZipFile zipFile = new ZipFile(root);
                try {
                    for (Enumeration<? extends ZipEntry> e = zipFile.entries(); e.hasMoreElements();) {
                        ZipEntry zipEntry = e.nextElement();
                        if ("class".equals(Files.getFileExtension(zipEntry.getName()))) {
                            sink.apply(ByteStreams.toByteArray(new ZipInputSupplier(zipFile, zipEntry)));
                        }
                    }
                } finally {
                    zipFile.close();
                }
            }
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}
