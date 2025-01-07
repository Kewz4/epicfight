package yesman.epicfight.api.animation.types;

import java.util.Map;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.animation.AnimationClip;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.JointTransform;
import yesman.epicfight.api.animation.Keyframe;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.TransformSheet;
import yesman.epicfight.api.animation.types.EntityState.StateFactor;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.animation.property.JointMaskEntry;
import yesman.epicfight.api.utils.datastruct.TypeFlexibleHashMap;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class LinkAnimation extends DynamicAnimation implements AnimationAccessor<LinkAnimation> {
	protected AssetAccessor<? extends DynamicAnimation> fromAnimation;
	protected AssetAccessor<? extends StaticAnimation> toAnimation;
	protected float nextStartTime;
	
	public LinkAnimation() {
		this.animationClip = new AnimationClip();
	}
	
	@Override
	public void tick(LivingEntityPatch<?> entitypatch) {
		this.toAnimation.get().linkTick(entitypatch, this);
	}
	
	@Override
	public void end(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends DynamicAnimation> nextAnimation, boolean isEnd) {
		if (!isEnd) {
			this.toAnimation.get().end(entitypatch, nextAnimation, isEnd);
		} else {
			if (this.nextStartTime > 0.0F) {
				entitypatch.getAnimator().getPlayerFor(this).setElapsedTime(this.nextStartTime);
				entitypatch.getAnimator().getPlayerFor(this).markDoNotResetTime();
			}
		}
	}
	
	@Override
	public TypeFlexibleHashMap<StateFactor<?>> getStatesMap(LivingEntityPatch<?> entitypatch, float time) {
		TypeFlexibleHashMap<StateFactor<?>> map = this.toAnimation.get().getStatesMap(entitypatch, Math.max(this.nextStartTime + (time - this.getTotalTime()), this.nextStartTime));
		
		for (Map.Entry<StateFactor<?>, Object> entry : map.entrySet()) {
			Object val = this.toAnimation.get().getModifiedLinkState(entry.getKey(), entry.getValue(), entitypatch, time);
			map.put(entry.getKey(), val);
		}
		
		return map;
	}
	
	@Override
	public EntityState getState(LivingEntityPatch<?> entitypatch, float time) {
		EntityState state = this.toAnimation.get().getState(entitypatch, Math.max(this.nextStartTime + time - this.getTotalTime(), this.nextStartTime));
		TypeFlexibleHashMap<StateFactor<?>> map = state.getStateMap();
		
		for (Map.Entry<StateFactor<?>, Object> entry : map.entrySet()) {
			Object val = this.toAnimation.get().getModifiedLinkState(entry.getKey(), entry.getValue(), entitypatch, time);
			map.put(entry.getKey(), val);
		}
		
		return state;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getState(StateFactor<T> stateFactor, LivingEntityPatch<?> entitypatch, float time) {
		T state = this.toAnimation.get().getState(stateFactor, entitypatch, Math.max(this.nextStartTime + time - this.getTotalTime(), this.nextStartTime));
		
		return (T)this.toAnimation.get().getModifiedLinkState(stateFactor, state, entitypatch, time);
	}
	
	@Override
	public Pose getPoseByTime(LivingEntityPatch<?> entitypatch, float time, float partialTicks) {
		Pose nextStartingPose = this.toAnimation.get().getPoseByTime(entitypatch, this.nextStartTime, 1.0F);
		
		/**
		 * Update dest pose
		 */
		for (Map.Entry<String, JointTransform> entry : nextStartingPose.getJointTransformData().entrySet()) {
			if (this.animationClip.hasJointTransform(entry.getKey())) {
				Keyframe[] keyframe = this.animationClip.getJointTransform(entry.getKey()).getKeyframes();
				JointTransform jt = keyframe[keyframe.length - 1].transform();
				JointTransform newJt = nextStartingPose.getJointTransformData().get(entry.getKey());
				newJt.translation().set(jt.translation());
				jt.copyFrom(newJt);
			}
		}
		
		return super.getPoseByTime(entitypatch, time, partialTicks);
	}
	
	@Override
	public void modifyPose(DynamicAnimation animation, Pose pose, LivingEntityPatch<?> entitypatch, float time, float partialTicks) {
		// Bad implementation: Add root joint as coord in loading animation
		if (this.toAnimation instanceof ActionAnimation actionAnimation) {
			if (!this.getTransfroms().containsKey("Coord")) {
				actionAnimation.correctRootJoint(this, pose, entitypatch, time, partialTicks);
			}
		}
	}
	
	@Override
	public float getPlaySpeed(LivingEntityPatch<?> entitypatch, DynamicAnimation animation) {
		return this.toAnimation.get().getPlaySpeed(entitypatch, animation);
	}
	
	public void setConnectedAnimations(AssetAccessor<? extends DynamicAnimation> from, AssetAccessor<? extends StaticAnimation> to) {
		this.fromAnimation = from.get().getRealAnimation();
		this.toAnimation = to;
	}
	
	public AssetAccessor<? extends StaticAnimation> getNextAnimation() {
		return this.toAnimation;
	}
	
	@Override
	public TransformSheet getCoord() {
		if (this.getTransfroms().containsKey("Coord")) {
			return this.getTransfroms().get("Coord");
		} else if (this.getTransfroms().containsKey("Root")) {
			return this.getTransfroms().get("Root");
		}
		
		return TransformSheet.EMPTY_SHEET;
	}
	
	@OnlyIn(Dist.CLIENT)
	public Optional<JointMaskEntry> getJointMaskEntry(LivingEntityPatch<?> entitypatch, boolean useCurrentMotion) {
		return useCurrentMotion ? this.toAnimation.get().getJointMaskEntry(entitypatch, true) : this.fromAnimation.get().getJointMaskEntry(entitypatch, false);
	}
	
	@Override
	public boolean isMainFrameAnimation() {
		return this.toAnimation.get().isMainFrameAnimation();
	}
	
	@Override
	public boolean isReboundAnimation() {
		return this.toAnimation.get().isReboundAnimation();
	}
	
	@Override
	public boolean doesHeadRotFollowEntityHead() {
		return this.fromAnimation.get().doesHeadRotFollowEntityHead() && this.toAnimation.get().doesHeadRotFollowEntityHead();
	}
	
	@Override
	public AssetAccessor<? extends StaticAnimation> getRealAnimation() {
		return this.toAnimation;
	}
		
	public AssetAccessor<? extends DynamicAnimation> getFromAnimation() {
		return this.fromAnimation;
	} 
	
	@Override
	public AnimationAccessor<? extends DynamicAnimation> getAccessor() {
		return this;
	}
	
	public void copyTo(LinkAnimation dest) {
		dest.setConnectedAnimations(this.fromAnimation, this.toAnimation);
		dest.setTotalTime(this.getTotalTime());
		
		Map<String, TransformSheet> trnasforms = dest.getTransfroms();
		trnasforms.clear();
		trnasforms.putAll(this.getTransfroms());
	}
	
	public float getNextStartTime() {
		return this.nextStartTime;
	}
	
	public void setNextStartTime(float nextStartTime) {
		this.nextStartTime = nextStartTime;
	}
	
	public void resetNextStartTime() {
		this.nextStartTime = 0.0F;
	}
	
	@Override
	public boolean isLinkAnimation() {
		return true;
	}
	
	@Override
	public String toString() {
		return "From " + this.fromAnimation + " to " + this.toAnimation;
	}

	@Override
	public AnimationClip getAnimationClip() {
		return this.animationClip;
	}
	
	@Override
	public LinkAnimation get() {
		return this;
	}

	@Override
	public ResourceLocation registryName() {
		return null;
	}

	@Override
	public boolean isPresent() {
		return true;
	}

	@Override
	public int id() {
		return -1;
	}
}