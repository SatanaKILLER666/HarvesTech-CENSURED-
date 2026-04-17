package com.ogryzok.semencentrifuge.network;

import com.ogryzok.lifecrusher.network.GuiHandlerLifeCrusher;
import com.ogryzok.semencentrifuge.SemenCentrifugeRegistry;
import com.ogryzok.semencentrifuge.client.gui.GuiSemenCentrifugeBase;
import com.ogryzok.semencentrifuge.client.gui.GuiSemenCentrifugeMotor;
import com.ogryzok.semencentrifuge.container.ContainerSemenCentrifugeBase;
import com.ogryzok.semencentrifuge.container.ContainerSemenCentrifugeMotor;
import com.ogryzok.semencentrifuge.tile.TileSemenCentrifugeBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class GuiHandlerSemenCentrifuge implements IGuiHandler {

    private final GuiHandlerLifeCrusher lifeCrusherFallback = new GuiHandlerLifeCrusher();

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(new BlockPos(x, y, z));

        if (te instanceof TileSemenCentrifugeBase) {
            TileSemenCentrifugeBase tile = (TileSemenCentrifugeBase) te;

            if (ID == SemenCentrifugeRegistry.GUI_ID_BASE) {
                return new ContainerSemenCentrifugeBase(player.inventory, tile);
            }

            if (ID == SemenCentrifugeRegistry.GUI_ID_MOTOR) {
                return new ContainerSemenCentrifugeMotor(tile);
            }
        }

        return lifeCrusherFallback.getServerGuiElement(ID, player, world, x, y, z);
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(new BlockPos(x, y, z));

        if (te instanceof TileSemenCentrifugeBase) {
            TileSemenCentrifugeBase tile = (TileSemenCentrifugeBase) te;

            if (ID == SemenCentrifugeRegistry.GUI_ID_BASE) {
                return new GuiSemenCentrifugeBase(player.inventory, tile);
            }

            if (ID == SemenCentrifugeRegistry.GUI_ID_MOTOR) {
                return new GuiSemenCentrifugeMotor(
                        new ContainerSemenCentrifugeMotor(tile),
                        tile
                );
            }
        }

        return lifeCrusherFallback.getClientGuiElement(ID, player, world, x, y, z);
    }
}