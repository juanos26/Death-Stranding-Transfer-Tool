import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class TransferToolUI extends JFrame {
    private static final Dimension DEFAULT_WINDOW_SIZE = new Dimension(1040, 680);
    private static final Dimension MIN_WINDOW_SIZE = new Dimension(900, 600);
    private static final int PAGE_MARGIN = 34;

    private static final Color BG = new Color(12, 14, 18);
    private static final Color SURFACE = new Color(20, 24, 31);
    private static final Color SURFACE_2 = new Color(27, 32, 41);
    private static final Color LINE = new Color(58, 66, 78);
    private static final Color LINE_SOFT = new Color(39, 46, 57);
    private static final Color TEXT = new Color(238, 240, 235);
    private static final Color MUTED = new Color(158, 166, 178);
    private static final Color ACCENT = new Color(214, 166, 74);
    private static final Color ACCENT_DARK = new Color(158, 113, 43);
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 26);
    private static final Font BODY_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font SMALL_FONT = new Font("SansSerif", Font.BOLD, 11);

    private final SavePlatform currentPlatform = SavePlatform.current();
    private final PathDetector pathDetector = new PathDetector();
    private final TransferService transferService = new TransferService();

    private JLabel statusLabel;
    private JProgressBar progressBar;
    private ShimmerLabel creditLabel;

    private Path selectedImportFile;
    private Path selectedExportFile;
    private Path exportSourceFolder;
    private Path importDestinationFolder;
    private Path exportOutputFolder;

    public TransferToolUI() {
        super("Death Stranding Transfer Tool");
        setupFrame();
        autoDetectFolders();
        showHomeScreen();
    }

    private void setupFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(DEFAULT_WINDOW_SIZE);
        setMinimumSize(MIN_WINDOW_SIZE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG);
    }

    private void autoDetectFolders() {
        Optional<Path> currentSaveFolder = pathDetector.detectSaveFolder(currentPlatform);
        currentSaveFolder.ifPresent(path -> {
            exportSourceFolder = path;
            importDestinationFolder = path;
        });

        exportOutputFolder = FileSystemView.getFileSystemView().getHomeDirectory().toPath();
    }

    private void showHomeScreen() {
        JPanel root = createShell("TRANSFER BRIDGE", "Guided save conversion for macOS bundles and Windows .dat files.");

        JPanel hero = new CardPanel();
        hero.setLayout(new BorderLayout(28, 0));
        hero.setBorder(new EmptyBorder(28, 30, 28, 30));

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(createTitle("Death Stranding Transfer Tool"));
        copy.add(createBody("Import, export, and convert saves between macOS .bundle packages and Windows .dat files.", 520));
        copy.add(Box.createVerticalStrut(24));
        copy.add(createActionRow());

        JPanel workflow = new CardPanel();
        workflow.setLayout(new BoxLayout(workflow, BoxLayout.Y_AXIS));
        workflow.setBorder(new EmptyBorder(18, 18, 18, 18));
        workflow.setPreferredSize(new Dimension(300, 210));
        workflow.add(createKicker("WHAT IT HANDLES"));
        workflow.add(createBody("Mac bundle -> extract internal data.", 235));
        workflow.add(createBody("Windows .dat -> wrap as Mac bundle.", 235));
        workflow.add(createBody("Backups before overwrites.", 235));
        workflow.add(createBody("Browse fallback when folders are missing.", 235));

        hero.add(copy, BorderLayout.CENTER);
        hero.add(workflow, BorderLayout.EAST);
        root.add(hero, BorderLayout.CENTER);
        root.add(createFooter(), BorderLayout.SOUTH);

        display(root);
    }

    private JPanel createActionRow() {
        JPanel actionPanel = new JPanel(new GridLayout(1, 3, 14, 0));
        actionPanel.setOpaque(false);
        actionPanel.setMaximumSize(new Dimension(620, 56));

        StyledButton exportButton = new StyledButton("Export Game", true);
        exportButton.addActionListener(e -> showExportScreen());

        StyledButton importButton = new StyledButton("Import Game", true);
        importButton.addActionListener(e -> showImportScreen());

        StyledButton exitButton = new StyledButton("Exit", false);
        exitButton.addActionListener(e -> System.exit(0));

        actionPanel.add(exportButton);
        actionPanel.add(importButton);
        actionPanel.add(exitButton);
        return actionPanel;
    }

    private void showExportScreen() {
        JPanel root = createShell("EXPORT SAVE", "Choose a save and export it as either a Mac bundle or Windows .dat file.");
        exportSourceFolder = ensureFolder(exportSourceFolder, "source save folder");

        DefaultListModel<SaveFile> listModel = new DefaultListModel<>();
        refreshExportList(listModel);

        JList<SaveFile> fileList = new JList<>(listModel);
        fileList.setCellRenderer(new SaveFileRenderer());
        styleList(fileList);
        fileList.addListSelectionListener(e -> {
            SaveFile selected = fileList.getSelectedValue();
            selectedExportFile = selected == null ? null : selected.getPath();
        });

        JComboBox<SavePlatform> targetFormat = new JComboBox<>(SavePlatform.values());
        targetFormat.setSelectedItem(currentPlatform == SavePlatform.MAC ? SavePlatform.WINDOWS : SavePlatform.MAC);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setOpaque(false);
        controls.add(createPathRow("Source folder", () -> exportSourceFolder, path -> {
            exportSourceFolder = path;
            refreshExportList(listModel);
        }, true));
        controls.add(Box.createVerticalStrut(10));
        controls.add(createPathRow("Output folder", () -> exportOutputFolder, path -> exportOutputFolder = path, true));
        controls.add(Box.createVerticalStrut(10));
        controls.add(createComboRow("Export as", targetFormat));
        controls.add(Box.createVerticalStrut(14));
        controls.add(createDropZone("Drag save file here", path -> {
            selectedExportFile = path;
            appendStatus("Selected export source: " + path);
        }));

        JPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));
        card.add(controls, BorderLayout.NORTH);
        card.add(new JScrollPane(fileList), BorderLayout.CENTER);

        JPanel footer = createFooter();
        StyledButton backButton = new StyledButton("Back", false);
        backButton.addActionListener(e -> showHomeScreen());
        StyledButton exportButton = new StyledButton("Export Selected", true);
        exportButton.addActionListener(e -> runWithProgress("Exporting save...", () -> {
            Path source = selectedExportFile;
            if (source == null) {
                throw new IllegalStateException("Choose a save from the list or drag one into the drop box.");
            }

            confirmBackupPolicy("Export");
            TransferResult result = transferService.exportSave(source, (SavePlatform) targetFormat.getSelectedItem(), exportOutputFolder);
            return result.getMessage() + " Output: " + result.getOutputPath();
        }));

        addFooterButton(footer, backButton);
        addFooterButton(footer, exportButton);

        root.add(card, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        display(root);
    }

    private void showImportScreen() {
        JPanel root = createShell("IMPORT SAVE", "Drop or select a save file; the app converts it when needed for this computer.");
        importDestinationFolder = ensureFolder(importDestinationFolder, "destination save folder");

        JLabel selectedLabel = createMuted("No import file selected.");

        JPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(createTitle("Import to " + currentPlatform.getLabel()));
        top.add(createBody("Drop a save file below. If it does not match this computer, it will be converted before import.", 720));
        top.add(Box.createVerticalStrut(14));
        top.add(createPathRow("Destination folder", () -> importDestinationFolder, path -> importDestinationFolder = path, true));

        DropPanel dropPanel = createDropZone("Drag and drop here", path -> {
            selectedImportFile = path;
            SaveFile saveFile = new SaveFile(path);
            selectedLabel.setText(saveFile.describe());
            appendStatus("Selected import file: " + path);
        });
        dropPanel.setPreferredSize(new Dimension(720, 170));

        StyledButton browseFile = new StyledButton("Choose File", false);
        browseFile.addActionListener(e -> chooseImportFile(selectedLabel));
        browseFile.setMaximumSize(new Dimension(180, 42));

        JPanel bottom = new JPanel(new BorderLayout(0, 12));
        bottom.setOpaque(false);
        bottom.add(dropPanel, BorderLayout.CENTER);
        bottom.add(selectedLabel, BorderLayout.NORTH);
        bottom.add(browseFile, BorderLayout.SOUTH);

        card.add(top, BorderLayout.NORTH);
        card.add(bottom, BorderLayout.CENTER);

        JPanel footer = createFooter();
        StyledButton backButton = new StyledButton("Back", false);
        backButton.addActionListener(e -> showHomeScreen());
        StyledButton importButton = new StyledButton("Import", true);
        importButton.addActionListener(e -> runWithProgress("Importing save...", () -> {
            if (selectedImportFile == null) {
                throw new IllegalStateException("Choose a save file or drag one into the drop box.");
            }

            if (importDestinationFolder == null) {
                throw new IllegalStateException("Choose a destination save folder.");
            }

            confirmBackupPolicy("Import");
            TransferResult result = transferService.importSave(selectedImportFile, currentPlatform, importDestinationFolder);
            return result.getMessage() + " Destination: " + result.getOutputPath();
        }));

        addFooterButton(footer, backButton);
        addFooterButton(footer, importButton);

        root.add(card, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        display(root);
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(8, 0, 0, 0));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        buttons.setOpaque(false);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(180, 12));

        statusLabel = createMuted("Ready.");
        statusLabel.setBorder(new EmptyBorder(0, 12, 0, 12));

        JPanel statusPanel = new JPanel(new BorderLayout(10, 0));
        statusPanel.setOpaque(false);
        statusPanel.add(progressBar, BorderLayout.WEST);
        statusPanel.add(statusLabel, BorderLayout.CENTER);

        creditLabel = new ShimmerLabel("Made by Juan");
        creditLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        creditLabel.setPreferredSize(new Dimension(140, 32));

        footer.add(buttons, BorderLayout.WEST);
        footer.add(statusPanel, BorderLayout.CENTER);
        footer.add(creditLabel, BorderLayout.EAST);
        footer.putClientProperty("buttonPanel", buttons);
        return footer;
    }

    private void addFooterButton(JPanel footer, JButton button) {
        JPanel buttons = (JPanel) footer.getClientProperty("buttonPanel");
        buttons.add(button);
    }

    private void refreshExportList(DefaultListModel<SaveFile> listModel) {
        listModel.clear();

        if (exportSourceFolder == null || !Files.isDirectory(exportSourceFolder)) {
            appendStatus("Source folder was not found. Use Browse to choose it.");
            return;
        }

        try (Stream<Path> paths = Files.list(exportSourceFolder)) {
            paths
                    .map(SaveFile::new)
                    .filter(SaveFile::isSupported)
                    .forEach(listModel::addElement);
        } catch (Exception e) {
            appendStatus("Could not read source folder: " + e.getMessage());
        }
    }

    private JPanel createPathRow(String label, PathSupplier getter, PathConsumer setter, boolean directoriesOnly) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);

        JLabel text = createMuted(label + ": " + describePath(getter.get()));
        text.setToolTipText(describePath(getter.get()));
        StyledButton browse = new StyledButton("Browse", false);
        browse.setPreferredSize(new Dimension(110, 36));
        browse.setMaximumSize(new Dimension(110, 36));
        browse.addActionListener(e -> {
            Path selected = choosePath(directoriesOnly);
            if (selected != null) {
                setter.accept(selected);
                text.setText(label + ": " + describePath(selected));
                text.setToolTipText(describePath(selected));
                appendStatus(label + " set to: " + selected);
            }
        });

        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        row.add(text, BorderLayout.CENTER);
        row.add(browse, BorderLayout.EAST);
        return row;
    }

    private JPanel createComboRow(String label, JComboBox<SavePlatform> comboBox) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.add(createMuted(label), BorderLayout.WEST);
        row.add(comboBox, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        return row;
    }

    private DropPanel createDropZone(String text, PathConsumer onDrop) {
        DropPanel panel = new DropPanel(text);
        panel.setPreferredSize(new Dimension(240, 100));
        panel.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                try {
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) {
                        onDrop.accept(files.get(0).toPath());
                        return true;
                    }
                } catch (Exception e) {
                    appendStatus("Drop failed: " + e.getMessage());
                }

                return false;
            }
        });
        return panel;
    }

    private Path choosePath(boolean directoriesOnly) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(directoriesOnly ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_AND_DIRECTORIES);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile().toPath();
        }

        return null;
    }

    private void chooseImportFile(JLabel selectedLabel) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedImportFile = chooser.getSelectedFile().toPath();
            selectedLabel.setText(new SaveFile(selectedImportFile).describe());
            appendStatus("Selected import file: " + selectedImportFile);
        }
    }

    private void runWithProgress(String message, Task task) {
        appendStatus(message);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return task.run();
            }

            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                progressBar.setVisible(false);
                try {
                    appendStatus(get());
                    JOptionPane.showMessageDialog(TransferToolUI.this, get(), "Transfer complete", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    appendStatus("Failed: " + e.getMessage());
                    JOptionPane.showMessageDialog(TransferToolUI.this, e.getMessage(), "Transfer failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void confirmBackupPolicy(String action) {
        int choice = JOptionPane.showConfirmDialog(this,
                "Existing files with the same name will be backed up before overwrite. Continue?",
                "Confirm " + action.toLowerCase(),
                JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            throw new IllegalStateException(action + " cancelled.");
        }
    }

    private Path ensureFolder(Path currentFolder, String label) {
        if (currentFolder != null && Files.isDirectory(currentFolder)) {
            return currentFolder;
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "Could not auto-detect the " + label + ". Would you like to choose it now?",
                "Folder not found",
                JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            Path selected = choosePath(true);
            if (selected != null) {
                appendStatus("Selected " + label + ": " + selected);
                return selected;
            }
        }

        return currentFolder;
    }

    private JPanel createShell(String section, String subtitle) {
        JPanel root = new GradientPanel();
        root.setLayout(new BorderLayout(0, 18));
        root.setBorder(new EmptyBorder(28, PAGE_MARGIN, 18, PAGE_MARGIN));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(createKicker(section));
        copy.add(createMuted(subtitle));

        JLabel platform = createBadge(currentPlatform.getLabel());

        header.add(copy, BorderLayout.WEST);
        header.add(platform, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);
        return root;
    }

    private void display(JPanel root) {
        setContentPane(root);
        revalidate();
        repaint();

        if (!isVisible()) {
            setLocationRelativeTo(null);
            setVisible(true);
        }
    }

    private JLabel createTitle(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        label.setFont(TITLE_FONT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JLabel createBody(String text) {
        return createBody(text, 520);
    }

    private JLabel createBody(String text, int width) {
        JLabel label = new JLabel("<html><body style='width: " + width + "px'>" + text + "</body></html>");
        label.setForeground(MUTED);
        label.setFont(BODY_FONT);
        label.setBorder(new EmptyBorder(8, 0, 0, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JLabel createMuted(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED);
        label.setFont(BODY_FONT);
        return label;
    }

    private JLabel createKicker(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(ACCENT);
        label.setFont(SMALL_FONT);
        label.setBorder(new EmptyBorder(0, 0, 8, 0));
        return label;
    }

    private JLabel createBadge(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        label.setFont(SMALL_FONT);
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                new EmptyBorder(8, 12, 8, 12)
        ));
        return label;
    }

    private String describePath(Path path) {
        if (path == null) {
            return "Not detected";
        }

        String value = path.toString();
        if (value.length() <= 66) {
            return value;
        }

        return value.substring(0, 30) + "..." + value.substring(value.length() - 30);
    }

    private void appendStatus(String message) {
        if (statusLabel == null) {
            return;
        }

        statusLabel.setText(message);
        statusLabel.setToolTipText(message);
    }

    private void styleList(JList<?> list) {
        list.setBackground(SURFACE);
        list.setForeground(TEXT);
        list.setSelectionBackground(ACCENT);
        list.setSelectionForeground(Color.BLACK);
        list.setFont(BODY_FONT);
        list.setFixedCellHeight(42);
        list.setBorder(new EmptyBorder(10, 12, 10, 12));
    }

    private interface Task {
        String run() throws Exception;
    }

    private interface PathSupplier {
        Path get();
    }

    private interface PathConsumer {
        void accept(Path path);
    }

    private static class SaveFileRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            SaveFile saveFile = (SaveFile) value;
            return super.getListCellRendererComponent(list, saveFile.describe(), index, isSelected, cellHasFocus);
        }
    }

    private static class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            GradientPaint paint = new GradientPaint(0, 0, new Color(17, 22, 30), getWidth(), getHeight(), BG);
            g2.setPaint(paint);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(new Color(214, 166, 74, 24));
            g2.drawLine(42, 92, getWidth() - 42, 92);
            g2.dispose();
        }
    }

    private static class CardPanel extends JPanel {
        CardPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(SURFACE_2);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            g2.setColor(LINE_SOFT);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class DropPanel extends JPanel {
        private final String text;

        DropPanel(String text) {
            this.text = text;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float[] dash = {8f, 8f};
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
            g2.setColor(LINE);
            g2.drawRoundRect(8, 8, getWidth() - 16, getHeight() - 16, 14, 14);
            g2.setColor(MUTED);
            FontMetrics metrics = g2.getFontMetrics();
            int x = (getWidth() - metrics.stringWidth(text)) / 2;
            int y = getHeight() / 2;
            g2.drawString(text, x, y);
            g2.dispose();
        }
    }

    private static class StyledButton extends JButton {
        private final boolean primary;
        private boolean hovering;

        StyledButton(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setFont(new Font("SansSerif", Font.BOLD, 13));
            setForeground(primary ? Color.BLACK : TEXT);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(170, 48));
            setBorder(new EmptyBorder(12, 18, 12, 18));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovering = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovering = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color fill;
            Color border;
            if (!isEnabled()) {
                fill = new Color(42, 46, 54);
                border = LINE_SOFT;
                setForeground(MUTED);
            } else if (primary) {
                fill = hovering ? new Color(232, 185, 91) : ACCENT;
                border = ACCENT_DARK;
                setForeground(Color.BLACK);
            } else {
                fill = hovering ? new Color(45, 51, 62) : new Color(29, 34, 42);
                border = hovering ? ACCENT : LINE;
                setForeground(TEXT);
            }

            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g2.setColor(border);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class ShimmerLabel extends JLabel {
        private int tick;

        ShimmerLabel(String text) {
            super(text);
            setFont(new Font("SansSerif", Font.BOLD, 12));
            setBorder(new EmptyBorder(0, 12, 0, 0));

            Timer timer = new Timer(90, e -> {
                tick = (tick + 1) % 24;
                repaint();
            });
            timer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            int alpha = 90 + (int) (70 * Math.sin(tick / 24.0 * Math.PI * 2));
            setForeground(new Color(238, 240, 235, Math.max(60, alpha)));
            super.paintComponent(g);
        }
    }
}
