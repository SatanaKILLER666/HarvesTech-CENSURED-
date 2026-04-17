package com.ogryzok.semenenrichment;

import com.ogryzok.harvestech;
import com.ogryzok.semenenrichment.block.BlockSemenEnrichmentChamber;
import com.ogryzok.semenenrichment.client.render.SemenEnrichmentChamberRenderer;
import com.ogryzok.semenenrichment.tile.TileSemenEnrichmentChamber;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import com.ogryzok.client.item.ItemGeoBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.ShapedOreRecipe;

@Mod.EventBusSubscriber(modid = harvestech.MODID)
public final class SemenEnrichmentRegistry {
    public static final String MODID = harvestech.MODID;
    public static final int GUI_ID = 13;

    public static final BlockSemenEnrichmentChamber SEMEN_ENRICHMENT_CHAMBER = new BlockSemenEnrichmentChamber();
    private static boolean initialized = false;

    private SemenEnrichmentRegistry() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        GameRegistry.registerTileEntity(TileSemenEnrichmentChamber.class, new ResourceLocation(MODID, "semen_enrichment_chamber"));
    }

    public static void initClient() {
        ClientRegistry.bindTileEntitySpecialRenderer(TileSemenEnrichmentChamber.class, new SemenEnrichmentChamberRenderer());
    }

    public static Object getModInstance() {
        ModContainer container = Loader.instance().getIndexedModList().get(MODID);
        return container == null ? null : container.getMod();
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(SEMEN_ENRICHMENT_CHAMBER);
    }

        @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(new ItemGeoBlock(
                SEMEN_ENRICHMENT_CHAMBER,
                new ResourceLocation(MODID, "geo/semen_enrichment_chamber.geo.json"),
                new ResourceLocation(MODID, "textures/blocks/semen_enrichment_chamber.png"),
                new ResourceLocation(MODID, "animations/chair.animation.json")
        ));
    }



    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        Item enrichedAlloy = Item.getByNameOrId("mekanism:enrichedalloy");
        Item controlCircuit = Item.getByNameOrId("mekanism:controlcircuit");
        Item mechanicalPipe = Item.getByNameOrId("mekanism:transmitter");
        Item steelCasing = Item.getByNameOrId("mekanism:basicblock");
        Item fluidTank = Item.getByNameOrId("mekanism:machineblock2");

        if (enrichedAlloy == null || controlCircuit == null || mechanicalPipe == null || steelCasing == null || fluidTank == null) {
            return;
        }

        event.getRegistry().register(
                new ShapedOreRecipe(
                        new ResourceLocation(MODID, "semen_enrichment_chamber"),
                        new ItemStack(SEMEN_ENRICHMENT_CHAMBER),
                        "ACA",
                        "PTP",
                        "IBI",
                        'A', new ItemStack(enrichedAlloy, 1, 0),
                        'C', new ItemStack(controlCircuit, 1, 1),
                        'P', new ItemStack(mechanicalPipe, 1, 1),
                        'T', new ItemStack(steelCasing, 1, 8),
                        'I', "plateIron",
                        'B', new ItemStack(fluidTank, 1, 11)
                ).setRegistryName(new ResourceLocation(MODID, "semen_enrichment_chamber"))
        );
    }

    @Mod.EventBusSubscriber(modid = MODID, value = net.minecraftforge.fml.relauncher.Side.CLIENT)
    public static final class ClientOnlyEvents {
        @SubscribeEvent
        public static void registerModels(net.minecraftforge.client.event.ModelRegistryEvent event) {
            Item item = Item.getItemFromBlock(SEMEN_ENRICHMENT_CHAMBER);
            if (item != null) {
                net.minecraftforge.client.model.ModelLoader.setCustomModelResourceLocation(
                        item,
                        0,
                        new ModelResourceLocation("harvestech:semen_enrichment_chamber", "inventory")
                );
                if (item instanceof ItemGeoBlock) {
                    ((ItemGeoBlock) item).initTEISR();
                }
            }
        }
    }
}
