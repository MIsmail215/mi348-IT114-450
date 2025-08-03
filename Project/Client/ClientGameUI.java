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
        scissorsButton = new JButton("Scissors");

        actionPanel.add(readyButton);
        actionPanel.add(rockButton);
        actionPanel.add(paperButton);
        actionPanel.add(scissorsButton);
        frame.add(actionPanel, BorderLayout.SOUTH);

        frame.setVisible(true);

        // ✅ Corrected string syntax with "\n" instead of broken lines
        connectButton.addActionListener(e -> eventLog.append("Connecting to server...\n"));
        readyButton.addActionListener(e -> eventLog.append("Player marked ready\n"));
        rockButton.addActionListener(e -> eventLog.append("Picked ROCK\n"));
        paperButton.addActionListener(e -> eventLog.append("Picked PAPER\n"));
        scissorsButton.addActionListener(e -> eventLog.append("Picked SCISSORS\n"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientGameUI::new);
    }
}
