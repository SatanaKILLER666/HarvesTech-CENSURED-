package com.ogryzok.network;

import com.ogryzok.harvestech;
import com.ogryzok.manualharvest.network.PacketStartManualHarvest;
import com.ogryzok.manualharvest.network.PacketStartManualHarvestHandler;
import com.ogryzok.manualharvest.network.PacketStartBiomassBeam;
import com.ogryzok.manualharvest.network.PacketStartBiomassBeamHandler;
import com.ogryzok.player.semen.packet.PacketSyncSemen;
import com.ogryzok.player.semen.packet.PacketSyncSemenHandler;
import com.ogryzok.semencentrifuge.network.packet.PacketCentrifugeFinish;
import com.ogryzok.semencentrifuge.network.packet.PacketCentrifugeFinishHandler;
import com.ogryzok.semencentrifuge.network.packet.PacketCentrifugeHit;
import com.ogryzok.semencentrifuge.network.packet.PacketCentrifugeHitHandler;
import com.ogryzok.semencentrifuge.network.packet.PacketCentrifugeStart;
import com.ogryzok.semencentrifuge.network.packet.PacketCentrifugeStartHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class ModNetwork {

    public static final SimpleNetworkWrapper CHANNEL =
            NetworkRegistry.INSTANCE.newSimpleChannel(harvestech.MODID);

    private static int packetId = 0;

    public static void init() {
        CHANNEL.registerMessage(PacketSyncSemenHandler.class, PacketSyncSemen.class, packetId++, Side.CLIENT);

        CHANNEL.registerMessage(PacketCentrifugeStartHandler.class, PacketCentrifugeStart.class, packetId++, Side.SERVER);
        CHANNEL.registerMessage(PacketCentrifugeHitHandler.class, PacketCentrifugeHit.class, packetId++, Side.SERVER);
        CHANNEL.registerMessage(PacketCentrifugeFinishHandler.class, PacketCentrifugeFinish.class, packetId++, Side.SERVER);
        CHANNEL.registerMessage(PacketStartManualHarvestHandler.class, PacketStartManualHarvest.class, packetId++, Side.SERVER);
        CHANNEL.registerMessage(PacketStartBiomassBeamHandler.class, PacketStartBiomassBeam.class, packetId++, Side.CLIENT);
    }
}