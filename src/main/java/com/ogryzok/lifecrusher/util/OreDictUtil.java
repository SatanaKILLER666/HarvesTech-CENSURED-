package com.ogryzok.lifecrusher.util;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public final class OreDictUtil {
    private OreDictUtil() {}

    public static boolean matches(ItemStack stack, String oreName) {
        if (stack == null || stack.isEmpty()) return false;

        int[] ids = OreDictionary.getOreIDs(stack);
        for (int id : ids) {
            if (oreName.equals(OreDictionary.getOreName(id))) {
                return true;
            }
        }
        return false;
    }
}