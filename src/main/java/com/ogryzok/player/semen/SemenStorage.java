package com.ogryzok.player.semen;

public class SemenStorage implements ISemenStorage {

    public static final int CAPACITY = 1000;

    private int amount;
    private int tickCounter;
    private boolean manualHarvesting;
    private int manualHarvestTicks;
    private int manualHarvestCooldownTicks;
    private boolean steroidHarvest;
    private boolean steroidLoadedShot;
    private int steroidHarvestLevel;
    private int abstinenceTicks;
    private int abstinenceStage;
    private boolean seedKeeper;
    private double manualHarvestStartY;
    private int weaknessLockTicks;
    private int keeperHoverTicks;
    private int keeperGlideTicks;
    private int unstableBeamTicks;
    private int unstableBeamStage;
    private int lastBurstRollMinute;

    public SemenStorage() {
        this.amount = 0;
        this.tickCounter = 0;
        this.manualHarvesting = false;
        this.manualHarvestTicks = 0;
        this.manualHarvestCooldownTicks = 0;
        this.steroidHarvest = false;
        this.steroidLoadedShot = false;
        this.steroidHarvestLevel = 0;
        this.abstinenceTicks = 0;
        this.abstinenceStage = 0;
        this.seedKeeper = false;
        this.manualHarvestStartY = 0.0D;
        this.weaknessLockTicks = 0;
        this.keeperHoverTicks = 0;
        this.keeperGlideTicks = 0;
        this.unstableBeamTicks = 0;
        this.unstableBeamStage = 0;
        this.lastBurstRollMinute = 0;
    }

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    public int getCapacity() {
        return CAPACITY;
    }

    @Override
    public void setAmount(int amount) {
        if (amount < 0) amount = 0;
        if (amount > CAPACITY) amount = CAPACITY;
        if (amount < this.amount) {
            resetAbstinence();
        }
        this.amount = amount;
    }

    @Override
    public int fill(int amount) {
        if (amount <= 0 || this.amount >= CAPACITY) return 0;

        int accepted = Math.min(amount, CAPACITY - this.amount);
        this.amount += accepted;
        return accepted;
    }

    @Override
    public int drain(int amount) {
        if (amount <= 0 || this.amount <= 0) return 0;

        int drained = Math.min(amount, this.amount);
        if (drained > 0) {
            this.amount -= drained;
            resetAbstinence();
        }
        return drained;
    }

    @Override
    public void clear() {
        this.amount = 0;
        this.tickCounter = 0;
        this.manualHarvesting = false;
        this.manualHarvestTicks = 0;
        this.manualHarvestCooldownTicks = 0;
        this.steroidHarvest = false;
        this.steroidLoadedShot = false;
        this.steroidHarvestLevel = 0;
        this.manualHarvestStartY = 0.0D;
        this.weaknessLockTicks = 0;
        this.keeperHoverTicks = 0;
        this.keeperGlideTicks = 0;
        this.unstableBeamTicks = 0;
        this.unstableBeamStage = 0;
        this.lastBurstRollMinute = 0;
        resetAbstinence();
    }

    @Override
    public int getTickCounter() {
        return tickCounter;
    }

    @Override
    public void setTickCounter(int ticks) {
        this.tickCounter = Math.max(0, ticks);
    }

    @Override
    public void addTickCounter(int ticks) {
        if (ticks > 0) {
            this.tickCounter += ticks;
        }
    }

    @Override
    public boolean isFull() {
        return amount >= CAPACITY;
    }

    @Override
    public boolean isManualHarvesting() {
        return manualHarvesting;
    }

    @Override
    public void setManualHarvesting(boolean manualHarvesting) {
        this.manualHarvesting = manualHarvesting;
    }

    @Override
    public int getManualHarvestTicks() {
        return manualHarvestTicks;
    }

    @Override
    public void setManualHarvestTicks(int ticks) {
        this.manualHarvestTicks = Math.max(0, ticks);
    }

    @Override
    public int getManualHarvestCooldownTicks() {
        return manualHarvestCooldownTicks;
    }

    @Override
    public void setManualHarvestCooldownTicks(int ticks) {
        this.manualHarvestCooldownTicks = Math.max(0, ticks);
    }

    @Override
    public boolean isSteroidHarvest() {
        return steroidHarvest;
    }

    @Override
    public void setSteroidHarvest(boolean steroidHarvest) {
        this.steroidHarvest = steroidHarvest;
    }

    @Override
    public boolean isSteroidLoadedShot() {
        return steroidLoadedShot;
    }

    @Override
    public void setSteroidLoadedShot(boolean steroidLoadedShot) {
        this.steroidLoadedShot = steroidLoadedShot;
    }

    @Override
    public int getSteroidHarvestLevel() {
        return steroidHarvestLevel;
    }

    @Override
    public void setSteroidHarvestLevel(int level) {
        this.steroidHarvestLevel = Math.max(0, Math.min(2, level));
    }

    @Override
    public int getAbstinenceTicks() {
        return abstinenceTicks;
    }

    @Override
    public void setAbstinenceTicks(int ticks) {
        this.abstinenceTicks = Math.max(0, ticks);
    }

    @Override
    public int getAbstinenceStage() {
        return abstinenceStage;
    }

    @Override
    public void setAbstinenceStage(int stage) {
        this.abstinenceStage = Math.max(0, Math.min(4, stage));
    }

    @Override
    public boolean hasSeedKeeper() {
        return seedKeeper;
    }

    @Override
    public void setSeedKeeper(boolean seedKeeper) {
        this.seedKeeper = seedKeeper;
    }

    @Override
    public double getManualHarvestStartY() {
        return manualHarvestStartY;
    }

    @Override
    public void setManualHarvestStartY(double y) {
        this.manualHarvestStartY = y;
    }

    @Override
    public int getWeaknessLockTicks() {
        return weaknessLockTicks;
    }

    @Override
    public void setWeaknessLockTicks(int ticks) {
        this.weaknessLockTicks = Math.max(0, ticks);
    }


    @Override
    public int getKeeperHoverTicks() {
        return keeperHoverTicks;
    }

    @Override
    public void setKeeperHoverTicks(int ticks) {
        this.keeperHoverTicks = Math.max(0, ticks);
    }

    @Override
    public int getKeeperGlideTicks() {
        return keeperGlideTicks;
    }

    @Override
    public void setKeeperGlideTicks(int ticks) {
        this.keeperGlideTicks = Math.max(0, ticks);
    }

    @Override
    public int getUnstableBeamTicks() {
        return unstableBeamTicks;
    }

    @Override
    public void setUnstableBeamTicks(int ticks) {
        this.unstableBeamTicks = Math.max(0, ticks);
    }


    @Override
    public int getUnstableBeamStage() {
        return unstableBeamStage;
    }

    @Override
    public void setUnstableBeamStage(int stage) {
        this.unstableBeamStage = Math.max(0, Math.min(4, stage));
    }

    @Override
    public int getLastBurstRollMinute() {
        return lastBurstRollMinute;
    }

    @Override
    public void setLastBurstRollMinute(int minute) {
        this.lastBurstRollMinute = Math.max(0, minute);
    }

    @Override
    public void resetAbstinence() {
        this.abstinenceTicks = 0;
        this.abstinenceStage = 0;
        this.seedKeeper = false;
        this.lastBurstRollMinute = 0;
    }
}
