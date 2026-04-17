package com.ogryzok.mrnasynthesizer;

import com.ogryzok.client.item.ItemGeoBlock;
import com.ogryzok.harvestech;
import com.ogryzok.mrnasynthesizer.block.BlockMRNASynthesizer;
import com.ogryzok.mrnasynthesizer.client.render.MRNASynthesizerRenderer;
import com.ogryzok.mrnasynthesizer.tile.TileMRNASynthesizer;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.oredict.ShapedOreRecipe;

@Mod.EventBusSubscriber(modid = harvestech.MODID)
public final class MRNASynthesizerRegistry {
    public static final String MODID = harvestech.MODID;
    public static final int GUI_ID = 17;

    public static final BlockMRNASynthesizer MRNA_SYNTHESIZER = new BlockMRNASynthesizer();
    private static boolean initialized = false;

    private MRNASynthesizerRegistry() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        GameRegistry.registerTileEntity(TileMRNASynthesizer.class, new ResourceLocation(MODID, "mrna_synthesizer"));
    }

    public static void initClient() {
        ClientRegistry.bindTileEntitySpecialRenderer(TileMRNASynthesizer.class, new MRNASynthesizerRenderer());
    }

    public static Object getModInstance() {
        ModContainer container = Loader.instance().getIndexedModList().get(MODID);
        return container == null ? null : container.getMod();
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(MRNA_SYNTHESIZER);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(new ItemGeoBlock(
                MRNA_SYNTHESIZER,
                new ResourceLocation(MODID, "geo/vaccine_generator.geo.json"),
                new ResourceLocation(MODID, "textures/blocks/vaccine_generator.png"),
                new ResourceLocation(MODID, "animations/vaccine_generator.animation.json")
        ));
    }


    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        Item mechanicalPipe = Item.getByNameOrId("mekanism:transmitter");
        Item controlCircuit = Item.getByNameOrId("mekanism:controlcircuit");
        Item reinforcedAlloy = Item.getByNameOrId("mekanism:reinforcedalloy");
        Item steelCasing = Item.getByNameOrId("mekanism:basicblock");

        if (mechanicalPipe == null || controlCircuit == null || reinforcedAlloy == null || steelCasing == null) {
            return;
        }

        event.getRegistry().register(
                new ShapedOreRecipe(
                        new ResourceLocation(MODID, "mrna_synthesizer"),
                        new ItemStack(MRNA_SYNTHESIZER),
                        "PQP",
                        "ACA",
                        "TTT",
                        'P', new ItemStack(mechanicalPipe, 1, 0),
                        'Q', new ItemStack(controlCircuit, 1, 2),
                        'A', new ItemStack(reinforcedAlloy),
                        'C', new ItemStack(steelCasing, 1, 8),
                        'T', "plateSteel"
                ).setRegistryName(new ResourceLocation(MODID, "mrna_synthesizer"))
        );
    }

    @Mod.EventBusSubscriber(modid = MODID, value = net.minecraftforge.fml.relauncher.Side.CLIENT)
    public static final class ClientOnlyEvents {
        @SubscribeEvent
        public static void registerModels(net.minecraftforge.client.event.ModelRegistryEvent event) {
            Item item = Item.getItemFromBlock(MRNA_SYNTHESIZER);
            if (item != null) {
                net.minecraftforge.client.model.ModelLoader.setCustomModelResourceLocation(
                        item,
                        0,
                        new ModelResourceLocation(MODID + ":mrna_synthesizer", "inventory")
                );
                if (item instanceof ItemGeoBlock) {
                    ((ItemGeoBlock) item).initTEISR();
                }
            }
        }
    }
}
