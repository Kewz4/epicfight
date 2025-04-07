package yesman.epicfight.network.server;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.network.common.AnimatorControlPacket;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class SPAnimatorControl extends AnimatorControlPacket {
	protected int entityId;
	
	public SPAnimatorControl(AnimatorControlPacket.Action action, AssetAccessor<? extends StaticAnimation> animation, float transitionTimeModifier, LivingEntityPatch<?> entitypatch) {
		this(action, animation.get().getId(), entitypatch.getOriginal().getId(), transitionTimeModifier, false);
	}
	
	public SPAnimatorControl(AnimatorControlPacket.Action action, AssetAccessor<? extends StaticAnimation> animation, int entityId, float transitionTimeModifier, boolean pause) {
		this(action, animation.get().getId(), entityId, transitionTimeModifier, pause);
	}
	
	public SPAnimatorControl(AnimatorControlPacket.Action action, int animationId, int entityId, float transitionTimeModifier, boolean pause) {
		super(action, animationId, transitionTimeModifier, pause);
		this.entityId = entityId;
	}
	
	public <T extends SPAnimatorControl> void onArrive() {
		EpicFightCapabilities.getEntityPatchUnparameterized(Minecraft.getInstance().level.getEntity(this.entityId), LivingEntityPatch.class).ifPresent(this::process);
	}
	
	public static SPAnimatorControl fromBytes(FriendlyByteBuf buf) {
		return new SPAnimatorControl(buf.readEnum(Action.class), buf.readInt(), buf.readInt(), buf.readFloat(), buf.readBoolean());
	}
	
	public static void toBytes(SPAnimatorControl msg, FriendlyByteBuf buf) {
		buf.writeEnum(msg.action);
		buf.writeInt(msg.animationId);
		buf.writeInt(msg.entityId);
		buf.writeFloat(msg.transitionTimeModifier);
		buf.writeBoolean(msg.pause);
	}
	
	public static void handle(SPAnimatorControl msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			msg.onArrive();
		});
		
		ctx.get().setPacketHandled(true);
	}
}