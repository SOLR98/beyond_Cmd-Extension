package com.solr98.beyondcmdextension.network;

import com.solr98.beyondcmdextension.handler.NetworkAmmoExtractor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestAmmoCountPacket {

    private final ResourceLocation ammoId;

    public RequestAmmoCountPacket(ResourceLocation ammoId) {
        this.ammoId = ammoId;
    }

    public static void encode(RequestAmmoCountPacket msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.ammoId);
    }

    public static RequestAmmoCountPacket decode(FriendlyByteBuf buf) {
        return new RequestAmmoCountPacket(buf.readResourceLocation());
    }

    public static void handle(RequestAmmoCountPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            int count = NetworkAmmoExtractor.countAmmoInNetworkByAmmoId(msg.ammoId, player);
            PacketHandler.sendToPlayer(player, new AmmoCountResponsePacket(msg.ammoId, count));
        });
        ctx.get().setPacketHandled(true);
    }
}
