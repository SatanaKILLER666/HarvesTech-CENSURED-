package com.ogryzok.sdsfix;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(
        modid = SpatialDragonFix.MODID,
        name = SpatialDragonFix.NAME,
        version = SpatialDragonFix.VERSION
)
public class SpatialDragonFix {
    public static final String MODID = "spatialdragonfix";
    public static final String NAME = "Spatial Dragon Fix";
    public static final String VERSION = "1.0";

    @Mod.Instance(MODID)
    public static SpatialDragonFix INSTANCE;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new SpatialDragonSummonerFix());
        System.out.println("[SDSFIX] init done");
    }
}