package yesman.epicfight.network.common;

import yesman.epicfight.network.server.SPAnimatorControl;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class AnimatorControlPacket {
	protected Action action;
	protected int animationId;
	protected float transitionTimeModifier;
	protected boolean pause;
	
	public AnimatorControlPacket(Action action, int animationId, float transitionTimeModifier, boolean pause) {
		this.action = action;
		this.animationId = animationId;
		this.transitionTimeModifier = transitionTimeModifier;
		this.pause = pause;
	}
	
	public <T extends SPAnimatorControl> void process(LivingEntityPatch<?> entitypatch) {
		try {
			switch (this.action) {
			case PLAY -> {
				entitypatch.getAnimator().playAnimation(this.animationId, this.transitionTimeModifier);
			}
			case PLAY_INSTANTLY -> {
				entitypatch.getAnimator().playAnimationInstantly(this.animationId);
			}
			case RESERVE -> {
				entitypatch.getAnimator().reserveAnimation(this.animationId);
			}
			case STOP -> {
				entitypatch.getAnimator().stopPlaying(this.animationId);
			}
			case SHOT -> {
				entitypatch.getAnimator().playShootingAnimation();
			}
			case SOFT_PAUSE -> {
				entitypatch.getAnimator().setSoftPause(this.pause);
			}
			case HARD_PAUSE -> {
				entitypatch.getAnimator().setHardPause(this.pause);
			}
			}
		} catch (Exception e) {
			// print out exceptions since any exceptions that occurred in the packet queue won't be printed out
			e.printStackTrace();
		}
	}
	
	public enum Action {
		PLAY, PLAY_INSTANTLY, RESERVE, STOP, SHOT, SOFT_PAUSE, HARD_PAUSE
	}
}
