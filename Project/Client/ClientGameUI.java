// UCID: mi348
// Date: 2025-08-05
package Project.Client;

import javax.swing.*;
import java.awt.*;

public class ClientGameUI {

    private JFrame frame;
    private JTextField hostField;
    private JTextField portField;
    private JTextField nameField;
    private JTextArea eventLog;
    private JTable playerTable;
    private PlayerTableModel playerTableModel;
    private JLabel timerLabel;
    private JButton connectButton;
    private JButton spectatorButton;
    private JButton readyButton;
    private JToggleButton awayButton;
    private JCheckBox extraOptionsCheck; // <-- ADD THIS
    private JCheckBox cooldownCheck; // <-- ADD THIS
    private JButton rockButton, paperButton, scissorsButton, lizardButton, spockButton;
    private JButton listRoomsButton, createRoomButton;

    public ClientGameUI() {
        frame = new JFrame("RPS Multiplayer Game - Milestone 3");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout(10, 10));

        // Top panel for connection details
        JPanel connectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        connectPanel.setBorder(BorderFactory.createTitledBorder("Connection"));
        hostField = new JTextField("localhost", 10);
        portField = new JTextField("3000", 5);
        nameField = new JTextField("YourName", 10);
        connectButton = new JButton("Connect");
        spectatorButton = new JButton("Join as Spectator");
        connectPanel.add(new JLabel("Host:"));
        connectPanel.add(hostField);
        connectPanel.add(new JLabel("Port:"));
        connectPanel.add(portField);
        connectPanel.add(new JLabel("Username:"));
        connectPanel.add(nameField);
        connectPanel.add(connectButton);
        connectPanel.add(spectatorButton);
        frame.add(connectPanel, BorderLayout.NORTH);

        // Center panel for event logs and timer
        JPanel centerPanel = new JPanel(new BorderLayout());
        eventLog = new JTextArea();
        eventLog.setEditable(false);
        eventLog.setLineWrap(true);
        eventLog.setWrapStyleWord(true);
        JScrollPane eventScrollPane = new JScrollPane(eventLog);
        eventScrollPane.setBorder(BorderFactory.createTitledBorder("Game Events"));
        timerLabel = new JLabel("Time Remaining: --", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Serif", Font.BOLD, 16));
        centerPanel.add(timerLabel, BorderLayout.NORTH);
        centerPanel.add(eventScrollPane, BorderLayout.CENTER);
        frame.add(centerPanel, BorderLayout.CENTER);

        // Left panel for player status
        playerTableModel = new PlayerTableModel();
        playerTable = new JTable(playerTableModel);
        JScrollPane playerScrollPane = new JScrollPane(playerTable);
        playerScrollPane.setBorder(BorderFactory.createTitledBorder("Players"));
        playerScrollPane.setPreferredSize(new Dimension(250, 0));
        frame.add(playerScrollPane, BorderLayout.WEST);
        
        // Bottom panel for all actions
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));

        JPanel roomActionsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        roomActionsPanel.setBorder(BorderFactory.createTitledBorder("Rooms"));
        listRoomsButton = new JButton("List Rooms");
        createRoomButton = new JButton("Create Room");
        roomActionsPanel.add(listRoomsButton);
        roomActionsPanel.add(createRoomButton);
        
        JPanel gameActionsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        gameActionsPanel.setBorder(BorderFactory.createTitledBorder("Actions"));
        
        // Panel for the settings checkboxes
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        extraOptionsCheck = new JCheckBox("Enable Lizard/Spock");
        cooldownCheck = new JCheckBox("Enable Cooldown");
        settingsPanel.add(extraOptionsCheck);
        settingsPanel.add(cooldownCheck);
        
        readyButton = new JButton("Ready");
        awayButton = new JToggleButton("Away");
        rockButton = new JButton("Rock");
        paperButton = new JButton("Paper");
        scissorsButton = new JButton("Scissors");
        lizardButton = new JButton("Lizard");
        spockButton = new JButton("Spock");
        
        gameActionsPanel.add(settingsPanel); // Add settings panel to the layout
        gameActionsPanel.add(readyButton);
        gameActionsPanel.add(awayButton);
        gameActionsPanel.add(rockButton);
        gameActionsPanel.add(paperButton);
        gameActionsPanel.add(scissorsButton);
        gameActionsPanel.add(lizardButton);
        gameActionsPanel.add(spockButton);
        
        bottomPanel.add(roomActionsPanel);
        bottomPanel.add(gameActionsPanel);
        frame.add(bottomPanel, BorderLayout.SOUTH);
        
        Client.INSTANCE.registerUI(this);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
    // vvv ADD THESE GETTERS vvv
    public JCheckBox getExtraOptionsCheck() { return extraOptionsCheck; }
    public JCheckBox getCooldownCheck() { return cooldownCheck; }
    // ^^^ END OF GETTERS ^^^
    
    public JButton getSpectatorButton() { return spectatorButton; }
    public JToggleButton getAwayButton() { return awayButton; }
    public JFrame getFrame() { return frame; }
    public JTextField getHostField() { return hostField; }
    public JTextField getPortField() { return portField; }
    public JTextField getNameField() { return nameField; }
    public JTextArea getEventLog() { return eventLog; }
    public PlayerTableModel getPlayerTableModel() { return playerTableModel; }
    public JLabel getTimerLabel() { return timerLabel; }
    public JButton getConnectButton() { return connectButton; }
    public JButton getReadyButton() { return readyButton; }
    public JButton getRockButton() { return rockButton; }
    public JButton getPaperButton() { return paperButton; }
    public JButton getScissorsButton() { return scissorsButton; }
    public JButton getLizardButton() { return lizardButton; }
    public JButton getSpockButton() { return spockButton; }
    public JButton getListRoomsButton() { return listRoomsButton; }
    public JButton getCreateRoomButton() { return createRoomButton; }

    public static void main(String[] args) {
        Client.INSTANCE.start();
        SwingUtilities.invokeLater(ClientGameUI::new);
    }
}