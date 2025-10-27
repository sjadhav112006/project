package com.filezipper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipService {

    private static final int BUFFER_SIZE = 4096;

    /**
     * Compresses a list of individual files into a single ZIP file.
     */
    public void compressFiles(List<File> files, File outputFile, Consumer<Double> progressUpdater, Consumer<String> logUpdater) throws IOException {
        long totalSize = files.stream().mapToLong(File::length).sum();
        long compressedSize = 0;

        try (FileOutputStream fos = new FileOutputStream(outputFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            logUpdater.accept("Starting compression to " + outputFile.getName() + "...");
            double fileCount = files.size();

            for (int i = 0; i < fileCount; i++) {
                File file = files.get(i);
                if (!file.exists()) {
                    logUpdater.accept("Skipping (not found): " + file.getName());
                    continue;
                }
                
                logUpdater.accept("Adding: " + file.getName());
                ZipEntry zipEntry = new ZipEntry(file.getName());
                zos.putNextEntry(zipEntry);

                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                }
                zos.closeEntry();
                compressedSize += zipEntry.getCompressedSize(); // Note: May not be accurate until entry is closed
                progressUpdater.accept((i + 1) / fileCount);
            }

            logUpdater.accept("Compression complete.");
            logUpdater.accept(String.format("Original size: %,d bytes", totalSize));
            logUpdater.accept(String.format("Compressed size: %,d bytes", outputFile.length()));

        } catch (IOException e) {
            logUpdater.accept("Error: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Compresses an entire folder (recursively) into a single ZIP file.
     */
    public void compressFolder(File sourceFolder, File outputFile, Consumer<Double> progressUpdater, Consumer<String> logUpdater) throws IOException {
        
        // 1. Get all file paths relative to the source folder
        List<Path> allPaths = Files.walk(sourceFolder.toPath())
                                   .filter(Files::isRegularFile)
                                   .collect(Collectors.toList());
        
        if (allPaths.isEmpty()) {
            logUpdater.accept("Folder is empty. No zip file created.");
            return;
        }

        double fileCount = allPaths.size();
        Path sourcePath = sourceFolder.toPath();
        long totalSize = 0;

        try (FileOutputStream fos = new FileOutputStream(outputFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            
            logUpdater.accept("Starting folder compression to " + outputFile.getName() + "...");

            for (int i = 0; i < fileCount; i++) {
                Path filePath = allPaths.get(i);
                File file = filePath.toFile();
                totalSize += file.length();

                // Create a relative path for the ZipEntry
                String entryName = sourcePath.relativize(filePath).toString();
                // Ensure cross-platform compatibility
                entryName = entryName.replace(File.separator, "/"); 
                
                logUpdater.accept("Adding: " + entryName);
                zos.putNextEntry(new ZipEntry(entryName));
                
                Files.copy(filePath, zos);
                zos.closeEntry();
                
                progressUpdater.accept((i + 1) / fileCount);
            }

            logUpdater.accept("Folder compression complete.");
            logUpdater.accept(String.format("Original size: %,d bytes", totalSize));
            logUpdater.accept(String.format("Compressed size: %,d bytes", outputFile.length()));

        } catch (IOException e) {
            logUpdater.accept("Error: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Extracts a ZIP file to a specified output directory.
     */
    public void extractZip(File zipFile, File outputDir, Consumer<Double> progressUpdater, Consumer<String> logUpdater) throws IOException {
        logUpdater.accept("Starting extraction of " + zipFile.getName() + "...");

        // Get total number of entries for progress bar
        int totalEntries = 0;
        try (ZipInputStream zisCount = new ZipInputStream(new FileInputStream(zipFile))) {
            while (zisCount.getNextEntry() != null) {
                totalEntries++;
            }
        }
        
        if (totalEntries == 0) {
            logUpdater.accept("ZIP file is empty.");
            return;
        }

        int extractedEntries = 0;

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry zipEntry = zis.getNextEntry();
            byte[] buffer = new byte[BUFFER_SIZE];

            while (zipEntry != null) {
                File newFile = newFile(outputDir, zipEntry);
                logUpdater.accept("Extracting: " + zipEntry.getName());

                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    // Fix for directories that aren't explicitly listed
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }
                    
                    // Write file content
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                
                zis.closeEntry();
                extractedEntries++;
                progressUpdater.accept((double) extractedEntries / totalEntries);
                zipEntry = zis.getNextEntry();
            }
            logUpdater.accept("Extraction complete.");

        } catch (IOException e) {
            logUpdater.accept("Error: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Helper method to prevent "Zip Slip" vulnerability.
     * Ensures that extracted files are created inside the target directory.
     */
    private File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());
        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: "n + zipEntry.getName());
        }
        return destFile;
    }
}
