package com.ogryzok.manualharvest.client;

import com.ogryzok.harvestech;
import com.ogryzok.manualharvest.client.sound.ManualHarvestSoundController;

import com.ogryzok.manualharvest.client.render.ManualHarvestPelvisModel;
import com.ogryzok.manualharvest.network.PacketStartManualHarvest;
import com.ogryzok.manualharvest.tile.TileRottingTank;
import com.ogryzok.network.ModNetwork;
import com.ogryzok.player.semen.ISemenStorage;
import com.ogryzok.player.semen.SemenProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public class ManualHarvestClientHandler {
    public static final KeyBinding MANUAL_HARVEST_KEY = new KeyBinding("key.harvestech.manual_harvest", Keyboard.KEY_UP, "key.categories.harvestech");
    private static final ResourceLocation PELVIS_TEXTURE = new ResourceLocation(harvestech.MODID, "textures/entity/manual_harvest_pelvis.png");

    private final ModelPlayer steveArm = new ModelPlayer(0.0F, false);
    private final ModelPlayer alexArm = new ModelPlayer(0.0F, true);
    private final ModelPlayer pelvisProxy = new ModelPlayer(0.0F, false);
    private final ManualHarvestPelvisModel pelvisModel = new ManualHarvestPelvisModel();
    private static final float FIRST_PERSON_BODY_FORWARD_OFFSET = 0.22F;
    private static final float FIRST_PERSON_FOV = 52.0F;

    public ManualHarvestClientHandler() {
        ClientRegistry.registerKeyBinding(MANUAL_HARVEST_KEY);
    }



    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        BiomassBeamEffect.tick();
        ManualHarvestSoundController.onClientTick();

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null || mc.currentScreen != null) return;

        RayTraceResult hit = mc.objectMouseOver;
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK || hit.getBlockPos() == null) return;

        TileEntity te = mc.world.getTileEntity(hit.getBlockPos());
        if (te instanceof TileRottingTank) {
            String text = ((TileRottingTank) te).getOverlayText();
            if (text != null && !text.isEmpty()) {
                mc.player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(text), true);
            }
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null || mc.currentScreen != null) return;

        if (MANUAL_HARVEST_KEY.isPressed() && Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
            ModNetwork.CHANNEL.sendToServer(new PacketStartManualHarvest());
        }
    }

    @SubscribeEvent
    public void onMovement(InputUpdateEvent event) {
        ISemenStorage storage = event.getEntityPlayer().getCapability(SemenProvider.SEMEN_CAP, null);
        if (storage != null && storage.isManualHarvesting()) {
            event.getMovementInput().moveForward *= 0.08F;
            event.getMovementInput().moveStrafe *= 0.08F;
        }
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Pre event) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) return;
        ISemenStorage storage = player.getCapability(SemenProvider.SEMEN_CAP, null);
        if (storage != null && storage.isManualHarvesting()) {
            event.setCanceled(true);
        }
    }


    @SubscribeEvent
    public void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (!isActiveFirstPerson(player)) return;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onFovUpdate(FOVUpdateEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null || event.getEntity() != player) return;
        if (!isActiveFirstPerson(player)) return;
        event.setNewfov(FIRST_PERSON_FOV);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (!(player instanceof AbstractClientPlayer)) return;
        if (!isActiveFirstPerson(player)) return;

        AbstractClientPlayer clientPlayer = (AbstractClientPlayer) player;
        double x = interpolate(player.prevPosX, player.posX, event.getPartialTicks()) - mc.getRenderManager().viewerPosX;
        double y = interpolate(player.prevPosY, player.posY, event.getPartialTicks()) - mc.getRenderManager().viewerPosY;
        double z = interpolate(player.prevPosZ, player.posZ, event.getPartialTicks()) - mc.getRenderManager().viewerPosZ;

        renderFirstPersonBody(clientPlayer, x, y, z, event.getPartialTicks());
    }

    @SubscribeEvent
    public void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        EntityPlayer player = event.getEntityPlayer();
        ISemenStorage storage = player.getCapability(SemenProvider.SEMEN_CAP, null);
        if (storage == null || !storage.isManualHarvesting()) return;
        if (!(player instanceof AbstractClientPlayer)) return;
        if (!(event.getRenderer().getMainModel() instanceof ModelPlayer)) return;

        renderPlayerWithoutRightArm((AbstractClientPlayer) player, event.getRenderer(), event.getX(), event.getY(), event.getZ(), event.getPartialRenderTick());
        renderCustomArm((AbstractClientPlayer) player, event.getRenderer(), event.getX(), event.getY(), event.getZ(), event.getPartialRenderTick());
        renderPelvis((AbstractClientPlayer) player, event.getRenderer(), event.getX(), event.getY(), event.getZ(), event.getPartialRenderTick());
        event.setCanceled(true);
    }

    private void renderPlayerWithoutRightArm(AbstractClientPlayer player, RenderPlayer renderer, double x, double y, double z, float partialTicks) {
        ModelPlayer model = (ModelPlayer) renderer.getMainModel();
        ModelVisibilityState state = new ModelVisibilityState(model);

        try {
            model.isChild = false;
            model.bipedRightArm.showModel = false;
            model.bipedRightArmwear.showModel = false;
            model.bipedRightArm.isHidden = true;
            model.bipedRightArmwear.isHidden = true;

            model.swingProgress = 0.0F;
            model.isSneak = player.isSneaking();
            model.isRiding = player.isRiding();
            model.setLivingAnimations(player, 0.0F, 0.0F, partialTicks);
            model.setRotationAngles(0.0F, 0.0F, getAnimationTime(player, partialTicks), getHeadYaw(player, partialTicks), getHeadPitch(player, partialTicks), 0.0625F, player);

            Minecraft.getMinecraft().getTextureManager().bindTexture(getSkin(player));
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, z);
            applyPlayerModelTransforms(player, partialTicks);
            GlStateManager.enableCull();
            model.render(player, 0.0F, 0.0F, getAnimationTime(player, partialTicks), getHeadYaw(player, partialTicks), getHeadPitch(player, partialTicks), 0.0625F);
            GlStateManager.popMatrix();
        } finally {
            state.apply(model);
        }
    }

    private void renderCustomArm(AbstractClientPlayer player, RenderPlayer renderer, double x, double y, double z, float partialTicks) {
        ModelPlayer source = (ModelPlayer) renderer.getMainModel();
        boolean smallArms = "slim".equals(player.getSkinType());
        ModelPlayer armModel = smallArms ? alexArm : steveArm;

        armModel.setModelAttributes(source);
        armModel.isChild = false;
        armModel.isSneak = source.isSneak;

        armModel.isRiding = source.isRiding;
        armModel.swingProgress = source.swingProgress;
        armModel.setLivingAnimations(player, 0.0F, 0.0F, partialTicks);
        armModel.setRotationAngles(0.0F, 0.0F, getAnimationTime(player, partialTicks), getHeadYaw(player, partialTicks), getHeadPitch(player, partialTicks), 0.0625F, player);

        float animAngle = (float) Math.sin((player.ticksExisted + partialTicks) * 0.9F) * 0.35F - 0.45F;

        // Поза руки: вдоль тела, но с анимацией ручного сбора.
        armModel.bipedRightArm.rotateAngleX = 0.22F + animAngle * 0.42F;
        armModel.bipedRightArm.rotateAngleY = -0.06F;
        armModel.bipedRightArm.rotateAngleZ = -0.42F;

        armModel.bipedRightArmwear.rotateAngleX = armModel.bipedRightArm.rotateAngleX;
        armModel.bipedRightArmwear.rotateAngleY = armModel.bipedRightArm.rotateAngleY;
        armModel.bipedRightArmwear.rotateAngleZ = armModel.bipedRightArm.rotateAngleZ;


        armModel.bipedRightArm.rotationPointX = source.bipedRightArm.rotationPointX + 2.1F; //чем больше тем ближе к плечу рука
        armModel.bipedRightArm.rotationPointY = source.bipedRightArm.rotationPointY;
        armModel.bipedRightArm.rotationPointZ = source.bipedRightArm.rotationPointZ - 1.8F; //(чем отрицательнее тем рука более вперед)

        armModel.bipedRightArmwear.rotationPointX = armModel.bipedRightArm.rotationPointX;
        armModel.bipedRightArmwear.rotationPointY = armModel.bipedRightArm.rotationPointY;
        armModel.bipedRightArmwear.rotationPointZ = armModel.bipedRightArm.rotationPointZ;

        Minecraft.getMinecraft().getTextureManager().bindTexture(player.getLocationSkin());
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        applyPlayerModelTransforms(player, partialTicks);
        GlStateManager.enableCull();
        armModel.bipedRightArm.render(0.0625F);
        armModel.bipedRightArmwear.render(0.0625F);
        GlStateManager.popMatrix();
    }

    private void renderPelvis(AbstractClientPlayer player, RenderPlayer renderer, double x, double y, double z, float partialTicks) {
        ModelPlayer source = (ModelPlayer) renderer.getMainModel();
        pelvisProxy.setModelAttributes(source);
        pelvisProxy.isChild = false;
        pelvisProxy.isSneak = source.isSneak;
        pelvisProxy.isRiding = source.isRiding;
        pelvisProxy.setLivingAnimations(player, 0.0F, 0.0F, partialTicks);
        pelvisProxy.setRotationAngles(0.0F, 0.0F, getAnimationTime(player, partialTicks), getHeadYaw(player, partialTicks), getHeadPitch(player, partialTicks), 0.0625F, player);

        Minecraft.getMinecraft().getTextureManager().bindTexture(PELVIS_TEXTURE);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        applyPlayerModelTransforms(player, partialTicks);
        pelvisProxy.bipedBody.postRender(0.0625F);
        GlStateManager.translate(-0.05F, 0.65F, -0.09F); //положение ПЭТТИНСОНА (1 -левоправо, 2 - высота, 3-глубина - ..)
        if (player.isSneaking()) {
            GlStateManager.translate(0.0F, 0.04F, 0.08F);
        }
        GlStateManager.scale(1.3F, 1.3F, 1.3F);
        GlStateManager.enableCull();
        pelvisModel.render(player, 0.0F, 0.0F, getAnimationTime(player, partialTicks), getHeadYaw(player, partialTicks), getHeadPitch(player, partialTicks), 0.0625F);
        GlStateManager.popMatrix();
    }


    private void renderFirstPersonBody(AbstractClientPlayer player, double x, double y, double z, float partialTicks) {
        Render<?> render = Minecraft.getMinecraft().getRenderManager().getEntityRenderObject(player);
        if (!(render instanceof RenderPlayer)) return;
        RenderPlayer renderer = (RenderPlayer) render;
        if (!(renderer.getMainModel() instanceof ModelPlayer)) return;

        ModelPlayer model = (ModelPlayer) renderer.getMainModel();
        ModelVisibilityState state = new ModelVisibilityState(model);

        try {
            model.isChild = false;
            model.bipedHead.showModel = false;
            model.bipedHeadwear.showModel = false;
            model.bipedRightArm.showModel = false;
            model.bipedRightArmwear.showModel = false;
            model.bipedRightArm.isHidden = true;
            model.bipedRightArmwear.isHidden = true;
            model.swingProgress = 0.0F;
            model.isSneak = player.isSneaking();
            model.isRiding = player.isRiding();
            model.setLivingAnimations(player, 0.0F, 0.0F, partialTicks);
            model.setRotationAngles(0.0F, 0.0F, getAnimationTime(player, partialTicks), getHeadYaw(player, partialTicks), getHeadPitch(player, partialTicks), 0.0625F, player);

            Minecraft.getMinecraft().getTextureManager().bindTexture(getSkin(player));
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, z);
            applyPlayerModelTransforms(player, partialTicks);
            GlStateManager.translate(0.0F, 0.0F, FIRST_PERSON_BODY_FORWARD_OFFSET);
            GlStateManager.enableCull();
            model.render(player, 0.0F, 0.0F, getAnimationTime(player, partialTicks), getHeadYaw(player, partialTicks), getHeadPitch(player, partialTicks), 0.0625F);
            GlStateManager.popMatrix();
        } finally {
            state.apply(model);
        }

        renderCustomArmFirstPerson(player, renderer, x, y, z, partialTicks);
        renderPelvisFirstPerson(player, renderer, x, y, z, partialTicks);
    }

    private void renderCustomArmFirstPerson(AbstractClientPlayer player, RenderPlayer renderer, double x, double y, double z, float partialTicks) {
        ModelPlayer source = (ModelPlayer) renderer.getMainModel();
        boolean smallArms = "slim".equals(player.getSkinType());
        ModelPlayer armModel = smallArms ? alexArm : steveArm;

        armModel.setModelAttributes(source);
        armModel.isChild = false;
        armModel.isSneak = source.isSneak;
        armModel.isRiding = source.isRiding;
        armModel.swingProgress = source.swingProgress;
        armModel.setLivingAnimations(player, 0.0F, 0.0F, partialTicks);
        armModel.setRotationAngles(0.0F, 0.0F, getAnimationTime(player, partialTicks), getHeadYaw(player, partialTicks), getHeadPitch(player, partialTicks), 0.0625F, player);

        float animAngle = (float) Math.sin((player.ticksExisted + partialTicks) * 0.9F) * 0.35F - 0.45F;
        armModel.bipedRightArm.rotateAngleX = 0.22F + animAngle * 0.42F;
        armModel.bipedRightArm.rotateAngleY = -0.06F;
        armModel.bipedRightArm.rotateAngleZ = -0.42F;

        armModel.bipedRightArmwear.rotateAngleX = armModel.bipedRightArm.rotateAngleX;
        armModel.bipedRightArmwear.rotateAngleY = armModel.bipedRightArm.rotateAngleY;
        armModel.bipedRightArmwear.rotateAngleZ = armModel.bipedRightArm.rotateAngleZ;

        armModel.bipedRightArm.rotationPointX = source.bipedRightArm.rotationPointX + 2.1F;
        armModel.bipedRightArm.rotationPointY = source.bipedRightArm.rotationPointY;
        armModel.bipedRightArm.rotationPointZ = source.bipedRightArm.rotationPointZ - 1.8F;

        armModel.bipedRightArmwear.rotationPointX = armModel.bipedRightArm.rotationPointX;
        armModel.bipedRightArmwear.rotationPointY = armModel.bipedRightArm.rotationPointY;
        armModel.bipedRightArmwear.rotationPointZ = armModel.bipedRightArm.rotationPointZ;

        Minecraft.getMinecraft().getTextureManager().bindTexture(player.getLocationSkin());
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        applyPlayerModelTransforms(player, partialTicks);
        GlStateManager.translate(0.0F, 0.0F, FIRST_PERSON_BODY_FORWARD_OFFSET);
        GlStateManager.enableCull();
        armModel.bipedRightArm.render(0.0625F);
        armModel.bipedRightArmwear.render(0.0625F);
        GlStateManager.popMatrix();
    }

    private void renderPelvisFirstPerson(AbstractClientPlayer player, RenderPlayer renderer, double x, double y, double z, float partialTicks) {
        ModelPlayer source = (ModelPlayer) renderer.getMainModel();
        pelvisProxy.setModelAttributes(source);
        pelvisProxy.isChild = false;
        pelvisProxy.isSneak = source.isSneak;
        pelvisProxy.isRiding = source.isRiding;
        pelvisProxy.setLivingAnimations(player, 0.0F, 0.0F, partialTicks);
        pelvisProxy.setRotationAngles(0.0F, 0.0F, getAnimationTime(player, partialTicks), getHeadYaw(player, partialTicks), getHeadPitch(player, partialTicks), 0.0625F, player);

        Minecraft.getMinecraft().getTextureManager().bindTexture(PELVIS_TEXTURE);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        applyPlayerModelTransforms(player, partialTicks);
        GlStateManager.translate(0.0F, 0.0F, FIRST_PERSON_BODY_FORWARD_OFFSET);
        pelvisProxy.bipedBody.postRender(0.0625F);
        GlStateManager.translate(-0.05F, 0.65F, -0.09F);
        if (player.isSneaking()) {
            GlStateManager.translate(0.0F, 0.04F, 0.08F);
        }
        GlStateManager.scale(1.3F, 1.3F, 1.3F);
        GlStateManager.enableCull();
        pelvisModel.render(player, 0.0F, 0.0F, getAnimationTime(player, partialTicks), getHeadYaw(player, partialTicks), getHeadPitch(player, partialTicks), 0.0625F);
        GlStateManager.popMatrix();
    }

    private boolean isActiveFirstPerson(EntityPlayer player) {
        if (player == null) return false;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings == null || mc.gameSettings.thirdPersonView != 0) return false;
        ISemenStorage storage = player.getCapability(SemenProvider.SEMEN_CAP, null);
        return storage != null && storage.isManualHarvesting();
    }

    private ResourceLocation getSkin(EntityPlayer player) {
        if (player instanceof AbstractClientPlayer) {
            return ((AbstractClientPlayer) player).getLocationSkin();
        }
        return DefaultPlayerSkin.getDefaultSkin(player.getUniqueID());
    }

    private void applyPlayerModelTransforms(AbstractClientPlayer player, float partialTicks) {
        float bodyYaw = interpolate(player.prevRenderYawOffset, player.renderYawOffset, partialTicks);
        GlStateManager.rotate(180.0F - bodyYaw, 0.0F, 1.0F, 0.0F);
        float scale = 0.9375F;
        GlStateManager.scale(-scale, -scale, scale);
        GlStateManager.translate(0.0F, -1.501F, 0.0F);
    }

    private float interpolate(float prev, float current, float partialTicks) {
        return prev + (current - prev) * partialTicks;
    }

    private double interpolate(double prev, double current, float partialTicks) {
        return prev + (current - prev) * partialTicks;
    }

    private float getAnimationTime(EntityPlayer player, float partialTicks) {
        return player.ticksExisted + partialTicks;
    }

    private float getHeadYaw(EntityPlayer player, float partialTicks) {
        float bodyYaw = interpolate(player.prevRenderYawOffset, player.renderYawOffset, partialTicks);
        float headYaw = interpolate(player.prevRotationYawHead, player.rotationYawHead, partialTicks);
        return MathHelper.wrapDegrees(headYaw - bodyYaw);
    }

    private float getHeadPitch(EntityPlayer player, float partialTicks) {
        return interpolate(player.prevRotationPitch, player.rotationPitch, partialTicks);
    }

    private static final class ModelVisibilityState {
        private final boolean headShow;
        private final boolean headWearShow;
        private final boolean rightArmShow;
        private final boolean rightArmWearShow;
        private final boolean rightArmHidden;
        private final boolean rightArmWearHidden;
        private final boolean isChild;
        private final boolean isSneak;
        private final boolean isRiding;
        private final float swingProgress;

        private ModelVisibilityState(ModelPlayer model) {
            this.headShow = model.bipedHead.showModel;
            this.headWearShow = model.bipedHeadwear.showModel;
            this.rightArmShow = model.bipedRightArm.showModel;
            this.rightArmWearShow = model.bipedRightArmwear.showModel;
            this.rightArmHidden = model.bipedRightArm.isHidden;
            this.rightArmWearHidden = model.bipedRightArmwear.isHidden;
            this.isChild = model.isChild;
            this.isSneak = model.isSneak;
            this.isRiding = model.isRiding;
            this.swingProgress = model.swingProgress;
        }

        private void apply(ModelPlayer model) {
            model.bipedHead.showModel = headShow;
            model.bipedHeadwear.showModel = headWearShow;
            model.bipedRightArm.showModel = rightArmShow;
            model.bipedRightArmwear.showModel = rightArmWearShow;
            model.bipedRightArm.isHidden = rightArmHidden;
            model.bipedRightArmwear.isHidden = rightArmWearHidden;
            model.isChild = isChild;
            model.isSneak = isSneak;
            model.isRiding = isRiding;
            model.swingProgress = swingProgress;
        }
    }
}
