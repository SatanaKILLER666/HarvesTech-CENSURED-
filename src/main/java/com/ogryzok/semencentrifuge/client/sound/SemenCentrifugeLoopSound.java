package com.ogryzok.semencentrifuge.client.sound;

import com.ogryzok.semencentrifuge.tile.TileSemenCentrifugeBase;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;

public class SemenCentrifugeLoopSound extends MovingSound {
    private final TileSemenCentrifugeBase tile;

    public SemenCentrifugeLoopSound(SoundEvent sound, TileSemenCentrifugeBase tile) {
        super(sound, SoundCategory.BLOCKS);
        this.tile = tile;
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = 1.0F;
        this.pitch = 1.0F;
        this.attenuationType = AttenuationType.LINEAR;
        updatePosition();
    }

    public void stopLoop() {
        this.donePlaying = true;
    }

    @Override
    public void update() {
        if (this.tile == null || this.tile.isInvalid() || this.tile.getWorld() == null) {
            this.donePlaying = true;
            return;
        }
        updatePosition();
    }

    private void updatePosition() {
        this.xPosF = this.tile.getPos().getX() + 0.5F;
        this.yPosF = this.tile.getPos().getY() + 1.0F;
        this.zPosF = this.tile.getPos().getZ() + 0.5F;
    }
}
