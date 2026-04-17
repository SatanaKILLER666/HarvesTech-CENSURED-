package com.ogryzok.proteinformer;

import com.ogryzok.client.item.ItemGeoBlock;
import com.ogryzok.harvestech;
import com.ogryzok.proteinformer.block.BlockProteinFormer;
import com.ogryzok.proteinformer.client.render.ProteinFormerRenderer;
import com.ogryzok.proteinformer.tile.TileProteinFormer;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.init.Blocks;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.ShapedOreRecipe;

@Mod.EventBusSubscriber(modid = harvestech.MODID)
public final class ProteinFormerRegistry {
    public static final String MODID = harvestech.MODID;
    public static final int GUI_ID = 15;

    public static final BlockProteinFormer PROTEIN_FORMER = new BlockProteinFormer();
    private static boolean initialized = false;

    private ProteinFormerRegistry() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        GameRegistry.registerTileEntity(TileProteinFormer.class, new ResourceLocation(MODID, "protein_former"));
    }

    public static void initClient() {
        ClientRegistry.bindTileEntitySpecialRenderer(TileProteinFormer.class, new ProteinFormerRenderer());
    }

    public static Object getModInstance() {
        ModContainer container = Loader.instance().getIndexedModList().get(MODID);
        return container == null ? null : container.getMod();
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(PROTEIN_FORMER);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(new ItemGeoBlock(
                PROTEIN_FORMER,
                new ResourceLocation(MODID, "geo/protein_former.geo.json"),
                new ResourceLocation(MODID, "textures/blocks/protein_former.png"),
                new ResourceLocation(MODID, "animations/protein_former.animation.json")
        ));
    }



    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        Item itemduct = Item.getByNameOrId("thermaldynamics:duct_32");
        Item steelCasing = Item.getByNameOrId("mekanism:basicblock");

        if (itemduct == null || steelCasing == null) {
            return;
        }

        event.getRegistry().register(
                new ShapedOreRecipe(
                        new ResourceLocation(MODID, "protein_former"),
                        new ItemStack(PROTEIN_FORMER),
                        "GPG",
                        "TCT",
                        "SSS",
                        'G', new ItemStack(Blocks.GLASS_PANE),
                        'P', new ItemStack(Blocks.PISTON),
                        'T', new ItemStack(itemduct, 1, 0),
                        'C', new ItemStack(steelCasing, 1, 8),
                        'S', "plateSteel"
                ).setRegistryName(new ResourceLocation(MODID, "protein_former"))
        );
    }

    @Mod.EventBusSubscriber(modid = MODID, value = net.minecraftforge.fml.relauncher.Side.CLIENT)
    public static final class ClientOnlyEvents {
        @SubscribeEvent
        public static void registerModels(net.minecraftforge.client.event.ModelRegistryEvent event) {
            Item item = Item.getItemFromBlock(PROTEIN_FORMER);
            if (item != null) {
                net.minecraftforge.client.model.ModelLoader.setCustomModelResourceLocation(
                        item,
                        0,
                        new ModelResourceLocation("harvestech:protein_former", "inventory")
                );
                if (item instanceof ItemGeoBlock) {
                    ((ItemGeoBlock) item).initTEISR();
                }
            }
        }
    }
}
