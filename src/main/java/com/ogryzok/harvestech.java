package com.ogryzok;

import com.ogryzok.chair.ChairRegistry;
import com.ogryzok.lifecrusher.LifeCrusherRegistry;
import com.ogryzok.mtfix.MolecularTransformerFix;
import com.ogryzok.manualharvest.ManualHarvestRegistry;
import com.ogryzok.network.GuiHandlerMain;
import com.ogryzok.player.semen.SemenCapability;
import com.ogryzok.semencentrifuge.SemenCentrifugeRegistry;
import com.ogryzok.player.semen.SemenEvents;
import com.ogryzok.player.semen.SemenTickHandler;
import com.ogryzok.wmfix.WitherManufacturerFix;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.bernie.geckolib3.GeckoLib;
import com.ogryzok.player.semen.SemenHUD;
import com.ogryzok.network.ModNetwork;
import com.ogryzok.food.FoodRegistry;
import com.ogryzok.disease.DiseaseRegistry;
import com.ogryzok.fluids.ModFluids;
import com.ogryzok.semendestiller.SemenDestillerRegistry;
import com.ogryzok.semenenrichment.SemenEnrichmentRegistry;
import com.ogryzok.separator.SeparatorRegistry;
import com.ogryzok.proteinformer.ProteinFormerRegistry;
import com.ogryzok.blender.BlenderRegistry;
import com.ogryzok.mrnasynthesizer.MRNASynthesizerRegistry;
import com.ogryzok.player.abstinence.CommandAbstinence;
import com.ogryzok.player.abstinence.CommandBiomassBurst;

@Mod(modid = harvestech.MODID, name = harvestech.NAME, version = harvestech.VERSION)
public class harvestech {
	public static final String MODID = "harvestech";
	public static final String NAME = "HarvesTech";
	public static final String VERSION = "1.12.2";

	@Instance(MODID)
	public static harvestech INSTANCE;

	public static final Logger LOGGER = LogManager.getLogger(MODID);

	@EventHandler
	public void preinit(FMLPreInitializationEvent event) {
		LOGGER.info("HarvesTech loading...");

		GeckoLib.initialize();
		LOGGER.info("GeckoLib initialized.");

		ModFluids.register();
		LOGGER.info("Machine fluids registered.");

		SemenCapability.register();
		LOGGER.info("Semen capability registered.");

		ModNetwork.init();
		LOGGER.info("Network initialized.");
		NetworkRegistry.INSTANCE.registerGuiHandler(INSTANCE, new GuiHandlerMain());
		MinecraftForge.EVENT_BUS.register(new SemenEvents());
		MinecraftForge.EVENT_BUS.register(new SemenTickHandler());
		LOGGER.info("Semen storage systems registered.");

		MinecraftForge.EVENT_BUS.register(new MolecularTransformerFix());
		LOGGER.info("Molecular Transformer fix loaded.");

		MinecraftForge.EVENT_BUS.register(new WitherManufacturerFix());
		LOGGER.info("Wither Manufacturer fix loaded.");

		LifeCrusherRegistry.init();
		LOGGER.info("Life Crusher common init loaded.");

		FoodRegistry.init();
		LOGGER.info("Food recipes/smelting registered.");

		DiseaseRegistry.init();
		LOGGER.info("Disease system loaded.");

		ChairRegistry.init();
		LOGGER.info("Chair common init loaded.");

		SemenCentrifugeRegistry.init();
		LOGGER.info("Semen Centrifuge common init loaded.");

		SemenDestillerRegistry.init();
		LOGGER.info("Semen Destiller common init loaded.");

		SemenEnrichmentRegistry.init();
		LOGGER.info("Semen Enrichment Chamber common init loaded.");

		SeparatorRegistry.init();
		LOGGER.info("Separator common init loaded.");

		ProteinFormerRegistry.init();
		LOGGER.info("Protein Former common init loaded.");

		BlenderRegistry.init();
		LOGGER.info("Blender common init loaded.");

		MRNASynthesizerRegistry.init();
		LOGGER.info("mRNA Synthesizer common init loaded.");

		ManualHarvestRegistry.init();
		LOGGER.info("Manual Harvest common init loaded.");

		if (event.getSide().isClient()) {
			LifeCrusherRegistry.initClient();
			LOGGER.info("Life Crusher client renderer registered.");

			ChairRegistry.initClient();
			LOGGER.info("Chair client renderer registered.");

			SemenCentrifugeRegistry.initClient();
			LOGGER.info("Semen Centrifuge client renderer registered.");

			SemenDestillerRegistry.initClient();
			LOGGER.info("Semen Destiller client renderer registered.");

			SemenEnrichmentRegistry.initClient();
			LOGGER.info("Semen Enrichment Chamber client renderer registered.");

			SeparatorRegistry.initClient();
			LOGGER.info("Separator client renderer registered.");

			ProteinFormerRegistry.initClient();
			LOGGER.info("Protein Former client renderer registered.");

			BlenderRegistry.initClient();
			LOGGER.info("Blender client renderer registered.");

			MRNASynthesizerRegistry.initClient();
			LOGGER.info("mRNA Synthesizer client renderer registered.");

			ManualHarvestRegistry.initClient();
			LOGGER.info("Manual Harvest client init loaded.");

			DiseaseRegistry.initClient();
			LOGGER.info("Disease client overlay loaded.");
		}
		if (event.getSide().isClient()) {
			MinecraftForge.EVENT_BUS.register(new SemenHUD());
		}
	}

	@EventHandler
	public void serverStarting(FMLServerStartingEvent event) {
		event.registerServerCommand(new CommandAbstinence());
		event.registerServerCommand(new CommandBiomassBurst());
	}
}
