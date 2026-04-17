package com.ogryzok.proteinformer.tile;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ogryzok.harvestech;
import com.ogryzok.food.FoodRegistry;
import com.ogryzok.proteinformer.ProteinFormerRegistry;
import com.ogryzok.proteinformer.block.BlockProteinFormer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class TileProteinFormer extends TileEntity implements ITickable, IEnergyStorage, IAnimatable {
    private static final int MAX_ENERGY = 100000;
    private static final int ENERGY_PER_STEAK = 1000;
    private static final int OUTPUT_LIMIT = 64;
    private static final int DEFAULT_PROCESS_TIME_TICKS = 20;
    private static final int AUTO_TRANSFER_COOLDOWN = 8;
    private static final int PULL_COOLDOWN = 8;
    private static final int PROCESS_TIME_TICKS = resolveProcessTimeTicks();
    private static final String ANIMATION_NAME = resolveAnimationName();

    private final AnimationFactory factory = new AnimationFactory(this);
    private String lastAnimationKey = "";

    private int energyStored = 0;
    private boolean active = false;
    private int processTime = 0;
    private int processEnergyUsed = 0;
    private int transferCooldown = 0;
    private int pullCooldown = 0;
    private int animationCycle = 0;

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            markForSync();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) {
                return !stack.isEmpty() && stack.getItem() == FoodRegistry.PROTEIN_BIOMASS;
            }
            return false;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 1 ? OUTPUT_LIMIT : 64;
        }
    };

    private final IItemHandler leftInputHandler = new IItemHandler() {
        @Override public int getSlots() { return 1; }
        @Override public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(0); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return inventory.insertItem(0, stack, simulate); }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(0); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return inventory.isItemValid(0, stack); }
    };

    private final IItemHandler rightOutputHandler = new IItemHandler() {
        @Override public int getSlots() { return 1; }
        @Override public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(1); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return inventory.extractItem(1, amount, simulate); }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(1); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return false; }
    };

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        if (pullCooldown-- <= 0) {
            pullCooldown = PULL_COOLDOWN;
            pullInputFromLeft();
        }

        boolean worked = tickProcessing();

        if (transferCooldown-- <= 0) {
            transferCooldown = AUTO_TRANSFER_COOLDOWN;
            pushOutputRight();
        }

        if (active != worked) {
            active = worked;
            markForSync();
        }
    }

    private boolean tickProcessing() {
        ItemStack input = inventory.getStackInSlot(0);
        ItemStack output = inventory.getStackInSlot(1);
        boolean canOutput = output.isEmpty() || (output.getItem() == FoodRegistry.PROTEIN_STEAK && output.getCount() < Math.min(output.getMaxStackSize(), OUTPUT_LIMIT));
        boolean canProcess = !input.isEmpty()
                && input.getItem() == FoodRegistry.PROTEIN_BIOMASS
                && energyStored >= (ENERGY_PER_STEAK - processEnergyUsed)
                && canOutput;

        if (!canProcess) {
            if (processTime <= 0) {
                processEnergyUsed = 0;
            }
            return false;
        }

        processTime++;

        int targetEnergy = processTime * ENERGY_PER_STEAK / PROCESS_TIME_TICKS;
        int deltaEnergy = targetEnergy - processEnergyUsed;
        if (deltaEnergy > 0) {
            energyStored -= deltaEnergy;
            if (energyStored < 0) energyStored = 0;
            processEnergyUsed += deltaEnergy;
            markForSync();
        }

        if (processTime >= PROCESS_TIME_TICKS) {
            processTime = 0;
            processEnergyUsed = 0;
            animationCycle++;
            inventory.extractItem(0, 1, false);
            inventory.insertItem(1, new ItemStack(FoodRegistry.PROTEIN_STEAK), false);
            markForSync();
        }

        return true;
    }

    private void pullInputFromLeft() {
        if (world == null || pos == null) return;
        ItemStack current = inventory.getStackInSlot(0);
        if (!current.isEmpty() && current.getCount() >= current.getMaxStackSize()) return;

        EnumFacing in = getFacing().rotateY();
        TileEntity targetTile = world.getTileEntity(pos.offset(in));
        if (targetTile == null || !targetTile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, in.getOpposite())) return;

        IItemHandler target = targetTile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, in.getOpposite());
        if (target == null) return;

        for (int i = 0; i < target.getSlots(); i++) {
            ItemStack simulatedExtract = target.extractItem(i, 1, true);
            if (simulatedExtract.isEmpty() || simulatedExtract.getItem() != FoodRegistry.PROTEIN_BIOMASS) continue;
            ItemStack remainder = inventory.insertItem(0, simulatedExtract.copy(), true);
            if (!remainder.isEmpty()) continue;

            ItemStack extracted = target.extractItem(i, 1, false);
            if (extracted.isEmpty()) return;
            inventory.insertItem(0, extracted, false);
            markForSync();
            return;
        }
    }

    private void pushOutputRight() {
        if (world == null || pos == null) return;
        ItemStack output = inventory.getStackInSlot(1);
        if (output.isEmpty()) return;

        EnumFacing out = getFacing().rotateYCCW();
        TileEntity targetTile = world.getTileEntity(pos.offset(out));
        if (targetTile == null || !targetTile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, out.getOpposite())) return;

        IItemHandler target = targetTile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, out.getOpposite());
        if (target == null) return;

        ItemStack stackToMove = output.copy();
        int original = stackToMove.getCount();
        for (int i = 0; i < target.getSlots() && !stackToMove.isEmpty(); i++) {
            stackToMove = target.insertItem(i, stackToMove, false);
        }

        int moved = original - stackToMove.getCount();
        if (moved > 0) {
            inventory.extractItem(1, moved, false);
            markForSync();
        }
    }

    public EnumFacing getFacing() {
        if (world == null || pos == null) return EnumFacing.NORTH;
        IBlockState state = world.getBlockState(pos);
        return state.getBlock() == ProteinFormerRegistry.PROTEIN_FORMER ? state.getValue(BlockProteinFormer.FACING) : EnumFacing.NORTH;
    }

    public ItemStack getStackInSlot(int slot) {
        return inventory.getStackInSlot(slot);
    }

    public int getProcessProgressScaled(int pixels) {
        return PROCESS_TIME_TICKS <= 0 || processTime <= 0 ? 0 : Math.min(pixels, processTime * pixels / PROCESS_TIME_TICKS);
    }

    public boolean isCraftingNow() {
        return active;
    }

    public void markForSync() {
        if (world == null || world.isRemote) return;
        markDirty();
        IBlockState state = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, state, state, 3);
    }

    private boolean canExposeEnergyTo(@Nullable EnumFacing facing) {
        return facing == null || facing == EnumFacing.DOWN || facing == getFacing().getOpposite();
    }

    private boolean canInsertInputFrom(@Nullable EnumFacing facing) {
        return facing == null || facing == getFacing().rotateY();
    }

    private boolean canExtractOutputTo(@Nullable EnumFacing facing) {
        return facing == null || facing == getFacing().rotateYCCW();
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) return canExposeEnergyTo(facing);
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return facing == null || canInsertInputFrom(facing) || canExtractOutputTo(facing);
        }
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return canExposeEnergyTo(facing) ? CapabilityEnergy.ENERGY.cast(this) : null;
        }
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            if (facing == null) return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
            if (canInsertInputFrom(facing)) return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(leftInputHandler);
            if (canExtractOutputTo(facing)) return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(rightOutputHandler);
            return null;
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("Energy", energyStored);
        compound.setBoolean("Active", active);
        compound.setTag("Items", inventory.serializeNBT());
        compound.setInteger("ProcessTime", processTime);
        compound.setInteger("ProcessEnergyUsed", processEnergyUsed);
        compound.setInteger("AnimationCycle", animationCycle);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        energyStored = compound.getInteger("Energy");
        active = compound.getBoolean("Active");
        inventory.deserializeNBT(compound.getCompoundTag("Items"));
        processTime = compound.getInteger("ProcessTime");
        processEnergyUsed = compound.getInteger("ProcessEnergyUsed");
        animationCycle = compound.getInteger("AnimationCycle");
    }

    @Override public NBTTagCompound getUpdateTag() { return writeToNBT(new NBTTagCompound()); }
    @Nullable @Override public SPacketUpdateTileEntity getUpdatePacket() { return new SPacketUpdateTileEntity(this.pos, 0, getUpdateTag()); }
    @Override public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) { readFromNBT(pkt.getNbtCompound()); }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = Math.min(MAX_ENERGY - energyStored, Math.max(0, maxReceive));
        if (!simulate && received > 0) {
            energyStored += received;
            markForSync();
        }
        return received;
    }

    @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
    @Override public int getEnergyStored() { return energyStored; }
    @Override public int getMaxEnergyStored() { return MAX_ENERGY; }
    @Override public boolean canExtract() { return false; }
    @Override public boolean canReceive() { return true; }

    private <E extends IAnimatable> PlayState animationPredicate(AnimationEvent<E> event) {
        AnimationController<?> controller = event.getController();
        String animationKey = active ? (ANIMATION_NAME + "#" + animationCycle) : "idle";

        if (!animationKey.equals(lastAnimationKey)) {
            controller.markNeedsReload();
            lastAnimationKey = animationKey;
        }

        if (!active) {
            controller.setAnimationSpeed(0.0D);
            return PlayState.STOP;
        }

        controller.setAnimation(new AnimationBuilder().addAnimation(ANIMATION_NAME, false));
        controller.setAnimationSpeed(1.0D);
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimationData animationData) {
        animationData.addAnimationController(new AnimationController<TileProteinFormer>(this, "controller", 0.0f, this::animationPredicate));
    }

    @Override public AnimationFactory getFactory() { return factory; }

    private static int resolveProcessTimeTicks() {
        try {
            JsonObject animation = loadFirstAnimationObject();
            if (animation != null && animation.has("animation_length")) {
                double seconds = animation.get("animation_length").getAsDouble();
                return Math.max(1, (int) Math.round(seconds * 20.0D));
            }
        } catch (Exception ignored) {
        }
        return DEFAULT_PROCESS_TIME_TICKS;
    }

    private static String resolveAnimationName() {
        try {
            JsonObject root = loadAnimationRoot();
            if (root != null && root.has("animations")) {
                JsonObject animations = root.getAsJsonObject("animations");
                for (java.util.Map.Entry<String, JsonElement> entry : animations.entrySet()) {
                    return entry.getKey();
                }
            }
        } catch (Exception ignored) {
        }
        return "protein_former_work";
    }

    @Nullable
    private static JsonObject loadFirstAnimationObject() {
        JsonObject root = loadAnimationRoot();
        if (root == null || !root.has("animations")) return null;
        JsonObject animations = root.getAsJsonObject("animations");
        for (java.util.Map.Entry<String, JsonElement> entry : animations.entrySet()) {
            return entry.getValue().getAsJsonObject();
        }
        return null;
    }

    @Nullable
    private static JsonObject loadAnimationRoot() {
        ResourceLocation location = new ResourceLocation(harvestech.MODID, "animations/protein_former.animation.json");
        String path = "/assets/" + location.getNamespace() + "/" + location.getPath();
        try (InputStream stream = TileProteinFormer.class.getResourceAsStream(path)) {
            if (stream == null) return null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                return new JsonParser().parse(reader).getAsJsonObject();
            }
        } catch (Exception ignored) {
            return null;
        }
    }
}
