package com.ogryzok.manualharvest.client.sound;

import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;

public class ManualHarvestPlayerLoopSound extends MovingSound {
    private final EntityPlayer player;
    private final java.util.function.Predicate<EntityPlayer> keepPlaying;

    public ManualHarvestPlayerLoopSound(SoundEvent sound, EntityPlayer player, java.util.function.Predicate<EntityPlayer> keepPlaying) {
        super(sound, SoundCategory.PLAYERS);
        this.player = player;
        this.keepPlaying = keepPlaying;
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
        if (player == null || player.isDead || keepPlaying == null || !keepPlaying.test(player)) {
            this.donePlaying = true;
            return;
        }
        updatePosition();
    }

    private void updatePosition() {
        this.xPosF = (float) player.posX;
        this.yPosF = (float) (player.posY + player.height * 0.6D);
        this.zPosF = (float) player.posZ;
    }
}
