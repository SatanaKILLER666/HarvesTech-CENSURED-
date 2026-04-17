package com.ogryzok.manualharvest;

import com.ogryzok.disease.MalePowerHandler;
import com.ogryzok.food.FoodRegistry;
import com.ogryzok.manualharvest.block.BlockPallet;
import com.ogryzok.manualharvest.network.PacketStartBiomassBeam;
import com.ogryzok.network.ModNetwork;
import com.ogryzok.player.semen.ISemenStorage;
import com.ogryzok.player.semen.SemenProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ManualHarvestLogic {
    public static final int HARVEST_TICKS_TOTAL = 15 * 20;
    public static final int BASE_BEAM_TICKS = 3 * 20;
    public static final int STEROID_HARVEST_COOLDOWN_TICKS = 10 * 20;
    public static final int BEAM_DAMAGE_INTERVAL = 1;
    public static final int WEAKNESS_LOCK_TICKS = 60 * 20;
    public static final int UNSTABLE_BURST_TICKS = 3 * 20;

    private static final int BEAM_PACKET_DURATION = 3;
    private static final int KEEPER_HOVER_TICKS = 3 * 20;
    private static final int KEEPER_GLIDE_TICKS = 3 * 20;
    private static final double BEAM_KNOCKBACK_MULTIPLIER = 0.5D;
    private static final float BASE_BEAM_DAMAGE = 2.0F;
    private static final double BASE_BEAM_HIT_RADIUS = 0.45D;
    private static final double KEEPER_ASCENT_HEIGHT = 5.0D;

    private static final Map<UUID, BlockPos> LOCKED_TARGETS = new HashMap<>();

    private ManualHarvestLogic() {}

    public static boolean tryStart(EntityPlayerMP player, ISemenStorage storage) {
        if (storage == null || storage.isManualHarvesting() || storage.getUnstableBeamTicks() > 0 || player.isRiding()) {
            return false;
        }

        boolean steroidActive = MalePowerHandler.isMalePowerActive(player);
        if (!steroidActive && !storage.isFull()) {
            return false;
        }

        if (steroidActive && storage.getManualHarvestCooldownTicks() > 0) {
            return false;
        }

        storage.setManualHarvesting(true);
        storage.setManualHarvestTicks(0);
        storage.setManualHarvestStartY(player.posY);
        storage.setKeeperHoverTicks(0);
        storage.setKeeperGlideTicks(0);
        storage.setSteroidHarvest(steroidActive);
        storage.setSteroidLoadedShot(storage.isFull());
        storage.setSteroidHarvestLevel(steroidActive ? MalePowerHandler.getActiveLevel(player) : 0);
        if (steroidActive) {
            storage.setManualHarvestCooldownTicks(STEROID_HARVEST_COOLDOWN_TICKS);
        }

        LOCKED_TARGETS.remove(player.getUniqueID());
        player.swingArm(EnumHand.MAIN_HAND);
        return true;
    }

    public static void serverTick(EntityPlayer player, ISemenStorage storage) {
        if (storage == null) return;

        BeamStats stats = getManualHarvestBeamStats(player, storage);
        int totalTicks = getManualHarvestTotalTicks(player, storage);
        int beamStartTick = Math.max(1, totalTicks - stats.durationTicks);

        applyManualHarvestMovementPenalty(player, storage);

        int ticks = storage.getManualHarvestTicks() + 1;
        storage.setManualHarvestTicks(ticks);

        if (storage.getAbstinenceStage() >= 4) {
            applyKeeperLevitation(player, storage, ticks, totalTicks);
        }

        if (ticks >= beamStartTick && ticks < totalTicks) {
            BeamTrace beamTrace = traceBeam(player, stats);
            BlockPos liveTarget = getFloorTarget(player);
            LOCKED_TARGETS.put(player.getUniqueID(), liveTarget);
            sendLiveBeamForNearbyPlayers(player, beamTrace, stats);

            if (((ticks - beamStartTick) % BEAM_DAMAGE_INTERVAL) == 0) {
                damageEntitiesInBeam(player, beamTrace, stats);
            }
        }

        if (ticks >= totalTicks) {
            finish(player, storage);
        }
    }

    public static void tickUnstableBurst(EntityPlayer player, ISemenStorage storage) {
        if (storage == null || storage.getUnstableBeamTicks() <= 0) {
            return;
        }

        BeamStats stats = getUnstableBurstStats(player, storage);
        BeamTrace beamTrace = traceBeam(player, stats);
        sendLiveBeamForNearbyPlayers(player, beamTrace, stats);
        damageEntitiesInBeam(player, beamTrace, stats);
        pullPlayerBackwards(player, beamTrace, stats);
        player.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 5, 6, false, false));

        int left = storage.getUnstableBeamTicks() - 1;
        storage.setUnstableBeamTicks(left);
        if (left <= 0) {
            storage.setUnstableBeamStage(0);
        }
    }

    private static void applyManualHarvestMovementPenalty(EntityPlayer player, ISemenStorage storage) {
        if (player == null || storage == null) {
            return;
        }

        if (storage.getAbstinenceStage() >= 4) {
            return;
        }

        // Was Slowness VI during manual harvest. Lowered to Slowness II to make
        // the walking speed restriction roughly three times softer.
        player.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 10, 1, false, false));
    }

    public static boolean triggerForcedUnstableBurst(EntityPlayer player, ISemenStorage storage) {
        if (player == null || storage == null || storage.getUnstableBeamTicks() > 0 || storage.isManualHarvesting()) {
            return false;
        }
        if (storage.getAbstinenceStage() <= 0 || storage.getAbstinenceStage() >= 4) {
            return false;
        }
        if (!MalePowerHandler.isMalePowerActive(player)) {
            return false;
        }
        triggerUnstableBurst(player, storage);
        return true;
    }

    public static int getCurrentBurstChancePercent(EntityPlayer player, ISemenStorage storage) {
        if (player == null || storage == null) {
            return 0;
        }
        if (!MalePowerHandler.isMalePowerActive(player)) {
            return 0;
        }
        int stage = storage.getAbstinenceStage();
        if (stage <= 0 || stage >= 4) {
            return 0;
        }
        int minutes = MalePowerHandler.getElapsedFullMinutes(player);
        if (minutes <= 1) {
            return 1;
        }
        return Math.min(99, 1 + (minutes - 1) * 7);
    }

    public static void tryTriggerInstability(EntityPlayer player, ISemenStorage storage) {
        if (player == null || storage == null || player.world.isRemote) {
            return;
        }
        if (storage.isManualHarvesting() || storage.getUnstableBeamTicks() > 0) {
            return;
        }
        if (!MalePowerHandler.isMalePowerActive(player)) {
            storage.setLastBurstRollMinute(0);
            return;
        }
        int stage = storage.getAbstinenceStage();
        if (stage <= 0 || stage >= 4) {
            return;
        }

        int fullMinutes = MalePowerHandler.getElapsedFullMinutes(player);
        if (fullMinutes <= 0 || fullMinutes <= storage.getLastBurstRollMinute()) {
            return;
        }

        storage.setLastBurstRollMinute(fullMinutes);
        int chancePercent = getCurrentBurstChancePercent(player, storage);
        if (player.getRNG().nextInt(100) < chancePercent) {
            triggerUnstableBurst(player, storage);
        }
    }

    private static void triggerUnstableBurst(EntityPlayer player, ISemenStorage storage) {
        storage.setManualHarvesting(false);
        storage.setManualHarvestTicks(0);
        storage.setUnstableBeamStage(storage.getAbstinenceStage());
        storage.setUnstableBeamTicks(UNSTABLE_BURST_TICKS);
        storage.setWeaknessLockTicks(WEAKNESS_LOCK_TICKS);
        storage.setKeeperHoverTicks(0);
        storage.setKeeperGlideTicks(0);
        storage.setSteroidHarvest(true);
        storage.setSteroidLoadedShot(storage.isFull());
        storage.setSteroidHarvestLevel(Math.max(storage.getSteroidHarvestLevel(), MalePowerHandler.getActiveLevel(player)));
        storage.setAmount(0);
        storage.setTickCounter(0);
        LOCKED_TARGETS.remove(player.getUniqueID());

        player.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, WEAKNESS_LOCK_TICKS, 1, false, false));
    }

    public static void cancel(EntityPlayer player, ISemenStorage storage) {
        if (storage == null) return;
        storage.setManualHarvesting(false);
        storage.setManualHarvestTicks(0);
        storage.setSteroidHarvest(false);
        storage.setSteroidLoadedShot(false);
        storage.setSteroidHarvestLevel(0);
        storage.setManualHarvestStartY(0.0D);
        storage.setKeeperHoverTicks(0);
        storage.setKeeperGlideTicks(0);
        LOCKED_TARGETS.remove(player.getUniqueID());
    }

    public static void finish(EntityPlayer player, ISemenStorage storage) {
        boolean steroidHarvest = storage.isSteroidHarvest();
        boolean loadedShot = storage.isSteroidLoadedShot();
        boolean keeperHarvest = storage.getAbstinenceStage() >= 4;

        if (!steroidHarvest || loadedShot) {
            storage.setAmount(0);
            storage.setTickCounter(0);
        }

        storage.setManualHarvesting(false);
        storage.setManualHarvestTicks(0);
        storage.setSteroidHarvest(false);
        storage.setSteroidLoadedShot(false);
        storage.setSteroidHarvestLevel(0);
        if (keeperHarvest) {
            storage.setKeeperHoverTicks(KEEPER_HOVER_TICKS);
            storage.setKeeperGlideTicks(KEEPER_GLIDE_TICKS);
        }
        storage.setManualHarvestStartY(0.0D);

        BlockPos finalTarget = LOCKED_TARGETS.containsKey(player.getUniqueID())
                ? LOCKED_TARGETS.remove(player.getUniqueID())
                : getFloorTarget(player);

        BlockPos palletTarget = getLookedPalletTarget(player);
        if (palletTarget == null) {
            palletTarget = getPalletAtOrAbove(finalTarget, player);
        }

        if (!steroidHarvest || loadedShot) {
            if (palletTarget != null) {
                IBlockState palletState = player.world.getBlockState(palletTarget);
                player.world.setBlockState(palletTarget, palletState.withProperty(BlockPallet.STAGE, BlockPallet.STAGE_FULL), 3);
            } else if (canPlaceResidue(player, finalTarget)) {
                player.world.setBlockState(finalTarget, ManualHarvestRegistry.BIOMASS_RESIDUE.getDefaultState(), 3);
            }
        }

        if (player.world instanceof WorldServer) {
            WorldServer ws = (WorldServer) player.world;
            ws.spawnParticle(EnumParticleTypes.SNOW_SHOVEL, finalTarget.getX() + 0.5D, finalTarget.getY() + 0.05D, finalTarget.getZ() + 0.5D, 12, 0.18D, 0.02D, 0.18D, 0.01D);
        }
        player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_SLIME_SQUISH, SoundCategory.PLAYERS, 0.7F, 0.8F);
    }

    private static void applyKeeperLevitation(EntityPlayer player, ISemenStorage storage, int ticks, int totalTicks) {
        int midpoint = totalTicks / 2;
        if (ticks < midpoint) {
            return;
        }

        double targetY = storage.getManualHarvestStartY() + KEEPER_ASCENT_HEIGHT;
        if (player.collidedVertically && player.motionY > 0.0D) {
            player.motionY = 0.0D;
            player.velocityChanged = true;
            return;
        }

        if (player.posY < targetY - 0.35D) {
            player.addPotionEffect(new PotionEffect(MobEffects.LEVITATION, 5, 1, false, false));
        } else if (!player.collidedVertically) {
            player.motionY = 0.0D;
            player.velocityChanged = true;
        }
    }

    public static void tickKeeperAfterglow(EntityPlayer player, ISemenStorage storage) {
        if (player == null || storage == null || storage.isManualHarvesting() || storage.getUnstableBeamTicks() > 0) {
            return;
        }

        if (storage.getKeeperHoverTicks() > 0) {
            storage.setKeeperHoverTicks(storage.getKeeperHoverTicks() - 1);
            if (!player.onGround) {
                if (player.motionY < 0.0D) {
                    player.motionY = 0.0D;
                }
                player.fallDistance = 0.0F;
                player.velocityChanged = true;
            } else {
                storage.setKeeperHoverTicks(0);
                storage.setKeeperGlideTicks(0);
            }
            return;
        }

        if (storage.getKeeperGlideTicks() > 0) {
            if (player.onGround) {
                storage.setKeeperGlideTicks(0);
                return;
            }

            storage.setKeeperGlideTicks(storage.getKeeperGlideTicks() - 1);
            player.motionY = Math.max(player.motionY, -0.08D);
            player.fallDistance = 0.0F;
            player.velocityChanged = true;
        }
    }

    private static int getManualHarvestTotalTicks(EntityPlayer player, ISemenStorage storage) {
        return HARVEST_TICKS_TOTAL + Math.max(0, getManualHarvestBeamStats(player, storage).durationTicks - BASE_BEAM_TICKS);
    }

    private static BeamStats getManualHarvestBeamStats(EntityPlayer player, ISemenStorage storage) {
        int stage = storage != null ? storage.getAbstinenceStage() : 0;
        int steroidLevel = storage != null ? storage.getSteroidHarvestLevel() : 0;

        float baseDamage;
        int duration;
        double radius;
        double visualWidth;
        boolean keeperStyle = false;
        boolean whiteCore = false;

        switch (stage) {
            case 1:
                baseDamage = 2.4F;
                duration = 70;
                radius = BASE_BEAM_HIT_RADIUS;
                visualWidth = 1.0D;
                break;
            case 2:
                baseDamage = BASE_BEAM_DAMAGE * 3.0F;
                duration = 80;
                radius = BASE_BEAM_HIT_RADIUS;
                visualWidth = 1.0D;
                break;
            case 3:
                baseDamage = BASE_BEAM_DAMAGE * 5.0F;
                duration = 90;
                radius = 0.65D;
                visualWidth = 1.15D;
                break;
            case 4:
                baseDamage = 10.0F;
                duration = 100;
                radius = 1.35D;
                visualWidth = 3.3D;
                keeperStyle = true;
                whiteCore = true;
                break;
            default:
                baseDamage = BASE_BEAM_DAMAGE;
                duration = BASE_BEAM_TICKS;
                radius = BASE_BEAM_HIT_RADIUS;
                visualWidth = 1.0D;
                break;
        }

        float steroidMultiplier = getSteroidMultiplier(stage, steroidLevel);
        double beamRange = keeperStyle ? 40.0D : 15.0D;
        return new BeamStats(baseDamage * steroidMultiplier, radius * Math.max(1.0D, steroidMultiplier * 0.18D), duration,
                visualWidth * Math.max(1.0D, steroidMultiplier * 0.12D), keeperStyle, whiteCore, stage >= 3 ? 0.55D : 0.0D, beamRange);
    }

    private static BeamStats getUnstableBurstStats(EntityPlayer player, ISemenStorage storage) {
        int savedStage = storage != null ? storage.getUnstableBeamStage() : 0;
        int oldStage = storage != null ? storage.getAbstinenceStage() : 0;
        if (storage != null) {
            storage.setAbstinenceStage(savedStage);
        }
        BeamStats base = getManualHarvestBeamStats(player, storage);
        if (storage != null) {
            storage.setAbstinenceStage(oldStage);
        }
        double radius = Math.max(base.hitRadius, 0.8D);
        double width = Math.max(base.visualWidth, 1.25D);
        return new BeamStats(Math.max(base.damagePerTick, 12.0F), radius, UNSTABLE_BURST_TICKS, width, base.keeperStyle, base.whiteCore, 0.85D, base.range);
    }

    private static float getSteroidMultiplier(int stage, int steroidLevel) {
        if (steroidLevel <= 0) {
            return 1.0F;
        }
        if (stage >= 4) {
            return steroidLevel >= 2 ? 2.2F : 1.6F;
        }
        return steroidLevel >= 2 ? 5.0F : 3.0F;
    }

    private static void sendLiveBeamForNearbyPlayers(EntityPlayer player, BeamTrace beamTrace, BeamStats stats) {
        if (!(player instanceof EntityPlayerMP) || beamTrace == null) return;

        ModNetwork.CHANNEL.sendToAllAround(
                new PacketStartBiomassBeam(beamTrace.from, beamTrace.to, BEAM_PACKET_DURATION, stats.visualWidth, stats.keeperStyle, stats.whiteCore),
                new NetworkRegistry.TargetPoint(player.dimension, player.posX, player.posY, player.posZ, 48.0D)
        );
    }

    private static BeamTrace traceBeam(EntityPlayer player, BeamStats stats) {
        Vec3d look = player.getLookVec();
        double lookLen = look.length();
        if (lookLen > 1.0E-6D) {
            look = look.scale(1.0D / lookLen);
        } else {
            look = new Vec3d(0.0D, 0.0D, 1.0D);
        }

        Vec3d from = new Vec3d(
                player.posX,
                player.posY + Math.max(0.15D, player.height * 0.52D),
                player.posZ
        ).add(look.scale(0.18D));

        double range = stats != null ? stats.range : 15.0D;
        Vec3d rawTo = from.add(look.scale(range));
        RayTraceResult blockHit = player.world.rayTraceBlocks(from, rawTo, false, true, false);
        Vec3d to = blockHit != null && blockHit.hitVec != null ? blockHit.hitVec : rawTo;
        return new BeamTrace(from, to);
    }

    private static void damageEntitiesInBeam(EntityPlayer player, BeamTrace beamTrace, BeamStats stats) {
        Vec3d delta = beamTrace.to.subtract(beamTrace.from);
        double lenSq = delta.lengthSquared();
        if (lenSq < 1.0E-6D) {
            return;
        }

        AxisAlignedBB searchBox = new AxisAlignedBB(beamTrace.from.x, beamTrace.from.y, beamTrace.from.z, beamTrace.to.x, beamTrace.to.y, beamTrace.to.z)
                .grow(stats.hitRadius + 0.5D);

        for (Entity entity : player.world.getEntitiesWithinAABBExcludingEntity(player, searchBox)) {
            if (!(entity instanceof EntityLivingBase) || !entity.canBeCollidedWith()) {
                continue;
            }

            AxisAlignedBB box = entity.getEntityBoundingBox().grow(0.15D);
            RayTraceResult intercept = box.calculateIntercept(beamTrace.from, beamTrace.to);
            boolean hit = intercept != null;

            if (!hit) {
                Vec3d entityCenter = new Vec3d(entity.posX, entity.posY + entity.height * 0.5D, entity.posZ);
                double distanceSq = distanceSqToSegment(beamTrace.from, beamTrace.to, entityCenter);
                hit = distanceSq <= (stats.hitRadius * stats.hitRadius);
            }

            if (!hit) {
                continue;
            }

            entity.attackEntityFrom(DamageSource.causePlayerDamage(player), stats.damagePerTick);
            applyBeamKnockback(entity, beamTrace, player, stats);
            ((EntityLivingBase) entity).addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 30 * 20, 1, false, true));
        }
    }

    private static void applyBeamKnockback(Entity entity, BeamTrace beamTrace, EntityPlayer player, BeamStats stats) {
        if (!(entity instanceof EntityLivingBase) || entity.world.isRemote) {
            return;
        }

        Vec3d dir = beamTrace.to.subtract(beamTrace.from);
        double len = dir.length();
        if (len < 1.0E-6D) {
            return;
        }
        dir = dir.scale(1.0D / len);

        double horizontalStrength = (0.55D + Math.max(0.0D, stats.damagePerTick * 0.08D)) * BEAM_KNOCKBACK_MULTIPLIER;
        double verticalStrength = (0.18D + Math.min(0.9D, stats.hitRadius * 0.35D)) * BEAM_KNOCKBACK_MULTIPLIER;
        entity.motionX = dir.x * horizontalStrength;
        entity.motionY = Math.max(entity.motionY, verticalStrength);
        entity.motionZ = dir.z * horizontalStrength;
        entity.velocityChanged = true;
        entity.isAirBorne = true;
    }

    private static void pullPlayerBackwards(EntityPlayer player, BeamTrace beamTrace, BeamStats stats) {
        Vec3d dir = beamTrace.to.subtract(beamTrace.from);
        double len = dir.length();
        if (len < 1.0E-6D) {
            return;
        }
        dir = dir.scale(1.0D / len);
        player.motionX = -dir.x * stats.selfRecoil * BEAM_KNOCKBACK_MULTIPLIER;
        player.motionZ = -dir.z * stats.selfRecoil * BEAM_KNOCKBACK_MULTIPLIER;
        player.motionY = Math.max(player.motionY, 0.08D * BEAM_KNOCKBACK_MULTIPLIER);
        player.velocityChanged = true;
    }

    private static double distanceSqToSegment(Vec3d start, Vec3d end, Vec3d point) {
        Vec3d segment = end.subtract(start);
        double lenSq = segment.lengthSquared();
        if (lenSq < 1.0E-6D) {
            return point.squareDistanceTo(start);
        }

        double t = point.subtract(start).dotProduct(segment) / lenSq;
        t = Math.max(0.0D, Math.min(1.0D, t));
        Vec3d closest = start.add(segment.scale(t));
        return point.squareDistanceTo(closest);
    }

    private static boolean canPlaceResidue(EntityPlayer player, BlockPos pos) {
        IBlockState current = player.world.getBlockState(pos);
        if (!current.getMaterial().isReplaceable()) return false;
        IBlockState below = player.world.getBlockState(pos.down());
        return below.isTopSolid() || below.getMaterial().blocksMovement();
    }

    public static BlockPos getFloorTarget(EntityPlayer player) {
        int maxPlaceY = MathHelper.floor(player.getEntityBoundingBox().minY) + 1;
        BlockPos fallback = findNearestAllowedFloor(player, new BlockPos(player.posX, maxPlaceY, player.posZ), maxPlaceY);
        if (fallback == null) {
            fallback = new BlockPos(player.posX, maxPlaceY, player.posZ);
        }

        double reach = 5.0D;
        if (player instanceof EntityPlayerMP) {
            reach = ((EntityPlayerMP) player).interactionManager.getBlockReachDistance();
        }

        Vec3d eyes = player.getPositionEyes(1.0F);
        Vec3d end = eyes.add(player.getLookVec().scale(reach));
        RayTraceResult hit = player.world.rayTraceBlocks(eyes, end, false, true, false);
        Vec3d target = hit != null && hit.hitVec != null ? hit.hitVec : end;

        Vec3d delta = target.subtract(eyes);
        double length = delta.length();
        if (length <= 1.0E-6D) {
            return fallback;
        }

        BlockPos best = fallback;
        double step = 0.20D;
        for (double traveled = 0.0D; traveled <= length; traveled += step) {
            double t = traveled / length;
            double sx = eyes.x + delta.x * t;
            double sy = Math.min(eyes.y + delta.y * t, maxPlaceY + 0.99D);
            double sz = eyes.z + delta.z * t;

            BlockPos probe = new BlockPos(sx, sy, sz);
            BlockPos candidate = findNearestAllowedFloor(player, probe, maxPlaceY);
            if (candidate != null) {
                best = candidate;
            }
        }

        return best;
    }

    private static BlockPos findNearestAllowedFloor(EntityPlayer player, BlockPos probe, int maxPlaceY) {
        int x = probe.getX();
        int z = probe.getZ();
        int startY = Math.min(probe.getY(), maxPlaceY);
        int minY = Math.max(0, maxPlaceY - 3);

        for (int y = startY; y >= minY; y--) {
            BlockPos candidate = new BlockPos(x, y, z);
            if (canPlaceResidue(player, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static BlockPos getPalletAtOrAbove(BlockPos targetPos, EntityPlayer player) {
        IBlockState targetState = player.world.getBlockState(targetPos);
        if (targetState.getBlock() == ManualHarvestRegistry.PALLET) {
            return targetPos;
        }

        BlockPos above = targetPos.up();
        IBlockState aboveState = player.world.getBlockState(above);
        if (aboveState.getBlock() == ManualHarvestRegistry.PALLET) {
            return above;
        }

        return null;
    }

    private static BlockPos getLookedPalletTarget(EntityPlayer player) {
        double reach = 5.0D;
        if (player instanceof EntityPlayerMP) {
            reach = ((EntityPlayerMP) player).interactionManager.getBlockReachDistance();
        }

        Vec3d eyes = player.getPositionEyes(1.0F);
        Vec3d end = eyes.add(player.getLookVec().scale(reach));
        RayTraceResult hit = player.world.rayTraceBlocks(eyes, end, false, true, false);
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK || hit.getBlockPos() == null) {
            return null;
        }

        BlockPos hitPos = hit.getBlockPos();
        IBlockState hitState = player.world.getBlockState(hitPos);
        if (hitState.getBlock() != ManualHarvestRegistry.PALLET) {
            return null;
        }

        double cx = hitPos.getX() + 0.5D;
        double cy = hitPos.getY() + 0.5D;
        double cz = hitPos.getZ() + 0.5D;
        double dx = cx - player.posX;
        double dy = cy - (player.posY + 0.5D);
        double dz = cz - player.posZ;
        double distSq = dx * dx + dy * dy + dz * dz;
        return distSq <= 4.0D ? hitPos : null;
    }

    private static final class BeamTrace {
        private final Vec3d from;
        private final Vec3d to;

        private BeamTrace(Vec3d from, Vec3d to) {
            this.from = from;
            this.to = to;
        }
    }

    private static final class BeamStats {
        private final float damagePerTick;
        private final double hitRadius;
        private final int durationTicks;
        private final double visualWidth;
        private final boolean keeperStyle;
        private final boolean whiteCore;
        private final double selfRecoil;
        private final double range;

        private BeamStats(float damagePerTick, double hitRadius, int durationTicks, double visualWidth, boolean keeperStyle, boolean whiteCore, double selfRecoil, double range) {
            this.damagePerTick = damagePerTick;
            this.hitRadius = hitRadius;
            this.durationTicks = durationTicks;
            this.visualWidth = visualWidth;
            this.keeperStyle = keeperStyle;
            this.whiteCore = whiteCore;
            this.selfRecoil = selfRecoil;
            this.range = range;
        }
    }

    public static boolean useCanAndGive(EntityPlayer player, ItemStack held, ItemStack result) {
        if (held.isEmpty()) return false;

        ItemStack actualResult = result.copy();
        if (result.getItem() == FoodRegistry.BIOMASS_CAN) {
            actualResult = FoodRegistry.getFilledBiomassContainer(held);
        } else if (result.getItem() == ManualHarvestRegistry.DIRTY_BIOMASS) {
            actualResult = FoodRegistry.getDirtyBiomassContainer(held);
        }

        if (actualResult.isEmpty()) return false;

        if (!player.capabilities.isCreativeMode) {
            held.shrink(1);
        }
        if (!player.inventory.addItemStackToInventory(actualResult)) {
            player.dropItem(actualResult, false);
        }
        return true;
    }
}
