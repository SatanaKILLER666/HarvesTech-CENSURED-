package com.ogryzok.disease.recipe;

import com.ogryzok.disease.DiseaseRegistry;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.oredict.OreDictionary;

public class SyringeRecipe extends net.minecraftforge.registries.IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

    @Override
    public boolean matches(InventoryCrafting inv, World worldIn) {
        if (inv.getWidth() < 3 || inv.getHeight() < 3) return false;

        for (int y = 0; y <= inv.getHeight() - 3; y++) {
            for (int x = 0; x <= inv.getWidth() - 3; x++) {
                if (matchesAt(inv, x, y)) return true;
            }
        }
        return false;
    }

    private boolean matchesAt(InventoryCrafting inv, int startX, int startY) {
        for (int y = 0; y < inv.getHeight(); y++) {
            for (int x = 0; x < inv.getWidth(); x++) {
                ItemStack stack = inv.getStackInRowAndColumn(x, y);
                int lx = x - startX;
                int ly = y - startY;

                if (lx == 1 && ly == 0) {
                    if (!isTin(stack)) return false;
                } else if (lx == 1 && ly == 1) {
                    if (!isTin(stack)) return false;
                } else if (lx == 1 && ly == 2) {
                    if (!isSteel(stack)) return false;
                } else if (!stack.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isTin(ItemStack stack) {
        return hasOre(stack, "nuggetTin") || hasOre(stack, "ingotTin");
    }

    private boolean isSteel(ItemStack stack) {
        return hasOre(stack, "ingotSteel");
    }

    private boolean hasOre(ItemStack stack, String name) {
        for (int id : OreDictionary.getOreIDs(stack)) {
            if (OreDictionary.getOreName(id).equals(name)) return true;
        }
        return false;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.withSize(9, Ingredient.EMPTY);

        NonNullList<ItemStack> tin = OreDictionary.getOres("nuggetTin");
        NonNullList<ItemStack> steel = OreDictionary.getOres("ingotSteel");

        if (!tin.isEmpty()) {
            list.set(1, Ingredient.fromStacks(tin.toArray(new ItemStack[0])));
            list.set(4, Ingredient.fromStacks(tin.toArray(new ItemStack[0])));
        }

        if (!steel.isEmpty()) {
            list.set(7, Ingredient.fromStacks(steel.toArray(new ItemStack[0])));
        }

        return list;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        return new ItemStack(DiseaseRegistry.SYRINGE);
    }

    @Override
    public boolean canFit(int w, int h) {
        return w >= 3 && h >= 3;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return new ItemStack(DiseaseRegistry.SYRINGE);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
        return ForgeHooks.defaultRecipeGetRemainingItems(inv);
    }

    @Override
    public boolean isDynamic() {
        return false;
    }
}
