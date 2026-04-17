package com.ogryzok.client.item;

import com.ogryzok.client.render.GeoBlockItemRenderer;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

public class ItemGeoBlock extends ItemBlock implements IAnimatable {
    private final AnimationFactory factory = new AnimationFactory(this);
    private final ResourceLocation geoModel;
    private final ResourceLocation texture;
    private final ResourceLocation animation;

    public ItemGeoBlock(Block block, ResourceLocation geoModel, ResourceLocation texture, ResourceLocation animation) {
        super(block);
        this.geoModel = geoModel;
        this.texture = texture;
        this.animation = animation;
        setRegistryName(block.getRegistryName());
    }

    @SideOnly(Side.CLIENT)
    public void initTEISR() {
        this.setTileEntityItemStackRenderer(new GeoBlockItemRenderer());
    }

    public ResourceLocation getGeoModel() {
        return geoModel;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public ResourceLocation getAnimation() {
        return animation;
    }

    @Override
    public void registerControllers(AnimationData data) {
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }
}
