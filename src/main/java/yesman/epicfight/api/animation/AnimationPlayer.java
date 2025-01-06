package yesman.epicfight.api.animation;

import com.ibm.icu.impl.Pair;

import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.property.AnimationProperty.PlaybackSpeedModifier;
import yesman.epicfight.api.animation.property.AnimationProperty.PlaybackTimeModifier;
import yesman.epicfight.api.animation.property.AnimationProperty.StaticAnimationProperty;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.config.EpicFightOptions;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class AnimationPlayer {
	protected float elapsedTime;
	protected float prevElapsedTime;
	protected boolean isEnd;
	protected boolean doNotResetTime;
	protected boolean reversed;
	protected AnimationAccessor<? extends DynamicAnimation> play;
	
	public AnimationPlayer() {
		this.setPlayAnimation(Animations.EMPTY_ANIMATION);
	}
	
	public void tick(LivingEntityPatch<?> entitypatch) {
		DynamicAnimation currentPlay = this.getAnimation().get();
		DynamicAnimation currentPlayStatic = currentPlay.getRealAnimation().get();
		
		this.prevElapsedTime = this.elapsedTime;
		
		float playbackSpeed = currentPlay.getPlaySpeed(entitypatch, currentPlay);
		PlaybackSpeedModifier playSpeedModifier = currentPlayStatic.getProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER).orElse(null);
		
		if (playSpeedModifier != null) {
			playbackSpeed = playSpeedModifier.modify(currentPlay, entitypatch, playbackSpeed, this.prevElapsedTime, this.elapsedTime);
		}
		
		this.elapsedTime += EpicFightOptions.A_TICK * playbackSpeed * (this.isReversed() && currentPlay.canBePlayedReverse() ? -1.0F : 1.0F);
		PlaybackTimeModifier playTimeModifier = currentPlayStatic.getProperty(StaticAnimationProperty.ELAPSED_TIME_MODIFIER).orElse(null);
		
		if (playTimeModifier != null) {
			Pair<Float, Float> time = playTimeModifier.modify(currentPlay, entitypatch, playbackSpeed, this.prevElapsedTime, this.elapsedTime);
			this.prevElapsedTime = time.first;
			this.elapsedTime = time.second;
		}
		
		if (this.elapsedTime > currentPlay.getTotalTime()) {
			if (currentPlay.isRepeat()) {
				this.prevElapsedTime = this.prevElapsedTime - currentPlay.getTotalTime();
				this.elapsedTime %= currentPlay.getTotalTime();
			} else {
				this.elapsedTime = currentPlay.getTotalTime();
				currentPlay.end(entitypatch, null, true);
				this.isEnd = true;
			}
		} else if (this.elapsedTime < 0) {
			if (currentPlay.isRepeat()) {
				this.prevElapsedTime = currentPlay.getTotalTime() - this.elapsedTime;
				this.elapsedTime = currentPlay.getTotalTime() + this.elapsedTime;
			} else {
				this.elapsedTime = 0.0F;
				currentPlay.end(entitypatch, null, true);
				this.isEnd = true;
			}
		}
	}
	
	public void reset() {
		this.elapsedTime = 0;
		this.prevElapsedTime = 0;
		this.isEnd = false;
	}
	
	public void setPlayAnimation(AnimationAccessor<? extends DynamicAnimation> animation) {
		if (this.doNotResetTime) {
			this.doNotResetTime = false;
			this.isEnd = false;
		} else {
			this.reset();
		}
		
		this.play = animation;
	}
	
	public Pose getCurrentPose(LivingEntityPatch<?> entitypatch, float partialTicks) {
		return this.play.get().getPoseByTime(entitypatch, this.prevElapsedTime + (this.elapsedTime - this.prevElapsedTime) * partialTicks, partialTicks);
	}
	
	public float getElapsedTime() {
		return this.elapsedTime;
	}
	
	public float getPrevElapsedTime() {
		return this.prevElapsedTime;
	}
	
	public void setElapsedTimeCurrent(float elapsedTime) {
		this.elapsedTime = elapsedTime;
		this.isEnd = false;
	}
	
	public void setElapsedTime(float elapsedTime) {
		this.elapsedTime = elapsedTime;
		this.prevElapsedTime = elapsedTime;
		this.isEnd = false;
	}
	
	public void setElapsedTime(float prevElapsedTime, float elapsedTime) {
		this.elapsedTime = elapsedTime;
		this.prevElapsedTime = prevElapsedTime;
		this.isEnd = false;
	}
	
	public void begin(AnimationAccessor<? extends DynamicAnimation> animation, LivingEntityPatch<?> entitypatch) {
		animation.get().tick(entitypatch);
	}
	
	public AnimationAccessor<? extends DynamicAnimation> getAnimation() {
		return this.play;
	}
	
	public void markDoNotResetTime() {
		this.doNotResetTime = true;
	}

	public boolean isEnd() {
		return this.isEnd;
	}
	
	public void terminate() {
		this.isEnd = true;
	}
	
	public boolean isReversed() {
		return this.reversed;
	}
	
	public void setReversed(boolean reversed) {
		this.reversed = reversed;
	}
	
	public boolean isEmpty() {
		return this.play == Animations.EMPTY_ANIMATION;
	}
	
	@Override
	public String toString() {
		return this.getAnimation() + " " + this.prevElapsedTime + " " + this.elapsedTime;
	}
}