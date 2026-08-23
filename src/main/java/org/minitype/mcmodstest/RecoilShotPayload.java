package org.minitype.mcmodstest;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RecoilShotPayload() implements CustomPayload {

    public static final Id<RecoilShotPayload> ID =
            new Id<>(Identifier.of("unbnd_weapons", "recoil_shot"));

    public static final PacketCodec<RegistryByteBuf, RecoilShotPayload> CODEC =
            PacketCodec.unit(new RecoilShotPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
