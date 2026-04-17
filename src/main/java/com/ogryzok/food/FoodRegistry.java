package com.ogryzok.food;

import com.ogryzok.harvestech;
import com.ogryzok.food.item.ItemBasic;
import com.ogryzok.food.item.ItemFoodBasic;
import com.ogryzok.food.item.ItemDirtyBiomassFood;
import com.ogryzok.manualharvest.ManualHarvestRegistry;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = harvestech.MODID)
public final class FoodRegistry {

    public static final Item CAN = new ItemBasic("can", CreativeTabs.MISC);
    public static final Item WOODEN_JAR = new ItemBasic("wooden_jar", CreativeTabs.MISC);

    // Все банки/кадушки кроме пустых - съедобные
    public static final Item BIOMASS_CAN = new ItemFoodBasic("biomass_can", 4, 0.5F, false, true, CAN);
    public static final Item DISTILLED_BIOMASS_CAN = new ItemFoodBasic("distilled_biomass_can", 4, 0.5F, false, true, CAN);
    public static final Item ENRICHED_BIOMASS_CAN = new ItemFoodBasic("enriched_biomass_can", 6, 0.6666667F, false, true, CAN);
    public static final Item BIOMASS_JAR = singleStack(new ItemFoodBasic("biomass_jar", 4, 0.5F, false, true, WOODEN_JAR));
    public static final Item DISTILLED_BIOMASS_JAR = singleStack(new ItemFoodBasic("distilled_biomass_jar", 4, 0.5F, false, true, WOODEN_JAR));
    public static final Item ENRICHED_BIOMASS_JAR = singleStack(new ItemFoodBasic("enriched_biomass_jar", 6, 0.6666667F, false, true, WOODEN_JAR));
    public static final Item DIRTY_BIOMASS_WOODEN_JAR = singleStack(new ItemDirtyBiomassFood("dirty_biomass_wooden_jar", 4, 0.5F, true, WOODEN_JAR, 0.5F));

    // protein_biomass пока оставил как было, потому что ты для него новые значения не задавал
    public static final Item PROTEIN_BIOMASS = new ItemFoodBasic("protein_biomass", 5, 0.2F, false);
    public static final Item EVAPORATED_BIOMASS = new ItemBasic("evaporated_biomass", CreativeTabs.MATERIALS);
    public static final Item TOXIC_BIOMASS = new ItemBasic("toxic_biomass", CreativeTabs.MATERIALS);
    public static final Item PROTEIN_RISSOLE = new ItemFoodBasic("protein_rissole", 6, 1.0F, false, true);
    public static final Item PROTEIN_STEAK = new ItemFoodBasic("protein_steak", 8, 1.25F, true, true);
    public static final Item PROTEIN_BURGER = new ItemFoodBasic("protein_burger", 14, 1.1428571F, true);

    private FoodRegistry() {
    }

    public static void init() {
        GameRegistry.addSmelting(PROTEIN_STEAK, new ItemStack(PROTEIN_RISSOLE), 0.35F);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
                CAN,
                WOODEN_JAR,
                BIOMASS_CAN,
                DISTILLED_BIOMASS_CAN,
                ENRICHED_BIOMASS_CAN,
                BIOMASS_JAR,
                DISTILLED_BIOMASS_JAR,
                ENRICHED_BIOMASS_JAR,
                DIRTY_BIOMASS_WOODEN_JAR,
                PROTEIN_BIOMASS,
                EVAPORATED_BIOMASS,
                TOXIC_BIOMASS,
                PROTEIN_RISSOLE,
                PROTEIN_STEAK,
                PROTEIN_BURGER
        );
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        event.getRegistry().register(
                new ShapedOreRecipe(
                        new ResourceLocation(harvestech.MODID, "wooden_jar"),
                        new ItemStack(WOODEN_JAR),
                        "P P",
                        "P P",
                        "PPP",
                        'P', "plankWood"
                ).setRegistryName(new ResourceLocation(harvestech.MODID, "wooden_jar"))
        );

        event.getRegistry().register(
                new ShapedOreRecipe(
                        new ResourceLocation(harvestech.MODID, "protein_burger"),
                        new ItemStack(PROTEIN_BURGER),
                        " B ",
                        " R ",
                        " B ",
                        'B', new ItemStack(net.minecraft.init.Items.BREAD),
                        'R', new ItemStack(PROTEIN_RISSOLE)
                ).setRegistryName(new ResourceLocation(harvestech.MODID, "protein_burger"))
        );

    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        registerModel(CAN);
        registerModel(WOODEN_JAR);
        registerModel(BIOMASS_CAN);
        registerModel(DISTILLED_BIOMASS_CAN);
        registerModel(ENRICHED_BIOMASS_CAN);
        registerModel(BIOMASS_JAR);
        registerModel(DISTILLED_BIOMASS_JAR);
        registerModel(ENRICHED_BIOMASS_JAR);
        registerModel(DIRTY_BIOMASS_WOODEN_JAR);
        registerModel(PROTEIN_BIOMASS);
        registerModel(EVAPORATED_BIOMASS);
        registerModel(TOXIC_BIOMASS);
        registerModel(PROTEIN_RISSOLE);
        registerModel(PROTEIN_STEAK);
        registerModel(PROTEIN_BURGER);
    }


    public static boolean isEmptyBiomassContainer(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() == CAN || stack.getItem() == WOODEN_JAR);
    }

    public static boolean isDirtyBiomassContainer(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == DIRTY_BIOMASS_WOODEN_JAR;
    }

    public static ItemStack getFilledBiomassContainer(ItemStack emptyContainer) {
        if (emptyContainer.isEmpty()) return ItemStack.EMPTY;
        if (emptyContainer.getItem() == CAN) return new ItemStack(BIOMASS_CAN);
        if (emptyContainer.getItem() == WOODEN_JAR) return new ItemStack(BIOMASS_JAR);
        return ItemStack.EMPTY;
    }

    public static ItemStack getFilledDistilledContainer(ItemStack emptyContainer) {
        if (emptyContainer.isEmpty()) return ItemStack.EMPTY;
        if (emptyContainer.getItem() == CAN) return new ItemStack(DISTILLED_BIOMASS_CAN);
        if (emptyContainer.getItem() == WOODEN_JAR) return new ItemStack(DISTILLED_BIOMASS_JAR);
        return ItemStack.EMPTY;
    }

    public static ItemStack getFilledEnrichedContainer(ItemStack emptyContainer) {
        if (emptyContainer.isEmpty()) return ItemStack.EMPTY;
        if (emptyContainer.getItem() == CAN) return new ItemStack(ENRICHED_BIOMASS_CAN);
        if (emptyContainer.getItem() == WOODEN_JAR) return new ItemStack(ENRICHED_BIOMASS_JAR);
        return ItemStack.EMPTY;
    }

    public static ItemStack getDirtyBiomassContainer(ItemStack emptyContainer) {
        if (emptyContainer.isEmpty()) return ItemStack.EMPTY;
        if (emptyContainer.getItem() == CAN) return new ItemStack(ManualHarvestRegistry.DIRTY_BIOMASS);
        if (emptyContainer.getItem() == WOODEN_JAR) return new ItemStack(DIRTY_BIOMASS_WOODEN_JAR);
        return ItemStack.EMPTY;
    }

    public static ItemStack getEmptyContainer(ItemStack filledContainer) {
        if (filledContainer.isEmpty()) return ItemStack.EMPTY;
        if (filledContainer.getItem() == ManualHarvestRegistry.DIRTY_BIOMASS) {
            return new ItemStack(CAN);
        }
        if (filledContainer.getItem() == DIRTY_BIOMASS_WOODEN_JAR
                || filledContainer.getItem() == BIOMASS_JAR
                || filledContainer.getItem() == DISTILLED_BIOMASS_JAR
                || filledContainer.getItem() == ENRICHED_BIOMASS_JAR) {
            return new ItemStack(WOODEN_JAR);
        }
        if (filledContainer.getItem() == BIOMASS_CAN
                || filledContainer.getItem() == DISTILLED_BIOMASS_CAN
                || filledContainer.getItem() == ENRICHED_BIOMASS_CAN) {
            return new ItemStack(CAN);
        }
        return ItemStack.EMPTY;
    }


    private static Item singleStack(Item item) {
        item.setMaxStackSize(1);
        return item;
    }

    @SideOnly(Side.CLIENT)
    private static void registerModel(Item item) {
        ModelLoader.setCustomModelResourceLocation(
                item,
                0,
                new ModelResourceLocation(item.getRegistryName(), "inventory")
        );
    }
}