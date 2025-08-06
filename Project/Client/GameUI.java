
// UCID: mi348 | Updated: 2025-08-04
package Project.Client;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Vector;

public class GameUI extends JFrame {
    private JPanel readyPanel;
    private JPanel gameAreaPanel;
    private JButton readyButton;
    private JTextArea gameEventsArea;
    private JTable playerTable;
    private DefaultTableModel playerTableModel;

    public GameUI() {
        setTitle("Rock Paper Scissors - Game UI");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Ready Panel
        readyPanel = new JPanel();
        readyButton = new JButton("I'm Ready");
        readyPanel.add(readyButton);
        add(readyPanel, BorderLayout.NORTH);

        // Game Area Panel
        gameAreaPanel = new JPanel(new BorderLayout());

        // Player Table
        String[] columnNames = { "Username", "Client ID", "Points", "Status" };
        playerTableModel = new DefaultTableModel(columnNames, 0);
        playerTable = new JTable(playerTableModel);
        JScrollPane tableScroll = new JScrollPane(playerTable);

        // Game Events Log
        gameEventsArea = new JTextArea();
        gameEventsArea.setEditable(false);
        JScrollPane eventScroll = new JScrollPane(gameEventsArea);
        eventScroll.setBorder(BorderFactory.createTitledBorder("Game Events"));

        // Add to Game Panel
        gameAreaPanel.add(tableScroll, BorderLayout.CENTER);
        gameAreaPanel.add(eventScroll, BorderLayout.SOUTH);

        add(gameAreaPanel, BorderLayout.CENTER);

        setVisible(true);
    }

    public JButton getReadyButton() {
        return readyButton;
    }

    public void updateGameEvent(String event) {
        gameEventsArea.append(event + "\n");
    }

    public void updatePlayerTable(Vector<Vector<Object>> data) {
        playerTableModel.setRowCount(0);
        for (Vector<Object> row : data) {
            playerTableModel.addRow(row);
        }
    }

    public void resetForNewSession() {
        gameEventsArea.setText("");
        playerTableModel.setRowCount(0);
        readyPanel.setVisible(true);
    }

    public void hideReadyPanel() {
        readyPanel.setVisible(false);
    }
}
