package com.ogryzok.semendestiller.client.render;

import com.ogryzok.semendestiller.tile.TileSemenDestillerBase;
import software.bernie.geckolib3.renderers.geo.GeoBlockRenderer;

public class SemenDestillerBaseRenderer extends GeoBlockRenderer<TileSemenDestillerBase> {
    public SemenDestillerBaseRenderer() {
        super(new SemenDestillerBaseGeoModel());
    }
}