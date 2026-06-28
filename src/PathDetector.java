import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PathDetector {
    private static final String MAC_SAVE_FOLDER = "Library/Mobile Documents/iCloud~com~505games~deathstranding";

    public Optional<Path> detectSaveFolder(SavePlatform platform) {
        for (Path candidate : getCandidates(platform)) {
            if (Files.isDirectory(candidate)) {
                return Optional.of(candidate);
            }
        }

        return Optional.empty();
    }

    private List<Path> getCandidates(SavePlatform platform) {
        String home = System.getProperty("user.home");
        List<Path> candidates = new ArrayList<>();

        if (platform == SavePlatform.MAC) {
            candidates.add(Paths.get(home, MAC_SAVE_FOLDER));
        } else {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null) {
                candidates.addAll(findProfileFolders(Paths.get(localAppData, "KojimaProductions", "DeathStrandingDC")));
                candidates.addAll(findProfileFolders(Paths.get(localAppData, "KojimaProductions", "DeathStranding")));
                candidates.add(Paths.get(localAppData, "KojimaProductions", "DeathStrandingDC"));
                candidates.add(Paths.get(localAppData, "KojimaProductions", "DeathStranding"));
            }

            candidates.addAll(findProfileFolders(Paths.get(home, "AppData", "Local", "KojimaProductions", "DeathStrandingDC")));
            candidates.addAll(findProfileFolders(Paths.get(home, "AppData", "Local", "KojimaProductions", "DeathStranding")));
            candidates.add(Paths.get(home, "AppData", "Local", "KojimaProductions", "DeathStrandingDC"));
            candidates.add(Paths.get(home, "AppData", "Local", "KojimaProductions", "DeathStranding"));
        }

        return candidates;
    }

    private List<Path> findProfileFolders(Path baseFolder) {
        List<Path> folders = new ArrayList<>();

        if (!Files.isDirectory(baseFolder)) {
            return folders;
        }

        try {
            Files.list(baseFolder)
                    .filter(Files::isDirectory)
                    .forEach(folders::add);
        } catch (Exception ignored) {
            // Auto-detection is best effort; the UI will ask the user to browse if this fails.
        }

        return folders;
    }
}
