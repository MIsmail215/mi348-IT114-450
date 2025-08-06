package Project.Client;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class RoomListDialog extends JDialog {

    public RoomListDialog(JFrame parent, List<String> rooms) {
        super(parent, "Available Rooms", true);
        setLayout(new BorderLayout());
        setSize(300, 400);
        setLocationRelativeTo(parent);

        JPanel roomsPanel = new JPanel();
        roomsPanel.setLayout(new BoxLayout(roomsPanel, BoxLayout.Y_AXIS));

        if (rooms == null || rooms.isEmpty()) {
            roomsPanel.add(new JLabel("No rooms available."));
        } else {
            for (String roomName : rooms) {
                roomsPanel.add(createRoomEntryPanel(roomName));
            }
        }

        JScrollPane scrollPane = new JScrollPane(roomsPanel);
        add(scrollPane, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(closeButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createRoomEntryPanel(String roomName) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createEtchedBorder());
        
        JLabel nameLabel = new JLabel(roomName);
        nameLabel.setPreferredSize(new Dimension(150, 20));
        
        JButton joinButton = new JButton("Join");
        joinButton.addActionListener(e -> {
            Client.INSTANCE.sendCommand("/joinroom " + roomName);
            dispose();
        });

        panel.add(nameLabel);
        panel.add(joinButton);
        return panel;
    }
}