package yesman.epicfight.api.animation.types;

import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.config.EpicFightOptions;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class MovementAnimation extends StaticAnimation {
	public MovementAnimation(boolean isRepeat, AnimationAccessor<? extends MovementAnimation> accessor, Armature armature) {
		super(EpicFightOptions.GENERAL_ANIMATION_TRANSITION_TIME, isRepeat, accessor, armature);
	}
	
	public MovementAnimation(float convertTime, boolean isRepeat, AnimationAccessor<? extends MovementAnimation> accessor, Armature armature) {
		super(convertTime, isRepeat, accessor, armature);
	}
	
	/**
	 * For internal use
	 */
	public MovementAnimation(float convertTime, boolean isRepeat, String path, Armature armature) {
		super(convertTime, isRepeat, path, armature);
	}
	
	@Override
	public float getPlaySpeed(LivingEntityPatch<?> entitypatch, DynamicAnimation animation) {
		if (animation.isLinkAnimation()) {
			return 1.0F;
		}
		
		float movementSpeed = 1.0F;
		
		if (Math.abs(entitypatch.getOriginal().walkAnimation.speed() - entitypatch.getOriginal().walkAnimation.speed(1)) < 0.007F) {
			movementSpeed *= (entitypatch.getOriginal().walkAnimation.speed() * 1.16F);
		}
		
		return movementSpeed;
	}
	
	@Override
	public boolean canBePlayedReverse() {
		return true;
	}
}