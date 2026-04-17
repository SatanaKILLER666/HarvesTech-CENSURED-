package com.ogryzok.semencentrifuge.container;

import com.ogryzok.food.FoodRegistry;
import com.ogryzok.semencentrifuge.tile.TileSemenCentrifugeBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerSemenCentrifugeBase extends net.minecraft.inventory.Container {
    private final TileSemenCentrifugeBase tile;

    public ContainerSemenCentrifugeBase(InventoryPlayer playerInv, TileSemenCentrifugeBase tile) {
        this.tile = tile;
        this.addSlotToContainer(new SlotItemHandler(tile.getInventory(), 0, 128, 25) {
            @Override public boolean isItemValid(ItemStack stack) { return FoodRegistry.isEmptyBiomassContainer(stack); }
        });
        this.addSlotToContainer(new SlotItemHandler(tile.getInventory(), 1, 128, 50) {
            @Override public boolean isItemValid(ItemStack stack) { return false; }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlotToContainer(new net.minecraft.inventory.Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 93 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlotToContainer(new net.minecraft.inventory.Slot(playerInv, col, 8 + col * 18, 151));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return tile != null && !tile.isInvalid() && tile.isAssembled() && playerIn.getDistanceSq(tile.getPos()) <= 64.0D;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack empty = ItemStack.EMPTY;
        net.minecraft.inventory.Slot slot = this.inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return empty;

        ItemStack stack = slot.getStack();
        ItemStack copy = stack.copy();

        if (index < 2) {
            if (!this.mergeItemStack(stack, 2, this.inventorySlots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (FoodRegistry.isEmptyBiomassContainer(stack)) {
                if (!this.mergeItemStack(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY);
        else slot.onSlotChanged();
        return copy;
    }
}
