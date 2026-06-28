public enum SavePlatform {
    MAC("Mac"),
    WINDOWS("Windows");

    private final String label;

    SavePlatform(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static SavePlatform current() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("win") ? WINDOWS : MAC;
    }
}
