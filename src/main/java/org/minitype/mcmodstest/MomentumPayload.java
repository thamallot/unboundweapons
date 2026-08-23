package org.minitype.mcmodstest;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record MomentumPayload() implements CustomPayload {

    public static final Id<MomentumPayload> ID =
            new Id<>(Identifier.of("unbnd_weapons", "momentum"));

    public static final PacketCodec<RegistryByteBuf, MomentumPayload> CODEC =
            PacketCodec.unit(new MomentumPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
