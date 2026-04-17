package com.ogryzok.separator.client.render;

import com.ogryzok.separator.item.ItemSeparatorWhisk;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;

public class SeparatorWhiskItemRenderer extends GeoItemRenderer<ItemSeparatorWhisk> {
    public SeparatorWhiskItemRenderer() {
        super(new SeparatorWhiskGeoModel());
    }
}
