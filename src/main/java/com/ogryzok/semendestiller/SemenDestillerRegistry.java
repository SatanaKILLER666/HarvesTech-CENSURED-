package com.ogryzok.semendestiller;

import com.ogryzok.harvestech;
import com.ogryzok.semendestiller.block.BlockSemenDestillerBase;
import com.ogryzok.semendestiller.block.BlockSemenDestillerMotor;
import com.ogryzok.semendestiller.client.render.SemenDestillerBaseRenderer;
import com.ogryzok.semendestiller.client.render.SemenDestillerMotorRenderer;
import com.ogryzok.semendestiller.tile.TileSemenDestillerBase;
import com.ogryzok.semendestiller.tile.TileSemenDestillerMotor;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import com.ogryzok.client.item.ItemGeoBlock;
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
public final class SemenDestillerRegistry {
    public static final String MODID = harvestech.MODID;
    public static final int GUI_ID = 12;

    public static final BlockSemenDestillerBase SEMEN_DESTILLER_BASE = new BlockSemenDestillerBase();
    public static final BlockSemenDestillerMotor SEMEN_DESTILLER_MOTOR = new BlockSemenDestillerMotor();
    private static boolean initialized = false;

    private SemenDestillerRegistry() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        GameRegistry.registerTileEntity(TileSemenDestillerBase.class, new ResourceLocation(MODID, "semen_destiller_base"));
        GameRegistry.registerTileEntity(TileSemenDestillerMotor.class, new ResourceLocation(MODID, "semen_destiller_motor"));
    }

    public static void initClient() {
        ClientRegistry.bindTileEntitySpecialRenderer(TileSemenDestillerBase.class, new SemenDestillerBaseRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileSemenDestillerMotor.class, new SemenDestillerMotorRenderer());
    }

    public static Object getModInstance() {
        ModContainer container = Loader.instance().getIndexedModList().get(MODID);
        return container == null ? null : container.getMod();
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(SEMEN_DESTILLER_BASE);
        event.getRegistry().register(SEMEN_DESTILLER_MOTOR);
    }

        @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(new ItemGeoBlock(
                SEMEN_DESTILLER_BASE,
                new ResourceLocation(MODID, "geo/semen_destiller_base.geo.json"),
                new ResourceLocation(MODID, "textures/blocks/semen_destiller.png"),
                new ResourceLocation(MODID, "animations/chair.animation.json")
        ));
        event.getRegistry().register(new ItemGeoBlock(
                SEMEN_DESTILLER_MOTOR,
                new ResourceLocation(MODID, "geo/semen_destiller_motor.geo.json"),
                new ResourceLocation(MODID, "textures/blocks/semen_destiller.png"),
                new ResourceLocation(MODID, "animations/chair.animation.json")
        ));
    }


    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        Item mechanicalPipe = Item.getByNameOrId("mekanism:transmitter");
        Item steelCasing = Item.getByNameOrId("mekanism:basicblock");
        Item basicFluidTank = Item.getByNameOrId("mekanism:machineblock2");

        if (mechanicalPipe == null || steelCasing == null || basicFluidTank == null) {
            return;
        }

        registerDestillerRecipes(event, "plateAluminum", "aluminum");
        registerDestillerRecipes(event, "plateAluminium", "aluminium");
    }

    private static void registerDestillerRecipes(RegistryEvent.Register<IRecipe> event, String aluminumPlateOreName, String suffix) {
        Item mechanicalPipe = Item.getByNameOrId("mekanism:transmitter");
        Item steelCasing = Item.getByNameOrId("mekanism:basicblock");
        Item basicFluidTank = Item.getByNameOrId("mekanism:machineblock2");

        if (mechanicalPipe == null || steelCasing == null || basicFluidTank == null) {
            return;
        }

        event.getRegistry().register(
                new ShapedOreRecipe(
                        new ResourceLocation(MODID, "semen_destiller_motor_" + suffix),
                        new ItemStack(SEMEN_DESTILLER_MOTOR),
                        "AAA",
                        "ACA",
                        "APA",
                        'A', aluminumPlateOreName,
                        'C', new ItemStack(steelCasing, 1, 8),
                        'P', new ItemStack(mechanicalPipe, 1, 1)
                ).setRegistryName(new ResourceLocation(MODID, "semen_destiller_motor_" + suffix))
        );

        event.getRegistry().register(
                new ShapedOreRecipe(
                        new ResourceLocation(MODID, "semen_destiller_base_" + suffix),
                        new ItemStack(SEMEN_DESTILLER_BASE),
                        "ATA",
                        "ACA",
                        "PAP",
                        'A', aluminumPlateOreName,
                        'T', new ItemStack(basicFluidTank, 1, 11),
                        'C', new ItemStack(steelCasing, 1, 8),
                        'P', new ItemStack(mechanicalPipe, 1, 1)
                ).setRegistryName(new ResourceLocation(MODID, "semen_destiller_base_" + suffix))
        );
    }

    @Mod.EventBusSubscriber(modid = MODID, value = net.minecraftforge.fml.relauncher.Side.CLIENT)
    public static final class ClientOnlyEvents {
        @SubscribeEvent
        public static void registerModels(net.minecraftforge.client.event.ModelRegistryEvent event) {
            Item baseItem = Item.getItemFromBlock(SEMEN_DESTILLER_BASE);
            if (baseItem != null) {
                net.minecraftforge.client.model.ModelLoader.setCustomModelResourceLocation(baseItem, 0,
                        new ModelResourceLocation("harvestech:semen_destiller_base", "inventory"));
                if (baseItem instanceof ItemGeoBlock) {
                    ((ItemGeoBlock) baseItem).initTEISR();
                }
            }
            Item motorItem = Item.getItemFromBlock(SEMEN_DESTILLER_MOTOR);
            if (motorItem != null) {
                net.minecraftforge.client.model.ModelLoader.setCustomModelResourceLocation(motorItem, 0,
                        new ModelResourceLocation("harvestech:semen_destiller_motor", "inventory"));
                if (motorItem instanceof ItemGeoBlock) {
                    ((ItemGeoBlock) motorItem).initTEISR();
                }
            }
        }
    }
}
