package org.minitype.mcmodstest;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record GravityWellPayload() implements CustomPayload {

    public static final Id<GravityWellPayload> ID =
            new Id<>(Identifier.of("unbnd_weapons", "gravity_well"));

    public static final PacketCodec<RegistryByteBuf, GravityWellPayload> CODEC =
            PacketCodec.unit(new GravityWellPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
