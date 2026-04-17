package com.ogryzok.semenenrichment.container;

import com.ogryzok.food.FoodRegistry;
import com.ogryzok.semenenrichment.tile.TileSemenEnrichmentChamber;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerSemenEnrichmentChamber extends Container {
    private final TileSemenEnrichmentChamber tile;

    public ContainerSemenEnrichmentChamber(InventoryPlayer playerInventory, TileSemenEnrichmentChamber tile) {
        this.tile = tile;

        IItemHandler itemHandler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);

        // реальные слоты подогнаны под вертикальные рисованные
        this.addSlotToContainer(new SlotItemHandler(itemHandler, 0, 131, 57) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return FoodRegistry.isEmptyBiomassContainer(stack);
            }
        });

        this.addSlotToContainer(new SlotItemHandler(itemHandler, 1, 131, 79) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return false;
            }
        });

        // инвентарь игрока
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 108 + row * 18));
            }
        }

        // хотбар
        for (int col = 0; col < 9; ++col) {
            this.addSlotToContainer(new Slot(playerInventory, col, 8 + col * 18, 166));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return !playerIn.isDead && playerIn.getDistanceSq(tile.getPos()) <= 64.0D;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack stackInSlot = slot.getStack();
            itemstack = stackInSlot.copy();

            if (index < 2) {
                if (!this.mergeItemStack(stackInSlot, 2, this.inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (FoodRegistry.isEmptyBiomassContainer(stackInSlot)) {
                    if (!this.mergeItemStack(stackInSlot, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }

        return itemstack;
    }
}