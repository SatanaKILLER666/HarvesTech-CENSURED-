package com.ogryzok.jei;

import com.ogryzok.blender.BlenderRegistry;
import com.ogryzok.blender.client.gui.GuiBlender;
import com.ogryzok.disease.DiseaseRegistry;
import com.ogryzok.harvestech;
import com.ogryzok.food.FoodRegistry;
import com.ogryzok.fluids.ModFluids;
import com.ogryzok.manualharvest.ManualHarvestRegistry;
import com.ogryzok.manualharvest.tile.TileRottingTank;
import com.ogryzok.mrnasynthesizer.MRNASynthesizerRegistry;
import com.ogryzok.mrnasynthesizer.client.gui.GuiMRNASynthesizer;
import com.ogryzok.proteinformer.ProteinFormerRegistry;
import com.ogryzok.proteinformer.client.gui.GuiProteinFormer;
import com.ogryzok.semencentrifuge.SemenCentrifugeRegistry;
import com.ogryzok.semencentrifuge.client.gui.GuiSemenCentrifugeBase;
import com.ogryzok.semendestiller.SemenDestillerRegistry;
import com.ogryzok.semendestiller.client.gui.GuiSemenDestiller;
import com.ogryzok.semenenrichment.SemenEnrichmentRegistry;
import com.ogryzok.semenenrichment.client.gui.GuiSemenEnrichmentChamber;
import com.ogryzok.separator.SeparatorRegistry;
import com.ogryzok.separator.client.gui.GuiSeparator;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import net.minecraft.client.resources.I18n;
import java.util.Arrays;
import java.util.List;

@JEIPlugin
public class JeiPluginHarvestech implements IModPlugin {
    public static final String UID_BLENDER = harvestech.MODID + ":00_blender";
    public static final String UID_PROTEIN_FORMER = harvestech.MODID + ":01_protein_former";
    public static final String UID_DESTILLER = harvestech.MODID + ":02_semen_destiller";
    public static final String UID_DESTILLER_FILL = harvestech.MODID + ":03_semen_destiller_fill";
    public static final String UID_ENRICHMENT = harvestech.MODID + ":04_semen_enrichment";
    public static final String UID_ENRICHMENT_FILL = harvestech.MODID + ":05_semen_enrichment_fill";
    public static final String UID_SEPARATOR = harvestech.MODID + ":06_separator";
    public static final String UID_MRNA_SYNTH = harvestech.MODID + ":07_mrna_synth";
    public static final String UID_MRNA_FILL = harvestech.MODID + ":08_mrna_fill";
    public static final String UID_MANUAL_COLLECTION = harvestech.MODID + ":09_manual_collection";
    public static final String UID_PALLET_COLLECTION = harvestech.MODID + ":10_pallet_collection";
    public static final String UID_CENTRIFUGE_FILL = harvestech.MODID + ":11_centrifuge_fill";
    public static final String UID_ROTTING_TANK = harvestech.MODID + ":12_rotting_tank";

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        IGuiHelper guiHelper = registry.getJeiHelpers().getGuiHelper();
        registry.addRecipeCategories(
                new BlenderRecipeCategory(guiHelper),
                new ProteinFormerRecipeCategory(guiHelper),
                new DestillerRecipeCategory(guiHelper),
                new DestillerFillRecipeCategory(guiHelper),
                new EnrichmentRecipeCategory(guiHelper),
                new EnrichmentFillRecipeCategory(guiHelper),
                new ManualCollectionRecipeCategory(guiHelper),
                new PalletCollectionRecipeCategory(guiHelper),
                new CentrifugeFillRecipeCategory(guiHelper),
                new RottingTankRecipeCategory(guiHelper),
                new SeparatorRecipeCategory(guiHelper),
                new MrnaSynthesisRecipeCategory(guiHelper),
                new MrnaFillRecipeCategory(guiHelper)
        );
    }

    @Override
    public void register(IModRegistry registry) {
        registry.addRecipes(createBlenderRecipes(), UID_BLENDER);
        registry.addRecipes(createProteinFormerRecipes(), UID_PROTEIN_FORMER);
        registry.addRecipes(createDestillerRecipes(), UID_DESTILLER);
        registry.addRecipes(createDestillerFillRecipes(), UID_DESTILLER_FILL);
        registry.addRecipes(createEnrichmentRecipes(), UID_ENRICHMENT);
        registry.addRecipes(createEnrichmentFillRecipes(), UID_ENRICHMENT_FILL);
        registry.addRecipes(createManualCollectionRecipes(), UID_MANUAL_COLLECTION);
        registry.addRecipes(createPalletCollectionRecipes(), UID_PALLET_COLLECTION);
        registry.addRecipes(createCentrifugeFillRecipes(), UID_CENTRIFUGE_FILL);
        registry.addRecipes(createRottingTankRecipes(), UID_ROTTING_TANK);
        registry.addRecipes(createSeparatorRecipes(), UID_SEPARATOR);
        registry.addRecipes(createMrnaSynthRecipes(), UID_MRNA_SYNTH);
        registry.addRecipes(createMrnaFillRecipes(), UID_MRNA_FILL);

        registry.addRecipeCatalyst(new ItemStack(BlenderRegistry.BLENDER), UID_BLENDER);
        registry.addRecipeCatalyst(new ItemStack(ProteinFormerRegistry.PROTEIN_FORMER), UID_PROTEIN_FORMER);
        registry.addRecipeCatalyst(new ItemStack(SemenDestillerRegistry.SEMEN_DESTILLER_BASE), UID_DESTILLER, UID_DESTILLER_FILL);
        registry.addRecipeCatalyst(new ItemStack(SemenEnrichmentRegistry.SEMEN_ENRICHMENT_CHAMBER), UID_ENRICHMENT, UID_ENRICHMENT_FILL);
        registry.addRecipeCatalyst(new ItemStack(ManualHarvestRegistry.PALLET), UID_PALLET_COLLECTION);

        registry.addRecipeCatalyst(new ItemStack(SemenCentrifugeRegistry.SEMEN_CENTRIFUGE_BASE), UID_CENTRIFUGE_FILL);
        registry.addRecipeCatalyst(new ItemStack(ManualHarvestRegistry.ROTTING_TANK), UID_ROTTING_TANK);
        registry.addRecipeCatalyst(new ItemStack(SeparatorRegistry.SEPARATOR), UID_SEPARATOR);
        registry.addRecipeCatalyst(new ItemStack(MRNASynthesizerRegistry.MRNA_SYNTHESIZER), UID_MRNA_SYNTH, UID_MRNA_FILL);

        registry.addRecipeClickArea(GuiBlender.class, 70, 46, 20, 10, UID_BLENDER);
        registry.addRecipeClickArea(GuiProteinFormer.class, 64, 54, 28, 10, UID_PROTEIN_FORMER);
        registry.addRecipeClickArea(GuiSemenDestiller.class, 64, 50, 28, 10, UID_DESTILLER, UID_DESTILLER_FILL);
        registry.addRecipeClickArea(GuiSemenEnrichmentChamber.class, 64, 50, 28, 10, UID_ENRICHMENT, UID_ENRICHMENT_FILL);
        registry.addRecipeClickArea(GuiSemenCentrifugeBase.class, 16, 40, 36, 14, UID_CENTRIFUGE_FILL);
        registry.addRecipeClickArea(GuiSeparator.class, 64, 50, 16, 10, UID_SEPARATOR);
        registry.addRecipeClickArea(GuiMRNASynthesizer.class, 65, 40, 10, 8, UID_MRNA_SYNTH);
        registry.addRecipeClickArea(GuiMRNASynthesizer.class, 101, 40, 10, 8, UID_MRNA_SYNTH);
        registry.addRecipeClickArea(GuiMRNASynthesizer.class, 65, 67, 10, 8, UID_MRNA_SYNTH);
        registry.addRecipeClickArea(GuiMRNASynthesizer.class, 101, 67, 10, 8, UID_MRNA_SYNTH);
    }

    private static List<BlenderRecipe> createBlenderRecipes() {
        List<BlenderRecipe> recipes = new ArrayList<>();
        Item assimilated = ForgeRegistries.ITEMS.getValue(new ResourceLocation("srparasites", "assimilated_flesh"));
        if (assimilated != null) {
            recipes.add(new BlenderRecipe(new ItemStack(assimilated, 16), new FluidStack(ModFluids.TOXIC_FLESH, 500), 5000));
        }
        return recipes;
    }

    private static List<ProteinFormerRecipe> createProteinFormerRecipes() {
        List<ProteinFormerRecipe> recipes = new ArrayList<>();
        recipes.add(new ProteinFormerRecipe(new ItemStack(FoodRegistry.PROTEIN_BIOMASS), new ItemStack(FoodRegistry.PROTEIN_STEAK), 1000));
        return recipes;
    }

    private static List<DestillerRecipe> createDestillerRecipes() {
        List<DestillerRecipe> recipes = new ArrayList<>();
        recipes.add(new DestillerRecipe(new FluidStack(ModFluids.BIOMASS, 200), new FluidStack(ModFluids.DISTILLED_BIOMASS, 200), 1000));
        recipes.add(new DestillerRecipe(new FluidStack(ModFluids.FERMENTED_SEMEN, 200), new FluidStack(ModFluids.DISTILLED_FERMENTED_SEMEN, 200), 1000));
        recipes.add(new DestillerRecipe(new FluidStack(ModFluids.TOXIC_FLESH, 200), new FluidStack(ModFluids.NECRO_SUBSTRATE, 200), 1000));
        return recipes;
    }

    private static List<DestillerFillRecipe> createDestillerFillRecipes() {
        List<DestillerFillRecipe> recipes = new ArrayList<>();
        recipes.add(new DestillerFillRecipe(new ItemStack(FoodRegistry.CAN), new FluidStack(ModFluids.DISTILLED_BIOMASS, 200), new ItemStack(FoodRegistry.DISTILLED_BIOMASS_CAN), 80));
        recipes.add(new DestillerFillRecipe(new ItemStack(FoodRegistry.WOODEN_JAR), new FluidStack(ModFluids.DISTILLED_BIOMASS, 200), new ItemStack(FoodRegistry.DISTILLED_BIOMASS_JAR), 80));
        return recipes;
    }

    private static List<EnrichmentRecipe> createEnrichmentRecipes() {
        List<EnrichmentRecipe> recipes = new ArrayList<>();
        recipes.add(new EnrichmentRecipe(new FluidStack(ModFluids.DISTILLED_BIOMASS, 200), new FluidStack(ModFluids.ENRICHED_BIOMASS, 200), 1000));

        Fluid ic2Biomass = findIc2BiomassFluid();
        if (ic2Biomass != null) {
            recipes.add(new EnrichmentRecipe(new FluidStack(ModFluids.DISTILLED_FERMENTED_SEMEN, 200), new FluidStack(ic2Biomass, 200), 1000));
        }

        recipes.add(new EnrichmentRecipe(new FluidStack(ModFluids.NECRO_SUBSTRATE, 200), new FluidStack(ModFluids.BIO_TOXIN, 200), 1000));
        return recipes;
    }

    private static List<EnrichmentFillRecipe> createEnrichmentFillRecipes() {
        List<EnrichmentFillRecipe> recipes = new ArrayList<>();
        recipes.add(new EnrichmentFillRecipe(new ItemStack(FoodRegistry.CAN), new FluidStack(ModFluids.ENRICHED_BIOMASS, 200), new ItemStack(FoodRegistry.ENRICHED_BIOMASS_CAN), 80));
        recipes.add(new EnrichmentFillRecipe(new ItemStack(FoodRegistry.WOODEN_JAR), new FluidStack(ModFluids.ENRICHED_BIOMASS, 200), new ItemStack(FoodRegistry.ENRICHED_BIOMASS_JAR), 80));
        return recipes;
    }

    private static List<ManualCollectionRecipe> createManualCollectionRecipes() {
        List<ManualCollectionRecipe> recipes = new ArrayList<>();
        String baseText = I18n.format("jei.harvestech.manual_collection.base");
        recipes.add(new ManualCollectionRecipe(
                new ItemStack(ManualHarvestRegistry.MANUAL_COLLECTION_ICON),
                new ItemStack(FoodRegistry.CAN),
                new ItemStack(ManualHarvestRegistry.DIRTY_BIOMASS),
                Arrays.asList(baseText, I18n.format("jei.harvestech.manual_collection.can"))
        ));
        recipes.add(new ManualCollectionRecipe(
                new ItemStack(ManualHarvestRegistry.MANUAL_COLLECTION_ICON),
                new ItemStack(FoodRegistry.WOODEN_JAR),
                new ItemStack(FoodRegistry.DIRTY_BIOMASS_WOODEN_JAR),
                Arrays.asList(baseText, I18n.format("jei.harvestech.manual_collection.jar"))
        ));
        return recipes;
    }

    private static List<PalletCollectionRecipe> createPalletCollectionRecipes() {
        List<PalletCollectionRecipe> recipes = new ArrayList<>();
        String text = I18n.format("jei.harvestech.pallet_collection.base");
        recipes.add(new PalletCollectionRecipe(
                new ItemStack(ManualHarvestRegistry.MANUAL_COLLECTION_ICON),
                new ItemStack(ManualHarvestRegistry.PALLET),
                new ItemStack(FoodRegistry.BIOMASS_CAN, 2),
                Arrays.asList(text, I18n.format("jei.harvestech.pallet_collection.can"))
        ));
        recipes.add(new PalletCollectionRecipe(
                new ItemStack(ManualHarvestRegistry.MANUAL_COLLECTION_ICON),
                new ItemStack(ManualHarvestRegistry.PALLET),
                new ItemStack(FoodRegistry.BIOMASS_JAR, 2),
                Arrays.asList(text, I18n.format("jei.harvestech.pallet_collection.jar"))
        ));
        return recipes;
    }

    private static List<CentrifugeFillRecipe> createCentrifugeFillRecipes() {
        List<CentrifugeFillRecipe> recipes = new ArrayList<>();
        recipes.add(new CentrifugeFillRecipe(new ItemStack(FoodRegistry.WOODEN_JAR), new FluidStack(ModFluids.BIOMASS, 200), new ItemStack(FoodRegistry.BIOMASS_JAR), 80));
        recipes.add(new CentrifugeFillRecipe(new ItemStack(FoodRegistry.CAN), new FluidStack(ModFluids.BIOMASS, 200), new ItemStack(FoodRegistry.BIOMASS_CAN), 80));
        return recipes;
    }

    private static List<RottingTankRecipe> createRottingTankRecipes() {
        List<RottingTankRecipe> recipes = new ArrayList<>();
        List<ItemStack> acceptedInputs = Arrays.asList(
                new ItemStack(FoodRegistry.BIOMASS_CAN),
                new ItemStack(FoodRegistry.DISTILLED_BIOMASS_CAN),
                new ItemStack(FoodRegistry.ENRICHED_BIOMASS_CAN),
                new ItemStack(FoodRegistry.BIOMASS_JAR),
                new ItemStack(FoodRegistry.DISTILLED_BIOMASS_JAR),
                new ItemStack(FoodRegistry.ENRICHED_BIOMASS_JAR),
                new ItemStack(FoodRegistry.DIRTY_BIOMASS_WOODEN_JAR),
                new ItemStack(ManualHarvestRegistry.DIRTY_BIOMASS)
        );
        recipes.add(new RottingTankRecipe(
                acceptedInputs,
                new ItemStack(ManualHarvestRegistry.ROTTING_TANK),
                new FluidStack(ModFluids.FERMENTED_SEMEN, 200),
                TileRottingTank.ROTTING_TICKS
        ));
        return recipes;
    }

    private static List<SeparatorRecipe> createSeparatorRecipes() {
        List<SeparatorRecipe> recipes = new ArrayList<>();
        recipes.add(new SeparatorRecipe(new FluidStack(ModFluids.ENRICHED_BIOMASS, 200), new ItemStack(FoodRegistry.PROTEIN_BIOMASS), new FluidStack(FluidRegistry.WATER, 100), 1000));
        recipes.add(new SeparatorRecipe(new FluidStack(ModFluids.BIO_TOXIN, 1000), new ItemStack(FoodRegistry.TOXIC_BIOMASS), new FluidStack(FluidRegistry.WATER, 700), 1000));

        Fluid ic2Biomass = findIc2BiomassFluid();
        if (ic2Biomass != null) {
            recipes.add(new SeparatorRecipe(new FluidStack(ic2Biomass, 1000), new ItemStack(FoodRegistry.EVAPORATED_BIOMASS), new FluidStack(FluidRegistry.WATER, 500), 1000));
        }
        return recipes;
    }

    private static List<MrnaSynthesisRecipe> createMrnaSynthRecipes() {
        List<MrnaSynthesisRecipe> recipes = new ArrayList<>();
        recipes.add(new MrnaSynthesisRecipe(
                new ItemStack(FoodRegistry.EVAPORATED_BIOMASS),
                new ItemStack(FoodRegistry.TOXIC_BIOMASS),
                new FluidStack(FluidRegistry.WATER, 1000),
                new FluidStack(ModFluids.AIDS_VACCINE, 1000),
                7000
        ));
        recipes.add(new MrnaSynthesisRecipe(
                new ItemStack(FoodRegistry.EVAPORATED_BIOMASS),
                new ItemStack(FoodRegistry.PROTEIN_BIOMASS, 5),
                new FluidStack(FluidRegistry.WATER, 1000),
                new FluidStack(ModFluids.MALE_POWER_STEROID, 500),
                7000
        ));
        return recipes;
    }

    private static List<MrnaFillRecipe> createMrnaFillRecipes() {
        List<MrnaFillRecipe> recipes = new ArrayList<>();
        recipes.add(new MrnaFillRecipe(new ItemStack(DiseaseRegistry.SYRINGE), new FluidStack(ModFluids.AIDS_VACCINE, 250), new ItemStack(DiseaseRegistry.AIDS_VACCINE), 0));
        recipes.add(new MrnaFillRecipe(new ItemStack(DiseaseRegistry.SYRINGE, 2), new FluidStack(ModFluids.MALE_POWER_STEROID, 500), new ItemStack(DiseaseRegistry.MALE_POWER_STEROID, 2), 0));
        return recipes;
    }

    private static Fluid findIc2BiomassFluid() {
        Fluid direct = FluidRegistry.getFluid("biomass");
        if (looksLikeIc2Biomass(direct)) {
            return direct;
        }

        for (Fluid fluid : FluidRegistry.getRegisteredFluids().values()) {
            if (looksLikeIc2Biomass(fluid)) {
                return fluid;
            }
        }
        return null;
    }

    private static boolean looksLikeIc2Biomass(Fluid fluid) {
        if (fluid == null) return false;
        if (fluid == ModFluids.BIOMASS || fluid == ModFluids.DISTILLED_BIOMASS || fluid == ModFluids.ENRICHED_BIOMASS
                || fluid == ModFluids.FERMENTED_SEMEN || fluid == ModFluids.DISTILLED_FERMENTED_SEMEN) {
            return false;
        }

        String name = fluid.getName() == null ? "" : fluid.getName().toLowerCase(java.util.Locale.ROOT);
        String unloc = fluid.getUnlocalizedName() == null ? "" : fluid.getUnlocalizedName().toLowerCase(java.util.Locale.ROOT);
        if (!name.contains("biomass") && !unloc.contains("biomass")) {
            return false;
        }

        if (fluid.getBlock() != null && fluid.getBlock().getRegistryName() != null) {
            String ns = fluid.getBlock().getRegistryName().getNamespace().toLowerCase(java.util.Locale.ROOT);
            String path = fluid.getBlock().getRegistryName().getPath().toLowerCase(java.util.Locale.ROOT);
            if ("ic2".equals(ns) && path.contains("biomass")) {
                return true;
            }
        }

        if (fluid.getStill() != null) {
            String ns = fluid.getStill().getNamespace().toLowerCase(java.util.Locale.ROOT);
            String path = fluid.getStill().getPath().toLowerCase(java.util.Locale.ROOT);
            if ("ic2".equals(ns) && path.contains("biomass")) {
                return true;
            }
        }

        if (fluid.getFlowing() != null) {
            String ns = fluid.getFlowing().getNamespace().toLowerCase(java.util.Locale.ROOT);
            String path = fluid.getFlowing().getPath().toLowerCase(java.util.Locale.ROOT);
            if ("ic2".equals(ns) && path.contains("biomass")) {
                return true;
            }
        }

        return unloc.contains("ic2") && unloc.contains("biomass");
    }
}
