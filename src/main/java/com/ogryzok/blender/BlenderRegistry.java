package com.ogryzok.blender;

import com.ogryzok.blender.block.BlockBlender;
import com.ogryzok.blender.item.ItemBlenderWhisk;
import com.ogryzok.blender.client.render.BlenderRenderer;
import com.ogryzok.blender.tile.TileBlender;
import com.ogryzok.client.item.ItemGeoBlock;
import com.ogryzok.harvestech;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

@Mod.EventBusSubscriber(modid = harvestech.MODID)
public final class BlenderRegistry {
    public static final String MODID = harvestech.MODID;
    public static final int GUI_ID = 16;

    public static final BlockBlender BLENDER = new BlockBlender();
    public static final ItemBlenderWhisk BLENDER_WHISK = new ItemBlenderWhisk();
    private static boolean initialized = false;

    private BlenderRegistry() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        GameRegistry.registerTileEntity(TileBlender.class, new ResourceLocation(MODID, "blender"));
    }

    public static void initClient() {
        ClientRegistry.bindTileEntitySpecialRenderer(TileBlender.class, new BlenderRenderer());
    }

    public static Object getModInstance() {
        ModContainer container = Loader.instance().getIndexedModList().get(MODID);
        return container == null ? null : container.getMod();
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(BLENDER);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
                new ItemGeoBlock(
                        BLENDER,
                        new ResourceLocation(MODID, "geo/blender.geo.json"),
                        new ResourceLocation(MODID, "textures/blocks/blender.png"),
                        new ResourceLocation(MODID, "animations/blender.animation.json")
                ),
                BLENDER_WHISK
        );
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        registerBlenderWhiskRecipe(event);
        registerBlenderRecipe(event);
    }

    private static void registerBlenderWhiskRecipe(RegistryEvent.Register<IRecipe> event) {
        Item enrichedAlloy = Item.getByNameOrId("mekanism:enrichedalloy");
        if (enrichedAlloy == null || OreDictionary.getOres("ingotSteel").isEmpty()) {
            return;
        }

        event.getRegistry().register(
                new ShapedOreRecipe(
                        new ResourceLocation(MODID, "blender_whisk"),
                        new ItemStack(BLENDER_WHISK),
                        " A ",
                        " S ",
                        " H ",
                        'A', new ItemStack(enrichedAlloy, 1, 0),
                        'S', "ingotSteel",
                        'H', new ItemStack(Items.SHEARS)
                ).setRegistryName(new ResourceLocation(MODID, "blender_whisk"))
        );
    }

    private static void registerBlenderRecipe(RegistryEvent.Register<IRecipe> event) {
        if (OreDictionary.getOres("plateSteel").isEmpty()) {
            return;
        }

        Item itemduct = Item.getByNameOrId("thermaldynamics:duct_32");
        Item steelCasing = Item.getByNameOrId("mekanism:basicblock");
        if (itemduct == null || steelCasing == null) {
            return;
        }

        event.getRegistry().register(
                new ShapedOreRecipe(
                        new ResourceLocation(MODID, "blender"),
                        new ItemStack(BLENDER),
                        "PWP",
                        "DCD",
                        "PSP",
                        'P', "plateSteel",
                        'W', new ItemStack(BLENDER_WHISK),
                        'D', new ItemStack(itemduct, 1, 0),
                        'C', new ItemStack(Blocks.HOPPER),
                        'S', new ItemStack(steelCasing, 1, 8)
                ).setRegistryName(new ResourceLocation(MODID, "blender"))
        );
    }

    @Mod.EventBusSubscriber(modid = MODID, value = net.minecraftforge.fml.relauncher.Side.CLIENT)
    public static final class ClientOnlyEvents {
        @SubscribeEvent
        public static void registerModels(net.minecraftforge.client.event.ModelRegistryEvent event) {
            Item item = Item.getItemFromBlock(BLENDER);
            if (item != null) {
                net.minecraftforge.client.model.ModelLoader.setCustomModelResourceLocation(
                        item,
                        0,
                        new ModelResourceLocation("harvestech:blender", "inventory")
                );
                if (item instanceof ItemGeoBlock) {
                    ((ItemGeoBlock) item).initTEISR();
                }
            }

            net.minecraftforge.client.model.ModelLoader.setCustomModelResourceLocation(
                    BLENDER_WHISK,
                    0,
                    new ModelResourceLocation("harvestech:blender_whisk", "inventory")
            );
            BLENDER_WHISK.initTEISR();
        }
    }
}
