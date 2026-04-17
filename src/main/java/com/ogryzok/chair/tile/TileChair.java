package com.ogryzok.chair.tile;

import com.ogryzok.chair.entity.EntitySeat;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import java.util.List;

public class TileChair extends TileEntity implements IAnimatable {
    private final AnimationFactory factory = new AnimationFactory(this);

    @Override
    public void registerControllers(AnimationData animationData) {
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }

    public boolean isOccupied() {
        if (world == null || pos == null) {
            return false;
        }

        List<EntitySeat> seats = world.getEntitiesWithinAABB(EntitySeat.class, new AxisAlignedBB(pos).grow(0.25D));
        for (EntitySeat seat : seats) {
            if (!seat.isDead && !seat.getPassengers().isEmpty()) {
                return true;
            }
        }

        return false;
    }
}
