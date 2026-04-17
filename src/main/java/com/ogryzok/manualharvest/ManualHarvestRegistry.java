package com.ogryzok.manualharvest;

import com.ogryzok.client.item.ItemGeoSimple;
import com.ogryzok.harvestech;
import com.ogryzok.food.item.ItemBasic;
import com.ogryzok.food.item.ItemDirtyBiomassFood;
import com.ogryzok.manualharvest.block.BlockBiomassResidue;
import com.ogryzok.manualharvest.block.BlockFilter;
import com.ogryzok.manualharvest.block.BlockFilterBase;
import com.ogryzok.manualharvest.block.BlockPallet;
import com.ogryzok.manualharvest.block.BlockRottingTank;
import com.ogryzok.manualharvest.client.ManualHarvestClientHandler;
import com.ogryzok.manualharvest.client.render.FilterGeoRenderer;
import com.ogryzok.manualharvest.client.render.PalletGeoRenderer;
import com.ogryzok.manualharvest.client.render.RottingTankGeoRenderer;
import com.ogryzok.manualharvest.tile.TileFilter;
import com.ogryzok.manualharvest.tile.TilePallet;
import com.ogryzok.manualharvest.tile.TileRottingTank;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.item.crafting.IRecipe;

@Mod.EventBusSubscriber(modid = harvestech.MODID)
public final class ManualHarvestRegistry {
    public static final BlockPallet PALLET = new BlockPallet();
    public static final BlockBiomassResidue BIOMASS_RESIDUE = new BlockBiomassResidue();
    public static final BlockFilter FILTER = new BlockFilter();
    public static final BlockFilterBase FILTER_BASE = new BlockFilterBase();
    public static final BlockRottingTank ROTTING_TANK = new BlockRottingTank();

    public static final Item FILTER_HOPPER = new ItemGeoSimple(
            "filter_hopper",
            CreativeTabs.MATERIALS,
            new ResourceLocation(harvestech.MODID, "geo/filter_hopper.geo.json"),
            new ResourceLocation(harvestech.MODID, "textures/blocks/filter.png"),
            new ResourceLocation(harvestech.MODID, "animations/filter.animation.json")
    );
    public static final Item DIRTY_BIOMASS = new ItemDirtyBiomassFood("dirty_biomass", 4, 0.5F, true, com.ogryzok.food.FoodRegistry.CAN, 0.5F);
    public static final Item MANUAL_COLLECTION_ICON = new ItemBasic("manual_collection_icon", null);

    private static boolean commonInit;
    private static boolean clientInit;

    private ManualHarvestRegistry() {
    }

    public static void init() {
        if (commonInit) return;
        commonInit = true;
        GameRegistry.registerTileEntity(TilePallet.class, new ResourceLocation(harvestech.MODID, "pallet"));
        GameRegistry.registerTileEntity(TileFilter.class, new ResourceLocation(harvestech.MODID, "filter"));
        GameRegistry.registerTileEntity(TileRottingTank.class, new ResourceLocation(harvestech.MODID, "rotting_tank"));
    }

    @SideOnly(Side.CLIENT)
    public static void initClient() {
        if (clientInit) return;
        clientInit = true;
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new ManualHarvestClientHandler());
        ClientRegistry.bindTileEntitySpecialRenderer(TilePallet.class, new PalletGeoRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileFilter.class, new FilterGeoRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileRottingTank.class, new RottingTankGeoRenderer());
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(PALLET);
        event.getRegistry().register(BIOMASS_RESIDUE);
        event.getRegistry().register(FILTER);
        event.getRegistry().register(FILTER_BASE);
        event.getRegistry().register(ROTTING_TANK);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(new com.ogryzok.client.item.ItemGeoBlock(
                PALLET,
                new ResourceLocation(harvestech.MODID, "geo/pallet.geo.json"),
                new ResourceLocation(harvestech.MODID, "textures/blocks/pallet.png"),
                new ResourceLocation(harvestech.MODID, "animations/chair.animation.json")
        ));
        event.getRegistry().register(new com.ogryzok.client.item.ItemGeoBlock(
                FILTER,
                new ResourceLocation(harvestech.MODID, "geo/empty_filter.geo.json"),
                new ResourceLocation(harvestech.MODID, "textures/blocks/filter.png"),
                new ResourceLocation(harvestech.MODID, "animations/filter.animation.json")
        ));
        event.getRegistry().register(new com.ogryzok.client.item.ItemGeoBlock(
                FILTER_BASE,
                new ResourceLocation(harvestech.MODID, "geo/filter_base.geo.json"),
                new ResourceLocation(harvestech.MODID, "textures/blocks/filter.png"),
                new ResourceLocation(harvestech.MODID, "animations/filter.animation.json")
        ));
        event.getRegistry().register(new com.ogryzok.client.item.ItemGeoBlock(
                ROTTING_TANK,
                new ResourceLocation(harvestech.MODID, "geo/rotting_tank.geo.json"),
                new ResourceLocation(harvestech.MODID, "textures/blocks/rotting_tank_biomass.png"),
                new ResourceLocation(harvestech.MODID, "animations/chair.animation.json")
        ));
        event.getRegistry().register(FILTER_HOPPER);
        event.getRegistry().register(DIRTY_BIOMASS);
        event.getRegistry().register(MANUAL_COLLECTION_ICON);
    }


    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        event.getRegistry().register(
                new ShapedOreRecipe(
                        new ResourceLocation(harvestech.MODID, "filter_hopper"),
                        new ItemStack(FILTER_HOPPER),
                        " P ",
                        "S S",
                        "STS",
                        'P', new ItemStack(Items.FLOWER_POT),
                        'S', "stickWood",
                        'T', new ItemStack(Blocks.TRAPDOOR)
                ).setRegistryName(new ResourceLocation(harvestech.MODID, "filter_hopper"))
        );


        event.getRegistry().register(
                new ShapedOreRecipe(
                        new ResourceLocation(harvestech.MODID, "filter_base"),
                        new ItemStack(FILTER_BASE),
                        "PLP",
                        "FIF",
                        "PLP",
                        'P', "plankWood",
                        'L', "logWood",
                        'F', "fenceWood",
                        'I', new ItemStack(Blocks.IRON_BARS)
                ).setRegistryName(new ResourceLocation(harvestech.MODID, "filter_base"))
        );


        event.getRegistry().register(
                new ShapedOreRecipe(
                        new ResourceLocation(harvestech.MODID, "pallet"),
                        new ItemStack(PALLET),
                        "ISI",
                        "SBS",
                        "ISI",
                        'I', new ItemStack(Items.IRON_INGOT),
                        'S', "ingotSteel",
                        'B', new ItemStack(Blocks.IRON_BARS)
                ).setRegistryName(new ResourceLocation(harvestech.MODID, "pallet"))
        );

        event.getRegistry().register(
                new ShapedOreRecipe(
                        new ResourceLocation(harvestech.MODID, "rotting_tank"),
                        new ItemStack(ROTTING_TANK),
                        "   ",
                        "P P",
                        "PPP",
                        'P', "plateIron"
                ).setRegistryName(new ResourceLocation(harvestech.MODID, "rotting_tank"))
        );
    }
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerModels(net.minecraftforge.client.event.ModelRegistryEvent event) {
        registerModel(Item.getItemFromBlock(PALLET));
        registerModel(Item.getItemFromBlock(FILTER));
        registerModel(Item.getItemFromBlock(FILTER_BASE));
        registerModel(Item.getItemFromBlock(ROTTING_TANK));
        registerModel(FILTER_HOPPER);
        registerModel(DIRTY_BIOMASS);
        registerModel(MANUAL_COLLECTION_ICON);

        Item palletItem = Item.getItemFromBlock(PALLET);
        if (palletItem instanceof com.ogryzok.client.item.ItemGeoBlock) {
            ((com.ogryzok.client.item.ItemGeoBlock) palletItem).initTEISR();
        }

        Item filterItem = Item.getItemFromBlock(FILTER);
        if (filterItem instanceof com.ogryzok.client.item.ItemGeoBlock) {
            ((com.ogryzok.client.item.ItemGeoBlock) filterItem).initTEISR();
        }

        Item filterBaseItem = Item.getItemFromBlock(FILTER_BASE);
        if (filterBaseItem instanceof com.ogryzok.client.item.ItemGeoBlock) {
            ((com.ogryzok.client.item.ItemGeoBlock) filterBaseItem).initTEISR();
        }

        Item rottingTankItem = Item.getItemFromBlock(ROTTING_TANK);
        if (rottingTankItem instanceof com.ogryzok.client.item.ItemGeoBlock) {
            ((com.ogryzok.client.item.ItemGeoBlock) rottingTankItem).initTEISR();
        }

        if (FILTER_HOPPER instanceof ItemGeoSimple) {
            ((ItemGeoSimple) FILTER_HOPPER).initTEISR();
        }
    }

    @SideOnly(Side.CLIENT)
    private static void registerModel(Item item) {
        net.minecraftforge.client.model.ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }
}
