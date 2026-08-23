package org.minitype.mcmodstest;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record VolleyArmPayload() implements CustomPayload {
    public static final Id<VolleyArmPayload> ID =
            new Id<>(Identifier.of("unbnd_weapons", "volley_arm"));
    public static final PacketCodec<RegistryByteBuf, VolleyArmPayload> CODEC =
            PacketCodec.unit(new VolleyArmPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
