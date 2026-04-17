package com.ogryzok.semencentrifuge.client.render;

import com.ogryzok.semencentrifuge.tile.TileSemenCentrifugeMotor;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumFacing;
import org.lwjgl.opengl.GL11;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.renderers.geo.GeoBlockRenderer;

public class SemenCentrifugeMotorRenderer extends GeoBlockRenderer<TileSemenCentrifugeMotor> {

    private enum RenderPass {
        SOLID,
        COVER_GLASS,
        PIPE_GLASS
    }

    private RenderPass renderPass = RenderPass.SOLID;

    public SemenCentrifugeMotorRenderer() {
        super(new SemenCentrifugeMotorGeoModel());
    }

    @Override
    public void render(TileSemenCentrifugeMotor tile, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        try {
            // 1. Обычные непрозрачные части
            renderPass = RenderPass.SOLID;
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.depthMask(true);
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            super.render(tile, x, y, z, partialTicks, destroyStage, alpha);

            // 2. Красивое стекло перед внутренностями
            renderPass = RenderPass.COVER_GLASS;
            GlStateManager.enableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.blendFunc(
                    GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
            );
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
            GlStateManager.depthMask(false);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            super.render(tile, x, y, z, partialTicks, destroyStage, alpha);

            // 3. Трубы — красивый вариант с записью в depth
            renderPass = RenderPass.PIPE_GLASS;
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(-2.0F, -20.0F);

            GlStateManager.enableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.blendFunc(
                    GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
            );
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.05F);
            GlStateManager.depthMask(true);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            super.render(tile, x, y, z, partialTicks, destroyStage, alpha);

            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);

        } finally {
            renderPass = RenderPass.SOLID;
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    @Override
    public void renderRecursively(BufferBuilder builder, GeoBone bone, float red, float green, float blue, float alpha) {
        String name = bone.getName().toLowerCase();

        boolean isGlass = name.contains("glass");
        boolean isPipeGlass = name.contains("pipe");
        boolean isCoverGlass = isGlass && !isPipeGlass;

        switch (renderPass) {
            case SOLID:
                if (isGlass) {
                    return;
                }
                break;

            case COVER_GLASS:
                if (!isCoverGlass) {
                    return;
                }
                break;

            case PIPE_GLASS:
                if (!isPipeGlass) {
                    return;
                }
                break;
        }

        super.renderRecursively(builder, bone, red, green, blue, alpha);
    }

    @Override
    public void rotateBlock(EnumFacing facing) {
        if (facing == null) {
            return;
        }

        switch (facing) {
            case NORTH:
                GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
                break;
            case EAST:
                GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                break;
            case SOUTH:
                GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
                break;
            case WEST:
            default:
                break;
        }
    }
}