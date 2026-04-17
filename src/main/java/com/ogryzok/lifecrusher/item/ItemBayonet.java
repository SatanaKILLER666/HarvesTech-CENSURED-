package com.ogryzok.lifecrusher.item;

import com.google.common.collect.Multimap;
import com.ogryzok.lifecrusher.client.render.BayonetItemRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import net.minecraft.potion.PotionEffect;
import com.ogryzok.lifecrusher.LifeCrusherRegistry;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.world.World;
import java.util.List;
import net.minecraft.util.text.translation.I18n;

import java.util.UUID;

public class ItemBayonet extends ItemSword implements IAnimatable {

    public static final Item.ToolMaterial BAYONET_MATERIAL =
            EnumHelper.addToolMaterial("BAYONET_MATERIAL", 0, 250, 4.0F, 0.0F, 10);

    private static final UUID REACH_MODIFIER_ID = UUID.fromString("9f4d9d5b-7e62-4e7e-9d1f-3a9dd5f1c401");

    private final AnimationFactory factory = new AnimationFactory(this);

    public ItemBayonet() {
        super(BAYONET_MATERIAL);

        setRegistryName("harvestech", "bayonet");
        setTranslationKey("harvestech.bayonet");
        setCreativeTab(CreativeTabs.COMBAT);
        setMaxStackSize(1);
        setMaxDamage(250);
    }

    @Override
    public void registerControllers(AnimationData data) {
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }

    @SideOnly(Side.CLIENT)
    public void initTEISR() {
        setTileEntityItemStackRenderer(new TileEntityItemStackRenderer() {
            private final BayonetItemRenderer renderer = new BayonetItemRenderer();

            @Override
            public void renderByItem(ItemStack stack) {
                renderer.renderByItem(stack);
            }
        });
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        boolean result = super.hitEntity(stack, target, attacker);

        double x = attacker.posX - target.posX;
        double z = attacker.posZ - target.posZ;

        if (x * x + z * z > 1.0E-4D) {
            target.knockBack(attacker, 0.35F, x, z);
            target.addPotionEffect(new PotionEffect(
                    LifeCrusherRegistry.STUPOR,
                    60,   // 3 секунды
                    0
            ));
        }

        return result;
    }

    @Override
    public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
        Multimap<String, AttributeModifier> modifiers = super.getItemAttributeModifiers(slot);

        if (slot == EntityEquipmentSlot.MAINHAND) {
            modifiers.removeAll(SharedMonsterAttributes.ATTACK_DAMAGE.getName());
            modifiers.removeAll(SharedMonsterAttributes.ATTACK_SPEED.getName());

            modifiers.put(
                    SharedMonsterAttributes.ATTACK_DAMAGE.getName(),
                    new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", 5.7D, 0)
            );

            modifiers.put(
                    SharedMonsterAttributes.ATTACK_SPEED.getName(),
                    new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", -2.2D, 0)
            );

            modifiers.put(
                    EntityPlayer.REACH_DISTANCE.getName(),
                    new AttributeModifier(REACH_MODIFIER_ID, "Bayonet reach modifier", -1.5D, 0)
            );
        }

        return modifiers;
    }
    @Override
@SideOnly(Side.CLIENT)
public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {

    tooltip.add(TextFormatting.GRAY + I18n.translateToLocal("tooltip.harvestech.bayonet.knockback"));
    tooltip.add(TextFormatting.DARK_RED + I18n.translateToLocal("tooltip.harvestech.bayonet.stupor"));
}
}