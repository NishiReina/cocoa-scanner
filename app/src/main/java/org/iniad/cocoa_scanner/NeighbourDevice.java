package org.iniad.cocoa_scanner;

public class NeighbourDevice {
    private String id;
    private int rssi;
    private long receivedAt;
    private boolean valid;

    public NeighbourDevice(String id, int rssi, long receivedAt) {
        this.id = id;
        this.rssi = rssi;
        this.receivedAt = receivedAt;
        this.valid = true;
    }

    public void update(int rssi, long receivedAt) {
        this.rssi = rssi;
        this.receivedAt = receivedAt;
    }

    public void invalidate() {
        this.valid = false;
    }

    public String getId() {
        return id;
    }

    public int getRssi() {
        return rssi;
    }

    public long getRecevedAt() {
        return receivedAt;
    }

    public boolean isValid() {
        return valid;
    }
}
