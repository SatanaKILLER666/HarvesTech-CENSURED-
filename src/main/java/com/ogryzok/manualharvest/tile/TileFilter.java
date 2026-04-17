package com.ogryzok.manualharvest.tile;

import com.ogryzok.food.FoodRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

public class TileFilter extends TileEntity implements ITickable, IAnimatable {
    public static final int MAX_SAND = 64;
    public static final int MAX_STAGE = 5;
    private static final int FIRST_STAGE_DELAY_TICKS = 40;
    private static final int ANIMATION_LOOP_TICKS = 148;
    private static final int FIRST_STAGE_LOOPS = 4;
    private static final int NEXT_STAGE_LOOPS = 3;

    private final AnimationFactory factory = new AnimationFactory(this);
    private boolean assembled;
    private String lastAnimationKey = "";

    private int sandCount = 0;
    private int sandUses = 0;
    private int stage = 0;
    private boolean processing = false;
    private int processTicks = 0;
    private int processDuration = 0;

    public TileFilter() {
        this(true);
    }

    public TileFilter(boolean assembled) {
        this.assembled = assembled;
    }

    public boolean isAssembled() {
        return assembled;
    }

    public int getSandCount() {
        return sandCount;
    }

    public int getStage() {
        return stage;
    }

    public boolean isProcessing() {
        return processing;
    }

    public boolean hasSand() {
        return sandCount > 0;
    }

    public boolean shouldShowFirstFilledModel() {
        return processing && stage == 0 && processTicks >= FIRST_STAGE_DELAY_TICKS;
    }

    public boolean tryAddSand(EntityPlayer player, EnumHand hand, ItemStack held) {
        if (processing) {
            sendActionbar(player, "Filtering in progress");
            return true;
        }

        if (sandCount >= MAX_SAND) {
            sendActionbar(player, sandCount + "/" + MAX_SAND);
            return true;
        }

        if (!player.capabilities.isCreativeMode) {
            held.shrink(1);
        }
        sandCount++;
        playSandPlaceSound();
        sync();
        sendActionbar(player, sandCount + "/" + MAX_SAND);
        return true;
    }

    public boolean tryStartFiltering(EntityPlayer player, EnumHand hand, ItemStack held) {
        if (processing) {
            sendActionbar(player, "Filtering in progress");
            return true;
        }

        if (stage >= MAX_STAGE) {
            sendActionbar(player, "Filter is full");
            return true;
        }

        if (sandCount <= 0) {
            sendActionbar(player, "No sand");
            return true;
        }

        ItemStack emptyContainerBack = FoodRegistry.getEmptyContainer(held);

        if (!player.capabilities.isCreativeMode) {
            consumeHeldAndGiveBackToSameHand(player, hand, held, emptyContainerBack);
        }
        processing = true;
        processTicks = 0;
        processDuration = stage == 0
                ? FIRST_STAGE_DELAY_TICKS + (FIRST_STAGE_LOOPS * ANIMATION_LOOP_TICKS)
                : (NEXT_STAGE_LOOPS * ANIMATION_LOOP_TICKS);
        sync();
        playFilterSound();
        return true;
    }

    public boolean tryExtractBiomass(EntityPlayer player, EnumHand hand, ItemStack held) {
        if (processing) {
            sendActionbar(player, "Filtering in progress");
            return true;
        }

        if (stage <= 0) {
            return false;
        }

        ItemStack result = FoodRegistry.getFilledBiomassContainer(held);
        if (result.isEmpty()) {
            return false;
        }

        if (!player.capabilities.isCreativeMode) {
            consumeHeldAndGiveBackToSameHand(player, hand, held, result);
        }

        stage--;
        sync();
        return true;
    }

    @Override
    public void update() {
        if (world == null || world.isRemote || !processing) {
            return;
        }

        processTicks++;
        if (processTicks == FIRST_STAGE_DELAY_TICKS && stage == 0) {
            sync();
        }

        if (processTicks >= processDuration) {
            finishProcessing();
        }
    }

    private void finishProcessing() {
        processing = false;
        processTicks = 0;
        processDuration = 0;
        if (stage < MAX_STAGE) {
            stage++;
        }

        sandUses++;
        if (sandUses >= 3) {
            sandUses = 0;
            if (sandCount > 0) {
                sandCount--;
            }
        }

        sync();
    }


    private void consumeHeldAndGiveBackToSameHand(EntityPlayer player, EnumHand hand, ItemStack held, ItemStack returnStack) {
        if (returnStack.isEmpty()) {
            held.shrink(1);
            if (held.getCount() <= 0) {
                player.setHeldItem(hand, ItemStack.EMPTY);
            }
            return;
        }

        if (held.getCount() <= 1) {
            player.setHeldItem(hand, returnStack.copy());
            return;
        }

        held.shrink(1);
        if (!player.inventory.addItemStackToInventory(returnStack.copy())) {
            player.dropItem(returnStack.copy(), false);
        }
    }

    private void playSandPlaceSound() {
        if (world == null || pos == null) {
            return;
        }

        world.playSound(null, pos, SoundEvents.BLOCK_SAND_PLACE, SoundCategory.BLOCKS, 0.9F, 1.0F);
    }

    private void playFilterSound() {
        if (world == null || pos == null) {
            return;
        }

        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(new net.minecraft.util.ResourceLocation("block.water.ambient"));
        if (sound != null) {
            world.playSound(null, pos, sound, SoundCategory.BLOCKS, 0.8F, 1.0F);
        }
    }

    private void sendActionbar(EntityPlayer player, String text) {
        player.sendStatusMessage(new TextComponentString(text), true);
    }

    private void sync() {
        markDirty();
        if (world != null) {
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }
    }

    private int getStageAmountMb() {
        switch (stage) {
            case 1:
                return 100;
            case 2:
                return 300;
            case 3:
                return 500;
            case 4:
                return 700;
            case 5:
                return 1000;
            default:
                return 0;
        }
    }

    public String getCurrentModelKey() {
        if (!assembled) {
            return "filter_base";
        }

        if (processing) {
            if (stage == 0) {
                if (processTicks >= FIRST_STAGE_DELAY_TICKS) {
                    return "filter_100";
                }
                return "filter_fulled_input";
            }

            int amount = getStageAmountMb();
            return "work_" + amount;
        }

        if (stage <= 0) {
            return hasSand() ? "filter_with_sand" : "empty_filter";
        }

        int amount = getStageAmountMb();
        return hasSand() ? ("empty_" + amount) : ("nofilter_" + amount);
    }

    private <E extends IAnimatable> PlayState animationPredicate(AnimationEvent<E> event) {
        AnimationController<?> controller = event.getController();
        String animationKey = processing ? "kapli" : "idle";

        if (!animationKey.equals(lastAnimationKey)) {
            controller.markNeedsReload();
            lastAnimationKey = animationKey;
        }

        if (!processing) {
            controller.setAnimationSpeed(0.0D);
            return PlayState.STOP;
        }

        controller.setAnimation(new AnimationBuilder().addAnimation("kapli", true));
        controller.setAnimationSpeed(1.0D);
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<TileFilter>(this, "controller", 0.0f, this::animationPredicate));
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setBoolean("Assembled", assembled);
        tag.setInteger("SandCount", sandCount);
        tag.setInteger("SandUses", sandUses);
        tag.setInteger("Stage", stage);
        tag.setBoolean("Processing", processing);
        tag.setInteger("ProcessTicks", processTicks);
        tag.setInteger("ProcessDuration", processDuration);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        assembled = !tag.hasKey("Assembled") || tag.getBoolean("Assembled");
        sandCount = tag.getInteger("SandCount");
        sandUses = tag.getInteger("SandUses");
        stage = tag.getInteger("Stage");
        processing = tag.getBoolean("Processing");
        processTicks = tag.getInteger("ProcessTicks");
        processDuration = tag.getInteger("ProcessDuration");
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        readFromNBT(tag);
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        handleUpdateTag(pkt.getNbtCompound());
    }
}
