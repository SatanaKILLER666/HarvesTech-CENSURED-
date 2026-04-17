1) В главном мод-классе сделай public static harvestech INSTANCE; и поправь аннотацию:
@Mod(modid = harvestech.MODID, name = harvestech.NAME, version = harvestech.VERSION)
public class harvestech {
    @Mod.Instance(MODID)
    public static harvestech INSTANCE;

2) В preInit:
MinecraftForge.EVENT_BUS.register(LifeCrusherRegistry.RegistrationHandler.class);
LifeCrusherRegistry.init();
if (event.getSide().isClient()) {
    LifeCrusherRegistry.clientInit();
}

3) Если хочешь отдельный GUI id, в BlockLifeCrusher openGui поставь не 0, а свой id, и в GuiHandler проверяй ID.
