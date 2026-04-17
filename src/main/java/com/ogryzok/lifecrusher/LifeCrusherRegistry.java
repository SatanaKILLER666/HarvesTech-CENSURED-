package com.ogryzok.lifecrusher;

import com.ogryzok.lifecrusher.block.BlockLifeCrusher;
import com.ogryzok.lifecrusher.client.render.LifeCrusherGeoRenderer;
import com.ogryzok.lifecrusher.item.ItemBayonet;
import com.ogryzok.lifecrusher.item.ItemLifeCrusherBase;
import com.ogryzok.lifecrusher.item.ItemLifeCrusherRod;
import com.ogryzok.lifecrusher.item.ItemRubberRod;
import com.ogryzok.lifecrusher.player.PlayerBerserkHandler;
import com.ogryzok.lifecrusher.potion.PotionStupor;
import com.ogryzok.lifecrusher.tile.TileLifeCrusher;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

@Mod.EventBusSubscriber(modid = LifeCrusherRegistry.MODID)
public final class LifeCrusherRegistry {
    public static final Potion STUPOR = new PotionStupor();

    public static final String MODID = "harvestech";
    public static final int GUI_ID = 0;

    public static final BlockLifeCrusher LIFE_CRUSHER = new BlockLifeCrusher();
    public static final ItemLifeCrusherBase LIFE_CRUSHER_BASE = new ItemLifeCrusherBase();
    public static final ItemLifeCrusherRod LIFE_CRUSHER_ROD = new ItemLifeCrusherRod();
    public static final ItemRubberRod RUBBER_ROD = new ItemRubberRod();
    public static final ItemBayonet BAYONET = new ItemBayonet();

    private static boolean initialized = false;

    private LifeCrusherRegistry() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        initialized = true;

        GameRegistry.registerTileEntity(TileLifeCrusher.class, new ResourceLocation(MODID, "life_crusher"));

        MinecraftForge.EVENT_BUS.register(new PlayerBerserkHandler());
    }

    public static void initClient() {
        ClientRegistry.bindTileEntitySpecialRenderer(TileLifeCrusher.class, new LifeCrusherGeoRenderer());
        LIFE_CRUSHER_BASE.initTEISR();
        LIFE_CRUSHER_ROD.initTEISR();
        RUBBER_ROD.initTEISR();
        BAYONET.initTEISR();
    }

    public static Object getModInstance() {
        ModContainer container = Loader.instance().getIndexedModList().get(MODID);
        return container.getMod();
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(LIFE_CRUSHER);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(new ItemBlock(LIFE_CRUSHER).setRegistryName(LIFE_CRUSHER.getRegistryName()));
        event.getRegistry().register(LIFE_CRUSHER_BASE);
        event.getRegistry().register(LIFE_CRUSHER_ROD);
        event.getRegistry().register(RUBBER_ROD);
        event.getRegistry().register(BAYONET);
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        event.getRegistry().register(
                new ShapedOreRecipe(
                        new ResourceLocation(MODID, "life_crusher_base"),
                        new ItemStack(LIFE_CRUSHER_BASE),
                        " S ",
                        "TTT",
                        "BIB",
                        'S', "ingotSteel",
                        'T', "plateTin",
                        'B', "gearBronze",
                        'I', Blocks.IRON_BARS
                ).setRegistryName(new ResourceLocation(MODID, "life_crusher_base"))
        );

        event.getRegistry().register(
                new ShapedOreRecipe(
                        new ResourceLocation(MODID, "life_crusher_rod"),
                        new ItemStack(LIFE_CRUSHER_ROD),
                        " GP",
                        "BPS",
                        "PR ",
                        'G', "gearIron",
                        'P', "plateBronze",
                        'B', "gearBronze",
                        'S', new ItemStack(BAYONET, 1, 0),
                        'R', Blocks.REDSTONE_BLOCK
                ).setRegistryName(new ResourceLocation(MODID, "life_crusher_rod"))
        );

        registerRubberRodRecipe(event, "itemRubber", "rubber_rod_itemrubber");
        registerRubberRodRecipe(event, "materialRubber", "rubber_rod_materialrubber");
        registerRubberRodRecipe(event, "rubber", "rubber_rod_rubber");

        registerBayonetRecipe(event, "itemRubber", "bayonet_itemrubber");
        registerBayonetRecipe(event, "materialRubber", "bayonet_materialrubber");
        registerBayonetRecipe(event, "rubber", "bayonet_rubber");
    }


    private static void registerRubberRodRecipe(RegistryEvent.Register<IRecipe> event, String oreName, String recipeName) {
        if (OreDictionary.getOres(oreName).isEmpty()) {
            return;
        }

        event.getRegistry().register(
                new ShapedOreRecipe(
                        new ResourceLocation(MODID, recipeName),
                        new ItemStack(RUBBER_ROD),
                        " RR",
                        " SR",
                        "RS ",
                        'R', oreName,
                        'S', "stickWood"
                ).setRegistryName(new ResourceLocation(MODID, recipeName))
        );
    }

    private static void registerBayonetRecipe(RegistryEvent.Register<IRecipe> event, String oreName, String recipeName) {
        if (OreDictionary.getOres(oreName).isEmpty()) {
            return;
        }

        event.getRegistry().register(
                new ShapedOreRecipe(
                        new ResourceLocation(MODID, recipeName),
                        new ItemStack(BAYONET),
                        " R ",
                        " R ",
                        "RSR",
                        'R', oreName,
                        'S', "stickWood"
                ).setRegistryName(new ResourceLocation(MODID, recipeName))
        );
    }

    @SubscribeEvent
    public static void registerPotions(RegistryEvent.Register<Potion> event) {
        event.getRegistry().register(STUPOR);
    }

    @Mod.EventBusSubscriber(modid = MODID, value = net.minecraftforge.fml.relauncher.Side.CLIENT)
    public static final class ClientOnlyEvents {
        private ClientOnlyEvents() {
        }

        @SubscribeEvent
        public static void registerModels(ModelRegistryEvent event) {
            Item blockItem = Item.getItemFromBlock(LIFE_CRUSHER);
            if (blockItem != null) {
                ModelLoader.setCustomModelResourceLocation(
                        blockItem,
                        0,
                        new ModelResourceLocation("harvestech:life_crusher", "inventory")
                );
            }

            ModelLoader.setCustomModelResourceLocation(
                    LIFE_CRUSHER_BASE,
                    0,
                    new ModelResourceLocation("harvestech:life_crusher_base", "inventory")
            );

            ModelLoader.setCustomModelResourceLocation(
                    LIFE_CRUSHER_ROD,
                    0,
                    new ModelResourceLocation("harvestech:life_crusher_rod", "inventory")
            );

            ModelLoader.setCustomModelResourceLocation(
                    RUBBER_ROD,
                    0,
                    new ModelResourceLocation("harvestech:rubber_rod", "inventory")
            );

            ModelLoader.setCustomModelResourceLocation(
                    BAYONET,
                    0,
                    new ModelResourceLocation("harvestech:bayonet", "inventory")
            );
        }
    }
}