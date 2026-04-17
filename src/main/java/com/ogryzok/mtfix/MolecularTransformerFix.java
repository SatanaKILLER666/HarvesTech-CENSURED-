package com.ogryzok.mtfix;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MolecularTransformerFix {

    private static final String TILE_CLASS = "com.bordlistian.requious.tile.TileEntityAssembly";
    private static final String BLOCK_ID = "requious:molecular_transformer";

    private static final String VAR_RECIPE = "mt_recipe";
    private static final String VAR_PROGRESS = "mt_progress";

    private static final List<RecipeSpec> RECIPES = Arrays.asList(
            new RecipeSpec("base_materia",
                    stack("ic2:crafting", 23, 1),
                    stack("ic2:misc_resource", 3, 1),
                    140_000_000L
            ),
            new RecipeSpec("compressed_materia",
                    stack("ic2:crafting", 24, 3),
                    stack("contenttweaker:perfect_uu_matter", 0, 1),
                    4_480_000_000L
            ),
            new RecipeSpec("sunnarium",
                    stack("minecraft:glowstone_dust", 0, 1),
                    stack("contenttweaker:sunnariumpart", 0, 1),
                    16_000_000L
            ),
            new RecipeSpec("ae_singularity",
                    stack("contenttweaker:faded_singularity", 0, 1),
                    stack("appliedenergistics2:material", 47, 1),
                    40_000_000L
            ),
            new RecipeSpec("pulsar",
                    stack("draconicevolution:chaos_shard", 0, 1),
                    stack("contenttweaker:stern_vw", 0, 1),
                    6_400_000_000L
            )
    );

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.world == null || event.world.isRemote) return;

        List<TileEntity> tiles = new ArrayList<>(event.world.loadedTileEntityList);
        for (TileEntity tile : tiles) {
            try {
                if (!isTargetTile(tile)) continue;
                tickMachine(tile, event.world);
            } catch (Throwable t) {
                System.out.println("[MTFIX] tick error: " + t);
                t.printStackTrace();
            }
        }
    }

    private void tickMachine(TileEntity tile, World world) throws Exception {
        Object processor = invoke(tile, "getProcessor");
        if (processor == null) return;

        Object inputSlot  = invoke(processor, "getSlot", 0, 0);
        Object energySlot = invoke(processor, "getSlot", 0, 1);
        Object outputSlot = invoke(processor, "getSlot", 0, 2);
        Object timerSlot  = invoke(processor, "getSlot", 3, 0);

        if (inputSlot == null || energySlot == null || outputSlot == null || timerSlot == null) return;

        ItemStack input = getSlotStack(inputSlot);

        String recipeName = asString(invoke(processor, "getVariable", VAR_RECIPE));
        long progress = asLong(invoke(processor, "getVariable", VAR_PROGRESS));

        RecipeSpec recipe = null;
        if (!recipeName.isEmpty()) {
            recipe = RecipeSpec.byName(recipeName);
        }

        if (recipe == null) {
            recipe = findRecipe(input);
            if (recipe != null) {
                invoke(processor, "setVariable", VAR_RECIPE, recipe.name);
                invoke(processor, "setVariable", VAR_PROGRESS, 0L);
                progress = 0L;
            }
        }

        if (recipe == null) {
            setIdle(processor, timerSlot);
            sync(tile, world);
            return;
        }

        if (!recipe.matches(input)) {
            reset(processor, timerSlot, tile, world);
            return;
        }

        ItemStack outputExisting = getSlotStack(outputSlot);
        ItemStack outputToMake = recipe.output.create();

        updateText(processor, recipe, progress, 0L);

        if (!canFit(outputExisting, outputToMake)) {
            invoke(processor, "setVariable", "dispProg", "Забито!");
            invoke(processor, "setVariable", "dispTick", "0");
            invoke(processor, "setVariable", "visualActiveMol", 0);
            invoke(processor, "setVariable", "active", 0);

            setTimer(timerSlot, 100, percent(progress, recipe.energy), false);
            sync(tile, world);
            return;
        }

        long stored = getEnergyAmount(energySlot);
        long need = Math.max(0L, recipe.energy - progress);
        long taken = 0L;

        if (stored > 0L && need > 0L) {
            // сначала пробуем через processor.extractEnergy("feInput", ...)
            try {
                Object val = invoke(processor, "extractEnergy", "feInput", Math.min(stored, need));
                taken = asLong(val);
            } catch (Throwable ignored) {
                // запасной вариант: напрямую из energy slot
                taken = extractEnergy(energySlot, Math.min(stored, need));
            }

            if (taken > 0L) {
                progress += taken;
            }
        }

        updateText(processor, recipe, progress, taken);

        int perc = percent(progress, recipe.energy);
        setTimer(timerSlot, 100, perc, taken > 0L);

        invoke(processor, "setVariable", VAR_PROGRESS, progress);
        invoke(processor, "setVariable", "visualActiveMol", taken > 0L ? 1 : 0);
        invoke(processor, "setVariable", "active", taken > 0L ? 2 : 0);

        if (progress >= recipe.energy) {
            ItemStack newInput = input.copy();
            newInput.shrink(recipe.input.count);
            if (newInput.getCount() <= 0) {
                newInput = ItemStack.EMPTY;
            }
            setSlotStack(inputSlot, newInput);

            if (outputExisting.isEmpty()) {
                setSlotStack(outputSlot, outputToMake.copy());
            } else {
                ItemStack merged = outputExisting.copy();
                merged.grow(outputToMake.getCount());
                setSlotStack(outputSlot, merged);
            }

            reset(processor, timerSlot, tile, world);
            return;
        }

        sync(tile, world);
    }

    private void setIdle(Object processor, Object timerSlot) throws Exception {
        invoke(processor, "setVariable", "dispIn", "Ожидание...");
        invoke(processor, "setVariable", "dispOut", "-");
        invoke(processor, "setVariable", "dispEn", "0");
        invoke(processor, "setVariable", "dispTick", "0");
        invoke(processor, "setVariable", "dispProg", "0");
        invoke(processor, "setVariable", "visualActiveMol", 0);
        invoke(processor, "setVariable", "active", 0);

        setTimer(timerSlot, 100, 0, false);
    }

    private void reset(Object processor, Object timerSlot, TileEntity tile, World world) throws Exception {
        invoke(processor, "setVariable", VAR_RECIPE, "");
        invoke(processor, "setVariable", VAR_PROGRESS, 0L);
        setIdle(processor, timerSlot);
        sync(tile, world);
    }

    private void updateText(Object processor, RecipeSpec recipe, long progress, long taken) throws Exception {
        ItemStack in = recipe.input.create();
        ItemStack out = recipe.output.create();

        invoke(processor, "setVariable", "dispIn", in.isEmpty() ? "-" : in.getDisplayName());
        invoke(processor, "setVariable", "dispOut", out.isEmpty() ? "-" : out.getDisplayName());
        invoke(processor, "setVariable", "dispEn", String.valueOf(recipe.energy));
        invoke(processor, "setVariable", "dispTick", String.valueOf(taken));
        invoke(processor, "setVariable", "dispProg", String.valueOf(percent(progress, recipe.energy)));
    }

    private int percent(long progress, long max) {
        if (max <= 0L) return 100;
        return (int) Math.min(100L, (progress * 100L) / max);
    }

    private RecipeSpec findRecipe(ItemStack input) {
        if (input == null || input.isEmpty()) return null;
        for (RecipeSpec recipe : RECIPES) {
            if (recipe.matches(input)) return recipe;
        }
        return null;
    }

    private boolean canFit(ItemStack existing, ItemStack add) {
        if (add == null || add.isEmpty()) return false;
        if (existing == null || existing.isEmpty()) return true;
        if (!ItemStack.areItemsEqual(existing, add)) return false;
        if (!ItemStack.areItemStackTagsEqual(existing, add)) return false;
        return existing.getCount() + add.getCount() <= existing.getMaxStackSize();
    }

    private long getEnergyAmount(Object energySlot) throws Exception {
        Object value = invoke(energySlot, "getAmount");
        return asLong(value);
    }

    private long extractEnergy(Object energySlot, long amount) throws Exception {
        try {
            Object result = invoke(energySlot, "extract", amount, false);
            return asLong(result);
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    private void setTimer(Object timerSlot, int duration, int time, boolean active) throws Exception {
        invoke(timerSlot, "setDuration", duration);
        invoke(timerSlot, "setTime", time);
        invoke(timerSlot, "setActive", active);
    }

    private boolean isTargetTile(TileEntity tile) {
        if (tile == null) return false;
        if (!tile.getClass().getName().equals(TILE_CLASS)) return false;

        Block block = tile.getBlockType();
        if (block == null || block.getRegistryName() == null) return false;

        return BLOCK_ID.equals(block.getRegistryName().toString());
    }

    private ItemStack getSlotStack(Object slot) throws Exception {
        Object helper = invoke(slot, "getItem");
        if (helper == null) return ItemStack.EMPTY;

        Object stack = invoke(helper, "getStack");
        if (stack instanceof ItemStack) {
            return (ItemStack) stack;
        }
        return ItemStack.EMPTY;
    }

    private void setSlotStack(Object slot, ItemStack stack) throws Exception {
        Object helper = invoke(slot, "getItem");
        if (helper != null) {
            invoke(helper, "setStack", stack);
        }
    }

    private void sync(TileEntity tile, World world) {
        try {
            tile.markDirty();
            if (world != null && !world.isRemote) {
                world.notifyBlockUpdate(tile.getPos(), world.getBlockState(tile.getPos()), world.getBlockState(tile.getPos()), 3);
            }
        } catch (Throwable ignored) {
        }
    }

    private Object invoke(Object target, String methodName, Object... args) throws Exception {
        if (target == null) return null;

        Method found = null;
        Method[] methods = target.getClass().getMethods();

        outer:
        for (Method method : methods) {
            if (!method.getName().equals(methodName)) continue;

            Class<?>[] types = method.getParameterTypes();
            if (types.length != args.length) continue;

            for (int i = 0; i < types.length; i++) {
                Object arg = args[i];
                if (arg == null) continue;

                Class<?> need = wrap(types[i]);
                if (!need.isAssignableFrom(arg.getClass())) {
                    continue outer;
                }
            }

            found = method;
            break;
        }

        if (found == null) {
            throw new NoSuchMethodException(target.getClass().getName() + "#" + methodName);
        }

        found.setAccessible(true);
        return found.invoke(target, args);
    }

    private Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == boolean.class) return Boolean.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == short.class) return Short.class;
        if (type == byte.class) return Byte.class;
        if (type == char.class) return Character.class;
        return type;
    }

    private static String asString(Object obj) {
        return obj == null ? "" : String.valueOf(obj);
    }

    private static long asLong(Object obj) {
        if (obj == null) return 0L;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try {
            return Long.parseLong(String.valueOf(obj));
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    private static StackRef stack(String id, int meta, int count) {
        return new StackRef(id, meta, count);
    }

    private static final class StackRef {
        final String id;
        final int meta;
        final int count;

        StackRef(String id, int meta, int count) {
            this.id = id;
            this.meta = meta;
            this.count = count;
        }

        boolean matches(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return false;

            Item item = Item.REGISTRY.getObject(new ResourceLocation(id));
            if (item == null) return false;

            return stack.getItem() == item
                    && stack.getMetadata() == meta
                    && stack.getCount() >= count;
        }

        ItemStack create() {
            Item item = Item.REGISTRY.getObject(new ResourceLocation(id));
            if (item == null) return ItemStack.EMPTY;
            return new ItemStack(item, count, meta);
        }
    }

    private static final class RecipeSpec {
        final String name;
        final StackRef input;
        final StackRef output;
        final long energy;

        RecipeSpec(String name, StackRef input, StackRef output, long energy) {
            this.name = name;
            this.input = input;
            this.output = output;
            this.energy = energy;
        }

        boolean matches(ItemStack stack) {
            return input.matches(stack);
        }

        static RecipeSpec byName(String name) {
            for (RecipeSpec recipe : RECIPES) {
                if (recipe.name.equals(name)) return recipe;
            }
            return null;
        }
    }
}