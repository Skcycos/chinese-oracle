package com.tanrunn.chineseoracle.common.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client → server handshake sent on world join. Tells the server whether the
 * client has ApricityUI installed, so it can skip the chat display and push
 * the fortune screen payload instead.
 */
public record AuiHandshakeC2S(boolean hasAui) implements CustomPacketPayload {

    public static final Type<AuiHandshakeC2S> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("chinese_oracle", "aui_handshake"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AuiHandshakeC2S> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, AuiHandshakeC2S::hasAui,
            AuiHandshakeC2S::new);

    @Override
    public Type<AuiHandshakeC2S> type() {
        return TYPE;
    }
}
