package com.ogryzok.blender.client.render;

import com.ogryzok.blender.item.ItemBlenderWhisk;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;

public class BlenderWhiskItemRenderer extends GeoItemRenderer<ItemBlenderWhisk> {
    public BlenderWhiskItemRenderer() {
        super(new BlenderWhiskGeoModel());
    }
}
