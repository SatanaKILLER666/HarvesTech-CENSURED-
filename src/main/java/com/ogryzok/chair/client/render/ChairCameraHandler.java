package com.ogryzok.chair.client.render;

import com.ogryzok.chair.entity.EntitySeat;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

@SideOnly(Side.CLIENT)
public class ChairCameraHandler {
    // First person keeps the normal vanilla camera; only a small local offset is applied.
    private static final double HEAD_CAMERA_LOWER_FIRST_PERSON = 0.0D;
    private static final double HEAD_CAMERA_FORWARD_FIRST_PERSON = 0.42D;
    private static final double HEAD_CAMERA_FORWARD_FIRST_PERSON_EAST_WEST = 0.34D;

    // Pitch limits while seated
    private static final float SEATED_PITCH_LIMIT_FIRST_PERSON = 60.0F;
    private static final float SEATED_PITCH_LIMIT_THIRD_BACK = 60.0F;
    private static final float SEATED_PITCH_MIN_THIRD_FRONT = -60.0F;
    private static final float SEATED_PITCH_MAX_THIRD_FRONT = 0.0F;

    public static boolean isCustomFrontViewActive(@Nullable EntityPlayer player) {
        return false;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null || mc.world == null) {
            return;
        }

        if (!(player.getRidingEntity() instanceof EntitySeat)) {
            return;
        }

        float clampedPitch = clampPitchForCurrentView(player.rotationPitch, mc.gameSettings.thirdPersonView);
        float clampedPrevPitch = clampPitchForCurrentView(player.prevRotationPitch, mc.gameSettings.thirdPersonView);

        player.rotationPitch = clampedPitch;
        player.prevRotationPitch = clampedPrevPitch;
    }

    @SubscribeEvent
    public void onCameraSetup(EntityViewRenderEvent.CameraSetup event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null) {
            return;
        }

        if (!(player.getRidingEntity() instanceof EntitySeat)) {
            return;
        }

        // Clamp actual rendered pitch depending on current camera mode.
        float clampedPitch = clampPitchForCurrentView(event.getPitch(), mc.gameSettings.thirdPersonView);
        event.setPitch(clampedPitch);

        if (mc.gameSettings.thirdPersonView == 0) {
            float forwardOffset = (float) HEAD_CAMERA_FORWARD_FIRST_PERSON;

            if (player.getRidingEntity() instanceof EntitySeat) {
                EntitySeat seat = (EntitySeat) player.getRidingEntity();
                switch (seat.getSeatFacing()) {
                    case EAST:
                    case WEST:
                        forwardOffset = (float) HEAD_CAMERA_FORWARD_FIRST_PERSON_EAST_WEST;
                        break;
                    default:
                        break;
                }
            }

            net.minecraft.client.renderer.GlStateManager.translate(
                    0.0F,
                    (float) HEAD_CAMERA_LOWER_FIRST_PERSON,
                    forwardOffset
            );
        }
    }

    @SubscribeEvent
    public void onFovUpdate(FOVUpdateEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null || event.getEntity() != player) {
            return;
        }

        if (player.getRidingEntity() instanceof EntitySeat && mc.gameSettings.thirdPersonView == 0) {
            event.setNewfov(70.0F);
        }
    }

    @SubscribeEvent
    public void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player != null && player.getRidingEntity() instanceof EntitySeat) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player != null
                && player.getRidingEntity() instanceof EntitySeat
                && (event.getType() == RenderGameOverlayEvent.ElementType.HOTBAR
                || event.getType() == RenderGameOverlayEvent.ElementType.HEALTH
                || event.getType() == RenderGameOverlayEvent.ElementType.EXPERIENCE)) {
            event.setCanceled(true);
        }
    }

    private float clampPitchForCurrentView(float pitch, int thirdPersonView) {
        // 0 = first person
        if (thirdPersonView == 0) {
            return clamp(pitch, -SEATED_PITCH_LIMIT_FIRST_PERSON, SEATED_PITCH_LIMIT_FIRST_PERSON);
        }

        // 1 = third person back
        if (thirdPersonView == 1) {
            return clamp(pitch, -SEATED_PITCH_LIMIT_THIRD_BACK, SEATED_PITCH_LIMIT_THIRD_BACK);
        }

        // 2 = third person front
        return clamp(pitch, SEATED_PITCH_MIN_THIRD_FRONT, SEATED_PITCH_MAX_THIRD_FRONT);
    }

    private float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}