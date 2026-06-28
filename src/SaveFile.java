import java.nio.file.Files;
import java.nio.file.Path;

public class SaveFile {
    public static final String MAC_BUNDLE_EXTENSION = ".bundle";
    public static final String WINDOWS_SAVE_EXTENSION = ".dat";
    public static final String MAC_BUNDLE_DATA_FILE = "data";

    private final Path path;

    public SaveFile(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

    public String getFileName() {
        return path.getFileName().toString();
    }

    public SaveFormat getFormat() {
        if (isMacBundle()) {
            return SaveFormat.MAC_BUNDLE;
        }

        if (isWindowsDat()) {
            return SaveFormat.WINDOWS_DAT;
        }

        return SaveFormat.UNKNOWN;
    }

    public SaveType getSaveType() {
        return SaveType.fromName(getFileName());
    }

    public boolean isSupported() {
        return getFormat() != SaveFormat.UNKNOWN;
    }

    public boolean isMacBundle() {
        return Files.isDirectory(path) && getFileName().toLowerCase().endsWith(MAC_BUNDLE_EXTENSION);
    }

    public boolean isWindowsDat() {
        return Files.isRegularFile(path) && getFileName().toLowerCase().endsWith(WINDOWS_SAVE_EXTENSION);
    }

    public Path getMacBundleDataPath() {
        return path.resolve(MAC_BUNDLE_DATA_FILE);
    }

    public String getWindowsFileName() {
        if (isWindowsDat()) {
            return getFileName();
        }

        if (isMacBundle()) {
            String name = getFileName();
            if (name.toLowerCase().endsWith(MAC_BUNDLE_EXTENSION)) {
                return name.substring(0, name.length() - MAC_BUNDLE_EXTENSION.length());
            }
        }

        return "checkpoint.dat";
    }

    public String getMacBundleName() {
        if (isMacBundle()) {
            return getFileName();
        }

        return getFileName() + MAC_BUNDLE_EXTENSION;
    }

    public String describe() {
        return getFileName() + " (" + getFormat().getLabel() + ", " + getSaveType().getLabel() + ")";
    }
}
