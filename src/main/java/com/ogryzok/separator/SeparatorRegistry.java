package com.ogryzok.separator;

import com.ogryzok.harvestech;
import com.ogryzok.separator.block.BlockSeparator;
import com.ogryzok.separator.client.SeparatorClientHandler;
import com.ogryzok.separator.client.render.SeparatorRenderer;
import com.ogryzok.separator.item.ItemSeparatorWhisk;
import com.ogryzok.separator.tile.TileSeparator;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import com.ogryzok.client.item.ItemGeoBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
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
public final class SeparatorRegistry {
    public static final String MODID = harvestech.MODID;
    public static final int GUI_ID = 14;
    public static final BlockSeparator SEPARATOR = new BlockSeparator();
    public static final ItemSeparatorWhisk SEPARATOR_WHISK = new ItemSeparatorWhisk();
    private static boolean initialized = false;
    private static boolean clientInitialized = false;

    private SeparatorRegistry() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        GameRegistry.registerTileEntity(TileSeparator.class, new ResourceLocation(MODID, "separator"));
    }

    public static void initClient() {
        if (clientInitialized) return;
        clientInitialized = true;
        MinecraftForge.EVENT_BUS.register(new SeparatorClientHandler());
        ClientRegistry.bindTileEntitySpecialRenderer(TileSeparator.class, new SeparatorRenderer());
    }

    public static Object getModInstance() {
        ModContainer container = Loader.instance().getIndexedModList().get(MODID);
        return container == null ? null : container.getMod();
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(SEPARATOR);
    }

        @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
                new ItemGeoBlock(
                        SEPARATOR,
                        new ResourceLocation(MODID, "geo/separator_base.geo.json"),
                        new ResourceLocation(MODID, "textures/blocks/separator_base.png"),
                        new ResourceLocation(MODID, "animations/separator.animation.json")
                ),
                SEPARATOR_WHISK
        );
    }


    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        registerSeparatorBladeRecipe(event);
        registerSeparatorBaseRecipes(event, "plateAluminum", "aluminum");
        registerSeparatorBaseRecipes(event, "plateAluminium", "aluminium");
    }

    private static void registerSeparatorBladeRecipe(RegistryEvent.Register<IRecipe> event) {
        if (OreDictionary.getOres("ingotSteel").isEmpty() || OreDictionary.getOres("plateIron").isEmpty()) {
            return;
        }

        event.getRegistry().register(
                new ShapedOreRecipe(
                        new ResourceLocation(MODID, "separator_blade"),
                        new ItemStack(SEPARATOR_WHISK),
                        " S ",
                        " P ",
                        "C C",
                        'S', "ingotSteel",
                        'P', "plateIron",
                        'C', new ItemStack(Items.SHEARS)
                ).setRegistryName(new ResourceLocation(MODID, "separator_blade"))
        );
    }

    private static void registerSeparatorBaseRecipes(RegistryEvent.Register<IRecipe> event, String aluminumPlateOreName, String suffix) {
        if (OreDictionary.getOres(aluminumPlateOreName).isEmpty() || OreDictionary.getOres("plateSteel").isEmpty()) {
            return;
        }

        Item mechanicalPipe = Item.getByNameOrId("mekanism:transmitter");
        Item steelCasing = Item.getByNameOrId("mekanism:basicblock");
        Item itemduct = Item.getByNameOrId("thermaldynamics:duct_32");

        if (mechanicalPipe == null || steelCasing == null || itemduct == null) {
            return;
        }

        event.getRegistry().register(
                new ShapedOreRecipe(
                        new ResourceLocation(MODID, "separator_base_" + suffix),
                        new ItemStack(SEPARATOR),
                        "AGA",
                        "MCD",
                        "SSS",
                        'A', aluminumPlateOreName,
                        'G', new ItemStack(net.minecraft.init.Blocks.GLASS_PANE),
                        'M', new ItemStack(mechanicalPipe, 1, 1),
                        'C', new ItemStack(steelCasing, 1, 8),
                        'D', new ItemStack(itemduct, 1, 0),
                        'S', "plateSteel"
                ).setRegistryName(new ResourceLocation(MODID, "separator_base_" + suffix))
        );
    }

    @Mod.EventBusSubscriber(modid = MODID, value = net.minecraftforge.fml.relauncher.Side.CLIENT)
    public static final class ClientOnlyEvents {
        @SubscribeEvent
        public static void registerModels(net.minecraftforge.client.event.ModelRegistryEvent event) {
            Item item = Item.getItemFromBlock(SEPARATOR);
            if (item != null) {
                net.minecraftforge.client.model.ModelLoader.setCustomModelResourceLocation(item, 0,
                        new ModelResourceLocation("harvestech:separator", "inventory"));
                if (item instanceof ItemGeoBlock) {
                    ((ItemGeoBlock) item).initTEISR();
                }
            }

            net.minecraftforge.client.model.ModelLoader.setCustomModelResourceLocation(
                    SEPARATOR_WHISK,
                    0,
                    new ModelResourceLocation("harvestech:separator_whisk", "inventory"));
            SEPARATOR_WHISK.initTEISR();
        }
    }
}
