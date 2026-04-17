package com.ogryzok.semencentrifuge.container;

import com.ogryzok.semencentrifuge.tile.TileSemenCentrifugeBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

public class ContainerSemenCentrifugeMotor extends Container {
    private final TileSemenCentrifugeBase tile;

    public ContainerSemenCentrifugeMotor(TileSemenCentrifugeBase tile) {
        this.tile = tile;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return tile != null
                && !tile.isInvalid()
                && tile.isAssembled()
                && tile.canPlayerUseMotorGui(playerIn)
                && playerIn.getDistanceSq(tile.getPos()) <= 64.0D;
    }
}
