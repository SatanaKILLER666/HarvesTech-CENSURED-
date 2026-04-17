package com.ogryzok.lifecrusher.container;

import com.ogryzok.lifecrusher.tile.TileLifeCrusher;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;

public class ContainerLifeCrusher extends Container {
    private final TileLifeCrusher tile;

    public ContainerLifeCrusher(InventoryPlayer playerInventory, TileLifeCrusher tile) {
        this.tile = tile;

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; ++col) {
            addSlotToContainer(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return tile != null && !tile.isInvalid() && playerIn.getDistanceSq(tile.getPos()) <= 64.0D;
    }
}
