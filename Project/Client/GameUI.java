
package Project.Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

public class GameUI {
    private JFrame frame;
    private JPanel connectPanel, controlPanel, gamePanel, eventPanel;
    private JTextField hostField, portField, nameField;
    private JButton connectButton, readyButton, rockButton, paperButton, scissorsButton;
    private JTextArea eventLog;
    private JList<String> playerList;
    private DefaultListModel<String> playerListModel;

    private String lastPick = "";

    public GameUI() {
        frame = new JFrame("Rock Paper Scissors Multiplayer - Milestone 3");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());

        setupConnectPanel();
        setupGamePanel();
        setupEventPanel();
        setupControlPanel();

        frame.setVisible(true);
    }

    private void setupConnectPanel() {
        connectPanel = new JPanel(new FlowLayout());
        hostField = new JTextField("localhost", 10);
        portField = new JTextField("3000", 5);
        nameField = new JTextField("YourName", 10);
        connectButton = new JButton("Connect");

        connectButton.addActionListener(e -> {
            eventLog.append("Connecting to " + hostField.getText() + ":" + portField.getText() + " as " + nameField.getText() + "\n");
            // Implement connection logic to server via Client.java if needed
        });

        connectPanel.add(new JLabel("Host:"));
        connectPanel.add(hostField);
        connectPanel.add(new JLabel("Port:"));
        connectPanel.add(portField);
        connectPanel.add(new JLabel("Username:"));
        connectPanel.add(nameField);
        connectPanel.add(connectButton);

        frame.add(connectPanel, BorderLayout.NORTH);
    }

    private void setupGamePanel() {
        gamePanel = new JPanel(new BorderLayout());

        playerListModel = new DefaultListModel<>();
        playerList = new JList<>(playerListModel);
        gamePanel.add(new JScrollPane(playerList), BorderLayout.CENTER);

        frame.add(gamePanel, BorderLayout.WEST);
    }

    private void setupEventPanel() {
        eventPanel = new JPanel(new BorderLayout());
        eventLog = new JTextArea();
        eventLog.setEditable(false);
        eventPanel.add(new JScrollPane(eventLog), BorderLayout.CENTER);
        frame.add(eventPanel, BorderLayout.CENTER);
    }

    private void setupControlPanel() {
        controlPanel = new JPanel(new FlowLayout());
        readyButton = new JButton("Ready");
        rockButton = new JButton("Rock");
        paperButton = new JButton("Paper");
        scissorsButton = new JButton("Scissors");

        readyButton.addActionListener(e -> eventLog.append("You marked ready\n"));

        rockButton.addActionListener(e -> sendPick("rock"));
        paperButton.addActionListener(e -> sendPick("paper"));
        scissorsButton.addActionListener(e -> sendPick("scissors"));

        controlPanel.add(readyButton);
        controlPanel.add(rockButton);
        controlPanel.add(paperButton);
        controlPanel.add(scissorsButton);

        frame.add(controlPanel, BorderLayout.SOUTH);
    }

    private void sendPick(String pick) {
        if (lastPick.equals(pick)) {
            eventLog.append("Cooldown: Cannot pick same option twice in a row!\n");
            return;
        }
        lastPick = pick;
        eventLog.append("You picked: " + pick + "\n");
        // Implement sending pick to server via Client.INSTANCE.sendGamePick(pick);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameUI::new);
    }
}
