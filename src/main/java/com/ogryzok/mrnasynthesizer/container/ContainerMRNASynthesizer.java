package com.ogryzok.mrnasynthesizer.container;

import com.ogryzok.disease.DiseaseRegistry;
import com.ogryzok.food.FoodRegistry;
import com.ogryzok.mrnasynthesizer.tile.TileMRNASynthesizer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerMRNASynthesizer extends Container {
    private final TileMRNASynthesizer tile;

    public ContainerMRNASynthesizer(InventoryPlayer playerInventory, TileMRNASynthesizer tile) {
        this.tile = tile;
        IItemHandler itemHandler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);

        final int fillerSlotX = 40;
        final int antigenSlotX = 118;

        addSlotToContainer(new SlotItemHandler(itemHandler, TileMRNASynthesizer.SLOT_FILLER_TOP, fillerSlotX, 35) {
            @Override public boolean isItemValid(ItemStack stack) { return !stack.isEmpty() && stack.getItem() == FoodRegistry.EVAPORATED_BIOMASS; }
        });
        addSlotToContainer(new SlotItemHandler(itemHandler, TileMRNASynthesizer.SLOT_FILLER_BOTTOM, fillerSlotX, 62) {
            @Override public boolean isItemValid(ItemStack stack) { return !stack.isEmpty() && stack.getItem() == FoodRegistry.EVAPORATED_BIOMASS; }
        });
        addSlotToContainer(new SlotItemHandler(itemHandler, TileMRNASynthesizer.SLOT_CATALYST_TOP, antigenSlotX, 35) {
            @Override public boolean isItemValid(ItemStack stack) { return !stack.isEmpty() && tile.isValidAntigen(stack); }
        });
        addSlotToContainer(new SlotItemHandler(itemHandler, TileMRNASynthesizer.SLOT_CATALYST_BOTTOM, antigenSlotX, 62) {
            @Override public boolean isItemValid(ItemStack stack) { return !stack.isEmpty() && tile.isValidAntigen(stack); }
        });

        for (int i = 0; i < 4; i++) {
            final int idx = TileMRNASynthesizer.SLOT_SYRINGE_IN_0 + i;
            addSlotToContainer(new SlotItemHandler(itemHandler, idx, 37 + i * 28, 119) {
                @Override public boolean isItemValid(ItemStack stack) { return !stack.isEmpty() && stack.getItem() == DiseaseRegistry.SYRINGE; }
                @Override public int getSlotStackLimit() { return 1; }
            });
        }

        for (int i = 0; i < 4; i++) {
            final int idx = TileMRNASynthesizer.SLOT_VACCINE_OUT_0 + i;
            addSlotToContainer(new SlotItemHandler(itemHandler, idx, 37 + i * 28, 143) {
                @Override public boolean isItemValid(ItemStack stack) { return false; }
            });
        }

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 176 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlotToContainer(new Slot(playerInventory, col, 8 + col * 18, 234));
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

            if (index < 12) {
                if (!this.mergeItemStack(stackInSlot, 12, this.inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (stackInSlot.getItem() == FoodRegistry.EVAPORATED_BIOMASS) {
                    if (!this.mergeItemStack(stackInSlot, 0, 2, false)) return ItemStack.EMPTY;
                } else if (tile.isValidAntigen(stackInSlot)) {
                    if (!this.mergeItemStack(stackInSlot, 2, 4, false)) return ItemStack.EMPTY;
                } else if (stackInSlot.getItem() == DiseaseRegistry.SYRINGE) {
                    if (!this.mergeItemStack(stackInSlot, 4, 8, false)) return ItemStack.EMPTY;
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
