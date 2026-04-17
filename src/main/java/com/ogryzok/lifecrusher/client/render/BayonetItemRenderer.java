package com.ogryzok.lifecrusher.client.render;

import com.ogryzok.lifecrusher.item.ItemBayonet;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;

public class BayonetItemRenderer extends GeoItemRenderer<ItemBayonet> {

    public BayonetItemRenderer() {
        super(new BayonetGeoModel());
    }
}