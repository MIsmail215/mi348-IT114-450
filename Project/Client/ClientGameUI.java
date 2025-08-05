package Project.Client;

import javax.swing.*;
import java.awt.*;

public class ClientGameUI {

    private JFrame frame;
    private JTextField hostField;
    private JTextField portField;
    private JTextField nameField;
    private JTextArea eventLog;
    private JButton connectButton;
    private JButton readyButton;
    private JButton rockButton;
    private JButton paperButton;
    private JButton scissorsButton;

    private String lastPick = "";

    public ClientGameUI() {
        frame = new JFrame("RPS Multiplayer Game - Milestone 3");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLayout(new BorderLayout());

        // Top panel for connection
        JPanel connectPanel = new JPanel();
        connectPanel.setLayout(new FlowLayout());

        hostField = new JTextField("localhost", 10);
        portField = new JTextField("3000", 5);
        nameField = new JTextField("YourName", 10);
        connectButton = new JButton("Connect");

        connectPanel.add(new JLabel("Host:"));
        connectPanel.add(hostField);
        connectPanel.add(new JLabel("Port:"));
        connectPanel.add(portField);
        connectPanel.add(new JLabel("Username:"));
        connectPanel.add(nameField);
        connectPanel.add(connectButton);

        frame.add(connectPanel, BorderLayout.NORTH);

        // Center panel for logs
        eventLog = new JTextArea(10, 30);
        eventLog.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(eventLog);
        frame.add(scrollPane, BorderLayout.CENTER);

        // Bottom panel for game buttons
        JPanel actionPanel = new JPanel();
        readyButton = new JButton("Ready");
        rockButton = new JButton("Rock");
        paperButton = new JButton("Paper");
        scissorsButton = new JButton("scissors");

        actionPanel.add(readyButton);
        actionPanel.add(rockButton);
        actionPanel.add(paperButton);
        actionPanel.add(scissorsButton);
        frame.add(actionPanel, BorderLayout.SOUTH);

        frame.setVisible(true);

        // Action handlers
        connectButton.addActionListener(e -> {
            String host = hostField.getText().trim();
            String port = portField.getText().trim();
            String name = nameField.getText().trim();
            eventLog.append("Connecting to " + host + ":" + port + " as " + name + "\n");
            // Client.INSTANCE.connect(host, Integer.parseInt(port));
            // Client.INSTANCE.setUserName(name);
        });

        readyButton.addActionListener(e -> eventLog.append("Player marked ready\n"));
        
        rockButton.addActionListener(e -> sendPick("rock"));
        paperButton.addActionListener(e -> sendPick("paper"));
        scissorsButton.addActionListener(e -> sendPick("scissors"));
    }

    private void sendPick(String pick) {
        if (pick.equals(lastPick)) {
            eventLog.append("Cooldown: Can't pick same option twice in a row!\n");
            return;
        }
        lastPick = pick;
        eventLog.append("Picked " + pick.toUpperCase() + "\n");
        // Client.INSTANCE.sendGamePick(pick);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientGameUI::new);
    }
}
