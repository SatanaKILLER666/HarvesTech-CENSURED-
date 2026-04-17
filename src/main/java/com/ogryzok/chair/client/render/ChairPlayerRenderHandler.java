package com.ogryzok.chair.client.render;

import com.ogryzok.chair.entity.EntitySeat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ChairPlayerRenderHandler {
    private static final float HEAD_Y_OFFSET = 1.495F;
    private static final float EAST_WEST_HEAD_BACK_OFFSET = 0.125F;

    @SubscribeEvent
    public void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        EntityPlayer player = event.getEntityPlayer();
        if (!(player.getRidingEntity() instanceof EntitySeat)) {
            return;
        }

        ModelPlayer model = event.getRenderer().getMainModel();
        ModelVisibilityState state = new ModelVisibilityState(model);

        try {
            setHeadOnly(model);
            renderHeadOnly(event, player, model);
            event.setCanceled(true);
        } finally {
            state.apply(model);
        }
    }

    private void setHeadOnly(ModelPlayer model) {
        model.bipedHead.showModel = true;
        model.bipedHeadwear.showModel = true;

        model.bipedBody.showModel = false;
        model.bipedBodyWear.showModel = false;

        model.bipedLeftArm.showModel = false;
        model.bipedLeftArmwear.showModel = false;
        model.bipedRightArm.showModel = false;
        model.bipedRightArmwear.showModel = false;

        model.bipedLeftLeg.showModel = false;
        model.bipedLeftLegwear.showModel = false;
        model.bipedRightLeg.showModel = false;
        model.bipedRightLegwear.showModel = false;
    }

    private void renderHeadOnly(RenderPlayerEvent.Pre event, EntityPlayer player, ModelPlayer model) {
        GlStateManager.pushMatrix();
        GlStateManager.disableCull();

        double renderX = event.getX();
        double renderY = event.getY() + HEAD_Y_OFFSET;
        double renderZ = event.getZ();

        if (player.getRidingEntity() instanceof EntitySeat) {
            EntitySeat seat = (EntitySeat) player.getRidingEntity();
            EnumFacing facing = seat.getSeatFacing();

            switch (facing) {
                case EAST:
                    renderX -= EAST_WEST_HEAD_BACK_OFFSET;
                    break;
                case WEST:
                    renderX += EAST_WEST_HEAD_BACK_OFFSET;
                    break;
                default:
                    break;
            }
        }

        GlStateManager.translate(renderX, renderY, renderZ);

        float partialTicks = event.getPartialRenderTick();

        float bodyYaw = interpolateRotation(player.prevRenderYawOffset, player.renderYawOffset, partialTicks);
        float headYaw = interpolateRotation(player.prevRotationYawHead, player.rotationYawHead, partialTicks);
        float headPitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;

        // ПОВОРАЧИВАЕМ МАТРИЦУ ПОД ТЕЛО
        // Это главное, чего не хватало для SOUTH/EAST/WEST
        GlStateManager.rotate(180.0F - bodyYaw, 0.0F, 1.0F, 0.0F);

        // Как в ванильном рендере игрока
        GlStateManager.scale(-1.0F, -1.0F, 1.0F);

        Minecraft.getMinecraft().getTextureManager().bindTexture(getSkin(player));

        float netHeadYaw;
        if (ChairCameraHandler.isCustomFrontViewActive(player)) {
            netHeadYaw = 0.0F;
            headPitch = 0.0F;
        } else {
            // Голова относительно тела
            netHeadYaw = wrapDegrees(headYaw - bodyYaw);
        }

        model.swingProgress = 0.0F;
        model.isSneak = false;
        model.isRiding = true;
        model.rightArmPose = ModelBiped.ArmPose.EMPTY;
        model.leftArmPose = ModelBiped.ArmPose.EMPTY;

        model.setLivingAnimations(player, 0.0F, 0.0F, partialTicks);
        model.setRotationAngles(0.0F, 0.0F, 0.0F, netHeadYaw, headPitch, 0.0625F, player);

        if (ChairCameraHandler.isCustomFrontViewActive(player)) {
            model.bipedHead.rotateAngleY = 0.0F;
            model.bipedHeadwear.rotateAngleY = 0.0F;
            model.bipedHead.rotateAngleX = 0.0F;
            model.bipedHeadwear.rotateAngleX = 0.0F;
        } else {
            float yawRad = netHeadYaw * 0.017453292F;
            float pitchRad = headPitch * 0.017453292F;

            model.bipedHead.rotateAngleY = yawRad;
            model.bipedHeadwear.rotateAngleY = yawRad;
            model.bipedHead.rotateAngleX = pitchRad;
            model.bipedHeadwear.rotateAngleX = pitchRad;
        }

        model.bipedHead.render(0.0625F);
        model.bipedHeadwear.render(0.0625F);

        GlStateManager.enableCull();
        GlStateManager.popMatrix();
    }

    private ResourceLocation getSkin(EntityPlayer player) {
        if (player instanceof AbstractClientPlayer) {
            return ((AbstractClientPlayer) player).getLocationSkin();
        }

        return DefaultPlayerSkin.getDefaultSkin(player.getUniqueID());
    }

    private float interpolateRotation(float prev, float current, float partialTicks) {
        float delta;
        for (delta = current - prev; delta < -180.0F; delta += 360.0F) {
        }

        while (delta >= 180.0F) {
            delta -= 360.0F;
        }

        return prev + partialTicks * delta;
    }

    private float wrapDegrees(float value) {
        value = value % 360.0F;

        if (value >= 180.0F) {
            value -= 360.0F;
        }

        if (value < -180.0F) {
            value += 360.0F;
        }

        return value;
    }

    private static final class ModelVisibilityState {
        private final boolean head;
        private final boolean headwear;
        private final boolean body;
        private final boolean bodyWear;
        private final boolean leftArm;
        private final boolean leftArmWear;
        private final boolean rightArm;
        private final boolean rightArmWear;
        private final boolean leftLeg;
        private final boolean leftLegWear;
        private final boolean rightLeg;
        private final boolean rightLegWear;

        private ModelVisibilityState(ModelPlayer model) {
            this.head = model.bipedHead.showModel;
            this.headwear = model.bipedHeadwear.showModel;
            this.body = model.bipedBody.showModel;
            this.bodyWear = model.bipedBodyWear.showModel;
            this.leftArm = model.bipedLeftArm.showModel;
            this.leftArmWear = model.bipedLeftArmwear.showModel;
            this.rightArm = model.bipedRightArm.showModel;
            this.rightArmWear = model.bipedRightArmwear.showModel;
            this.leftLeg = model.bipedLeftLeg.showModel;
            this.leftLegWear = model.bipedLeftLegwear.showModel;
            this.rightLeg = model.bipedRightLeg.showModel;
            this.rightLegWear = model.bipedRightLegwear.showModel;
        }

        private void apply(ModelPlayer model) {
            model.bipedHead.showModel = head;
            model.bipedHeadwear.showModel = headwear;
            model.bipedBody.showModel = body;
            model.bipedBodyWear.showModel = bodyWear;
            model.bipedLeftArm.showModel = leftArm;
            model.bipedLeftArmwear.showModel = leftArmWear;
            model.bipedRightArm.showModel = rightArm;
            model.bipedRightArmwear.showModel = rightArmWear;
            model.bipedLeftLeg.showModel = leftLeg;
            model.bipedLeftLegwear.showModel = leftLegWear;
            model.bipedRightLeg.showModel = rightLeg;
            model.bipedRightLegwear.showModel = rightLegWear;
        }
    }
}