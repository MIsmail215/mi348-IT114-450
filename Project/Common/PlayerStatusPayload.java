package Project.Common;

public class PlayerStatusPayload extends Payload {
    private long clientId;
    private PlayerStatus status;

    public PlayerStatusPayload(long clientId, PlayerStatus status) {
        setPayloadType(PayloadType.PLAYER_STATUS);
        this.clientId = clientId;
        this.status = status;
        setMessage(String.format("Player %d status updated to %s", clientId, status));
    }

    public long getClientId() {
        return clientId;
    }

    public PlayerStatus getStatus() {
        return status;
    }
}