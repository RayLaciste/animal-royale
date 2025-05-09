package myGame;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

public class TextureSelectionWindow extends JFrame {
    private String selectedTexture = "frog.png"; // Default texture
    private boolean selectionMade = false;

    public TextureSelectionWindow() {
        super("Select Your Warrior");
        setSize(800, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Create background panel with image
        JPanel backgroundPanel = new JPanel() {
            private Image backgroundImage;

            {
                // Load background image
                backgroundImage = new ImageIcon("assets/textures/fire.jpg").getImage();
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    // Draw background image scaled to fit the window
                    g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };
        backgroundPanel.setLayout(new GridBagLayout());

        // Create content panel with semi-transparent background
        JPanel contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                // Create semi-transparent overlay
                g.setColor(new Color(0, 0, 0, 180)); // Dark overlay with 70% opacity
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 20, 10, 20);

        // Wrapped title label
        JLabel titleLabel = new JLabel(
                "<html><div style='width:350px; text-align:center; color:white;'>" +
                        "Welcome to Super Mega Animal Royale Jamboree Gaiden 2: " +
                        "Electric Boogaloo - Definitive Edition Remastered" +
                        "</div></html>",
                JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        contentPanel.add(titleLabel, gbc);

        // Character selection panel
        JPanel characterPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        characterPanel.setOpaque(false);

        // Frog option (centered)
        JPanel frogPanel = new JPanel();
        frogPanel.setOpaque(false);
        frogPanel.setLayout(new BoxLayout(frogPanel, BoxLayout.Y_AXIS));
        ImageIcon frogIcon = createScaledImageIcon("assets/textures/frog_profile.png", 100, 100);
        JLabel frogImage = new JLabel(frogIcon);
        frogImage.setAlignmentX(Component.CENTER_ALIGNMENT);
        JRadioButton frogButton = new JRadioButton("Frog");
        frogButton.setForeground(Color.BLACK);
        frogButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        frogButton.setSelected(true);
        frogPanel.add(frogImage);
        frogPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        frogPanel.add(frogButton);

        // Bear option (centered)
        JPanel bearPanel = new JPanel();
        bearPanel.setOpaque(false);
        bearPanel.setLayout(new BoxLayout(bearPanel, BoxLayout.Y_AXIS));
        ImageIcon bearIcon = createScaledImageIcon("assets/textures/bear_profile.png", 100, 100);
        JLabel bearImage = new JLabel(bearIcon);
        bearImage.setAlignmentX(Component.CENTER_ALIGNMENT);
        JRadioButton bearButton = new JRadioButton("Bear");
        bearButton.setForeground(Color.BLACK);
        bearButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        bearPanel.add(bearImage);
        bearPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        bearPanel.add(bearButton);

        // Group radio buttons
        ButtonGroup group = new ButtonGroup();
        group.add(frogButton);
        group.add(bearButton);

        characterPanel.add(frogPanel);
        characterPanel.add(bearPanel);
        contentPanel.add(characterPanel, gbc);

        // Start button (centered)
        JButton startButton = new JButton("Start Game");
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        startButton.addActionListener(e -> {
            if (frogButton.isSelected()) {
                selectedTexture = "frog.png";
            } else if (bearButton.isSelected()) {
                selectedTexture = "bear.png";
            }
            selectionMade = true;
            dispose();
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(startButton);
        buttonPanel.add(Box.createHorizontalGlue());
        contentPanel.add(buttonPanel, gbc);

        backgroundPanel.add(contentPanel);
        setContentPane(backgroundPanel);
        setVisible(true);
    }

    private ImageIcon createScaledImageIcon(String path, int width, int height) {
        // Try to load the image
        ImageIcon icon = new ImageIcon(path);
        if (icon.getIconWidth() <= 0) {
            // If image not found, create a colored rectangle as placeholder
            Image img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = ((BufferedImage) img).createGraphics();
            g2d.setColor(Color.GRAY);
            g2d.fillRect(0, 0, width, height);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(0, 0, width - 1, height - 1);
            g2d.dispose();
            return new ImageIcon(img);
        }

        // Scale the image to desired size
        Image img = icon.getImage();
        Image newImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(newImg);
    }

    public String getSelectedTexture() {
        return selectedTexture;
    }

    public boolean isSelectionMade() {
        return selectionMade;
    }

    // Wait for the user to make a selection
    public String waitForSelection() {
        while (!selectionMade) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return selectedTexture;
    }
}