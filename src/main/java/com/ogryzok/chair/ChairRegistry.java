package com.ogryzok.chair;

import com.ogryzok.chair.block.BlockChair;
import com.ogryzok.chair.client.render.ChairCameraHandler;
import com.ogryzok.chair.client.render.ChairGeoRenderer;
import com.ogryzok.chair.client.render.ChairPlayerRenderHandler;
import com.ogryzok.chair.entity.EntitySeat;
import com.ogryzok.chair.tile.TileChair;
import com.ogryzok.harvestech;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraft.item.crafting.IRecipe;
import software.bernie.geckolib3.renderers.geo.GeoBlockRenderer;

@Mod.EventBusSubscriber(modid = harvestech.MODID)
public final class ChairRegistry {
    public static final String MODID = harvestech.MODID;

    public static final BlockChair CHAIR = new BlockChair();

    private static boolean initialized;

    private ChairRegistry() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        GameRegistryCompat.registerTileEntity(TileChair.class, new ResourceLocation(MODID, "chair"));
        EntityRegistry.registerModEntity(
                new ResourceLocation(MODID, "chair_seat"),
                EntitySeat.class,
                "chair_seat",
                200,
                ChairRegistry.getModInstance(),
                64,
                1,
                false
        );
    }

    @SideOnly(Side.CLIENT)
    public static void initClient() {
        ClientRegistry.bindTileEntitySpecialRenderer(TileChair.class, (GeoBlockRenderer<TileChair>) new ChairGeoRenderer());
        RenderingRegistry.registerEntityRenderingHandler(EntitySeat.class, manager -> new Render<EntitySeat>(manager) {
            @Override
            public void doRender(EntitySeat entity, double x, double y, double z, float entityYaw, float partialTicks) {
            }

            @Override
            protected ResourceLocation getEntityTexture(EntitySeat entity) {
                return null;
            }
        });
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new ChairPlayerRenderHandler());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new ChairCameraHandler());
    }

    public static Object getModInstance() {
        return net.minecraftforge.fml.common.Loader.instance()
                .getIndexedModList()
                .get(MODID)
                .getMod();
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(CHAIR);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(new com.ogryzok.client.item.ItemGeoBlock(
                CHAIR,
                new ResourceLocation(MODID, "geo/chair.geo.json"),
                new ResourceLocation(MODID, "textures/blocks/chair.png"),
                new ResourceLocation(MODID, "animations/chair.animation.json")
        ));
    }


    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        event.getRegistry().register(
                new ShapedOreRecipe(
                        new ResourceLocation(MODID, "chair"),
                        new ItemStack(CHAIR),
                        "III",
                        "P P",
                        "P P",
                        'I', new ItemStack(net.minecraft.init.Items.IRON_INGOT),
                        'P', "plateIron"
                ).setRegistryName(new ResourceLocation(MODID, "chair"))
        );
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        Item chairItem = Item.getItemFromBlock(CHAIR);
        net.minecraftforge.client.model.ModelLoader.setCustomModelResourceLocation(
                chairItem,
                0,
                new ModelResourceLocation(new ResourceLocation(MODID, "chair"), "inventory")
        );

        if (chairItem instanceof com.ogryzok.client.item.ItemGeoBlock) {
            ((com.ogryzok.client.item.ItemGeoBlock) chairItem).initTEISR();
        }
    }

    private static final class GameRegistryCompat {
        private static void registerTileEntity(Class<TileChair> tileClass, ResourceLocation id) {
            net.minecraftforge.fml.common.registry.GameRegistry.registerTileEntity(tileClass, id);
        }
    }
}
