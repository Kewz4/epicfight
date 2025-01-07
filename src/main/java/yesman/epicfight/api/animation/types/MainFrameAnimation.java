package yesman.epicfight.api.animation.types;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.animation.Layer;
import yesman.epicfight.api.client.animation.property.ClientAnimationProperties;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.entity.eventlistener.ActionEvent;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;

public class MainFrameAnimation extends StaticAnimation {
	public MainFrameAnimation(float convertTime, AnimationAccessor<? extends MainFrameAnimation> accessor, AssetAccessor<? extends Armature> armature) {
		super(convertTime, false, accessor, armature);
	}
	
	public MainFrameAnimation(float convertTime, String path, AssetAccessor<? extends Armature> armature) {
		super(convertTime, false, path, armature);
	}
	
	@Override
	public void begin(LivingEntityPatch<?> entitypatch) {
		super.begin(entitypatch);
		
		entitypatch.updateEntityState();
		
		if (entitypatch.isLogicalClient()) {
			entitypatch.getClientAnimator().resetMotion(false);
			entitypatch.getClientAnimator().resetCompositeMotion(false);
			entitypatch.getClientAnimator().getPlayerFor(this.getAccessor()).setReversed(false);
		}
		
		if (entitypatch instanceof PlayerPatch<?> playerpatch) {
			if (playerpatch.isLogicalClient()) {
				if (playerpatch.getOriginal().isLocalPlayer()) {
					playerpatch.getEventListener().triggerEvents(EventType.ACTION_EVENT_CLIENT, new ActionEvent<>(playerpatch, this.getAccessor()));
				}
			} else {
				playerpatch.getEventListener().triggerEvents(EventType.ACTION_EVENT_SERVER, new ActionEvent<>(playerpatch, this.getAccessor()));
			}
		}
	}
	
	@Override
	public void tick(LivingEntityPatch<?> entitypatch) {
		super.tick(entitypatch);
		entitypatch.getOriginal().walkAnimation.setSpeed(0);
	}
	
	@Override
	public boolean isMainFrameAnimation() {
		return true;
	}
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public Layer.Priority getPriority() {
		return this.getProperty(ClientAnimationProperties.PRIORITY).orElse(Layer.Priority.HIGHEST);
	}
}