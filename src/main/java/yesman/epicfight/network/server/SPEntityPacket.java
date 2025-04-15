package yesman.epicfight.network.server;

import java.util.function.Supplier;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.EntityPatch;

public class SPEntityPacket {
	private final int entityId;
	private final FriendlyByteBuf buffer;
	
	public SPEntityPacket() {
		this.entityId = 0;
		this.buffer = new FriendlyByteBuf(Unpooled.buffer());
	}
	
	public SPEntityPacket(int entityId) {
		this.entityId = entityId;
		this.buffer = new FriendlyByteBuf(Unpooled.buffer());
	}
	
	public FriendlyByteBuf getBuffer() {
		return this.buffer;
	}
	
	public static SPEntityPacket fromBytes(FriendlyByteBuf buf) {
		SPEntityPacket msg = new SPEntityPacket(buf.readInt());

		while (buf.isReadable()) {
			msg.buffer.writeByte(buf.readByte());
		}

		return msg;
	}
	
	public static void toBytes(SPEntityPacket msg, FriendlyByteBuf buf) {
		buf.writeInt(msg.entityId);

		while (msg.buffer.isReadable()) {
			buf.writeByte(msg.buffer.readByte());
		}
	}
	
	public static void handle(SPEntityPacket msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			Minecraft mc = Minecraft.getInstance();
			Entity entity = mc.player.level().getEntity(msg.entityId);
			
			if (entity != null) {
				EntityPatch<?> entitypatch = entity.getCapability(EpicFightCapabilities.CAPABILITY_ENTITY).orElse(null);
				
				if (entitypatch != null) {
					entitypatch.processEntityPacket(msg.getBuffer());
				}
			}
		});
		
		ctx.get().setPacketHandled(true);
	}
}