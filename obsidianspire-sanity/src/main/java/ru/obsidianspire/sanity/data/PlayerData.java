package ru.obsidianspire.sanity.data;

import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private double sanity;
    private int toleranceAminazine;
    private int toleranceHaloperidol;
    private int toleranceClozapine;
    private long lastTimeIsolated;

    // Potions active timers
    private long aminazineActiveUntil;
    private long haloperidolActiveUntil;
    private long clozapineActiveUntil;

    // Tolerance decay timers
    private long lastAminazineUse;
    private long lastHaloperidolUse;
    private long lastClozapineUse;

    // Hallucinations states
    private boolean hallucinationActivePhantom;
    private boolean hallucinationActiveChestScream;
    private int phantomHungerOriginalFood = -1;
    private float phantomHungerOriginalSat = -1f;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.sanity = 100.0;
        this.toleranceAminazine = 0;
        this.toleranceHaloperidol = 0;
        this.toleranceClozapine = 0;
        this.lastTimeIsolated = System.currentTimeMillis();
        this.aminazineActiveUntil = 0;
        this.haloperidolActiveUntil = 0;
        this.clozapineActiveUntil = 0;
        this.hallucinationActivePhantom = false;

        long now = System.currentTimeMillis();
        this.lastAminazineUse = now;
        this.lastHaloperidolUse = now;
        this.lastClozapineUse = now;
    }

    public UUID getUuid() { return uuid; }

    public double getSanity() { return sanity; }
    public void setSanity(double sanity) {
        this.sanity = Math.max(0, Math.min(100, sanity));
    }
    public void addSanity(double amount) { setSanity(this.sanity + amount); }
    public void removeSanity(double amount) { setSanity(this.sanity - amount); }

    public int getToleranceAminazine() { return toleranceAminazine; }
    public void setToleranceAminazine(int toleranceAminazine) { this.toleranceAminazine = Math.max(0, toleranceAminazine); }
    public void addToleranceAminazine() { this.toleranceAminazine++; this.lastAminazineUse = System.currentTimeMillis(); }

    public int getToleranceHaloperidol() { return toleranceHaloperidol; }
    public void setToleranceHaloperidol(int toleranceHaloperidol) { this.toleranceHaloperidol = Math.max(0, toleranceHaloperidol); }
    public void addToleranceHaloperidol() { this.toleranceHaloperidol++; this.lastHaloperidolUse = System.currentTimeMillis(); }

    public int getToleranceClozapine() { return toleranceClozapine; }
    public void setToleranceClozapine(int toleranceClozapine) { this.toleranceClozapine = Math.max(0, toleranceClozapine); }
    public void addToleranceClozapine() { this.toleranceClozapine++; this.lastClozapineUse = System.currentTimeMillis(); }

    public long getLastAminazineUse() { return lastAminazineUse; }
    public void setLastAminazineUse(long time) { this.lastAminazineUse = time; }

    public long getLastHaloperidolUse() { return lastHaloperidolUse; }
    public void setLastHaloperidolUse(long time) { this.lastHaloperidolUse = time; }

    public long getLastClozapineUse() { return lastClozapineUse; }
    public void setLastClozapineUse(long time) { this.lastClozapineUse = time; }

    public long getLastTimeIsolated() { return lastTimeIsolated; }
    public void setLastTimeIsolated(long lastTimeIsolated) { this.lastTimeIsolated = lastTimeIsolated; }

    public long getAminazineActiveUntil() { return aminazineActiveUntil; }
    public void setAminazineActiveUntil(long time) { this.aminazineActiveUntil = time; }
    public boolean isAminazineActive() { return System.currentTimeMillis() < aminazineActiveUntil; }

    public long getHaloperidolActiveUntil() { return haloperidolActiveUntil; }
    public void setHaloperidolActiveUntil(long time) { this.haloperidolActiveUntil = time; }
    public boolean isHaloperidolActive() { return System.currentTimeMillis() < haloperidolActiveUntil; }

    public long getClozapineActiveUntil() { return clozapineActiveUntil; }
    public void setClozapineActiveUntil(long time) { this.clozapineActiveUntil = time; }
    public boolean isClozapineActive() { return System.currentTimeMillis() < clozapineActiveUntil; }

    public boolean isHallucinationActivePhantom() { return hallucinationActivePhantom; }
    public void setHallucinationActivePhantom(boolean active) { this.hallucinationActivePhantom = active; }

    public boolean isHallucinationActiveChestScream() { return hallucinationActiveChestScream; }
    public void setHallucinationActiveChestScream(boolean active) { this.hallucinationActiveChestScream = active; }

    public int getPhantomHungerOriginalFood() { return phantomHungerOriginalFood; }
    public void setPhantomHungerOriginalFood(int food) { this.phantomHungerOriginalFood = food; }
    public float getPhantomHungerOriginalSat() { return phantomHungerOriginalSat; }
    public void setPhantomHungerOriginalSat(float sat) { this.phantomHungerOriginalSat = sat; }
}
