package com.ogryzok.client.item;

import com.ogryzok.client.render.GeoSimpleItemRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

public class ItemGeoSimple extends Item implements IAnimatable {
    private final AnimationFactory factory = new AnimationFactory(this);
    private final ResourceLocation geoModel;
    private final ResourceLocation texture;
    private final ResourceLocation animation;

    public ItemGeoSimple(String name, CreativeTabs tab, ResourceLocation geoModel, ResourceLocation texture, ResourceLocation animation) {
        this.geoModel = geoModel;
        this.texture = texture;
        this.animation = animation;
        setRegistryName(geoModel.getNamespace(), name);
        setTranslationKey(geoModel.getNamespace() + "." + name);
        setCreativeTab(tab);
        setMaxStackSize(64);
    }

    @SideOnly(Side.CLIENT)
    public void initTEISR() {
        setTileEntityItemStackRenderer(new TileEntityItemStackRenderer() {
            private final GeoSimpleItemRenderer renderer = new GeoSimpleItemRenderer();

            @Override
            public void renderByItem(ItemStack stack) {
                renderer.renderByItem(stack);
            }
        });
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
