public enum SaveType {
    MANUAL("Manual save"),
    QUICK("Quick save"),
    CHECKPOINT("Checkpoint"),
    UNKNOWN("Unknown");

    private final String label;

    SaveType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static SaveType fromName(String fileName) {
        String lowerName = fileName.toLowerCase();

        if (lowerName.contains("manual")) {
            return MANUAL;
        }

        if (lowerName.contains("quick")) {
            return QUICK;
        }

        if (lowerName.contains("checkpoint")) {
            return CHECKPOINT;
        }

        return UNKNOWN;
    }
}
