package Project.Client;

import javax.swing.table.AbstractTableModel;
import Project.Common.User;
import Project.Common.PlayerStatus; // Make sure this is imported
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PlayerTableModel extends AbstractTableModel {
    private final List<User> players;
    private final String[] columnNames = {"Player", "Score", "Status"};

    public PlayerTableModel() {
        this.players = new ArrayList<>();
    }

    public void setPlayers(List<User> playerList) {
        players.clear();
        players.addAll(playerList);
        players.sort(Comparator.comparing(User::getPoints).reversed()
                                .thenComparing(User::getClientName));
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return players.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        User player = players.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return player.getDisplayName();
            case 1:
                return player.getPoints();
            case 2:
                // vvv THIS IS THE CHANGE vvv
                // Display SPECTATING if the user is a spectator, otherwise show their normal status
                if (player.isSpectator()) {
                    return PlayerStatus.SPECTATING.toString();
                }
                return player.getStatus().toString();
                // ^^^ END OF CHANGE ^^^
            default:
                return null;
        }
    }
}