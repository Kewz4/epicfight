package yesman.epicfight.compat;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.joml.Vector3f;

import com.alrex.parcool.ParCool;
import com.alrex.parcool.api.unstable.action.ParCoolActionEvent;
import com.alrex.parcool.client.animation.impl.ClingToCliffAnimator;
import com.alrex.parcool.client.animation.impl.DiveAnimationHostAnimator;
import com.alrex.parcool.client.animation.impl.WallSlideAnimator;
import com.alrex.parcool.common.action.impl.ClimbUp;
import com.alrex.parcool.common.action.impl.ClingToCliff;
import com.alrex.parcool.common.action.impl.WallSlide;
import com.alrex.parcool.common.capability.Parkourability;
import com.alrex.parcool.common.capability.capabilities.Capabilities;
import com.alrex.parcool.utilities.VectorUtil;
import com.google.common.collect.Maps;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import yesman.epicfight.api.animation.JointTransform;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.property.AnimationEvent;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.StaticAnimationProperty;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.client.forgeevent.UpdatePlayerMotionEvent;
import yesman.epicfight.api.forgeevent.AnimationRegistryEvent;
import yesman.epicfight.api.forgeevent.InitAnimatorEvent;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.mixin.ParCoolMixinAnimation;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public class ParCoolCompat implements ICompatModule {
	public static StaticAnimation BIPED_CLING_TO_CLIFF;
	public static StaticAnimation BIPED_CLING_TO_CLIFF_LEFT;
	public static StaticAnimation BIPED_CLING_TO_CLIFF_RIGHT;
	public static StaticAnimation BIPED_DIVE;
	public static StaticAnimation BIPED_WALL_SLIDE_LEFT;
	public static StaticAnimation BIPED_WALL_SLIDE_RIGHT;
	
	public static StaticAnimation BIPED_WALL_JUMP;
	public static StaticAnimation BIPED_CLIMB_UP;
	
	public static final Map<Class<? extends com.alrex.parcool.common.action.Action>, StaticAnimation> PARCOOL_ONE_SHOT_ANIMATIONS_MAPPING = Maps.newHashMap();
	public static final Map<Class<? extends com.alrex.parcool.client.animation.Animator>, BiConsumer<Parkourability, UpdatePlayerMotionEvent.BaseLayer>> PARCOOL_LIFECYCLE_ANIMATIONS_MAPPING = Maps.newHashMap();
	public static final Map<Class<? extends com.alrex.parcool.common.action.Action>, Function<LivingEntityPatch<?>, Boolean>> PARCOOL_ACTION_CANCEL_EVENTS = Maps.newHashMap();
	
	public static void registerAnimations(AnimationRegistryEvent event) {
		event.getRegistryMap().put(ParCool.MOD_ID, ParCoolCompat::build);
	}
	
	private static void build() {
		HumanoidArmature biped = Armatures.BIPED;
		
		BIPED_CLING_TO_CLIFF = new StaticAnimation(true, "biped/cling_to_cliff", biped);
		BIPED_CLING_TO_CLIFF_LEFT = new StaticAnimation(true, "biped/cling_to_cliff_left", biped);
		BIPED_CLING_TO_CLIFF_RIGHT = new StaticAnimation(true, "biped/cling_to_cliff_right", biped);
		
		BIPED_WALL_SLIDE_LEFT = new StaticAnimation(true, "biped/wall_slide_left", biped);
		BIPED_WALL_SLIDE_RIGHT = new StaticAnimation(true, "biped/wall_slide_right", biped);
		
		BIPED_DIVE = new StaticAnimation(true, "biped/dive", biped).addProperty(StaticAnimationProperty.POSE_MODIFIER, new AnimationProperty.PoseModifier() {
			public static final Vector3f RANDOM_AXIS = new Vector3f();
			
			@Override
			public void modify(DynamicAnimation self, Pose pose, LivingEntityPatch<?> entitypatch, float elapsedTime, float partialTicks) {
				float modifier = Math.min(elapsedTime / 1.0F, 1.0F) * 0.01F;
				RandomSource random = entitypatch.getOriginal().getRandom();
				
				JointTransform chestJt = pose.getJointTransformData().get("Root");
				RANDOM_AXIS.set(random.nextFloat(), random.nextFloat(), random.nextFloat()).normalize();
				chestJt.rotation().rotateAxis((float)random.nextGaussian() * modifier, RANDOM_AXIS);
				
				JointTransform thighL = pose.getJointTransformData().get("Thigh_L");
				RANDOM_AXIS.set(random.nextFloat(), random.nextFloat(), random.nextFloat()).normalize();
				thighL.rotation().rotateAxis((float)random.nextGaussian() * modifier, RANDOM_AXIS);
				
				JointTransform thighR = pose.getJointTransformData().get("Thigh_R");
				RANDOM_AXIS.set(random.nextFloat(), random.nextFloat(), random.nextFloat()).normalize();
				thighR.rotation().rotateAxis((float)random.nextGaussian() * modifier, RANDOM_AXIS);
			}
		});
		
		BIPED_WALL_JUMP = new ActionAnimation(0.15F, 0.7F, "biped/wall_jump", biped)
				.addStateRemoveOld(EntityState.MOVEMENT_LOCKED, false)
				.addStateRemoveOld(EntityState.TURNING_LOCKED, false);
		
		BIPED_CLIMB_UP = new ActionAnimation(0.15F, "biped/climb_up", biped)
				.addProperty(ActionAnimationProperty.MOVE_VERTICAL, true)
				.addProperty(ActionAnimationProperty.STOP_MOVEMENT, true)
				.addProperty(ActionAnimationProperty.REMOVE_DELTA_MOVEMENT, true)
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, AnimationEvent.create((entitypatch, animation, params) -> {
					entitypatch.setYRot(entitypatch.getOriginal().yBodyRot);
				}, AnimationEvent.Side.CLIENT))
				;
		
		PARCOOL_ONE_SHOT_ANIMATIONS_MAPPING.clear();
		PARCOOL_LIFECYCLE_ANIMATIONS_MAPPING.clear();
		PARCOOL_ACTION_CANCEL_EVENTS.clear();
		
		PARCOOL_ONE_SHOT_ANIMATIONS_MAPPING.put(ClimbUp.class, BIPED_CLIMB_UP);
		
		PARCOOL_LIFECYCLE_ANIMATIONS_MAPPING.put(ClingToCliffAnimator.class, (parkourability, livingMotionUpdateEvent) -> {
			switch (parkourability.get(ClingToCliff.class).getFacingDirection()) {
			case ToWall -> {
				livingMotionUpdateEvent.setMotion(ParcoolLivingMotions.CLING_TO_CLIFF);
			}
			case RightAgainstWall -> {
				livingMotionUpdateEvent.setMotion(ParcoolLivingMotions.CLING_TO_CLIFF_RIGHT);
			}
			case LeftAgainstWall -> {
				livingMotionUpdateEvent.setMotion(ParcoolLivingMotions.CLING_TO_CLIFF_LEFT);
			}
			}
		});
		
		PARCOOL_LIFECYCLE_ANIMATIONS_MAPPING.put(DiveAnimationHostAnimator.class, (parkourability, livingMotionUpdateEvent) -> {
			livingMotionUpdateEvent.setMotion(ParcoolLivingMotions.DIVE);
		});
		
		PARCOOL_LIFECYCLE_ANIMATIONS_MAPPING.put(WallSlideAnimator.class, (parkourability, livingMotionUpdateEvent) -> {
			Vec3 wall = parkourability.get(WallSlide.class).getLeanedWallDirection();
			
			if (wall == null) {
				return;
			}
			
			Vec3 bodyVec = VectorUtil.fromYawDegree(livingMotionUpdateEvent.getPlayerPatch().getOriginal().yBodyRot);
			Vec3 vec = new Vec3(bodyVec.x, 0, bodyVec.z).normalize();
			Vec3 dividedVec = new Vec3(vec.x * wall.x + vec.z * wall.z, 0, -vec.x * wall.z + vec.z * wall.x).normalize();
			
			if (dividedVec.z < 0) {
				livingMotionUpdateEvent.setMotion(ParcoolLivingMotions.WALL_SLIDING_RIGH);
			} else {
				livingMotionUpdateEvent.setMotion(ParcoolLivingMotions.WALL_SLIDING_LEFT);
			}
		});
		
		PARCOOL_ACTION_CANCEL_EVENTS.put(ClingToCliff.class, (entitypatch) -> {
			DynamicAnimation nowPlaying = entitypatch.getAnimator().getPlayerFor(null).getAnimation().getRealAnimation();
			return nowPlaying == BIPED_CLIMB_UP;
		});
	}
	
	@Override
	public void onModEventBus(IEventBus eventBus) {
		eventBus.<FMLConstructModEvent>addListener((event) -> {
			LivingMotion.ENUM_MANAGER.registerEnumCls(ParCool.MOD_ID, ParcoolLivingMotions.class);
		});
		
		eventBus.addListener(ParCoolCompat::registerAnimations);
	}
	
	@Override
	public void onForgeEventBus(IEventBus eventBus) {
		eventBus.<InitAnimatorEvent>addListener((event) -> {
			if (event.getEntityPatch() instanceof PlayerPatch<?>) {
				event.getAnimator().addLivingAnimation(ParcoolLivingMotions.CLING_TO_CLIFF, BIPED_CLING_TO_CLIFF);
				event.getAnimator().addLivingAnimation(ParcoolLivingMotions.CLING_TO_CLIFF_RIGHT, BIPED_CLING_TO_CLIFF_RIGHT);
				event.getAnimator().addLivingAnimation(ParcoolLivingMotions.CLING_TO_CLIFF_LEFT, BIPED_CLING_TO_CLIFF_LEFT);
				event.getAnimator().addLivingAnimation(ParcoolLivingMotions.DIVE, BIPED_DIVE);
				event.getAnimator().addLivingAnimation(ParcoolLivingMotions.WALL_SLIDING_LEFT, BIPED_WALL_SLIDE_LEFT);
				event.getAnimator().addLivingAnimation(ParcoolLivingMotions.WALL_SLIDING_RIGH, BIPED_WALL_SLIDE_RIGHT);
			}
		});
		
		eventBus.<ParCoolActionEvent.TryToStartEvent>addListener((event) -> {
			if (PARCOOL_ACTION_CANCEL_EVENTS.containsKey(event.getAction().getClass())) {
				LivingEntityPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(event.getPlayer(), LivingEntityPatch.class);
				
				if (PARCOOL_ACTION_CANCEL_EVENTS.get(event.getAction().getClass()).apply(entitypatch)) {
					event.setCanceled(true);
				}
			}
		});
		
		eventBus.<ParCoolActionEvent.StartEvent>addListener((event) -> {
			LivingEntityPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(event.getPlayer(), LivingEntityPatch.class);
			
			if (entitypatch != null && entitypatch.isLogicalClient() && PARCOOL_ONE_SHOT_ANIMATIONS_MAPPING.containsKey(event.getAction().getClass())) {
				entitypatch.getAnimator().playAnimation(PARCOOL_ONE_SHOT_ANIMATIONS_MAPPING.get(event.getAction().getClass()), 0.0F);
			}
		});
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public void onModEventBusClient(IEventBus eventBus) {
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public void onForgeEventBusClient(IEventBus eventBus) {
		eventBus.<UpdatePlayerMotionEvent.BaseLayer>addListener((event) -> {
			event.getPlayerPatch().getOriginal().getCapability(Capabilities.ANIMATION_CAPABILITY).ifPresent((animation) -> {
				ParCoolMixinAnimation animationAccessor = (ParCoolMixinAnimation)animation;
				com.alrex.parcool.client.animation.Animator animator = animationAccessor.getAnimator();
				Parkourability parkourability = Parkourability.get(event.getPlayerPatch().getOriginal());
				
				if (parkourability != null && animator != null && PARCOOL_LIFECYCLE_ANIMATIONS_MAPPING.containsKey(animator.getClass())) {
					PARCOOL_LIFECYCLE_ANIMATIONS_MAPPING.get(animator.getClass()).accept(parkourability, event);
				}
			});
		});
	}
	
	public enum ParcoolLivingMotions implements LivingMotion {
		CLING_TO_CLIFF, CLING_TO_CLIFF_LEFT, CLING_TO_CLIFF_RIGHT, DIVE, WALL_SLIDING_LEFT, WALL_SLIDING_RIGH;
		
		final int id;
		
		ParcoolLivingMotions() {
			this.id = LivingMotion.ENUM_MANAGER.assign(this);
		}
		
		public int universalOrdinal() {
			return this.id;
		}
	}
}