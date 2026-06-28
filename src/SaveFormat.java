public enum SaveFormat {
    MAC_BUNDLE("Mac bundle"),
    WINDOWS_DAT("Windows .dat"),
    UNKNOWN("Unknown");

    private final String label;

    SaveFormat(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
