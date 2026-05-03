package com.solr98.beyondcmdextension.network;

import com.solr98.beyondcmdextension.client.AmmoCountCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AmmoCountResponsePacket {

    private final ResourceLocation ammoId;
    private final int count;

    public AmmoCountResponsePacket(ResourceLocation ammoId, int count) {
        this.ammoId = ammoId;
        this.count = count;
    }

    public static void encode(AmmoCountResponsePacket msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.ammoId);
        buf.writeVarInt(msg.count);
    }

    public static AmmoCountResponsePacket decode(FriendlyByteBuf buf) {
        return new AmmoCountResponsePacket(buf.readResourceLocation(), buf.readVarInt());
    }

    public static void handle(AmmoCountResponsePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                AmmoCountCache.update(msg.ammoId, msg.count)
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
