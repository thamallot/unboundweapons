package org.minitype.mcmodstest;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record MeteorDropPayload() implements CustomPayload {

    public static final Id<MeteorDropPayload> ID =
            new Id<>(Identifier.of("unbnd_weapons", "meteor_drop"));

    public static final PacketCodec<RegistryByteBuf, MeteorDropPayload> CODEC =
            PacketCodec.unit(new MeteorDropPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
