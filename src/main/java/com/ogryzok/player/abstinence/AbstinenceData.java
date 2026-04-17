package com.ogryzok.player.abstinence;

public final class AbstinenceData {
    public static final int TICKS_PER_DAY = 24000;
    public static final int STAGE_1_TICKS = 3 * TICKS_PER_DAY;
    public static final int STAGE_2_TICKS = 6 * TICKS_PER_DAY;
    public static final int STAGE_3_TICKS = 9 * TICKS_PER_DAY;
    public static final int KEEPER_TICKS = 12 * TICKS_PER_DAY;

    private AbstinenceData() {
    }

    public static int getStageForTicks(int ticks) {
        if (ticks >= KEEPER_TICKS) return 4;
        if (ticks >= STAGE_3_TICKS) return 3;
        if (ticks >= STAGE_2_TICKS) return 2;
        if (ticks >= STAGE_1_TICKS) return 1;
        return 0;
    }

    public static int getThresholdForStage(int stage) {
        switch (stage) {
            case 1:
                return STAGE_1_TICKS;
            case 2:
                return STAGE_2_TICKS;
            case 3:
                return STAGE_3_TICKS;
            case 4:
                return KEEPER_TICKS;
            default:
                return 0;
        }
    }

    public static int getExtraHeartPairs(int stage) {
        if (stage >= 4) {
            return 5;
        }
        return Math.max(0, Math.min(stage, 3));
    }

    public static boolean isKeeper(int stage) {
        return stage >= 4;
    }
}
