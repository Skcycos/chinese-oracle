package com.tanrunn.chineseoracle.common.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → client payload carrying a display-ready fortune (design document
 * section 7.2, full-text sync). Sent when a player opens the 黄历 via item or
 * command; the client opens an ApricityUI screen when the mod is present.
 */
public record FortuneDisplay(
        long dayIndex,
        String tierName,
        int tierRank,
        List<String> yiNames,
        List<String> jiNames,
        @Nullable String poem,
        @Nullable String explain,
        @Nullable String wuxing,
        @Nullable String solarTerm,
        String shichen,
        boolean shichenAuspicious,
        @Nullable String festival) implements CustomPacketPayload {

    public static final Type<FortuneDisplay> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("chinese_oracle", "fortune_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FortuneDisplay> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public FortuneDisplay decode(RegistryFriendlyByteBuf buf) {
            return new FortuneDisplay(
                    buf.readVarLong(),
                    buf.readUtf(256),
                    buf.readVarInt(),
                    readStrings(buf),
                    readStrings(buf),
                    readOptionalString(buf),
                    readOptionalString(buf),
                    readOptionalString(buf),
                    readOptionalString(buf),
                    buf.readUtf(16),
                    buf.readBoolean(),
                    readOptionalString(buf));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, FortuneDisplay value) {
            buf.writeVarLong(value.dayIndex());
            buf.writeUtf(value.tierName(), 256);
            buf.writeVarInt(value.tierRank());
            writeStrings(buf, value.yiNames());
            writeStrings(buf, value.jiNames());
            writeOptionalString(buf, value.poem());
            writeOptionalString(buf, value.explain());
            writeOptionalString(buf, value.wuxing());
            writeOptionalString(buf, value.solarTerm());
            buf.writeUtf(value.shichen(), 16);
            buf.writeBoolean(value.shichenAuspicious());
            writeOptionalString(buf, value.festival());
        }
    };

    private static List<String> readStrings(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<String> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(buf.readUtf(64));
        }
        return list;
    }

    private static void writeStrings(RegistryFriendlyByteBuf buf, List<String> values) {
        buf.writeVarInt(values.size());
        values.forEach(s -> buf.writeUtf(s, 64));
    }

    private static String readOptionalString(RegistryFriendlyByteBuf buf) {
        return buf.readBoolean() ? buf.readUtf(512) : null;
    }

    private static void writeOptionalString(RegistryFriendlyByteBuf buf, @Nullable String value) {
        buf.writeBoolean(value != null);
        if (value != null) {
            buf.writeUtf(value, 512);
        }
    }

    @Override
    public Type<FortuneDisplay> type() {
        return TYPE;
    }
}
