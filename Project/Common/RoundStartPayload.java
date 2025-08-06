// UCID: mi348
// Date: 2025-08-05
package Project.Common;

public class RoundStartPayload extends Payload {
    private int roundNumber;
    private int roundDurationSeconds;

    public RoundStartPayload(int roundNumber, int roundDurationSeconds) {
        setPayloadType(PayloadType.ROUND_START);
        this.roundNumber = roundNumber;
        this.roundDurationSeconds = roundDurationSeconds;
        setMessage("Starting Round " + roundNumber + ". You have " + roundDurationSeconds + " seconds to make a pick!");
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public int getRoundDurationSeconds() {
        return roundDurationSeconds;
    }
}