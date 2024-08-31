import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;

public class GUI extends JFrame {
    private String osName = System.getProperty("os.name");

    public GUI() {
        super("Death Stranding Transfer tool");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 400);
        setVisible(true);
        setLocationRelativeTo(null);
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        setResizable(false);

        System.out.println("OS: " + osName);

        // Load and resize the picture
        try {
            BufferedImage originalImage = ImageIO.read(new File("src/Death_Stranding_Logo.png")); // Load the original image
            Image resizedImage = originalImage.getScaledInstance(200, 100, Image.SCALE_SMOOTH); // Resize the image
            ImageIcon imageIcon = new ImageIcon(resizedImage); // Create an ImageIcon with the resized image
            JLabel picLabel = new JLabel(imageIcon); // Set the ImageIcon to the JLabel
            add(picLabel);
        } catch (IOException e) {
            e.printStackTrace();
            // Handle error - perhaps set a default image or display an error message
            System.out.println("Could not load the image.");
        }

        // Add label
        JLabel label = new JLabel("What would you like to do?");
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(label);

        // Add buttons panel
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new FlowLayout());

        //Operation buttons for export, import and exit

        JButton exportButton = new JButton("Export Game");
        exportButton.addActionListener(e -> {
            dispose(); // Close the current window
            new GUI(1); // Launch a new GUI instance with option 1
        });

        JButton importButton = new JButton("Import Game");
        importButton.addActionListener(e -> {
            dispose(); // Close the current window
            new GUI(2); // Launch a new GUI instance with option 2
        });

        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(e -> System.exit(0));

        buttonsPanel.add(exportButton);
        buttonsPanel.add(importButton);
        buttonsPanel.add(exitButton);
        add(buttonsPanel);

        getContentPane().revalidate();
    }



    public GUI(int option) {
        super("Death Stranding Transfer tool");
        if (option == 1) {
            // Export Game
            System.out.println("Export Game");

                setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                setSize(400, 400);
                setVisible(true);
                setLocationRelativeTo(null);
                setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
                //setResizable(false);

                // Step 1: Add Label
                JLabel instructionLabel = new JLabel("Which file would you like to export?");
                add(instructionLabel);

                // Step 2: Create and Populate the List Model
                DefaultListModel<String> listModel = new DefaultListModel<>();
                File folder;
                if (osName.contains("Mac")){
                    folder = new File(System.getProperty("user.home") + "/Library/Mobile Documents/iCloud~com~505games~deathstranding"); // Specify the folder path
                }else if(osName.contains("windows")){
                    folder = new File("C:\\Users\\juan_\\AppData\\Local\\Packages\\505Games.DeathStrandingPC_1.0.0.0_x64__9h6a0fz03f5f0\\SystemAppData\\wgs"); // Specify the folder path
                } else {
                    folder = null;
                }
            File[] listOfEntries = folder.listFiles();
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

                for (File entry : listOfEntries) {
                    if (entry.getName().toLowerCase().contains("save")) {
                        String entryDetails = entry.getName() + " - Last Modified: " + sdf.format(entry.lastModified());
                        listModel.addElement(entryDetails);
                    }
                }

                // Create the JList and JScrollPane
                JList<String> fileList = new JList<>(listModel);
                JScrollPane scrollPane = new JScrollPane(fileList);
                add(scrollPane);

                // Step 3: Add Export Button
                JButton exportButton = new JButton("Export");
                exportButton.addActionListener(f -> {
                    String selectedValue = fileList.getSelectedValue();
                    if (selectedValue != null) {
                        System.out.println("Exporting: " + selectedValue);

                        try {
                            // Assuming selectedValue is the file name. Adjust if it includes more information.
                            String fileName = selectedValue.split(" - ")[0]; // Adjust this line if the format is different.

                            // Construct the source path
                            Path sourcePath = Paths.get(folder.getAbsolutePath(), fileName);

                            // Construct the destination path
                            String desktopPath = System.getProperty("user.home") + "/Desktop";
                            Path destinationPath = Paths.get(desktopPath, fileName);

                            // Copy the file
                            Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                            System.out.println("File copied to Desktop successfully.");
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("Failed to copy the file.");
                        }
                    }
                });
                add(exportButton);

                // Refresh the frame to display the new components
                revalidate();
                repaint();
        } else if (option==2) {
            // Import Game
            System.out.println("Import Game");

            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(400, 400);
            setVisible(true);
            setLocationRelativeTo(null);
            setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
            //setResizable(false);

            // Step 1: Add Label
            JLabel instructionLabel = new JLabel("Which file would you like to Import?");
            add(instructionLabel);




        }
    }

}
