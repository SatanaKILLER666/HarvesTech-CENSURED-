package com.ogryzok.manualharvest.item;

import com.ogryzok.food.item.ItemBasic;
import net.minecraft.creativetab.CreativeTabs;

public class ItemFilterHopper extends ItemBasic {
    public ItemFilterHopper() {
        super("filter_hopper", CreativeTabs.MATERIALS);
        setMaxStackSize(64);
    }
}
