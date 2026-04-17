package com.ogryzok.lifecrusher.item;

import com.google.common.collect.Multimap;
import com.ogryzok.lifecrusher.LifeCrusherRegistry;
import com.ogryzok.lifecrusher.client.render.RubberRodRenderer;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

public class ItemRubberRod extends ItemSword implements IAnimatable {
    public static final Item.ToolMaterial RUBBER_ROD_MATERIAL =
            EnumHelper.addToolMaterial("RUBBER_ROD_MATERIAL", 2, 512, 4.0F, 0.0F, 12);

    private static final UUID REACH_MODIFIER_ID = UUID.fromString("5f564ca9-cc13-4c9f-9689-3272b8e90015");
    private static final double ATTACK_DAMAGE = 8.0D;
    private static final double ATTACK_SPEED = -2.8D;
    private static final double REACH_BONUS = 0.75D;

    private final AnimationFactory factory = new AnimationFactory(this);

    public ItemRubberRod() {
        super(RUBBER_ROD_MATERIAL);

        setRegistryName(LifeCrusherRegistry.MODID, "rubber_rod");
        setTranslationKey(LifeCrusherRegistry.MODID + ".rubber_rod");
        setCreativeTab(CreativeTabs.COMBAT);
        setMaxStackSize(1);
        setMaxDamage(RUBBER_ROD_MATERIAL.getMaxUses());
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void setTileEntityItemStackRenderer(TileEntityItemStackRenderer renderer) {
        super.setTileEntityItemStackRenderer(renderer);
    }

    @SideOnly(Side.CLIENT)
    public void initTEISR() {
        this.setTileEntityItemStackRenderer(new RubberRodRenderer());
    }

    @Override
    public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
        Multimap<String, AttributeModifier> modifiers = super.getItemAttributeModifiers(slot);

        if (slot == EntityEquipmentSlot.MAINHAND) {
            modifiers.removeAll(SharedMonsterAttributes.ATTACK_DAMAGE.getName());
            modifiers.removeAll(SharedMonsterAttributes.ATTACK_SPEED.getName());

            modifiers.put(
                    SharedMonsterAttributes.ATTACK_DAMAGE.getName(),
                    new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", ATTACK_DAMAGE, 0)
            );

            modifiers.put(
                    SharedMonsterAttributes.ATTACK_SPEED.getName(),
                    new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", ATTACK_SPEED, 0)
            );

            modifiers.put(
                    net.minecraft.entity.player.EntityPlayer.REACH_DISTANCE.getName(),
                    new AttributeModifier(REACH_MODIFIER_ID, "Rubber rod reach modifier", REACH_BONUS, 0)
            );
        }

        return modifiers;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.GRAY + I18n.translateToLocal("tooltip.harvestech.rubber_rod.flexible"));
    }

    @Override
    public void registerControllers(AnimationData data) {
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }
}
