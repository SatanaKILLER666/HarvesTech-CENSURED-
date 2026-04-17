package com.ogryzok.jei;

import com.ogryzok.harvestech;
import com.ogryzok.manualharvest.ManualHarvestRegistry;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;

abstract class BaseCategory<T extends IRecipeWrapper> implements IRecipeCategory<T> {
    protected final IDrawable background;
    protected final IGuiHelper guiHelper;
    private final String uid;
    private final String title;

    BaseCategory(IGuiHelper guiHelper, String uid, String title, int width, int height) {
        this.guiHelper = guiHelper;
        this.background = guiHelper.createBlankDrawable(width, height);
        this.uid = uid;
        this.title = title;
    }

    @Override public String getUid() { return uid; }
    @Override public String getTitle() { return title; }
    @Override public String getModName() { return harvestech.MODID; }
    @Override public IDrawable getBackground() { return background; }

    protected static final int FRAME_FILL = 0xFFA1A1A1;
    protected static final int FRAME_BORDER = 0xFF727272;

    protected void panel(int x, int y, int w, int h) {
    }

    protected void slot(int x, int y) {
        Gui.drawRect(x, y, x + 18, y + 18, FRAME_BORDER);
        Gui.drawRect(x + 1, y + 1, x + 17, y + 17, FRAME_FILL);
    }

    protected void tank(int x, int y, int w, int h) {
        Gui.drawRect(x, y, x + w, y + h, FRAME_BORDER);
        Gui.drawRect(x + 1, y + 1, x + w - 1, y + h - 1, FRAME_FILL);
    }

    protected void wasteTankFrame(int x, int y) {
        Gui.drawRect(x, y, x + 12, y + 24, FRAME_BORDER);
        Gui.drawRect(x + 1, y + 1, x + 11, y + 23, FRAME_FILL);
    }

    protected void arrowFrame(int x, int y, int w, int h) {
        Gui.drawRect(x, y, x + w, y + h, FRAME_BORDER);
        Gui.drawRect(x + 1, y + 1, x + w - 1, y + h - 1, FRAME_FILL);
    }

    protected void drawWrappedText(Minecraft mc, String text, int x, int y, int width, int color) {
        int yy = y;
        for (String line : mc.fontRenderer.listFormattedStringToWidth(text, width)) {
            mc.fontRenderer.drawString(line, x, yy, color);
            yy += mc.fontRenderer.FONT_HEIGHT + 1;
        }
    }

    protected void drawTexture(Minecraft mc, ResourceLocation texture, int x, int y, int w, int h) {
        mc.getTextureManager().bindTexture(texture);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, w, h, w, h);
    }
}

class BlenderRecipeCategory extends BaseCategory<BlenderRecipe> {
    BlenderRecipeCategory(IGuiHelper guiHelper) { super(guiHelper, JeiPluginHarvestech.UID_BLENDER, I18n.format("jei.harvestech.category.blender"), 144, 92); }

    @Override public void setRecipe(IRecipeLayout layout, BlenderRecipe recipe, IIngredients ingredients) {
        layout.getItemStacks().init(0, true, 37, 38);
        layout.getFluidStacks().init(0, false, 107, 30, 14, 38, 4000, false, null);
        layout.getItemStacks().set(ingredients);
        layout.getFluidStacks().set(ingredients);
    }

    @Override public void drawExtras(Minecraft mc) {
        panel(6, 14, 20, 72);
        panel(28, 14, 36, 72);
        panel(68, 38, 24, 18);
        panel(96, 14, 36, 72);
        slot(37, 38);
        tank(105, 28, 18, 42);
        arrowFrame(70, 42, 20, 10);
    }
}

class ProteinFormerRecipeCategory extends BaseCategory<ProteinFormerRecipe> {
    ProteinFormerRecipeCategory(IGuiHelper guiHelper) { super(guiHelper, JeiPluginHarvestech.UID_PROTEIN_FORMER, "Protein Former", 140, 92); }

    @Override public void setRecipe(IRecipeLayout layout, ProteinFormerRecipe recipe, IIngredients ingredients) {
        layout.getItemStacks().init(0, true, 37, 45);
        layout.getItemStacks().init(1, false, 101, 45);
        layout.getItemStacks().set(ingredients);
    }

    @Override public void drawExtras(Minecraft mc) {
        panel(6, 14, 20, 72);
        panel(28, 14, 36, 72);
        panel(92, 14, 36, 72);
        slot(37, 45);
        slot(101, 45);
    }
}

class DestillerRecipeCategory extends BaseCategory<DestillerRecipe> {
    DestillerRecipeCategory(IGuiHelper guiHelper) { super(guiHelper, JeiPluginHarvestech.UID_DESTILLER, I18n.format("jei.harvestech.category.destiller"), 140, 92); }

    @Override public void setRecipe(IRecipeLayout layout, DestillerRecipe recipe, IIngredients ingredients) {
        layout.getFluidStacks().init(0, true, 37, 30, 18, 48, 4000, false, null);
        layout.getFluidStacks().init(1, false, 101, 30, 18, 48, 4000, false, null);
        layout.getFluidStacks().set(ingredients);
    }

    @Override public void drawExtras(Minecraft mc) {
        panel(6, 14, 20, 72);
        panel(28, 14, 36, 72);
        panel(92, 14, 36, 72);
        tank(37, 30, 18, 48);
        tank(101, 30, 18, 48);
    }
}

class DestillerFillRecipeCategory extends BaseCategory<DestillerFillRecipe> {
    DestillerFillRecipeCategory(IGuiHelper guiHelper) { super(guiHelper, JeiPluginHarvestech.UID_DESTILLER_FILL, I18n.format("jei.harvestech.category.destiller_fill"), 140, 92); }

    @Override public void setRecipe(IRecipeLayout layout, DestillerFillRecipe recipe, IIngredients ingredients) {
        layout.getItemStacks().init(0, true, 37, 45);
        layout.getFluidStacks().init(0, true, 70, 30, 18, 48, 4000, false, null);
        layout.getItemStacks().init(1, false, 101, 45);
        layout.getItemStacks().set(0, recipe.container);
        layout.getFluidStacks().set(0, recipe.fluid);
        layout.getItemStacks().set(1, recipe.result);
    }

    @Override public void drawExtras(Minecraft mc) {
        panel(6, 14, 20, 72);
        panel(28, 14, 36, 72);
        panel(65, 14, 28, 72);
        panel(92, 14, 36, 72);
        slot(37, 45);
        tank(70, 30, 18, 48);
        slot(101, 45);
    }
}

class EnrichmentRecipeCategory extends BaseCategory<EnrichmentRecipe> {
    EnrichmentRecipeCategory(IGuiHelper guiHelper) { super(guiHelper, JeiPluginHarvestech.UID_ENRICHMENT, I18n.format("jei.harvestech.category.enrichment"), 140, 92); }

    @Override public void setRecipe(IRecipeLayout layout, EnrichmentRecipe recipe, IIngredients ingredients) {
        layout.getFluidStacks().init(0, true, 37, 30, 18, 48, 4000, false, null);
        layout.getFluidStacks().init(1, false, 101, 30, 18, 48, 4000, false, null);
        layout.getFluidStacks().set(ingredients);
    }

    @Override public void drawExtras(Minecraft mc) {
        panel(6, 14, 20, 72);
        panel(28, 14, 36, 72);
        panel(92, 14, 36, 72);
        tank(37, 30, 18, 48);
        tank(101, 30, 18, 48);
    }
}

class EnrichmentFillRecipeCategory extends BaseCategory<EnrichmentFillRecipe> {
    EnrichmentFillRecipeCategory(IGuiHelper guiHelper) { super(guiHelper, JeiPluginHarvestech.UID_ENRICHMENT_FILL, I18n.format("jei.harvestech.category.enrichment_fill"), 140, 92); }

    @Override public void setRecipe(IRecipeLayout layout, EnrichmentFillRecipe recipe, IIngredients ingredients) {
        layout.getItemStacks().init(0, true, 37, 45);
        layout.getFluidStacks().init(0, true, 70, 30, 18, 48, 4000, false, null);
        layout.getItemStacks().init(1, false, 101, 45);
        layout.getItemStacks().set(0, recipe.container);
        layout.getFluidStacks().set(0, recipe.fluid);
        layout.getItemStacks().set(1, recipe.result);
    }

    @Override public void drawExtras(Minecraft mc) {
        panel(6, 14, 20, 72);
        panel(28, 14, 36, 72);
        panel(65, 14, 28, 72);
        panel(92, 14, 36, 72);
        slot(37, 45);
        tank(70, 30, 18, 48);
        slot(101, 45);
    }
}

class ManualCollectionRecipeCategory extends BaseCategory<ManualCollectionRecipe> {
    private static final ResourceLocation HAND_TEXTURE = new ResourceLocation(harvestech.MODID, "textures/gui/jei_hand.png");

    ManualCollectionRecipeCategory(IGuiHelper guiHelper) { super(guiHelper, JeiPluginHarvestech.UID_MANUAL_COLLECTION, I18n.format("jei.harvestech.category.manual_collection"), 174, 112); }

    @Override public IDrawable getIcon() {
        return guiHelper.createDrawableIngredient(new ItemStack(ManualHarvestRegistry.MANUAL_COLLECTION_ICON));
    }

    @Override public void setRecipe(IRecipeLayout layout, ManualCollectionRecipe recipe, IIngredients ingredients) {
        layout.getItemStacks().init(0, true, 18, 18);
        layout.getItemStacks().init(1, true, 44, 39);
        layout.getItemStacks().init(2, false, 112, 39);
        layout.getItemStacks().set(0, recipe.icon);
        layout.getItemStacks().set(1, recipe.container);
        layout.getItemStacks().set(2, recipe.output);
    }

    @Override public void drawExtras(Minecraft mc) {
        Gui.drawRect(8, 10, 166, 104, 0x33727272);
        slot(18, 18);
        slot(44, 39);
        arrowFrame(74, 44, 22, 10);
        slot(112, 39);
    }

}

class PalletCollectionRecipeCategory extends BaseCategory<PalletCollectionRecipe> {
    PalletCollectionRecipeCategory(IGuiHelper guiHelper) { super(guiHelper, JeiPluginHarvestech.UID_PALLET_COLLECTION, I18n.format("jei.harvestech.category.pallet_collection"), 174, 96); }

    @Override public IDrawable getIcon() {
        return guiHelper.createDrawableIngredient(new ItemStack(ManualHarvestRegistry.PALLET));
    }

    @Override public void setRecipe(IRecipeLayout layout, PalletCollectionRecipe recipe, IIngredients ingredients) {
        layout.getItemStacks().init(0, true, 28, 39);
        layout.getItemStacks().init(1, true, 46, 39);
        layout.getItemStacks().init(2, false, 116, 39);
        layout.getItemStacks().set(0, recipe.icon);
        layout.getItemStacks().set(1, recipe.pallet);
        layout.getItemStacks().set(2, recipe.output);
    }

    @Override public void drawExtras(Minecraft mc) {
        Gui.drawRect(8, 10, 166, 88, 0x33727272);
        slot(28, 39);
        slot(46, 39);
        arrowFrame(76, 44, 22, 10);
        slot(116, 39);
    }

}

class CentrifugeFillRecipeCategory extends BaseCategory<CentrifugeFillRecipe> {
    CentrifugeFillRecipeCategory(IGuiHelper guiHelper) { super(guiHelper, JeiPluginHarvestech.UID_CENTRIFUGE_FILL, I18n.format("jei.harvestech.category.centrifuge_fill"), 140, 92); }

    @Override public void setRecipe(IRecipeLayout layout, CentrifugeFillRecipe recipe, IIngredients ingredients) {
        layout.getItemStacks().init(0, true, 37, 45);
        layout.getFluidStacks().init(0, true, 70, 30, 18, 48, 4000, false, null);
        layout.getItemStacks().init(1, false, 101, 45);
        layout.getItemStacks().set(0, recipe.container);
        layout.getFluidStacks().set(0, recipe.fluid);
        layout.getItemStacks().set(1, recipe.result);
    }

    @Override public void drawExtras(Minecraft mc) {
        panel(6, 14, 20, 72);
        panel(28, 14, 36, 72);
        panel(65, 14, 28, 72);
        panel(92, 14, 36, 72);
        slot(37, 45);
        tank(70, 30, 18, 48);
        slot(101, 45);
    }
}

class RottingTankRecipeCategory extends BaseCategory<RottingTankRecipe> {
    RottingTankRecipeCategory(IGuiHelper guiHelper) { super(guiHelper, JeiPluginHarvestech.UID_ROTTING_TANK, I18n.format("jei.harvestech.category.rotting_tank"), 170, 96); }

    @Override public void setRecipe(IRecipeLayout layout, RottingTankRecipe recipe, IIngredients ingredients) {
        layout.getItemStacks().init(0, true, 18, 49);
        layout.getItemStacks().init(1, true, 72, 49);
        layout.getFluidStacks().init(0, false, 132, 34, 18, 48, 4000, false, null);
        layout.getItemStacks().set(0, recipe.inputs);
        layout.getItemStacks().set(1, recipe.tank);
        layout.getFluidStacks().set(0, recipe.output);
    }

    @Override public void drawExtras(Minecraft mc) {
        Gui.drawRect(8, 10, 160, 88, 0x33727272);
        slot(18, 49);
        arrowFrame(46, 53, 18, 10);
        slot(72, 49);
        arrowFrame(102, 53, 18, 10);
        tank(132, 34, 18, 48);
    }
}

class SeparatorRecipeCategory extends BaseCategory<SeparatorRecipe> {
    SeparatorRecipeCategory(IGuiHelper guiHelper) { super(guiHelper, JeiPluginHarvestech.UID_SEPARATOR, I18n.format("jei.harvestech.category.separator"), 170, 92); }

    @Override public void setRecipe(IRecipeLayout layout, SeparatorRecipe recipe, IIngredients ingredients) {
        layout.getFluidStacks().init(0, true, 37, 30, 18, 48, 4000, false, null);
        layout.getItemStacks().init(0, false, 102, 45);
        layout.getFluidStacks().init(1, false, 153, 42, 8, 20, 4000, false, null);
        layout.getFluidStacks().set(0, recipe.input);
        layout.getItemStacks().set(0, recipe.outputItem);
        if (recipe.outputFluid != null) {
            layout.getFluidStacks().set(1, recipe.outputFluid);
        }
    }

    @Override public void drawExtras(Minecraft mc) {
        panel(6, 14, 20, 72);
        panel(28, 14, 36, 72);
        panel(92, 14, 36, 72);
        panel(146, 14, 22, 72);
        tank(37, 30, 18, 48);
        slot(102, 45);
        wasteTankFrame(151, 40);
    }
}

class MrnaSynthesisRecipeCategory extends BaseCategory<MrnaSynthesisRecipe> {
    MrnaSynthesisRecipeCategory(IGuiHelper guiHelper) { super(guiHelper, JeiPluginHarvestech.UID_MRNA_SYNTH, I18n.format("jei.harvestech.category.mrna_synth"), 176, 170); }

    @Override public void setRecipe(IRecipeLayout layout, MrnaSynthesisRecipe recipe, IIngredients ingredients) {
        layout.getItemStacks().init(0, true, 48, 35);
        layout.getItemStacks().init(1, true, 48, 62);
        layout.getItemStacks().init(2, true, 126, 35);
        layout.getItemStacks().init(3, true, 126, 62);
        layout.getFluidStacks().init(0, true, 87, 24, 18, 62, 1000, false, null);
        layout.getFluidStacks().init(1, false, 87, 112, 18, 48, 1000, false, null);
        layout.getItemStacks().set(0, recipe.filler);
        layout.getItemStacks().set(1, net.minecraft.item.ItemStack.EMPTY);
        layout.getItemStacks().set(2, recipe.antigen);
        layout.getItemStacks().set(3, recipe.outputFluid != null && recipe.outputFluid.getFluid() == com.ogryzok.fluids.ModFluids.MALE_POWER_STEROID ? recipe.antigen : net.minecraft.item.ItemStack.EMPTY);
        layout.getFluidStacks().set(0, recipe.inputFluid);
        layout.getFluidStacks().set(1, recipe.outputFluid);
    }

    @Override public void drawExtras(Minecraft mc) {
        panel(14, 22, 20, 86);
        panel(41, 28, 32, 70);
        panel(119, 28, 32, 70);
        panel(82, 18, 28, 72);
        panel(82, 106, 28, 58);
        slot(48, 35);
        slot(48, 62);
        slot(126, 35);
        slot(126, 62);
        tank(85, 22, 22, 66);
        tank(85, 110, 22, 52);
    }
}

class MrnaFillRecipeCategory extends BaseCategory<MrnaFillRecipe> {
    MrnaFillRecipeCategory(IGuiHelper guiHelper) { super(guiHelper, JeiPluginHarvestech.UID_MRNA_FILL, I18n.format("jei.harvestech.category.mrna_fill"), 176, 170); }

    @Override public void setRecipe(IRecipeLayout layout, MrnaFillRecipe recipe, IIngredients ingredients) {
        layout.getFluidStacks().init(0, true, 79, 32, 18, 62, 1000, false, null);
        layout.getItemStacks().init(0, true, 37, 119);
        layout.getItemStacks().init(1, false, 37, 143);
        layout.getFluidStacks().set(0, recipe.fluid);
        layout.getItemStacks().set(0, recipe.syringe);
        layout.getItemStacks().set(1, recipe.result);
    }

    @Override public void drawExtras(Minecraft mc) {
        panel(14, 22, 20, 86);
        panel(74, 24, 28, 78);
        panel(20, 110, 136, 58);
        tank(77, 30, 22, 66);
        slot(37, 119);
        slot(37, 143);
        slot(65, 119);
        slot(65, 143);
        slot(93, 119);
        slot(93, 143);
        slot(121, 119);
        slot(121, 143);
    }
}
