package yesman.epicfight.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.Entity;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.EntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Mixin(value = Entity.class)
public abstract class MixinEntity {
	@Inject(at = @At(value = "HEAD"), method = "setOldPosAndRot()V", cancellable = true)
	private void epicfight_setOldPosAndRot(CallbackInfo callbackInfo) {
		Entity self = (Entity)((Object)this);
		EntityPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(self, EntityPatch.class);
		
		if (entitypatch != null) {
			entitypatch.onOldPosUpdate();
		}
	}
	
	@Inject(at = @At(value = "TAIL"), method = "onAddedToWorld()V", cancellable = true, remap = false)
	private void epicfight_onAddedToWorld(CallbackInfo callbackInfo) {
		Entity self = (Entity)((Object)this);
		EntityPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(self, EntityPatch.class);
		
		if (entitypatch != null) {
			entitypatch.onAddedToWorld();
		}
	}
	
	@Inject(at = @At(value = "HEAD"), method = "lerpMotion(DDD)V", cancellable = true)
	public void lerpMotion(double pX, double pY, double pZ, CallbackInfo callback) {
		Entity e = (Entity)(Object)this;
		
		// Cancel reflecting server delta movement while playing action animation
		EpicFightCapabilities.getUnparameterizedEntityPatch(e, LivingEntityPatch.class).ifPresent(entitypatch -> {
			if (entitypatch.getAnimator().getPlayerFor(null).getRealAnimation().get().isMainFrameAnimation()) {
				callback.cancel();
			}
		});
	}
}