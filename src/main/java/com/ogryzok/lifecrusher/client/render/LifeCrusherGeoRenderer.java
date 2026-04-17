package com.ogryzok.lifecrusher.client.render;

import com.ogryzok.lifecrusher.tile.TileLifeCrusher;
import software.bernie.geckolib3.renderers.geo.GeoBlockRenderer;

public class LifeCrusherGeoRenderer extends GeoBlockRenderer<TileLifeCrusher> {
    public LifeCrusherGeoRenderer() {
        super(new LifeCrusherGeoModel());
    }
    @Override
    public void render(TileLifeCrusher te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        // Если это сообщение появится в консоли — значит код работает, и проблема в самом файле модели/текстуры
        // System.out.println("DEBUG: Рендерю блок на " + x + "," + y + "," + z);
        super.render(te, x, y, z, partialTicks, destroyStage, alpha);
    }
}
