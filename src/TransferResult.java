import java.nio.file.Path;

public class TransferResult {
    private final Path outputPath;
    private final String message;

    public TransferResult(Path outputPath, String message) {
        this.outputPath = outputPath;
        this.message = message;
    }

    public Path getOutputPath() {
        return outputPath;
    }

    public String getMessage() {
        return message;
    }
}
