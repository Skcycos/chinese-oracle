package com.tanrunn.chineseoracle.server.registry;

import com.tanrunn.chineseoracle.ChineseOracleMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Items & blocks (design document section 3.6): 竹签筒, 黄历书, 香 (改签消耗品), 求签台.
 */
public final class ModItems {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ChineseOracleMod.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ChineseOracleMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ChineseOracleMod.MODID);

    public static final DeferredBlock<Block> FORTUNE_TABLE = BLOCKS.registerSimpleBlock("fortune_table",
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2f, 3f));
    public static final DeferredItem<BlockItem> FORTUNE_TABLE_ITEM = ITEMS.registerSimpleBlockItem("fortune_table", FORTUNE_TABLE);

    public static final DeferredItem<Item> BAMBOO_SIGN_TUBE = ITEMS.registerSimpleItem("bamboo_sign_tube");
    public static final DeferredItem<Item> ALMANAC = ITEMS.registerSimpleItem("almanac");
    public static final DeferredItem<Item> INCENSE = ITEMS.registerSimpleItem("incense", new Item.Properties().stacksTo(16));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.chinese_oracle"))
            .icon(() -> new ItemStack(BAMBOO_SIGN_TUBE.get()))
            .displayItems((parameters, output) -> {
                output.accept(BAMBOO_SIGN_TUBE.get());
                output.accept(ALMANAC.get());
                output.accept(INCENSE.get());
                output.accept(FORTUNE_TABLE_ITEM.get());
            }).build());

    private ModItems() {
    }
}
