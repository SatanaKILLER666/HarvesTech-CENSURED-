package com.ogryzok.food.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

public class ItemBasic extends Item {

    public ItemBasic(String name, CreativeTabs tab) {
        this.setRegistryName(name);
        this.setTranslationKey(name);
        this.setCreativeTab(tab);
    }
}