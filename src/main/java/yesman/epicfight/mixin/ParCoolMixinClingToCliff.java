package yesman.epicfight.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.alrex.parcool.common.action.impl.ClingToCliff;
import com.alrex.parcool.common.capability.Parkourability;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

@Mixin(value = ClingToCliff.class)
public class ParCoolMixinClingToCliff {
	@Inject(at = @At(value = "HEAD"), method = "onRenderTick(Lnet/minecraft/world/entity/player/Player;Lcom/alrex/parcool/common/capability/Parkourability;)V", cancellable = true, remap = false)
	public void epicfight_onRenderTick(TickEvent.RenderTickEvent event, Player player, Parkourability parkourability, CallbackInfo callback) {
		PlayerPatch<?> playerpatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		
		if (playerpatch != null && playerpatch.isBattleMode()) {
			callback.cancel();
		}
	}
}