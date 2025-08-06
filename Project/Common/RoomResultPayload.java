// UCID: mi348
// Date: 2025-08-05 (Corrected)
package Project.Common;

import java.util.ArrayList;
import java.util.List;

public class RoomResultPayload extends Payload {
    private List<String> rooms = new ArrayList<>();

    public RoomResultPayload() {
        setPayloadType(PayloadType.ROOM_LIST);
    }

    public List<String> getRooms() {
        return this.rooms;
    }

    public void setRooms(List<String> rooms) {
        this.rooms = rooms;
    }

    @Override
    public String toString() {
        return super.toString() + " Rooms [" + String.join(",", rooms) + "]";
    }
}