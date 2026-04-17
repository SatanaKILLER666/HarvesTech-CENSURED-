package com.ogryzok.network;

import com.ogryzok.lifecrusher.client.gui.GuiLifeCrusher;
import com.ogryzok.lifecrusher.container.ContainerLifeCrusher;
import com.ogryzok.lifecrusher.tile.TileLifeCrusher;

import com.ogryzok.semencentrifuge.client.gui.GuiSemenCentrifugeBase;
import com.ogryzok.semencentrifuge.client.gui.GuiSemenCentrifugeMotor;
import com.ogryzok.semencentrifuge.container.ContainerSemenCentrifugeBase;
import com.ogryzok.semencentrifuge.container.ContainerSemenCentrifugeMotor;
import com.ogryzok.semencentrifuge.tile.TileSemenCentrifugeBase;

import com.ogryzok.semencentrifuge.SemenCentrifugeRegistry;
import com.ogryzok.semendestiller.SemenDestillerRegistry;
import com.ogryzok.semendestiller.client.gui.GuiSemenDestiller;
import com.ogryzok.semendestiller.container.ContainerSemenDestiller;
import com.ogryzok.semendestiller.tile.TileSemenDestillerBase;
import com.ogryzok.lifecrusher.LifeCrusherRegistry;
import com.ogryzok.semenenrichment.SemenEnrichmentRegistry;
import com.ogryzok.semenenrichment.client.gui.GuiSemenEnrichmentChamber;
import com.ogryzok.semenenrichment.container.ContainerSemenEnrichmentChamber;
import com.ogryzok.semenenrichment.tile.TileSemenEnrichmentChamber;
import com.ogryzok.separator.tile.TileSeparator;
import com.ogryzok.separator.container.ContainerSeparator;
import com.ogryzok.separator.client.gui.GuiSeparator;
import com.ogryzok.separator.SeparatorRegistry;
import com.ogryzok.proteinformer.ProteinFormerRegistry;
import com.ogryzok.proteinformer.client.gui.GuiProteinFormer;
import com.ogryzok.proteinformer.container.ContainerProteinFormer;
import com.ogryzok.proteinformer.tile.TileProteinFormer;
import com.ogryzok.blender.BlenderRegistry;
import com.ogryzok.blender.client.gui.GuiBlender;
import com.ogryzok.blender.container.ContainerBlender;
import com.ogryzok.blender.tile.TileBlender;
import com.ogryzok.mrnasynthesizer.MRNASynthesizerRegistry;
import com.ogryzok.mrnasynthesizer.client.gui.GuiMRNASynthesizer;
import com.ogryzok.mrnasynthesizer.container.ContainerMRNASynthesizer;
import com.ogryzok.mrnasynthesizer.tile.TileMRNASynthesizer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class GuiHandlerMain implements IGuiHandler {

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {

        TileEntity te = world.getTileEntity(new BlockPos(x, y, z));

        // ===== LIFE CRUSHER =====
        if (ID == LifeCrusherRegistry.GUI_ID) {
            if (te instanceof TileLifeCrusher) {
                return new ContainerLifeCrusher(player.inventory, (TileLifeCrusher) te);
            }
        }

        // ===== SEMEN CENTRIFUGE =====
        if (te instanceof TileSemenCentrifugeBase) {
            TileSemenCentrifugeBase tile = (TileSemenCentrifugeBase) te;

            if (ID == SemenCentrifugeRegistry.GUI_ID_BASE) {
                return new ContainerSemenCentrifugeBase(player.inventory, tile);
            }

            if (ID == SemenCentrifugeRegistry.GUI_ID_MOTOR) {
                return new ContainerSemenCentrifugeMotor(tile);
            }
        }

        if (te instanceof TileSemenDestillerBase) {
            TileSemenDestillerBase tile = (TileSemenDestillerBase) te;
            if (ID == SemenDestillerRegistry.GUI_ID) {
                return new ContainerSemenDestiller(player.inventory, tile);
            }
        }

        if (te instanceof TileSemenEnrichmentChamber) {
            TileSemenEnrichmentChamber tile = (TileSemenEnrichmentChamber) te;
            if (ID == SemenEnrichmentRegistry.GUI_ID) {
                return new ContainerSemenEnrichmentChamber(player.inventory, tile);
            }
        }

        if (te instanceof TileSeparator) {
            TileSeparator tile = (TileSeparator) te;
            if (ID == SeparatorRegistry.GUI_ID) {
                return new ContainerSeparator(player.inventory, tile);
            }
        }

        if (te instanceof TileProteinFormer) {
            TileProteinFormer tile = (TileProteinFormer) te;
            if (ID == ProteinFormerRegistry.GUI_ID) {
                return new ContainerProteinFormer(player.inventory, tile);
            }
        }

        if (te instanceof TileBlender) {
            TileBlender tile = (TileBlender) te;
            if (ID == BlenderRegistry.GUI_ID) {
                return new ContainerBlender(player.inventory, tile);
            }
        }

        if (te instanceof TileMRNASynthesizer) {
            TileMRNASynthesizer tile = (TileMRNASynthesizer) te;
            if (ID == MRNASynthesizerRegistry.GUI_ID) {
                return new ContainerMRNASynthesizer(player.inventory, tile);
            }
        }

        return null;
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {

        TileEntity te = world.getTileEntity(new BlockPos(x, y, z));

        // ===== LIFE CRUSHER =====
        if (ID == LifeCrusherRegistry.GUI_ID) {
            if (te instanceof TileLifeCrusher) {
                return new GuiLifeCrusher(player.inventory, (TileLifeCrusher) te);
            }
        }

        // ===== SEMEN CENTRIFUGE =====
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

        if (te instanceof TileSemenDestillerBase) {
            TileSemenDestillerBase tile = (TileSemenDestillerBase) te;
            if (ID == SemenDestillerRegistry.GUI_ID) {
                return new GuiSemenDestiller(player.inventory, tile);
            }
        }

        if (te instanceof TileSemenEnrichmentChamber) {
            TileSemenEnrichmentChamber tile = (TileSemenEnrichmentChamber) te;
            if (ID == SemenEnrichmentRegistry.GUI_ID) {
                return new GuiSemenEnrichmentChamber(player.inventory, tile);
            }
        }

        if (te instanceof TileSeparator) {
            TileSeparator tile = (TileSeparator) te;
            if (ID == SeparatorRegistry.GUI_ID) {
                return new GuiSeparator(player.inventory, tile);
            }
        }

        if (te instanceof TileProteinFormer) {
            TileProteinFormer tile = (TileProteinFormer) te;
            if (ID == ProteinFormerRegistry.GUI_ID) {
                return new GuiProteinFormer(player.inventory, tile);
            }
        }

        if (te instanceof TileBlender) {
            TileBlender tile = (TileBlender) te;
            if (ID == BlenderRegistry.GUI_ID) {
                return new GuiBlender(player.inventory, tile);
            }
        }

        if (te instanceof TileMRNASynthesizer) {
            TileMRNASynthesizer tile = (TileMRNASynthesizer) te;
            if (ID == MRNASynthesizerRegistry.GUI_ID) {
                return new GuiMRNASynthesizer(player.inventory, tile);
            }
        }

        return null;
    }
}