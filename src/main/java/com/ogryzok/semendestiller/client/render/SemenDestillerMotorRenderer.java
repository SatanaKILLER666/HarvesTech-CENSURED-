package com.ogryzok.semendestiller.client.render;

import com.ogryzok.semendestiller.tile.TileSemenDestillerMotor;
import software.bernie.geckolib3.renderers.geo.GeoBlockRenderer;

public class SemenDestillerMotorRenderer extends GeoBlockRenderer<TileSemenDestillerMotor> {
    public SemenDestillerMotorRenderer() {
        super(new SemenDestillerMotorGeoModel());
    }
}