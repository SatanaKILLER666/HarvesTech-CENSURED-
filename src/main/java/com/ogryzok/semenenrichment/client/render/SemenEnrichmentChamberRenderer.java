package com.ogryzok.semenenrichment.client.render;

import com.ogryzok.semenenrichment.tile.TileSemenEnrichmentChamber;
import software.bernie.geckolib3.renderers.geo.GeoBlockRenderer;

public class SemenEnrichmentChamberRenderer extends GeoBlockRenderer<TileSemenEnrichmentChamber> {
    public SemenEnrichmentChamberRenderer() {
        super(new SemenEnrichmentChamberGeoModel());
    }
}
