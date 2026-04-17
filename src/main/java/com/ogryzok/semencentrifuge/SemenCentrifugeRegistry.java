package com.ogryzok.semencentrifuge;

import com.ogryzok.harvestech;
import com.ogryzok.semencentrifuge.block.BlockSemenCentrifugeBase;
import com.ogryzok.semencentrifuge.block.BlockSemenCentrifugeMotor;
import com.ogryzok.semencentrifuge.client.render.SemenCentrifugeBaseRenderer;
import com.ogryzok.semencentrifuge.client.render.SemenCentrifugeMotorRenderer;
import com.ogryzok.semencentrifuge.tile.TileSemenCentrifugeBase;
import com.ogryzok.semencentrifuge.tile.TileSemenCentrifugeMotor;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import com.ogryzok.client.item.ItemGeoBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraft.item.crafting.IRecipe;

@Mod.EventBusSubscriber(modid = harvestech.MODID)
public final class SemenCentrifugeRegistry {
    public static final String MODID = harvestech.MODID;

    public static final int GUI_ID_BASE = 10;
    public static final int GUI_ID_MOTOR = 11;

    public static final BlockSemenCentrifugeBase SEMEN_CENTRIFUGE_BASE = new BlockSemenCentrifugeBase();
    public static final BlockSemenCentrifugeMotor SEMEN_CENTRIFUGE_MOTOR = new BlockSemenCentrifugeMotor();

    private static boolean initialized = false;

    private SemenCentrifugeRegistry() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        initialized = true;
        GameRegistry.registerTileEntity(TileSemenCentrifugeBase.class, new ResourceLocation(MODID, "semen_centrifuge_base"));
        GameRegistry.registerTileEntity(TileSemenCentrifugeMotor.class, new ResourceLocation(MODID, "semen_centrifuge_motor"));

    }

    public static void initClient() {
        ClientRegistry.bindTileEntitySpecialRenderer(TileSemenCentrifugeBase.class, new SemenCentrifugeBaseRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileSemenCentrifugeMotor.class, new SemenCentrifugeMotorRenderer());
    }

    public static Object getModInstance() {
        ModContainer container = Loader.instance().getIndexedModList().get(MODID);
        return container == null ? null : container.getMod();
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(SEMEN_CENTRIFUGE_BASE);
        event.getRegistry().register(SEMEN_CENTRIFUGE_MOTOR);
    }

        @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(new ItemGeoBlock(
                SEMEN_CENTRIFUGE_BASE,
                new ResourceLocation(MODID, "geo/semen_centrifuge_base.geo.json"),
                new ResourceLocation(MODID, "textures/blocks/semen_centrifuge.png"),
                new ResourceLocation(MODID, "animations/chair.animation.json")
        ));
        event.getRegistry().register(new ItemGeoBlock(
                SEMEN_CENTRIFUGE_MOTOR,
                new ResourceLocation(MODID, "geo/semen_centrifuge_motor.geo.json"),
                new ResourceLocation(MODID, "textures/blocks/semen_centrifuge.png"),
                new ResourceLocation(MODID, "animations/chair.animation.json")
        ));
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        Item mechanicalPipe = Item.getByNameOrId("mekanism:transmitter");
        Item steelCasing = Item.getByNameOrId("mekanism:basicblock");
        Item basicFluidTank = Item.getByNameOrId("mekanism:machineblock2");
        Item electricPump = Item.getByNameOrId("mekanism:machineblock");
        Item controlCircuit = Item.getByNameOrId("mekanism:controlcircuit");
        Item rubberRod = Item.getByNameOrId(MODID + ":rubber_rod");

        if (mechanicalPipe == null || steelCasing == null || basicFluidTank == null) {
            return;
        }

        event.getRegistry().register(
                new ShapedOreRecipe(
                        new ResourceLocation(MODID, "semen_centrifuge_base"),
                        new ItemStack(SEMEN_CENTRIFUGE_BASE),
                        "GSG",
                        "PTP",
                        "LBL",
                        'G', "gearIron",
                        'S', "ingotSteel",
                        'P', new ItemStack(mechanicalPipe, 1, 1),
                        'T', new ItemStack(steelCasing, 1, 8),
                        'L', "ingotLead",
                        'B', new ItemStack(basicFluidTank, 1, 11)
                ).setRegistryName(new ResourceLocation(MODID, "semen_centrifuge_base"))
        );

        if (electricPump == null || controlCircuit == null || rubberRod == null) {
            return;
        }

        event.getRegistry().register(
                new ShapedOreRecipe(
                        new ResourceLocation(MODID, "semen_centrifuge_motor"),
                        new ItemStack(SEMEN_CENTRIFUGE_MOTOR),
                        "PSP",
                        "TRT",
                        "ICI",
                        'P', new ItemStack(electricPump, 1, 12),
                        'S', new ItemStack(controlCircuit, 1, 2),
                        'T', new ItemStack(mechanicalPipe, 1, 1),
                        'R', new ItemStack(rubberRod, 1, 0),
                        'I', "plateIron",
                        'C', new ItemStack(steelCasing, 1, 8)
                ).setRegistryName(new ResourceLocation(MODID, "semen_centrifuge_motor"))
        );
    }

    @Mod.EventBusSubscriber(modid = MODID, value = net.minecraftforge.fml.relauncher.Side.CLIENT)
    public static final class ClientOnlyEvents {
        private ClientOnlyEvents() {
        }

        @SubscribeEvent
        public static void registerModels(ModelRegistryEvent event) {
            Item baseItem = Item.getItemFromBlock(SEMEN_CENTRIFUGE_BASE);
            if (baseItem != null) {
                net.minecraftforge.client.model.ModelLoader.setCustomModelResourceLocation(
                        baseItem,
                        0,
                        new ModelResourceLocation("harvestech:semen_centrifuge_base", "inventory")
                );
                if (baseItem instanceof ItemGeoBlock) {
                    ((ItemGeoBlock) baseItem).initTEISR();
                }
            }

            Item motorItem = Item.getItemFromBlock(SEMEN_CENTRIFUGE_MOTOR);
            if (motorItem != null) {
                net.minecraftforge.client.model.ModelLoader.setCustomModelResourceLocation(
                        motorItem,
                        0,
                        new ModelResourceLocation("harvestech:semen_centrifuge_motor", "inventory")
                );
                if (motorItem instanceof ItemGeoBlock) {
                    ((ItemGeoBlock) motorItem).initTEISR();
                }
            }
        }
    }
}
