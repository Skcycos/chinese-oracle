package com.tanrunn.chineseoracle;

import com.sighs.apricityui.resource.Font;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.io.IOException;
import java.io.InputStream;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = ChineseOracleMod.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = ChineseOracleMod.MODID, value = Dist.CLIENT)
public class ChineseOracleModClient {
    // AUI 页面中可用的内置粗体字体族名（思源黑体 Bold，子集化；与股市共用同一字体文件）
    public static final String UI_FONT = "chinese_oracle-ui";

    public ChineseOracleModClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json / zh_cn.json files.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Phase 3: 黄历 GUI, HUD, keybinds go here.
        event.enqueueWork(ChineseOracleModClient::registerUiFont);
        ChineseOracleMod.LOGGER.info("CLIENT SETUP");
    }

    private static void registerUiFont() {
        try (InputStream in = Minecraft.getInstance().getResourceManager()
                .open(ResourceLocation.fromNamespaceAndPath(ChineseOracleMod.MODID, "fonts/noto_sans_bold.otf"))) {
            if (Font.registerFont(UI_FONT, in)) {
                ChineseOracleMod.LOGGER.info("Registered built-in UI font: {}", UI_FONT);
            } else {
                ChineseOracleMod.LOGGER.warn("Failed to register built-in UI font: {}", UI_FONT);
            }
        } catch (IOException e) {
            ChineseOracleMod.LOGGER.error("Failed to load built-in UI font", e);
        }
    }
}
