package com.ogryzok.player.semen;

public interface ISemenStorage {
    int getAmount();
    int getCapacity();

    void setAmount(int amount);
    int fill(int amount);
    int drain(int amount);
    void clear();

    int getTickCounter();
    void setTickCounter(int ticks);
    void addTickCounter(int ticks);

    boolean isFull();

    boolean isManualHarvesting();
    void setManualHarvesting(boolean manualHarvesting);

    int getManualHarvestTicks();
    void setManualHarvestTicks(int ticks);

    int getManualHarvestCooldownTicks();
    void setManualHarvestCooldownTicks(int ticks);

    boolean isSteroidHarvest();
    void setSteroidHarvest(boolean steroidHarvest);

    boolean isSteroidLoadedShot();
    void setSteroidLoadedShot(boolean steroidLoadedShot);

    int getSteroidHarvestLevel();
    void setSteroidHarvestLevel(int level);

    int getAbstinenceTicks();
    void setAbstinenceTicks(int ticks);

    int getAbstinenceStage();
    void setAbstinenceStage(int stage);

    boolean hasSeedKeeper();
    void setSeedKeeper(boolean seedKeeper);

    double getManualHarvestStartY();
    void setManualHarvestStartY(double y);

    int getWeaknessLockTicks();
    void setWeaknessLockTicks(int ticks);

    int getKeeperHoverTicks();
    void setKeeperHoverTicks(int ticks);

    int getKeeperGlideTicks();
    void setKeeperGlideTicks(int ticks);

    int getUnstableBeamTicks();
    void setUnstableBeamTicks(int ticks);

    int getUnstableBeamStage();
    void setUnstableBeamStage(int stage);

    int getLastBurstRollMinute();
    void setLastBurstRollMinute(int minute);

    void resetAbstinence();
}
