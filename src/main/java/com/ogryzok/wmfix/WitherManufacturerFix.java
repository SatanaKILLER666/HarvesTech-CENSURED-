package com.ogryzok.wmfix;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WitherManufacturerFix {

    private static final String TILE_CLASS = "com.bordlistian.requious.tile.TileEntityAssembly";
    private static final String BLOCK_ID = "requious:wither_manufacturer";

    private static final int RECIPE_TIME = 100;
    private static final long TOTAL_FE = 120000L;

    private static final String VAR_ACTIVE = "wmfix_active";
    private static final String VAR_PROGRESS = "wmfix_progress";
    private static final String VAR_LEFT = "wmfix_left";

    // Эти две переменные читает GUI из .zs
    private static final String VAR_HEALTH = "health";
    private static final String VAR_BASE_HEALTH = "baseHealth";

    private static final int IDLE_SYNC_INTERVAL = 10;

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        World world = event.world;
        if (world == null || world.isRemote) return;

        List<TileEntity> tiles = new ArrayList<>(world.loadedTileEntityList);
        for (TileEntity tile : tiles) {
            try {
                if (!isTargetTile(tile)) continue;
                tickMachine(tile, world);
            } catch (Throwable t) {
                System.out.println("[WMFIX] tick error: " + t);
                t.printStackTrace();
            }
        }
    }

    private void tickMachine(TileEntity tile, World world) throws Exception {
        Object processor = invoke(tile, "getProcessor");
        if (processor == null) return;

        Object inputSoulSlot = invoke(processor, "getSlot", 0, 1);
        Object inputSkullSlot = invoke(processor, "getSlot", 1, 1);
        Object outputSlot = invoke(processor, "getSlot", 5, 1);
        Object energySlot = invoke(processor, "getSlot", 8, 0);

        if (inputSoulSlot == null || inputSkullSlot == null || outputSlot == null || energySlot == null) {
            return;
        }

        ItemStack soulStack = getSlotStack(inputSoulSlot);
        ItemStack skullStack = getSlotStack(inputSkullSlot);
        ItemStack outputStack = getSlotStack(outputSlot);
        ItemStack toolStack = findToolStack(processor);

        boolean active = asBoolean(invoke(processor, "getVariable", VAR_ACTIVE));
        int progress = asInt(invoke(processor, "getVariable", VAR_PROGRESS));
        long left = asLong(invoke(processor, "getVariable", VAR_LEFT));

        boolean hasRecipe = isSoulSand(soulStack) && soulStack.getCount() >= 4
                && isWitherSkull(skullStack) && skullStack.getCount() >= 3
                && isWeaponLike(toolStack)
                && canFit(outputStack, new ItemStack(Item.getByNameOrId("minecraft:nether_star")));

        if (!active) {
            if (world.getTotalWorldTime() % IDLE_SYNC_INTERVAL == 0L) {
                sync(tile, world);
            }

            if (!hasRecipe) {
                setGauge(processor, 0, RECIPE_TIME);
                return;
            }

            if (!hasEnoughEnergyBuffered(energySlot, TOTAL_FE)) {
                setGauge(processor, 0, RECIPE_TIME);
                return;
            }

            invoke(processor, "setVariable", VAR_ACTIVE, true);
            invoke(processor, "setVariable", VAR_PROGRESS, 0);
            invoke(processor, "setVariable", VAR_LEFT, TOTAL_FE);

            setGauge(processor, 0, RECIPE_TIME);

            System.out.println("[WMFIX] recipe started");
            sync(tile, world);
            return;
        }

        if (!hasRecipe) {
            reset(processor, tile, world);
            return;
        }

        int toolX = findToolX(processor);
        int toolY = findToolY(processor);
        if (toolX < 0 || toolY < 0) {
            // Если tool slot не нашли, всё равно разрешаем работу без падения
            // но раз у рецепта tool нужен — безопаснее сброситься
            reset(processor, tile, world);
            return;
        }

        Object toolSlot = invoke(processor, "getSlot", toolX, toolY);
        if (toolSlot == null) {
            reset(processor, tile, world);
            return;
        }

        if (progress < 0) progress = 0;
        if (progress > RECIPE_TIME) progress = 0;
        if (left < 0L) left = 0L;

        int ticksLeft = Math.max(1, RECIPE_TIME - progress);
        long needThisTick = Math.max(1L, (left + ticksLeft - 1L) / ticksLeft);
        long extracted = extractEnergy(processor, energySlot, needThisTick);

        if (extracted < needThisTick) {
            sync(tile, world);
            return;
        }

        left -= extracted;
        progress++;

        invoke(processor, "setVariable", VAR_PROGRESS, progress);
        invoke(processor, "setVariable", VAR_LEFT, left);
        setGauge(processor, progress, RECIPE_TIME);

        if (progress < RECIPE_TIME) {
            sync(tile, world);
            return;
        }

        // Финал рецепта
        ItemStack soulNew = soulStack.copy();
        soulNew.shrink(4);
        if (soulNew.getCount() <= 0) soulNew = ItemStack.EMPTY;
        setSlotStack(inputSoulSlot, soulNew);

        ItemStack skullNew = skullStack.copy();
        skullNew.shrink(3);
        if (skullNew.getCount() <= 0) skullNew = ItemStack.EMPTY;
        setSlotStack(inputSkullSlot, skullNew);

        ItemStack result = new ItemStack(Item.getByNameOrId("minecraft:nether_star"));
        if (outputStack.isEmpty()) {
            setSlotStack(outputSlot, result.copy());
        } else {
            ItemStack grown = outputStack.copy();
            grown.grow(result.getCount());
            setSlotStack(outputSlot, grown);
        }

        damageTool(toolSlot, toolStack);

        System.out.println("[WMFIX] recipe finished");
        reset(processor, tile, world);
    }

    private void damageTool(Object toolSlot, ItemStack toolStack) throws Exception {
        if (toolStack == null || toolStack.isEmpty()) return;
        if (!toolStack.isItemStackDamageable()) return;

        ItemStack copy = toolStack.copy();
        copy.setItemDamage(copy.getItemDamage() + 1);

        if (copy.getItemDamage() >= copy.getMaxDamage()) {
            setSlotStack(toolSlot, ItemStack.EMPTY);
        } else {
            setSlotStack(toolSlot, copy);
        }
    }

    private void reset(Object processor, TileEntity tile, World world) throws Exception {
        invoke(processor, "setVariable", VAR_ACTIVE, false);
        invoke(processor, "setVariable", VAR_PROGRESS, 0);
        invoke(processor, "setVariable", VAR_LEFT, 0L);
        setGauge(processor, 0, RECIPE_TIME);
        sync(tile, world);
    }

    private void setGauge(Object processor, int progress, int max) throws Exception {
        // GUI в .zs читает health/baseHealth
        // Делаем прямой прогресс: 0 -> max
        invoke(processor, "setVariable", VAR_HEALTH, progress);
        invoke(processor, "setVariable", VAR_BASE_HEALTH, max);
    }

    private boolean isSoulSand(ItemStack stack) {
        return hasId(stack, "minecraft:soul_sand");
    }

    private boolean isWitherSkull(ItemStack stack) {
        return hasId(stack, "minecraft:skull") && stack.getMetadata() == 1;
    }

    private boolean isWeaponLike(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        if (stack.isItemStackDamageable()) return true;

        try {
            Set<String> classes = stack.getItem().getToolClasses(stack);
            if (classes != null) {
                for (String s : classes) {
                    if ("sword".equalsIgnoreCase(s) || "axe".equalsIgnoreCase(s)) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        String cls = stack.getItem().getClass().getName().toLowerCase();
        return cls.contains("sword") || cls.contains("axe") || cls.contains("weapon");
    }

    private boolean canFit(ItemStack current, ItemStack result) {
        if (result == null || result.isEmpty()) return false;
        if (current == null || current.isEmpty()) return true;
        if (!ItemStack.areItemsEqual(current, result)) return false;
        if (!ItemStack.areItemStackTagsEqual(current, result)) return false;
        return current.getCount() + result.getCount() <= current.getMaxStackSize();
    }

    private boolean hasEnoughEnergyBuffered(Object energySlot, long amount) {
        try {
            Object sim = invoke(energySlot, "extract", amount, true);
            if (sim instanceof Number) {
                return ((Number) sim).longValue() >= amount;
            }
        } catch (Throwable ignored) {
        }

        Long stored = readStoredEnergy(energySlot);
        return stored != null && stored >= amount;
    }

    private long extractEnergy(Object processor, Object energySlot, long amount) throws Exception {
        // Сначала как в MolecularTransformerFix — через группу feInput
        try {
            Object v = invoke(processor, "extractEnergy", "feInput", amount);
            if (v instanceof Number) {
                return ((Number) v).longValue();
            }
        } catch (Throwable ignored) {
        }

        // Потом напрямую из energy slot
        try {
            Object v = invoke(energySlot, "extract", amount, false);
            if (v instanceof Number) {
                return ((Number) v).longValue();
            }
        } catch (Throwable ignored) {
        }

        return 0L;
    }

    private Long readStoredEnergy(Object energySlot) {
        String[] methods = new String[] { "getStored", "getEnergyStored", "getAmount", "getValue" };
        for (String m : methods) {
            try {
                Object v = invoke(energySlot, m);
                if (v instanceof Number) {
                    return ((Number) v).longValue();
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private ItemStack findToolStack(Object processor) throws Exception {
        int x = findToolX(processor);
        int y = findToolY(processor);
        if (x < 0 || y < 0) return ItemStack.EMPTY;

        Object slot = invoke(processor, "getSlot", x, y);
        if (slot == null) return ItemStack.EMPTY;

        return getSlotStack(slot);
    }

    private int findToolX(Object processor) throws Exception {
        for (int x = 0; x < 12; x++) {
            for (int y = 0; y < 6; y++) {
                Object slot = null;
                try {
                    slot = invoke(processor, "getSlot", x, y);
                } catch (Throwable ignored) {
                }
                if (slot == null) continue;

                try {
                    Object r = invoke(slot, "isGroup", "tool");
                    if (r instanceof Boolean && (Boolean) r) {
                        return x;
                    }
                } catch (Throwable ignored) {
                }

                // fallback на известные координаты из client zs
                if (x == 8 && y == 2) return x;
            }
        }
        return -1;
    }

    private int findToolY(Object processor) throws Exception {
        for (int x = 0; x < 12; x++) {
            for (int y = 0; y < 6; y++) {
                Object slot = null;
                try {
                    slot = invoke(processor, "getSlot", x, y);
                } catch (Throwable ignored) {
                }
                if (slot == null) continue;

                try {
                    Object r = invoke(slot, "isGroup", "tool");
                    if (r instanceof Boolean && (Boolean) r) return y;
                } catch (Throwable ignored) {}

                if (x == 8 && y == 2) return y;
            }
        }
        return -1;
    }

    private boolean isTargetTile(TileEntity tile) {
        if (tile == null) return false;
        if (!TILE_CLASS.equals(tile.getClass().getName())) return false;

        try {
            Object block = tile.getBlockType();
            if (block == null) return false;
            Object regName = invoke(block, "getRegistryName");
            return regName != null && BLOCK_ID.equals(String.valueOf(regName));
        } catch (Throwable t) {
            return false;
        }
    }

    private ItemStack getSlotStack(Object slot) throws Exception {
        if (slot == null) return ItemStack.EMPTY;

        Object itemHelper = invoke(slot, "getItem");
        if (itemHelper == null) return ItemStack.EMPTY;

        Object stack = invoke(itemHelper, "getStack");
        if (stack instanceof ItemStack) {
            return ((ItemStack) stack).copy();
        }

        return ItemStack.EMPTY;
    }

    private void setSlotStack(Object slot, ItemStack stack) throws Exception {
        if (slot == null) return;

        Object itemHelper = invoke(slot, "getItem");
        if (itemHelper != null) {
            invoke(itemHelper, "setStack", stack);
        }
    }

    private void sync(TileEntity tile, World world) {
        try {
            tile.markDirty();
        } catch (Throwable ignored) {
        }

        try {
            world.notifyBlockUpdate(tile.getPos(), world.getBlockState(tile.getPos()), world.getBlockState(tile.getPos()), 3);
        } catch (Throwable ignored) {
        }
    }

    private boolean hasId(ItemStack stack, String id) {
        if (stack == null || stack.isEmpty()) return false;
        try {
            return stack.getItem().getRegistryName() != null
                    && id.equals(String.valueOf(stack.getItem().getRegistryName()));
        } catch (Throwable t) {
            return false;
        }
    }

    private Object invoke(Object obj, String name, Object... args) throws Exception {
        if (obj == null) return null;

        Class<?> c = obj.getClass();
        while (c != null) {
            for (Method m : c.getDeclaredMethods()) {
                if (!m.getName().equals(name)) continue;

                Class<?>[] p = m.getParameterTypes();
                if (p.length != args.length) continue;

                boolean ok = true;
                for (int i = 0; i < p.length; i++) {
                    if (args[i] == null) continue;
                    if (!wrap(p[i]).isAssignableFrom(wrap(args[i].getClass()))) {
                        ok = false;
                        break;
                    }
                }

                if (!ok) continue;

                m.setAccessible(true);
                return m.invoke(obj, args);
            }
            c = c.getSuperclass();
        }

        throw new NoSuchMethodException(obj.getClass().getName() + "." + name);
    }

    private Class<?> wrap(Class<?> c) {
        if (!c.isPrimitive()) return c;
        if (c == int.class) return Integer.class;
        if (c == long.class) return Long.class;
        if (c == boolean.class) return Boolean.class;
        if (c == double.class) return Double.class;
        if (c == float.class) return Float.class;
        if (c == short.class) return Short.class;
        if (c == byte.class) return Byte.class;
        if (c == char.class) return Character.class;
        return c;
    }

    private boolean asBoolean(Object o) {
        if (o instanceof Boolean) return (Boolean) o;
        if (o instanceof Number) return ((Number) o).intValue() != 0;
        if (o == null) return false;
        return Boolean.parseBoolean(String.valueOf(o));
    }

    private int asInt(Object o) {
        if (o instanceof Number) return ((Number) o).intValue();
        if (o == null) return 0;
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (Throwable t) {
            return 0;
        }
    }

    private long asLong(Object o) {
        if (o instanceof Number) return ((Number) o).longValue();
        if (o == null) return 0L;
        try {
            return Long.parseLong(String.valueOf(o));
        } catch (Throwable t) {
            return 0L;
        }
    }
}