package com.ogryzok.jei;

import com.ogryzok.disease.DiseaseRegistry;
import com.ogryzok.food.FoodRegistry;
import com.ogryzok.manualharvest.ManualHarvestRegistry;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import java.util.Collections;
import java.util.List;

class JeiRenderUtils {
    private JeiRenderUtils() {}

    private static final int FRAME_FILL = 0xFFA1A1A1;
    private static final int FRAME_BORDER = 0xFF727272;

    static int fluidColor(FluidStack stack, int fallback) {
        if (stack == null || stack.getFluid() == null) {
            return fallback;
        }
        return 0xFF000000 | (stack.getFluid().getColor(stack) & 0x00FFFFFF);
    }

    static void drawGradientRect(int left, int top, int right, int bottom, int startColor, int endColor) {
        float a1 = (startColor >> 24 & 255) / 255.0F;
        float r1 = (startColor >> 16 & 255) / 255.0F;
        float g1 = (startColor >> 8 & 255) / 255.0F;
        float b1 = (startColor & 255) / 255.0F;

        float a2 = (endColor >> 24 & 255) / 255.0F;
        float r2 = (endColor >> 16 & 255) / 255.0F;
        float g2 = (endColor >> 8 & 255) / 255.0F;
        float b2 = (endColor & 255) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );
        GlStateManager.shadeModel(7425);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(right, top, 0).color(r1, g1, b1, a1).endVertex();
        buffer.pos(left, top, 0).color(r1, g1, b1, a1).endVertex();
        buffer.pos(left, bottom, 0).color(r2, g2, b2, a2).endVertex();
        buffer.pos(right, bottom, 0).color(r2, g2, b2, a2).endVertex();
        tessellator.draw();

        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    static void drawArrow(int x, int y, int w, int h, int color) {
        Gui.drawRect(x, y, x + w, y + h, FRAME_BORDER);
        Gui.drawRect(x + 1, y + 1, x + w - 1, y + h - 1, FRAME_FILL);
        Gui.drawRect(x + 2, y + 2, x + w - 6, y + h - 2, color);
        Gui.drawRect(x + w - 5, y + 3, x + w - 4, y + h - 3, color);
        Gui.drawRect(x + w - 4, y + 2, x + w - 3, y + h - 2, color);
        Gui.drawRect(x + w - 3, y + 3, x + w - 2, y + h - 3, color);
    }

    static void drawEnergyBar(int x, int y, int w, int h) {
        Gui.drawRect(x, y, x + w, y + h, FRAME_BORDER);
        Gui.drawRect(x + 1, y + 1, x + w - 1, y + h - 1, FRAME_FILL);
        JeiRenderUtils.drawGradientRect(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF5AFF7D, 0xFF1FAE46);
        Gui.drawRect(x + 2, y + 2, x + w - 2, Math.min(y + 5, y + h - 2), 0x66FFFFFF);
    }

    static void drawEnergyInfo(Minecraft mc, int panelX, int panelY, int panelW, int panelH, int barX, int barY, int barW, int barH, int energyCost) {
        drawEnergyBar(barX, barY, barW, barH);
    }

    static boolean isPointIn(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    static List<String> energyTooltip(int mouseX, int mouseY, int x, int y, int w, int h, int energyCost) {
        if (isPointIn(mouseX, mouseY, x, y, w, h)) {
            return Collections.singletonList(energyCost + " FE");
        }
        return Collections.emptyList();
    }
}

class BlenderRecipe implements IRecipeWrapper {
    final ItemStack input;
    final FluidStack output;
    final int energyCost;

    BlenderRecipe(ItemStack input, FluidStack output, int energyCost) {
        this.input = input;
        this.output = output;
        this.energyCost = energyCost;
    }

    @Override public void getIngredients(IIngredients ingredients) {
        ingredients.setInput(VanillaTypes.ITEM, input);
        ingredients.setOutput(VanillaTypes.FLUID, output);
    }

    @Override public void drawInfo(Minecraft mc, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        JeiRenderUtils.drawEnergyInfo(mc, 6, 14, 20, 72, 10, 22, 12, 56, energyCost);
        JeiRenderUtils.drawArrow(70, 42, 20, 10, 0xFFD58A98);
    }

    @Override public List<String> getTooltipStrings(int mouseX, int mouseY) {
        return JeiRenderUtils.energyTooltip(mouseX, mouseY, 10, 22, 12, 56, energyCost);
    }
}

class ProteinFormerRecipe implements IRecipeWrapper {
    final ItemStack input, output;
    final int energyCost;

    ProteinFormerRecipe(ItemStack input, ItemStack output, int energyCost) {
        this.input = input;
        this.output = output;
        this.energyCost = energyCost;
    }

    @Override public void getIngredients(IIngredients ingredients) {
        ingredients.setInput(VanillaTypes.ITEM, input);
        ingredients.setOutput(VanillaTypes.ITEM, output);
    }

    @Override public void drawInfo(Minecraft mc, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        JeiRenderUtils.drawEnergyInfo(mc, 6, 14, 20, 72, 10, 24, 12, 58, energyCost);
        JeiRenderUtils.drawArrow(64, 49, 28, 10, 0xFFDCE6F0);
    }

    @Override public List<String> getTooltipStrings(int mouseX, int mouseY) {
        return JeiRenderUtils.energyTooltip(mouseX, mouseY, 10, 24, 12, 58, energyCost);
    }
}

class DestillerRecipe implements IRecipeWrapper {
    final FluidStack input, output;
    final int energyCost;

    DestillerRecipe(FluidStack input, FluidStack output, int energyCost) {
        this.input = input;
        this.output = output;
        this.energyCost = energyCost;
    }

    @Override public void getIngredients(IIngredients ingredients) {
        ingredients.setInput(VanillaTypes.FLUID, input);
        ingredients.setOutput(VanillaTypes.FLUID, output);
    }

    @Override public void drawInfo(Minecraft mc, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        JeiRenderUtils.drawEnergyInfo(mc, 6, 14, 20, 72, 10, 24, 12, 58, energyCost);
        JeiRenderUtils.drawArrow(64, 49, 28, 10, JeiRenderUtils.fluidColor(output, 0xFF9AB8D6));
    }

    @Override public List<String> getTooltipStrings(int mouseX, int mouseY) {
        return JeiRenderUtils.energyTooltip(mouseX, mouseY, 10, 24, 12, 58, energyCost);
    }
}

class DestillerFillRecipe implements IRecipeWrapper {
    final ItemStack container, result;
    final FluidStack fluid;
    final int energyCost;

    DestillerFillRecipe(ItemStack container, FluidStack fluid, ItemStack result, int energyCost) {
        this.container = container;
        this.fluid = fluid;
        this.result = result;
        this.energyCost = energyCost;
    }

    @Override public void getIngredients(IIngredients ingredients) {
        ingredients.setInput(VanillaTypes.ITEM, container);
        ingredients.setInput(VanillaTypes.FLUID, fluid);
        ingredients.setOutput(VanillaTypes.ITEM, result);
    }

    @Override public void drawInfo(Minecraft mc, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        JeiRenderUtils.drawEnergyInfo(mc, 6, 14, 20, 72, 10, 24, 12, 58, energyCost);
        JeiRenderUtils.drawArrow(90, 49, 10, 10, JeiRenderUtils.fluidColor(fluid, 0xFF9AB8D6));
    }

    @Override public List<String> getTooltipStrings(int mouseX, int mouseY) {
        return JeiRenderUtils.energyTooltip(mouseX, mouseY, 10, 24, 12, 58, energyCost);
    }
}

class EnrichmentRecipe implements IRecipeWrapper {
    final FluidStack input, output;
    final int energyCost;

    EnrichmentRecipe(FluidStack input, FluidStack output, int energyCost) {
        this.input = input;
        this.output = output;
        this.energyCost = energyCost;
    }

    @Override public void getIngredients(IIngredients ingredients) {
        ingredients.setInput(VanillaTypes.FLUID, input);
        ingredients.setOutput(VanillaTypes.FLUID, output);
    }

    @Override public void drawInfo(Minecraft mc, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        JeiRenderUtils.drawEnergyInfo(mc, 6, 14, 20, 72, 10, 24, 12, 58, energyCost);
        JeiRenderUtils.drawArrow(64, 49, 28, 10, JeiRenderUtils.fluidColor(output, 0xFF7A9E5D));
    }

    @Override public List<String> getTooltipStrings(int mouseX, int mouseY) {
        return JeiRenderUtils.energyTooltip(mouseX, mouseY, 10, 24, 12, 58, energyCost);
    }
}

class EnrichmentFillRecipe implements IRecipeWrapper {
    final ItemStack container, result;
    final FluidStack fluid;
    final int energyCost;

    EnrichmentFillRecipe(ItemStack container, FluidStack fluid, ItemStack result, int energyCost) {
        this.container = container;
        this.fluid = fluid;
        this.result = result;
        this.energyCost = energyCost;
    }

    @Override public void getIngredients(IIngredients ingredients) {
        ingredients.setInput(VanillaTypes.ITEM, container);
        ingredients.setInput(VanillaTypes.FLUID, fluid);
        ingredients.setOutput(VanillaTypes.ITEM, result);
    }

    @Override public void drawInfo(Minecraft mc, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        JeiRenderUtils.drawEnergyInfo(mc, 6, 14, 20, 72, 10, 24, 12, 58, energyCost);
        JeiRenderUtils.drawArrow(90, 49, 10, 10, JeiRenderUtils.fluidColor(fluid, 0xFF7A9E5D));
    }

    @Override public List<String> getTooltipStrings(int mouseX, int mouseY) {
        return JeiRenderUtils.energyTooltip(mouseX, mouseY, 10, 24, 12, 58, energyCost);
    }
}

class ManualCollectionRecipe implements IRecipeWrapper {
    final ItemStack icon;
    final ItemStack container;
    final ItemStack output;
    final List<String> lines;

    ManualCollectionRecipe(ItemStack icon, ItemStack container, ItemStack output, List<String> lines) {
        this.icon = icon;
        this.container = container;
        this.output = output;
        this.lines = lines;
    }

    @Override public void getIngredients(IIngredients ingredients) {
        ingredients.setInput(VanillaTypes.ITEM, container);
        ingredients.setOutput(VanillaTypes.ITEM, output);
    }

    @Override public void drawInfo(Minecraft mc, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        String text = String.join(" ", lines);
        int yy = 66;
        for (String line : mc.fontRenderer.listFormattedStringToWidth(text, 145)) {
            mc.fontRenderer.drawString(line, 18, yy, 0xFF555555);
            yy += mc.fontRenderer.FONT_HEIGHT + 1;
        }
    }
}

class PalletCollectionRecipe implements IRecipeWrapper {
    final ItemStack icon;
    final ItemStack pallet;
    final ItemStack output;
    final List<String> lines;

    PalletCollectionRecipe(ItemStack icon, ItemStack pallet, ItemStack output, List<String> lines) {
        this.icon = icon;
        this.pallet = pallet;
        this.output = output;
        this.lines = lines;
    }

    @Override public void getIngredients(IIngredients ingredients) {
        ingredients.setOutput(VanillaTypes.ITEM, output);
    }

    @Override public void drawInfo(Minecraft mc, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        String text = String.join(" ", lines);
        int yy = 60;
        for (String line : mc.fontRenderer.listFormattedStringToWidth(text, 145)) {
            mc.fontRenderer.drawString(line, 18, yy, 0xFF555555);
            yy += mc.fontRenderer.FONT_HEIGHT + 1;
        }
    }
}

class CentrifugeFillRecipe implements IRecipeWrapper {
    final ItemStack container, result;
    final FluidStack fluid;
    final int energyCost;

    CentrifugeFillRecipe(ItemStack container, FluidStack fluid, ItemStack result, int energyCost) {
        this.container = container;
        this.fluid = fluid;
        this.result = result;
        this.energyCost = energyCost;
    }

    @Override public void getIngredients(IIngredients ingredients) {
        ingredients.setInput(VanillaTypes.ITEM, container);
        ingredients.setInput(VanillaTypes.FLUID, fluid);
        ingredients.setOutput(VanillaTypes.ITEM, result);
    }

    @Override public void drawInfo(Minecraft mc, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        JeiRenderUtils.drawEnergyInfo(mc, 6, 14, 20, 72, 10, 24, 12, 58, energyCost);
        JeiRenderUtils.drawArrow(90, 49, 10, 10, JeiRenderUtils.fluidColor(fluid, 0xFFE3E8EF));
    }

    @Override public List<String> getTooltipStrings(int mouseX, int mouseY) {
        return JeiRenderUtils.energyTooltip(mouseX, mouseY, 10, 24, 12, 58, energyCost);
    }
}

class RottingTankRecipe implements IRecipeWrapper {
    final List<ItemStack> inputs;
    final ItemStack tank;
    final FluidStack output;
    final int durationTicks;

    RottingTankRecipe(List<ItemStack> inputs, ItemStack tank, FluidStack output, int durationTicks) {
        this.inputs = inputs;
        this.tank = tank;
        this.output = output;
        this.durationTicks = durationTicks;
    }

    @Override public void getIngredients(IIngredients ingredients) {
        ingredients.setInputs(VanillaTypes.ITEM, inputs);
        ingredients.setOutput(VanillaTypes.FLUID, output);
    }

    @Override public void drawInfo(Minecraft mc, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        int totalSeconds = Math.max(0, durationTicks / 20);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        mc.fontRenderer.drawString(I18n.format("jei.harvestech.rotting_tank.time", minutes, seconds), 48, 16, 0xFF4E4E4E);
        mc.fontRenderer.drawString(I18n.format("jei.harvestech.rotting_tank.close_wait_1"), 48, 28, 0xFF666666);
        mc.fontRenderer.drawString(I18n.format("jei.harvestech.rotting_tank.close_wait_2"), 48, 39, 0xFF666666);
    }
}

class SeparatorRecipe implements IRecipeWrapper {
    final FluidStack input;
    final ItemStack outputItem;
    final FluidStack outputFluid;
    final int energyCost;

    SeparatorRecipe(FluidStack input, ItemStack outputItem, FluidStack outputFluid, int energyCost) {
        this.input = input;
        this.outputItem = outputItem;
        this.outputFluid = outputFluid;
        this.energyCost = energyCost;
    }

    @Override public void getIngredients(IIngredients ingredients) {
        ingredients.setInput(VanillaTypes.FLUID, input);
        ingredients.setOutput(VanillaTypes.ITEM, outputItem);
        if (outputFluid != null) {
            ingredients.setOutput(VanillaTypes.FLUID, outputFluid);
        }
    }

    @Override public void drawInfo(Minecraft mc, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        JeiRenderUtils.drawEnergyInfo(mc, 6, 14, 20, 72, 10, 24, 12, 58, energyCost);
        int color = outputFluid != null ? JeiRenderUtils.fluidColor(outputFluid, 0xFFB0825B) : JeiRenderUtils.fluidColor(input, 0xFFB0825B);
        JeiRenderUtils.drawArrow(64, 49, 16, 10, color);
    }

    @Override public List<String> getTooltipStrings(int mouseX, int mouseY) {
        return JeiRenderUtils.energyTooltip(mouseX, mouseY, 10, 24, 12, 58, energyCost);
    }
}

class MrnaSynthesisRecipe implements IRecipeWrapper {
    final ItemStack filler, antigen;
    final FluidStack inputFluid, outputFluid;
    final int energyCost;

    MrnaSynthesisRecipe(ItemStack filler, ItemStack antigen, FluidStack inputFluid, FluidStack outputFluid, int energyCost) {
        this.filler = filler;
        this.antigen = antigen;
        this.inputFluid = inputFluid;
        this.outputFluid = outputFluid;
        this.energyCost = energyCost;
    }

    @Override public void getIngredients(IIngredients ingredients) {
        ingredients.setInputs(VanillaTypes.ITEM, java.util.Arrays.asList(filler, antigen));
        ingredients.setInput(VanillaTypes.FLUID, inputFluid);
        ingredients.setOutput(VanillaTypes.FLUID, outputFluid);
    }

    @Override public void drawInfo(Minecraft mc, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        JeiRenderUtils.drawEnergyInfo(mc, 14, 22, 20, 86, 18, 32, 12, 70, energyCost);
        int sideArrowColor = JeiRenderUtils.fluidColor(outputFluid, 0xFFC53A69);
        int downArrowColor = JeiRenderUtils.fluidColor(outputFluid, 0xFFC53A69);
        JeiRenderUtils.drawArrow(73, 40, 10, 8, sideArrowColor);
        JeiRenderUtils.drawArrow(109, 40, 10, 8, sideArrowColor);
        JeiRenderUtils.drawArrow(73, 67, 10, 8, sideArrowColor);
        JeiRenderUtils.drawArrow(109, 67, 10, 8, sideArrowColor);
        Gui.drawRect(92, 92, 100, 106, 0xFF727272);
        Gui.drawRect(93, 93, 99, 102, 0xFFA1A1A1);
        Gui.drawRect(94, 94, 98, 103, downArrowColor);
        Gui.drawRect(93, 102, 99, 103, downArrowColor);
        Gui.drawRect(92, 103, 100, 104, downArrowColor);
        Gui.drawRect(91, 104, 101, 105, downArrowColor);
        Gui.drawRect(90, 105, 102, 106, downArrowColor);
    }

    @Override public List<String> getTooltipStrings(int mouseX, int mouseY) {
        return JeiRenderUtils.energyTooltip(mouseX, mouseY, 18, 32, 12, 70, energyCost);
    }
}

class MrnaFillRecipe implements IRecipeWrapper {
    final ItemStack syringe, result;
    final FluidStack fluid;
    final int energyCost;

    MrnaFillRecipe(ItemStack syringe, FluidStack fluid, ItemStack result, int energyCost) {
        this.syringe = syringe;
        this.fluid = fluid;
        this.result = result;
        this.energyCost = energyCost;
    }

    @Override public void getIngredients(IIngredients ingredients) {
        ingredients.setInput(VanillaTypes.ITEM, syringe);
        ingredients.setInput(VanillaTypes.FLUID, fluid);
        ingredients.setOutput(VanillaTypes.ITEM, result);
    }

    @Override public void drawInfo(Minecraft mc, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        JeiRenderUtils.drawEnergyInfo(mc, 14, 22, 20, 86, 18, 32, 12, 70, energyCost);
        int arrowColor = JeiRenderUtils.fluidColor(fluid, 0xFFC53A69);
        Gui.drawRect(42, 137, 50, 143, arrowColor);
        Gui.drawRect(70, 137, 78, 143, arrowColor);
        Gui.drawRect(98, 137, 106, 143, arrowColor);
        Gui.drawRect(126, 137, 134, 143, arrowColor);
    }

    @Override public List<String> getTooltipStrings(int mouseX, int mouseY) {
        return JeiRenderUtils.energyTooltip(mouseX, mouseY, 18, 32, 12, 70, energyCost);
    }
}
