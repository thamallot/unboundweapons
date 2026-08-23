package org.minitype.mcmodstest;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record VolleyCancelPayload() implements CustomPayload {
    public static final Id<VolleyCancelPayload> ID =
            new Id<>(Identifier.of("unbnd_weapons", "volley_cancel"));
    public static final PacketCodec<RegistryByteBuf, VolleyCancelPayload> CODEC =
            PacketCodec.unit(new VolleyCancelPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
