package com.ogryzok.sdsfix;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class SpatialDragonSummonerFix {

    private static final String TILE_CLASS = "com.bordlistian.requious.tile.TileEntityAssembly";
    private static final String BLOCK_ID = "requious:spatial_dragon_summoner";

    private static final String INPUT_END = "minecraft:end_crystal";
    private static final String INPUT_CHAOS = "contenttweaker:chaos_end_crystal";

    private static final int RECIPE_TIME = 100;
    private static final long BASE_FE = 140_000_000L;
    private static final long CHAOS_FE = 500_000_000L;

    private static final String VAR_ACTIVE = "sdsfix_active";
    private static final String VAR_MODE = "sdsfix_mode";
    private static final String VAR_PROGRESS = "sdsfix_progress";
    private static final String VAR_LEFT = "sdsfix_left";

    private static final int IDLE_SYNC_INTERVAL = 10;

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
                System.out.println("[SDSFIX] tick error: " + t);
                t.printStackTrace();
            }
        }
    }

    private void tickMachine(TileEntity tile, World world) throws Exception {
        Object processor = invoke(tile, "getProcessor");
        if (processor == null) return;

        Object inputSlot = invoke(processor, "getSlot", 0, 0);
        Object bowSlot = invoke(processor, "getSlot", 0, 1);
        Object energySlot = invoke(processor, "getSlot", 9, 0);
        Object timerSlot = invoke(processor, "getSlot", 9, 2);

        if (inputSlot == null || bowSlot == null || energySlot == null || timerSlot == null) return;

        ItemStack input = getSlotStack(inputSlot);
        ItemStack bow = getSlotStack(bowSlot);

        boolean active = asBoolean(invoke(processor, "getVariable", VAR_ACTIVE));
        String mode = asString(invoke(processor, "getVariable", VAR_MODE));
        int progress = asInt(invoke(processor, "getVariable", VAR_PROGRESS));
        long feLeft = asLong(invoke(processor, "getVariable", VAR_LEFT));

        RecipeSpec wanted = resolveRecipe(input);

        if (!active && world.getTotalWorldTime() % IDLE_SYNC_INTERVAL == 0) {
            sync(tile, world);
        }

        if (!active) {
            if (wanted != null && isBowLike(bow)) {
                if (hasEnoughEnergyBuffered(energySlot, wanted.totalFe)) {
                    invoke(processor, "setVariable", VAR_ACTIVE, true);
                    invoke(processor, "setVariable", VAR_MODE, wanted.mode);
                    invoke(processor, "setVariable", VAR_PROGRESS, 0);
                    invoke(processor, "setVariable", VAR_LEFT, wanted.totalFe);

                    invoke(timerSlot, "setDuration", RECIPE_TIME);
                    invoke(timerSlot, "setTime", 0);
                    invoke(timerSlot, "setActive", false);

                    System.out.println("[SDSFIX] recipe started: " + wanted.mode);
                    sync(tile, world);
                }
            }
            return;
        }

        RecipeSpec recipe = RecipeSpec.byMode(mode);
        if (recipe == null) {
            reset(processor, timerSlot, tile, world);
            return;
        }

        if (wanted == null || !recipe.mode.equals(wanted.mode)) {
            reset(processor, timerSlot, tile, world);
            return;
        }

        if (!isBowLike(bow)) {
            invoke(timerSlot, "setActive", false);
            sync(tile, world);
            return;
        }

        List<ItemStack> outputs = buildOutputs(recipe);
        if (outputs.isEmpty()) {
            System.out.println("[SDSFIX] outputs empty for mode: " + recipe.mode);
            invoke(timerSlot, "setActive", false);
            sync(tile, world);
            return;
        }

        int ticksLeft = Math.max(1, RECIPE_TIME - progress);
        long fePerTick = Math.max(1L, (feLeft + ticksLeft - 1L) / ticksLeft);

        long got = extractEnergy(energySlot, fePerTick);
        if (got < fePerTick) {
            invoke(timerSlot, "setActive", false);
            sync(tile, world);
            return;
        }

        feLeft -= fePerTick;
        progress++;

        invoke(processor, "setVariable", VAR_PROGRESS, progress);
        invoke(processor, "setVariable", VAR_LEFT, feLeft);
        invoke(timerSlot, "setTime", progress);
        invoke(timerSlot, "setActive", true);

        if (progress >= RECIPE_TIME) {
            if (finishRecipe(processor, inputSlot, bowSlot, recipe)) {
                System.out.println("[SDSFIX] recipe finished: " + recipe.mode);
                reset(processor, timerSlot, tile, world);
                return;
            } else {
                invoke(timerSlot, "setActive", false);
                sync(tile, world);
                return;
            }
        }

        sync(tile, world);
    }

    private boolean finishRecipe(Object processor, Object inputSlot, Object bowSlot, RecipeSpec recipe) throws Exception {
        ItemStack input = getSlotStack(inputSlot);
        if (input.isEmpty() || !hasId(input, recipe.inputId) || input.getCount() < 4) {
            System.out.println("[SDSFIX] finishRecipe: bad input");
            return false;
        }

        ItemStack bow = getSlotStack(bowSlot);
        if (!isBowLike(bow)) {
            System.out.println("[SDSFIX] finishRecipe: bad bow");
            return false;
        }

        List<ItemStack> outputs = buildOutputs(recipe);
        if (outputs.isEmpty()) {
            System.out.println("[SDSFIX] finishRecipe: outputs empty for " + recipe.mode);
            return false;
        }

        if (!canInsertAllOutputs(processor, outputs)) {
            System.out.println("[SDSFIX] finishRecipe: no room for outputs");
            return false;
        }

        ItemStack newInput = input.copy();
        newInput.shrink(4);
        if (newInput.getCount() <= 0) {
            newInput = ItemStack.EMPTY;
        }

        ItemStack newBow = bow.copy();
        newBow.setItemDamage(newBow.getItemDamage() + 1);

        setSlotStack(inputSlot, newInput);

        if (newBow.getMaxDamage() > 0 && newBow.getItemDamage() >= newBow.getMaxDamage()) {
            setSlotStack(bowSlot, ItemStack.EMPTY);
        } else {
            setSlotStack(bowSlot, newBow);
        }

        for (ItemStack out : outputs) {
            if (!insertOutput(processor, out)) {
                System.out.println("[SDSFIX] finishRecipe: failed to insert " + out);
                return false;
            }
        }

        return true;
    }

    private List<ItemStack> buildOutputs(RecipeSpec recipe) {
        List<ItemStack> outputs = new ArrayList<>();

        Item heart = Item.REGISTRY.getObject(new ResourceLocation("draconicevolution:dragon_heart"));
        if (heart == null) {
            System.out.println("[SDSFIX] missing dragon_heart");
            return outputs;
        }

        if ("base".equals(recipe.mode)) {
            Item breath = Items.DRAGON_BREATH;

            if (breath == null) {
                System.out.println("[SDSFIX] missing dragon_breath");
                return outputs;
            }

            outputs.add(new ItemStack(heart, 1));

            Item eggItem = Item.getItemFromBlock(Blocks.DRAGON_EGG);
            if (eggItem != null && eggItem != Item.getItemFromBlock(Blocks.AIR)) {
                outputs.add(new ItemStack(eggItem, 1));
            } else {
                System.out.println("[SDSFIX] dragon_egg item is unavailable, skipping egg output");
            }

            outputs.add(new ItemStack(breath, 16));
            return outputs;
        }

        if ("chaos".equals(recipe.mode)) {
            Item shard = Item.REGISTRY.getObject(new ResourceLocation("draconicevolution:chaos_shard"));

            if (shard == null) {
                System.out.println("[SDSFIX] missing chaos_shard");
                return outputs;
            }

            outputs.add(new ItemStack(heart, 1));
            outputs.add(new ItemStack(shard, 5));
            return outputs;
        }

        return outputs;
    }

    private boolean canInsertAllOutputs(Object processor, List<ItemStack> outputs) throws Exception {
        List<ItemStack> simulated = new ArrayList<>();

        for (int x = 2; x <= 8; x++) {
            for (int y = 0; y <= 2; y++) {
                Object slot = invoke(processor, "getSlot", x, y);
                if (slot == null) {
                    continue;
                }
                simulated.add(getSlotStack(slot));
            }
        }

        for (ItemStack out : outputs) {
            if (!simulateInsert(simulated, out)) {
                return false;
            }
        }

        return true;
    }

    private boolean simulateInsert(List<ItemStack> slots, ItemStack output) {
        for (int i = 0; i < slots.size(); i++) {
            ItemStack stack = slots.get(i);

            if (stack.isEmpty()) {
                slots.set(i, output.copy());
                return true;
            }

            if (ItemStack.areItemsEqual(stack, output)
                    && ItemStack.areItemStackTagsEqual(stack, output)
                    && stack.getCount() + output.getCount() <= stack.getMaxStackSize()) {
                ItemStack grown = stack.copy();
                grown.grow(output.getCount());
                slots.set(i, grown);
                return true;
            }
        }

        return false;
    }

    private boolean insertOutput(Object processor, ItemStack output) throws Exception {
        for (int x = 2; x <= 8; x++) {
            for (int y = 0; y <= 2; y++) {
                Object slot = invoke(processor, "getSlot", x, y);
                if (slot == null) {
                    continue;
                }

                ItemStack stack = getSlotStack(slot);

                if (stack.isEmpty()) {
                    setSlotStack(slot, output.copy());
                    return true;
                }

                if (ItemStack.areItemsEqual(stack, output)
                        && ItemStack.areItemStackTagsEqual(stack, output)
                        && stack.getCount() + output.getCount() <= stack.getMaxStackSize()) {
                    ItemStack grown = stack.copy();
                    grown.grow(output.getCount());
                    setSlotStack(slot, grown);
                    return true;
                }
            }
        }

        return false;
    }

    private boolean hasEnoughEnergyBuffered(Object energySlot, long amount) {
        try {
            Object simulated = invoke(energySlot, "extract", amount, true);
            if (simulated instanceof Number) {
                return ((Number) simulated).longValue() >= amount;
            }
        } catch (Throwable ignored) {
        }

        Long stored = readStoredEnergy(energySlot);
        return stored != null && stored >= amount;
    }

    private long extractEnergy(Object energySlot, long amount) throws Exception {
        Object out = invoke(energySlot, "extract", amount, false);
        if (out instanceof Number) return ((Number) out).longValue();
        return 0L;
    }

    private Long readStoredEnergy(Object energySlot) {
        String[] methodNames = new String[] {
                "getStored",
                "getEnergyStored",
                "getAmount",
                "getValue"
        };

        for (String name : methodNames) {
            try {
                Object out = invoke(energySlot, name);
                if (out instanceof Number) {
                    return ((Number) out).longValue();
                }
            } catch (Throwable ignored) {
            }
        }

        return null;
    }

    private void reset(Object processor, Object timerSlot, TileEntity tile, World world) throws Exception {
        invoke(processor, "setVariable", VAR_ACTIVE, false);
        invoke(processor, "setVariable", VAR_MODE, "");
        invoke(processor, "setVariable", VAR_PROGRESS, 0);
        invoke(processor, "setVariable", VAR_LEFT, 0L);

        invoke(timerSlot, "setDuration", RECIPE_TIME);
        invoke(timerSlot, "setTime", 0);
        invoke(timerSlot, "setActive", false);

        sync(tile, world);
    }

    private RecipeSpec resolveRecipe(ItemStack input) {
        if (input == null || input.isEmpty() || input.getCount() < 4) {
            return null;
        }

        String id = getStackId(input);

        if (INPUT_END.equals(id)) {
            return RecipeSpec.BASE;
        }

        if (INPUT_CHAOS.equals(id)) {
            return RecipeSpec.CHAOS;
        }

        return null;
    }

    private String getStackId(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem().getRegistryName() == null) {
            return "";
        }
        return stack.getItem().getRegistryName().toString();
    }

    private boolean isBowLike(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        if (stack.getItem() instanceof ItemBow) {
            return true;
        }

        String id = getStackId(stack);
        return "draconicevolution:wyvern_bow".equals(id)
                || "draconicevolution:draconic_bow".equals(id);
    }

    private boolean hasId(ItemStack stack, String id) {
        return id.equals(getStackId(stack));
    }

    private boolean isTargetTile(TileEntity tile) {
        if (tile == null) return false;
        if (!TILE_CLASS.equals(tile.getClass().getName())) return false;

        Block block = tile.getBlockType();
        return block != null && block.getRegistryName() != null && BLOCK_ID.equals(block.getRegistryName().toString());
    }

    private ItemStack getSlotStack(Object slot) throws Exception {
        if (slot == null) {
            return ItemStack.EMPTY;
        }

        Object itemHelper = invoke(slot, "getItem");
        if (itemHelper == null) return ItemStack.EMPTY;

        Object stack = invoke(itemHelper, "getStack");
        if (stack instanceof ItemStack) return ((ItemStack) stack).copy();
        return ItemStack.EMPTY;
    }

    private void setSlotStack(Object slot, ItemStack stack) throws Exception {
        if (slot == null) {
            return;
        }

        Object itemHelper = invoke(slot, "getItem");
        if (itemHelper != null) {
            invoke(itemHelper, "setStack", stack);
        }
    }

    private void sync(TileEntity tile, World world) {
        tile.markDirty();
        BlockPos pos = tile.getPos();
        IBlockState state = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, state, state, 3);
    }

    private Object invoke(Object target, String methodName, Object... args) throws Exception {
        if (target == null) {
            return null;
        }

        Method[] methods = target.getClass().getMethods();

        outer:
        for (Method m : methods) {
            if (!m.getName().equals(methodName)) continue;
            Class<?>[] types = m.getParameterTypes();
            if (types.length != args.length) continue;

            for (int i = 0; i < types.length; i++) {
                Object arg = args[i];
                if (arg == null) continue;
                if (!compatible(types[i], arg.getClass())) continue outer;
            }

            m.setAccessible(true);
            return m.invoke(target, args);
        }

        throw new NoSuchMethodException(target.getClass().getName() + "#" + methodName);
    }

    private boolean compatible(Class<?> p, Class<?> a) {
        if (p.isAssignableFrom(a)) return true;
        if (p == int.class && a == Integer.class) return true;
        if (p == long.class && a == Long.class) return true;
        if (p == boolean.class && a == Boolean.class) return true;
        return false;
    }

    private boolean asBoolean(Object o) {
        return o instanceof Boolean && (Boolean) o;
    }

    private String asString(Object o) {
        return o instanceof String ? (String) o : "";
    }

    private int asInt(Object o) {
        return o instanceof Number ? ((Number) o).intValue() : 0;
    }

    private long asLong(Object o) {
        return o instanceof Number ? ((Number) o).longValue() : 0L;
    }

    private static class RecipeSpec {
        static final RecipeSpec BASE = new RecipeSpec("base", INPUT_END, BASE_FE);
        static final RecipeSpec CHAOS = new RecipeSpec("chaos", INPUT_CHAOS, CHAOS_FE);

        final String mode;
        final String inputId;
        final long totalFe;

        RecipeSpec(String mode, String inputId, long totalFe) {
            this.mode = mode;
            this.inputId = inputId;
            this.totalFe = totalFe;
        }

        static RecipeSpec byMode(String mode) {
            if ("base".equals(mode)) return BASE;
            if ("chaos".equals(mode)) return CHAOS;
            return null;
        }
    }
}