package com.ogryzok.manualharvest.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class BiomassBeamEffect {
    private static final List<BeamInstance> ACTIVE = new ArrayList<>();

    private BiomassBeamEffect() {
    }

    public static void start(Vec3d from, Vec3d to, int durationTicks, double width, boolean keeperStyle, boolean whiteCore) {
        if (durationTicks <= 0) return;
        ACTIVE.add(new BeamInstance(from, to, durationTicks, width, keeperStyle, whiteCore));
    }

    public static void tick() {
        Minecraft mc = Minecraft.getMinecraft();
        WorldClient world = mc.world;
        if (world == null) {
            ACTIVE.clear();
            return;
        }

        Iterator<BeamInstance> it = ACTIVE.iterator();
        while (it.hasNext()) {
            BeamInstance beam = it.next();
            if (beam.ticksLeft <= 0) {
                it.remove();
                continue;
            }

            beam.spawn(world);
            beam.ticksLeft--;

            if (beam.ticksLeft <= 0) {
                it.remove();
            }
        }
    }

    private static final class BeamInstance {
        private final Vec3d from;
        private final Vec3d to;
        private final double width;
        private final boolean keeperStyle;
        private final boolean whiteCore;
        private int ticksLeft;

        private BeamInstance(Vec3d from, Vec3d to, int ticksLeft, double width, boolean keeperStyle, boolean whiteCore) {
            this.from = from;
            this.to = to;
            this.ticksLeft = ticksLeft;
            this.width = Math.max(0.7D, width);
            this.keeperStyle = keeperStyle;
            this.whiteCore = whiteCore;
        }

        private void spawn(WorldClient world) {
            Vec3d delta = to.subtract(from);
            double len = delta.length();
            if (len < 0.001D) return;

            Vec3d dir = delta.scale(1.0D / len);
            int count = Math.max(16, (int) (len * 18.0D * width));

            for (int i = 0; i <= count; i++) {
                double t = (double) i / (double) count;
                double px = from.x + delta.x * t;
                double py = from.y + delta.y * t;
                double pz = from.z + delta.z * t;

                double jitter = keeperStyle ? 0.02D * width : 0.05D * width;
                double ox = (world.rand.nextDouble() - 0.5D) * jitter;
                double oy = (world.rand.nextDouble() - 0.5D) * jitter;
                double oz = (world.rand.nextDouble() - 0.5D) * jitter;

                if (whiteCore) {
                    world.spawnParticle(EnumParticleTypes.END_ROD, px, py, pz, dir.x * 0.01D, dir.y * 0.01D, dir.z * 0.01D);
                    world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, px + ox * 0.4D, py + oy * 0.4D, pz + oz * 0.4D, 0.0D, 0.0D, 0.0D);
                }

                world.spawnParticle(
                        keeperStyle ? EnumParticleTypes.SPELL_MOB_AMBIENT : EnumParticleTypes.CLOUD,
                        px + ox, py + oy, pz + oz,
                        keeperStyle ? 1.0D : dir.x * 0.025D,
                        keeperStyle ? 0.92D : dir.y * 0.025D,
                        keeperStyle ? 0.78D : dir.z * 0.025D
                );

                if (keeperStyle) {
                    double ring = 0.10D * width;
                    world.spawnParticle(EnumParticleTypes.SPELL_MOB_AMBIENT,
                            px + (world.rand.nextDouble() - 0.5D) * ring,
                            py + (world.rand.nextDouble() - 0.5D) * ring,
                            pz + (world.rand.nextDouble() - 0.5D) * ring,
                            1.0D, 0.94D, 0.82D);
                } else if (world.rand.nextInt(3) == 0) {
                    world.spawnParticle(EnumParticleTypes.SPELL_MOB_AMBIENT, px + ox, py + oy, pz + oz, 1.0D, 1.0D, 1.0D);
                }
            }
        }
    }
}
