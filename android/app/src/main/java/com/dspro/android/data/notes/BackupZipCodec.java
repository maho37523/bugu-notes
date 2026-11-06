package com.dspro.android.data.notes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Pure-JDK bounded ZIP layer shared by the Android backup service and JVM regression checks. */
public final class BackupZipCodec {
    public static final int MAX_ENTRIES = 10_000;
    public static final int MAX_ENTRY_BYTES = 25 * 1024 * 1024;
    public static final long MAX_PAYLOAD_BYTES = 125L * 1024 * 1024;
    private BackupZipCodec() {}

    public static Map<String, byte[]> read(InputStream input) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        long total = 0;
        try (ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (entries.size() >= MAX_ENTRIES + 1) throw new IOException("Too many archive entries");
                if (entry.isDirectory() || !validName(name)) throw new IOException("Unsafe or unexpected archive path");
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int size = 0, n;
                while ((n = zip.read(buffer)) != -1) {
                    size += n; total += n;
                    if (size > MAX_ENTRY_BYTES || total > MAX_PAYLOAD_BYTES) throw new IOException("Archive is too large");
                    out.write(buffer, 0, n);
                }
                if (entries.putIfAbsent(name, out.toByteArray()) != null) throw new IOException("Duplicate archive entry");
                zip.closeEntry();
            }
        }
        if (!entries.containsKey("manifest.json")) throw new IOException("Backup manifest missing");
        return entries;
    }

    public static void write(OutputStream output, byte[] manifest, Map<String, File> files) throws IOException {
        if (manifest.length > MAX_ENTRY_BYTES || files.size() > MAX_ENTRIES) throw new IOException("Backup is too large");
        long total = manifest.length;
        for (File file : files.values()) {
            if (file.length() > MAX_ENTRY_BYTES || (total += file.length()) > MAX_PAYLOAD_BYTES) throw new IOException("Backup is too large");
        }
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("manifest.json")); zip.write(manifest); zip.closeEntry();
            byte[] buffer = new byte[8192];
            for (Map.Entry<String, File> item : files.entrySet()) {
                if (!validName(item.getKey()) || "manifest.json".equals(item.getKey())) throw new IOException("Unsafe archive path");
                zip.putNextEntry(new ZipEntry(item.getKey()));
                try (InputStream in = new FileInputStream(item.getValue())) {
                    int n; while ((n = in.read(buffer)) != -1) zip.write(buffer, 0, n);
                }
                zip.closeEntry();
            }
        }
    }

    public static boolean validName(String name) {
        if (name == null || name.startsWith("/") || name.indexOf('\\') >= 0) return false;
        for (String segment : name.split("/", -1)) if (segment.isEmpty() || "..".equals(segment) || ".".equals(segment)) return false;
        return "manifest.json".equals(name) || name.matches("files/f[0-9]{6}\\.bin");
    }
}
