package com.ogryzok.client.render;

import com.ogryzok.client.item.ItemGeoSimple;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;

public class GeoSimpleItemRenderer extends GeoItemRenderer<ItemGeoSimple> {
    public GeoSimpleItemRenderer() {
        super(new GeoSimpleItemModel());
    }
}
