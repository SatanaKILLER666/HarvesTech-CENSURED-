package com.ogryzok.lifecrusher.network;

import com.ogryzok.lifecrusher.LifeCrusherRegistry;
import com.ogryzok.lifecrusher.client.gui.GuiLifeCrusher;
import com.ogryzok.lifecrusher.container.ContainerLifeCrusher;
import com.ogryzok.lifecrusher.tile.TileLifeCrusher;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class GuiHandlerLifeCrusher implements IGuiHandler {
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID != LifeCrusherRegistry.GUI_ID) {
            return null;
        }
        TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
        if (te instanceof TileLifeCrusher) {
            return new ContainerLifeCrusher(player.inventory, (TileLifeCrusher) te);
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID != LifeCrusherRegistry.GUI_ID) {
            return null;
        }
        TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
        if (te instanceof TileLifeCrusher) {
            return new GuiLifeCrusher(player.inventory, (TileLifeCrusher) te);
        }
        return null;
    }
}
