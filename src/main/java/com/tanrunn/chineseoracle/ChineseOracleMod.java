package com.tanrunn.chineseoracle;

import com.mojang.logging.LogUtils;
import com.tanrunn.chineseoracle.command.OracleCommand;
import com.tanrunn.chineseoracle.network.ModPayloads;
import com.tanrunn.chineseoracle.server.fortune.FortuneData;
import com.tanrunn.chineseoracle.server.hook.FortuneHooks;
import com.tanrunn.chineseoracle.server.hook.ItemHooks;
import com.tanrunn.chineseoracle.server.registry.FortuneReloadListener;
import com.tanrunn.chineseoracle.server.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(ChineseOracleMod.MODID)
public class ChineseOracleMod {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "chinese_oracle";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);

    // Per-player persistent fortune state (section 5.1), stored with the player dat.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<FortuneData>> FORTUNE_DATA =
            ATTACHMENT_TYPES.<AttachmentType<FortuneData>>register("fortune_data",
                    () -> AttachmentType.serializable(FortuneData::new).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public ChineseOracleMod(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Network payloads (server → client fortune sync).
        modEventBus.addListener(ModPayloads::register);

        ATTACHMENT_TYPES.register(modEventBus);
        ModItems.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModItems.TABS.register(modEventBus);

        // Register ourselves and the event hooks for server and other game events.
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(FortuneHooks.class);
        NeoForge.EVENT_BUS.register(ItemHooks.class);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("{} initialized", MODID);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        OracleCommand.register(event.getServer().getCommands().getDispatcher());
    }

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new FortuneReloadListener());
    }
}
