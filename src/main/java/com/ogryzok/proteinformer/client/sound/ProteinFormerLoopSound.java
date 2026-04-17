package com.ogryzok.proteinformer.client.sound;

import com.ogryzok.proteinformer.tile.TileProteinFormer;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;

public class ProteinFormerLoopSound extends MovingSound {
    private final TileProteinFormer tile;

    public ProteinFormerLoopSound(SoundEvent sound, TileProteinFormer tile) {
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
        if (tile == null || tile.isInvalid() || tile.getWorld() == null || !tile.isCraftingNow()) {
            this.donePlaying = true;
            return;
        }

        updatePosition();
    }

    private void updatePosition() {
        this.xPosF = this.tile.getPos().getX() + 0.5F;
        this.yPosF = this.tile.getPos().getY() + 0.85F;
        this.zPosF = this.tile.getPos().getZ() + 0.5F;
    }
}
