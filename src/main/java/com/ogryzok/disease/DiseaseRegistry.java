package com.ogryzok.disease;

import com.ogryzok.disease.client.DiseaseOverlayHandler;
import com.ogryzok.disease.client.MalePowerClientHandler;
import com.ogryzok.disease.client.SeedKeeperClientHandler;
import com.ogryzok.disease.item.ItemAidsVaccine;
import com.ogryzok.disease.item.ItemSyringe;
import com.ogryzok.disease.potion.PotionDiseaseStage;
import com.ogryzok.disease.potion.PotionMalePower;
import com.ogryzok.disease.potion.PotionSeedKeeper;
import com.ogryzok.harvestech;
import com.ogryzok.food.FoodRegistry;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.potion.Potion;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = harvestech.MODID)
public final class DiseaseRegistry {
    public static final Item BIOMASS_CAN_TRIGGER_ITEM = FoodRegistry.BIOMASS_CAN;

    public static final Potion HIV = new PotionDiseaseStage("hiv", 0x7A0022, 0,
            "fa2d8f10-92e2-4b26-a4bf-1d1111111111", 0.8D,
            "fa2d8f10-92e2-4b26-a4bf-2d1111111111", 1.0D);

    public static final Potion AIDS_STAGE_1 = new PotionDiseaseStage("aids_stage_1", 0x8B1030, 1,
            "fa2d8f10-92e2-4b26-a4bf-1d2222222222", 0.4D,
            "fa2d8f10-92e2-4b26-a4bf-2d2222222222", 0.9D);

    public static final Potion AIDS_STAGE_2 = new PotionDiseaseStage("aids_stage_2", 0x9B1A35, 2,
            "fa2d8f10-92e2-4b26-a4bf-1d3333333333", 0.2D,
            "fa2d8f10-92e2-4b26-a4bf-2d3333333333", 0.5D);

    public static final Potion VEGETABLE = new PotionDiseaseStage("vegetable", 0x5B0E1A, 3,
            "fa2d8f10-92e2-4b26-a4bf-1d4444444444", 0.15D,
            "fa2d8f10-92e2-4b26-a4bf-2d4444444444", 0.1D);

    public static final Potion MALE_POWER = new PotionMalePower();
    public static final Potion SEED_KEEPER = new PotionSeedKeeper();

    public static final ItemAidsVaccine AIDS_VACCINE = new ItemAidsVaccine();
    public static final ItemAidsVaccine MALE_POWER_STEROID = new ItemAidsVaccine("male_power_steroid", "male_power_steroid", false);
    public static final ItemSyringe SYRINGE = new ItemSyringe();
    public static final DamageSource DISEASE_DAMAGE = new DamageSource("harvestech_disease").setDamageBypassesArmor();

    private static boolean initialized;

    private DiseaseRegistry() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        MinecraftForge.EVENT_BUS.register(new DiseaseHandler());
        MinecraftForge.EVENT_BUS.register(new MalePowerHandler());
    }

    @SideOnly(Side.CLIENT)
    public static void initClient() {
        MinecraftForge.EVENT_BUS.register(new DiseaseOverlayHandler());
        MinecraftForge.EVENT_BUS.register(new MalePowerClientHandler());
        MinecraftForge.EVENT_BUS.register(new SeedKeeperClientHandler());
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(AIDS_VACCINE, MALE_POWER_STEROID, SYRINGE);
    }


    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        Item ic2FluidCell = ForgeRegistries.ITEMS.getValue(new ResourceLocation("ic2", "fluid_cell"));
        if (ic2FluidCell == null) {
            return;
        }

        event.getRegistry().register(
                new ShapedOreRecipe(
                        new ResourceLocation(harvestech.MODID, "syringe"),
                        new ItemStack(SYRINGE),
                        " T ",
                        " C ",
                        " S ",
                        'T', "nuggetTin",
                        'C', new ItemStack(ic2FluidCell, 1, 0),
                        'S', "ingotSteel"
                ).setRegistryName(new ResourceLocation(harvestech.MODID, "syringe"))
        );
    }

    @SubscribeEvent
    public static void registerPotions(RegistryEvent.Register<Potion> event) {
        event.getRegistry().registerAll(HIV, AIDS_STAGE_1, AIDS_STAGE_2, VEGETABLE, MALE_POWER, SEED_KEEPER);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        AIDS_VACCINE.initTEISR();
        MALE_POWER_STEROID.initTEISR();
        SYRINGE.initTEISR();
        ModelLoader.setCustomModelResourceLocation(
                AIDS_VACCINE,
                0,
                new ModelResourceLocation(harvestech.MODID + ":aids_vaccine", "inventory")
        );
        ModelLoader.setCustomModelResourceLocation(
                MALE_POWER_STEROID,
                0,
                new ModelResourceLocation(harvestech.MODID + ":male_power_steroid", "inventory")
        );
        ModelLoader.setCustomModelResourceLocation(
                SYRINGE,
                0,
                new ModelResourceLocation(harvestech.MODID + ":syringe", "inventory")
        );
    }
}