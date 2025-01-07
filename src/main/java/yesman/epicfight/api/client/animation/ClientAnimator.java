package yesman.epicfight.api.client.animation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.ServerAnimator;
import yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.animation.types.EntityState.StateFactor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.animation.Layer.Priority;
import yesman.epicfight.api.client.animation.property.ClientAnimationProperties;
import yesman.epicfight.api.client.animation.property.JointMask.BindModifier;
import yesman.epicfight.api.client.animation.property.JointMask.JointMaskSet;
import yesman.epicfight.api.client.animation.property.JointMaskEntry;
import yesman.epicfight.api.utils.datastruct.TypeFlexibleHashMap;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@OnlyIn(Dist.CLIENT)
public class ClientAnimator extends Animator {
	public static Animator getAnimator(LivingEntityPatch<?> entitypatch) {
		return entitypatch.isLogicalClient() ? new ClientAnimator(entitypatch) : ServerAnimator.getAnimator(entitypatch);
	}
	
	private final Map<LivingMotion, AssetAccessor<? extends StaticAnimation>> compositeLivingAnimations;
	private final Map<LivingMotion, AssetAccessor<? extends StaticAnimation>> defaultLivingAnimations;
	private final Map<LivingMotion, AssetAccessor<? extends StaticAnimation>> defaultCompositeLivingAnimations;
	public final Layer.BaseLayer baseLayer;
	private LivingMotion currentMotion;
	private LivingMotion currentCompositeMotion;
	private boolean hardPaused;
	
	public ClientAnimator(LivingEntityPatch<?> entitypatch) {
		this(entitypatch, Layer.BaseLayer::new);
	}
	
	public ClientAnimator(LivingEntityPatch<?> entitypatch, Supplier<Layer.BaseLayer> layerSupplier) {
		super(entitypatch);
		
		this.currentMotion = LivingMotions.IDLE;
		this.currentCompositeMotion = LivingMotions.IDLE;
		this.compositeLivingAnimations = Maps.newHashMap();
		this.defaultLivingAnimations = Maps.newHashMap();
		this.defaultCompositeLivingAnimations = Maps.newHashMap();
		this.baseLayer = layerSupplier.get();
	}
	
	/** Play an animation by animation instance **/
	@Override
	public void playAnimation(AssetAccessor<? extends StaticAnimation> nextAnimation, float transitionTimeModifier) {
		Layer layer = nextAnimation.get().getLayerType() == Layer.LayerType.BASE_LAYER ? this.baseLayer : this.baseLayer.compositeLayers.get(nextAnimation.get().getPriority());
		layer.paused = false;
		layer.playAnimation(nextAnimation, this.entitypatch, transitionTimeModifier);
	}
	
	@Override
	public void playAnimationInstantly(AssetAccessor<? extends StaticAnimation> nextAnimation) {
		Layer layer = nextAnimation.get().getLayerType() == Layer.LayerType.BASE_LAYER ? this.baseLayer : this.baseLayer.compositeLayers.get(nextAnimation.get().getPriority());
		layer.paused  = false;
		layer.playAnimationInstantly(nextAnimation, this.entitypatch);
	}
	
	@Override
	public void reserveAnimation(AssetAccessor<? extends StaticAnimation> nextAnimation) {
		Layer layer = nextAnimation.get().getLayerType() == Layer.LayerType.BASE_LAYER ? this.baseLayer : this.baseLayer.compositeLayers.get(nextAnimation.get().getPriority());
		
		if (nextAnimation.get().getPriority().isHigherThan(layer.animationPlayer.getAnimation().get().getRealAnimation().get().getPriority())) {
			if (!layer.animationPlayer.isEnd() && layer.animationPlayer.getAnimation() != null) {
				layer.animationPlayer.getAnimation().get().end(this.entitypatch, nextAnimation, false);
			}
			
			layer.animationPlayer.terminate();
		}
		
		layer.nextAnimation = nextAnimation;
		layer.paused = false;
	}
	
	@Override
	public void addLivingAnimation(LivingMotion livingMotion, AssetAccessor<? extends StaticAnimation> animation) {
		Layer.LayerType layerType = animation.get().getLayerType();
		boolean isBaseLayer = (layerType == Layer.LayerType.BASE_LAYER);
		
		Map<LivingMotion, AssetAccessor<? extends StaticAnimation>> storage = layerType == Layer.LayerType.BASE_LAYER ? this.livingAnimations : this.compositeLivingAnimations;
		LivingMotion compareMotion = layerType == Layer.LayerType.BASE_LAYER ? this.currentMotion : this.currentCompositeMotion;
		Layer layer = layerType == Layer.LayerType.BASE_LAYER ? this.baseLayer : this.baseLayer.compositeLayers.get(animation.get().getPriority());
		storage.put(livingMotion, animation);
		
		if (livingMotion == compareMotion) {
			EntityState state = this.getEntityState();
			
			if (!state.inaction()) {
				layer.playLivingAnimation(animation, this.entitypatch);
			}
		}
		
		if (isBaseLayer) {
			animation.get().getProperty(ClientAnimationProperties.MULTILAYER_ANIMATION).ifPresent(multilayerAnimation -> {
				this.compositeLivingAnimations.put(livingMotion, multilayerAnimation);
				
				if (livingMotion == this.currentCompositeMotion) {
					EntityState state = getEntityState();
					
					if (!state.inaction()) {
						layer.playLivingAnimation(multilayerAnimation, this.entitypatch);
					}
				}
			});
		}
	}
	
	public void setCurrentMotionsAsDefault() {
		this.defaultLivingAnimations.putAll(this.livingAnimations);
		this.defaultCompositeLivingAnimations.putAll(this.compositeLivingAnimations);
	}
	
	@Override
	public void resetLivingAnimations() {
		super.resetLivingAnimations();
		this.compositeLivingAnimations.clear();
		this.defaultLivingAnimations.forEach((key, val) -> this.addLivingAnimation(key, val));
		this.defaultCompositeLivingAnimations.forEach((key, val) -> this.addLivingAnimation(key, val));
	}
	
	public AssetAccessor<? extends StaticAnimation> getLivingMotion(LivingMotion motion) {
		return this.livingAnimations.getOrDefault(motion, this.livingAnimations.get(LivingMotions.IDLE));
	}
	
	public AssetAccessor<? extends StaticAnimation> getCompositeLivingMotion(LivingMotion motion) {
		return this.compositeLivingAnimations.get(motion);
	}
	
	@Override
	public void postInit() {
		super.postInit();
		
		this.setCurrentMotionsAsDefault();
		
		AssetAccessor<? extends StaticAnimation> idleMotion = this.livingAnimations.get(this.currentMotion);
		this.baseLayer.playAnimationInstantly(idleMotion, this.entitypatch);
	}
	
	@Override
	public void tick() {
		// Layer debugging
		/**
		for (Layer layer : this.getAllLayers()) {
			System.out.println(layer);
		}
		System.out.println();
		**/
		if (this.hardPaused) {
			return;
		}
		
		this.baseLayer.update(this.entitypatch);
		
		if (this.baseLayer.animationPlayer.isEnd() && this.baseLayer.nextAnimation == null && this.currentMotion != LivingMotions.DEATH) {
			this.entitypatch.updateMotion(false);
			
			if (this.compositeLivingAnimations.containsKey(this.entitypatch.currentCompositeMotion)) {
				this.playAnimation(this.getCompositeLivingMotion(this.entitypatch.currentCompositeMotion), 0.0F);
			}
			
			this.baseLayer.playAnimation(this.getLivingMotion(this.entitypatch.currentLivingMotion), this.entitypatch, 0.0F);
		} else {
			if (!this.compareCompositeMotion(this.entitypatch.currentCompositeMotion)) {
				/* Turns off the multilayer of the base layer */
				this.getLivingMotion(this.currentCompositeMotion).get().getProperty(ClientAnimationProperties.MULTILAYER_ANIMATION).ifPresent((multilayerAnimation) -> {
					if (!this.compositeLivingAnimations.containsKey(this.entitypatch.currentCompositeMotion)) {
						this.getCompositeLayer(multilayerAnimation.get().getPriority()).off(this.entitypatch);
					}
				});
				
				if (this.compositeLivingAnimations.containsKey(this.currentCompositeMotion)) {
					AssetAccessor<? extends StaticAnimation> nextLivingAnimation = this.getCompositeLivingMotion(this.entitypatch.currentCompositeMotion);
					
					if (nextLivingAnimation == null || nextLivingAnimation.get().getPriority() != this.getCompositeLivingMotion(this.currentCompositeMotion).get().getPriority()) {
						this.getCompositeLayer(this.getCompositeLivingMotion(this.currentCompositeMotion).get().getPriority()).off(this.entitypatch);
					}
				}
				
				if (this.compositeLivingAnimations.containsKey(this.entitypatch.currentCompositeMotion)) {
					this.playAnimation(this.getCompositeLivingMotion(this.entitypatch.currentCompositeMotion), 0.0F);
				}
			}
			
			if (!this.compareMotion(this.entitypatch.currentLivingMotion) && this.entitypatch.currentLivingMotion != LivingMotions.DEATH) {
				if (this.livingAnimations.containsKey(this.entitypatch.currentLivingMotion)) {
					this.baseLayer.playAnimation(this.getLivingMotion(this.entitypatch.currentLivingMotion), this.entitypatch, 0.0F);
				}
			}
		}
		
		this.currentMotion = this.entitypatch.currentLivingMotion;
		this.currentCompositeMotion = this.entitypatch.currentCompositeMotion;
	}
	
	@Override
	public void playDeathAnimation() {
		if (!this.getPlayerFor(null).getAnimation().get().getProperty(ActionAnimationProperty.IS_DEATH_ANIMATION).orElse(false)) {
			this.playAnimation(this.livingAnimations.getOrDefault(LivingMotions.DEATH, Animations.EMPTY_ANIMATION), 0.0F);
			this.currentMotion = LivingMotions.DEATH;
		}
	}
	
	public AssetAccessor<? extends StaticAnimation> getJumpAnimation() {
		return this.livingAnimations.get(LivingMotions.JUMP);
	}
	
	public Layer getCompositeLayer(Layer.Priority priority) {
		return this.baseLayer.compositeLayers.get(priority);
	}
	
	public Collection<Layer> getAllLayers() {
		List<Layer> layerList = Lists.newArrayList();
		layerList.add(this.baseLayer);
		layerList.addAll(this.baseLayer.compositeLayers.values());
		
		return layerList;
	}
	
	@Override
	public Pose getPose(float partialTicks) {
		return this.getPose(partialTicks, true);
	}
	
	public Pose getPose(float partialTicks, boolean useCurrentMotion) {
		Pose composedPose = new Pose();
		Pose baseLayerPose = this.baseLayer.getEnabledPose(this.entitypatch, useCurrentMotion, partialTicks);
		
		Map<Layer.Priority, Pair<AssetAccessor<? extends DynamicAnimation>, Pose>> layerPoses = Maps.newLinkedHashMap();
		composedPose.putJointData(baseLayerPose);
		
		for (Layer.Priority priority : this.baseLayer.baseLayerPriority.uppers()) {
			Layer compositeLayer = this.baseLayer.compositeLayers.get(priority);
			
			if (priority == Layer.Priority.LOWEST && this.baseLayer.animationPlayer.getAnimation().get().isMainFrameAnimation()) {
				continue;
			}
			
			if (!compositeLayer.isDisabled() && !compositeLayer.animationPlayer.isEmpty()) {
				Pose layerPose = compositeLayer.getEnabledPose(this.entitypatch, useCurrentMotion, partialTicks);
				layerPoses.put(priority, Pair.of(compositeLayer.animationPlayer.getAnimation(), layerPose));
				composedPose.putJointData(layerPose);
			}
		}
		
		Joint rootJoint = this.entitypatch.getArmature().rootJoint;
		this.applyBindModifier(baseLayerPose, composedPose, rootJoint, layerPoses, useCurrentMotion);
		
		return composedPose;
	}
	
	public Pose getComposedLayerPoseBelow(Layer.Priority priorityLimit, float partialTicks) {
		Pose composedPose = this.baseLayer.getEnabledPose(this.entitypatch, true, partialTicks);
		Pose baseLayerPose = this.baseLayer.getEnabledPose(this.entitypatch, true, partialTicks);
		Map<Layer.Priority, Pair<AssetAccessor<? extends DynamicAnimation>, Pose>> layerPoses = Maps.newLinkedHashMap();
		
		for (Layer.Priority priority : priorityLimit.lowers()) {
			Layer compositeLayer = this.baseLayer.compositeLayers.get(priority);
			
			if (priority == Layer.Priority.LOWEST && this.baseLayer.animationPlayer.getAnimation().get().isMainFrameAnimation()) {
				continue;
			}
			
			if (!compositeLayer.isDisabled()) {
				Pose layerPose = compositeLayer.getEnabledPose(this.entitypatch, true, partialTicks);
				layerPoses.put(priority, Pair.of(compositeLayer.animationPlayer.getAnimation(), layerPose));
				composedPose.putJointData(layerPose);
			}
		}
		
		if (!layerPoses.isEmpty()) {
			this.applyBindModifier(baseLayerPose, composedPose, this.entitypatch.getArmature().rootJoint, layerPoses, true);
		}
		
		return composedPose;
	}
	
	public void applyBindModifier(Pose basePose, Pose result, Joint joint, Map<Layer.Priority, Pair<AssetAccessor<? extends DynamicAnimation>, Pose>> poses, boolean useCurrentMotion) {
		List<Priority> list = Lists.newArrayList(poses.keySet());
		Collections.reverse(list);
		
		for (Layer.Priority priority : list) {
			AssetAccessor<? extends DynamicAnimation> nowPlaying = poses.get(priority).getFirst();
			JointMaskEntry jointMaskEntry = nowPlaying.get().getJointMaskEntry(this.entitypatch, useCurrentMotion).orElse(null);
			
			if (jointMaskEntry != null) {
				LivingMotion livingMotion = this.getCompositeLayer(priority).getLivingMotion(this.entitypatch, useCurrentMotion);
				
				if (nowPlaying.get().hasTransformFor(joint.getName()) && !jointMaskEntry.isMasked(livingMotion, joint.getName())) {
					JointMaskSet set = jointMaskEntry.getMask(livingMotion);
					BindModifier bindModifier = set.getBindModifier(joint.getName());
					
					if (bindModifier != null) {
						bindModifier.modify(this.entitypatch, basePose, result, livingMotion, jointMaskEntry, priority, joint, poses);
						break;
					}
				}
			}
		}
		
		for (Joint subJoints : joint.getSubJoints()) {
			this.applyBindModifier(basePose, result, subJoints, poses, useCurrentMotion);
		}
	}
	
	public boolean compareMotion(LivingMotion motion) {
		return this.currentMotion.isSame(motion);
	}
	
	public boolean compareCompositeMotion(LivingMotion motion) {
		return this.currentCompositeMotion.isSame(motion);
	}
	
	public void resetMotion(boolean playLivingMotion) {
		this.entitypatch.updateMotion(false);
		this.currentMotion = this.entitypatch.currentLivingMotion;
		
		if (playLivingMotion && this.livingAnimations.containsKey(this.entitypatch.currentLivingMotion)) {
			this.baseLayer.playAnimation(this.getLivingMotion(this.entitypatch.currentLivingMotion), this.entitypatch, 0.0F);
		}
	}
	
	public void resetCompositeMotion(boolean playIdleMotion) {
		if (playIdleMotion && this.compositeLivingAnimations.containsKey(LivingMotions.IDLE)) {
			AssetAccessor<? extends StaticAnimation> nextLivingAnimation = this.getCompositeLivingMotion(LivingMotions.IDLE);
			AssetAccessor<? extends StaticAnimation> currentLivingMotion = this.getCompositeLivingMotion(this.currentCompositeMotion);
			
			if (nextLivingAnimation == null || (currentLivingMotion != null && nextLivingAnimation.get().getPriority() != currentLivingMotion.get().getPriority())) {
				this.getCompositeLayer(this.getCompositeLivingMotion(this.currentCompositeMotion).get().getPriority()).off(this.entitypatch);
			}
			
			this.playAnimation(this.getCompositeLivingMotion(LivingMotions.IDLE), 0.0F);
		}
		
		this.currentCompositeMotion = LivingMotions.NONE;
		this.entitypatch.currentCompositeMotion = LivingMotions.NONE;
	}
	
	public void offAllLayers() {
		for (Layer layer : this.baseLayer.compositeLayers.values()) {
			layer.off(this.entitypatch);
		}
	}
	
	public boolean isAiming() {
		return this.currentCompositeMotion == LivingMotions.AIM;
	}
	
	public void playReboundAnimation() {
		if (this.compositeLivingAnimations.containsKey(LivingMotions.SHOT)) {
			this.playAnimation(this.compositeLivingAnimations.get(LivingMotions.SHOT), 0.0F);
			this.entitypatch.currentCompositeMotion = LivingMotions.NONE;
			this.resetCompositeMotion(false);
		}
	}
	
	@Override
	public AnimationPlayer getPlayerFor(AssetAccessor<? extends DynamicAnimation> playingAnimation) {
		for (Layer layer : this.baseLayer.compositeLayers.values()) {
			if (layer.animationPlayer.getAnimation().get().getRealAnimation().equals(playingAnimation)) {
				return layer.animationPlayer;
			}
		}
		
		return this.baseLayer.animationPlayer;
	}
	
	public Layer.Priority getPriorityFor(AssetAccessor<? extends DynamicAnimation> playingAnimation) {
		for (Layer layer : this.baseLayer.compositeLayers.values()) {
			if (layer.animationPlayer.getAnimation().get().getRealAnimation().equals(playingAnimation)) {
				return layer.priority;
			}
		}
		
		return this.baseLayer.priority;
	}
	
	public LivingMotion currentMotion() {
		return this.currentMotion;
	}
	
	public LivingMotion currentCompositeMotion() {
		return this.currentCompositeMotion;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> Pair<AnimationPlayer, T> findFor(Class<T> animationType) {
		for (Layer layer : this.baseLayer.compositeLayers.values()) {
			if (animationType.isAssignableFrom(layer.animationPlayer.getAnimation().getClass())) {
				return Pair.of(layer.animationPlayer, (T)layer.animationPlayer.getAnimation());
			}
		}
		
		return animationType.isAssignableFrom(this.baseLayer.animationPlayer.getAnimation().getClass()) ? Pair.of(this.baseLayer.animationPlayer, (T)this.baseLayer.animationPlayer.getAnimation()) : null;
	}
	
	public LivingEntityPatch<?> getOwner() {
		return this.entitypatch;
	}
	
	@Override
	public EntityState getEntityState() {
		TypeFlexibleHashMap<StateFactor<?>> stateMap = new TypeFlexibleHashMap<> (false);
		
		for (Layer layer : this.baseLayer.compositeLayers.values()) {
			if (!layer.disabled) {
				stateMap.putAll(layer.animationPlayer.getAnimation().get().getStatesMap(this.entitypatch, layer.animationPlayer.getElapsedTime()));
			}
			
			if (layer.priority == this.baseLayer.baseLayerPriority) {
				stateMap.putAll(this.baseLayer.animationPlayer.getAnimation().get().getStatesMap(this.entitypatch, this.baseLayer.animationPlayer.getElapsedTime()));
			}
		}
		
		return new EntityState(stateMap);
	}
	
	@Override
	public void setSoftPause(boolean paused) {
		this.getAllLayers().forEach(layer -> layer.paused = paused);
	}

	@Override
	public void setHardPause(boolean paused) {
		this.hardPaused = paused;
	}
}