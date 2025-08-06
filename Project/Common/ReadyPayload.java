package Project.Common;

public class ReadyPayload extends Payload {
    private boolean extraOptionsEnabled;
    private boolean cooldownEnabled;

    public ReadyPayload() {
        setPayloadType(PayloadType.GAME_READY);
    }

    public boolean areExtraOptionsEnabled() {
        return extraOptionsEnabled;
    }

    public void setExtraOptionsEnabled(boolean extraOptionsEnabled) {
        this.extraOptionsEnabled = extraOptionsEnabled;
    }

    public boolean isCooldownEnabled() {
        return cooldownEnabled;
    }

    public void setCooldownEnabled(boolean cooldownEnabled) {
        this.cooldownEnabled = cooldownEnabled;
    }
}