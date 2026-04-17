1. Скопируй папку src из архива поверх своего mdk.

2. В главном классе мода в preInit добавь ровно это:
   com.ogryzok.lifecrusher.LifeCrusherRegistry.init();
   if (event.getSide().isClient()) {
       com.ogryzok.lifecrusher.LifeCrusherRegistry.clientInit();
   }

3. Ничего про INSTANCE добавлять не нужно.
   Этот пакет сам берёт инстанс мода через Loader.

4. Команда выдачи после сборки:
   /give @p harvestech:life_crusher 1

5. Если предмет не появляется, значит preInit не вызывает LifeCrusherRegistry.init().
