package myGame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class TextureSelectionWindow extends JFrame {
    private String selectedTexture = "frog.png"; // Default texture
    private boolean selectionMade = false;
    
    public TextureSelectionWindow() {
        super("Select Your Character");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        
        JLabel titleLabel = new JLabel("Choose Your Character", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        JPanel characterPanel = new JPanel();
        characterPanel.setLayout(new GridLayout(1, 2, 20, 0));
        
        // Frog option
        JPanel frogPanel = new JPanel();
        frogPanel.setLayout(new BoxLayout(frogPanel, BoxLayout.Y_AXIS));
        
        ImageIcon frogIcon = createScaledImageIcon("assets/images/frog.png", 100, 100);
        JLabel frogImage = new JLabel(frogIcon);
        frogImage.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JRadioButton frogButton = new JRadioButton("Frog");
        frogButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        frogButton.setSelected(true); // Default selection
        
        frogPanel.add(frogImage);
        frogPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        frogPanel.add(frogButton);
        
        // Bear option
        JPanel bearPanel = new JPanel();
        bearPanel.setLayout(new BoxLayout(bearPanel, BoxLayout.Y_AXIS));
        
        ImageIcon bearIcon = createScaledImageIcon("assets/images/bear.png", 100, 100);
        JLabel bearImage = new JLabel(bearIcon);
        bearImage.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JRadioButton bearButton = new JRadioButton("Bear");
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
        panel.add(characterPanel, BorderLayout.CENTER);
        
        // Start button
        JButton startButton = new JButton("Start Game");
        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (frogButton.isSelected()) {
                    selectedTexture = "frog.png";
                } else if (bearButton.isSelected()) {
                    selectedTexture = "bear.png";
                }
                selectionMade = true;
                dispose(); // Close the window
            }
        });
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(startButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(panel);
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
            g2d.drawRect(0, 0, width-1, height-1);
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