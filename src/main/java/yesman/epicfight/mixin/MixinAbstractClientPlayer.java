package yesman.epicfight.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.main.EpicFightMod;

@Mixin(value = AbstractClientPlayer.class)
public abstract class MixinAbstractClientPlayer {
	@Inject(at = @At(value = "HEAD"), method = "getCloakTextureLocation()Lnet/minecraft/resources/ResourceLocation;", cancellable = true)
	public void getCloakTextureLocation(CallbackInfoReturnable<ResourceLocation> info) {
		if (EpicFightMod.CLIENT_CONFIGS.enableDummyCape.getValue()) {
			info.setReturnValue(new ResourceLocation(EpicFightMod.MODID, "textures/entity/cape.png"));
			info.cancel();
		}
	}
	
	@Inject(at = @At(value = "HEAD"), method = "isCapeLoaded()Z", cancellable = true)
	public void isCapeLoaded(CallbackInfoReturnable<Boolean> info) {
		if (EpicFightMod.CLIENT_CONFIGS.enableDummyCape.getValue()) {
			info.setReturnValue(true);
			info.cancel();
		}
	}
}