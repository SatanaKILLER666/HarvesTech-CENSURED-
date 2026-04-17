package com.ogryzok.disease.client.render;

import com.ogryzok.disease.item.ItemAidsVaccine;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class VaccineGeoModel extends AnimatedGeoModel<ItemAidsVaccine> {
    @Override
    public ResourceLocation getModelLocation(ItemAidsVaccine object) {
        return new ResourceLocation("harvestech", "geo/vaccine.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(ItemAidsVaccine object) {
        String path = object.getRegistryName() != null ? object.getRegistryName().getPath() : "aids_vaccine";
        if ("male_power_steroid".equals(path)) {
            return new ResourceLocation("harvestech", "textures/items/steroid.png");
        }
        return new ResourceLocation("harvestech", "textures/items/vaccine.png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(ItemAidsVaccine animatable) {
        return new ResourceLocation("harvestech", "animations/vaccine.animation.json");
    }
}
