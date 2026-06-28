import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

public class TransferService {
    private static final DateTimeFormatter BACKUP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public TransferResult exportSave(Path source, SavePlatform targetPlatform, Path outputFolder) throws IOException {
        SaveFile saveFile = new SaveFile(source);
        validateSupported(saveFile);
        Files.createDirectories(outputFolder);

        if (targetPlatform == SavePlatform.WINDOWS) {
            return exportWindows(saveFile, outputFolder);
        }

        return exportMac(saveFile, outputFolder);
    }

    public TransferResult importSave(Path source, SavePlatform targetPlatform, Path destinationFolder) throws IOException {
        SaveFile saveFile = new SaveFile(source);
        validateSupported(saveFile);
        Files.createDirectories(destinationFolder);

        if (targetPlatform == SavePlatform.WINDOWS) {
            return importWindows(saveFile, destinationFolder);
        }

        return importMac(saveFile, destinationFolder);
    }

    private TransferResult exportWindows(SaveFile saveFile, Path outputFolder) throws IOException {
        if (saveFile.isWindowsDat()) {
            Path output = outputFolder.resolve(saveFile.getWindowsFileName());
            copyWithBackup(saveFile.getPath(), output);
            return new TransferResult(output, "Exported Windows save.");
        }

        Path dataFile = saveFile.getMacBundleDataPath();
        if (!Files.isRegularFile(dataFile)) {
            throw new IOException("Mac bundle does not contain a data file: " + dataFile);
        }

        Path output = outputFolder.resolve(saveFile.getWindowsFileName());
        copyWithBackup(dataFile, output);
        return new TransferResult(output, "Converted Mac bundle to Windows .dat.");
    }

    private TransferResult exportMac(SaveFile saveFile, Path outputFolder) throws IOException {
        if (saveFile.isMacBundle()) {
            Path output = outputFolder.resolve(saveFile.getMacBundleName());
            copyDirectoryWithBackup(saveFile.getPath(), output);
            return new TransferResult(output, "Exported Mac bundle.");
        }

        Path output = outputFolder.resolve(saveFile.getMacBundleName());
        createMacBundleFromDat(saveFile.getPath(), output);
        return new TransferResult(output, "Converted Windows .dat to Mac bundle.");
    }

    private TransferResult importWindows(SaveFile saveFile, Path destinationFolder) throws IOException {
        Path output = destinationFolder.resolve(saveFile.getWindowsFileName());

        if (saveFile.isWindowsDat()) {
            copyWithBackup(saveFile.getPath(), output);
            return new TransferResult(output, "Imported Windows .dat save.");
        }

        Path dataFile = saveFile.getMacBundleDataPath();
        if (!Files.isRegularFile(dataFile)) {
            throw new IOException("Mac bundle does not contain a data file: " + dataFile);
        }

        copyWithBackup(dataFile, output);
        return new TransferResult(output, "Converted Mac bundle and imported Windows .dat save.");
    }

    private TransferResult importMac(SaveFile saveFile, Path destinationFolder) throws IOException {
        Path output = destinationFolder.resolve(saveFile.getMacBundleName());

        if (saveFile.isMacBundle()) {
            copyDirectoryWithBackup(saveFile.getPath(), output);
            return new TransferResult(output, "Imported Mac bundle.");
        }

        createMacBundleFromDat(saveFile.getPath(), output);
        return new TransferResult(output, "Converted Windows .dat and imported Mac bundle.");
    }

    private void createMacBundleFromDat(Path datFile, Path bundlePath) throws IOException {
        backupIfExists(bundlePath);
        Files.createDirectories(bundlePath);
        Files.copy(datFile, bundlePath.resolve(SaveFile.MAC_BUNDLE_DATA_FILE), StandardCopyOption.REPLACE_EXISTING);
        Path metadata = bundlePath.resolve("metadata");
        if (!Files.exists(metadata)) {
            Files.createFile(metadata);
        }
    }

    private void copyWithBackup(Path source, Path target) throws IOException {
        backupIfExists(target);
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private void copyDirectoryWithBackup(Path source, Path target) throws IOException {
        backupIfExists(target);
        copyDirectory(source, target);
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        try (Stream<Path> paths = Files.walk(source)) {
            paths.forEach(path -> {
                try {
                    Path relativePath = source.relativize(path);
                    Path destination = target.resolve(relativePath);

                    if (Files.isDirectory(path)) {
                        Files.createDirectories(destination);
                    } else {
                        Files.createDirectories(destination.getParent());
                        Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new FileTransferException(e);
                }
            });
        } catch (FileTransferException e) {
            throw e.getCause();
        }
    }

    private void backupIfExists(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }

        Path backupPath = path.resolveSibling(path.getFileName() + ".backup-" + BACKUP_FORMAT.format(LocalDateTime.now()));
        if (Files.isDirectory(path)) {
            copyDirectory(path, backupPath);
        } else {
            Files.copy(path, backupPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void validateSupported(SaveFile saveFile) throws IOException {
        if (!saveFile.isSupported()) {
            throw new IOException("Unsupported save file: " + saveFile.getPath());
        }
    }

    private static class FileTransferException extends RuntimeException {
        FileTransferException(IOException cause) {
            super(cause);
        }

        @Override
        public synchronized IOException getCause() {
            return (IOException) super.getCause();
        }
    }
}
