package yesman.epicfight.compat;

import java.util.Map;

import com.alrex.parcool.client.animation.impl.ClingToCliffAnimator;
import com.alrex.parcool.client.animation.impl.DiveAnimationHostAnimator;
import com.alrex.parcool.client.animation.impl.FastRunningAnimator;
import com.alrex.parcool.client.animation.impl.HangAnimator;
import com.alrex.parcool.client.animation.impl.HorizontalWallRunAnimator;
import com.alrex.parcool.client.animation.impl.JumpChargingAnimator;
import com.alrex.parcool.client.animation.impl.SlidingAnimator;
import com.alrex.parcool.client.animation.impl.WallSlideAnimator;
import com.alrex.parcool.common.action.impl.ClingToCliff;
import com.alrex.parcool.common.action.impl.HangDown;
import com.alrex.parcool.common.action.impl.HangDown.BarAxis;
import com.alrex.parcool.common.action.impl.WallSlide;
import com.alrex.parcool.common.capability.Parkourability;
import com.alrex.parcool.common.capability.capabilities.Capabilities;
import com.alrex.parcool.utilities.VectorUtil;
import com.google.common.collect.Maps;

import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import yesman.epicfight.api.client.forgeevent.UpdatePlayerMotionEvent;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.compat.ParCoolCompat.ParcoolLivingMotions;
import yesman.epicfight.compat.ParCoolCompat.ParCoolUtils;
import yesman.epicfight.compat.ParCoolCompat.ClingType;
import yesman.epicfight.mixin.ParCoolMixinAnimation;
import yesman.epicfight.mixin.ParCoolMixinHorizontalWallRunAnimator;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

@OnlyIn(Dist.CLIENT)
public class ParCoolClientCompat implements ICompatModule {
	@FunctionalInterface
	public interface LifecycleAnimationLinker {
		void accept(com.alrex.parcool.client.animation.Animator animator, Parkourability parkourability, UpdatePlayerMotionEvent.BaseLayer animationUpdateEvent);
	}
	
	public static final Map<Class<? extends com.alrex.parcool.client.animation.Animator>, LifecycleAnimationLinker> PARCOOL_ANIMATOR_MAPPING = Maps.newHashMap();
	
	public static void buildClientStuff() {
		PARCOOL_ANIMATOR_MAPPING.clear();
		
		PARCOOL_ANIMATOR_MAPPING.put(JumpChargingAnimator.class, (animator, parkourability, livingMotionUpdateEvent) -> {
			livingMotionUpdateEvent.setMotion(ParcoolLivingMotions.CAT_LEAP_PREPARATION);
		});
		
		PARCOOL_ANIMATOR_MAPPING.put(ClingToCliffAnimator.class, (animator, parkourability, livingMotionUpdateEvent) -> {
			ClingType clingDirection = livingMotionUpdateEvent.getPlayerPatch().getAnimator().getVariables().getSharedVariable(ParCoolCompat.CLING_TYPE);
			
			if (clingDirection == ClingType.OUTER_CORNER) {
				livingMotionUpdateEvent.setMotion(ParcoolLivingMotions.CLING_TO_CLIFF_OUTER_CORNER);
			} else if (clingDirection == ClingType.INNER_CORNER) {
				livingMotionUpdateEvent.setMotion(ParcoolLivingMotions.CLING_TO_CLIFF_INNER_CORNER);
			} else {
				livingMotionUpdateEvent.setMotion(ParcoolLivingMotions.CLING_TO_CLIFF);
			}
		});
		
		PARCOOL_ANIMATOR_MAPPING.put(DiveAnimationHostAnimator.class, (animator, parkourability, livingMotionUpdateEvent) -> {
			livingMotionUpdateEvent.setMotion(ParcoolLivingMotions.DIVE);
		});
		
		PARCOOL_ANIMATOR_MAPPING.put(WallSlideAnimator.class, (animator, parkourability, livingMotionUpdateEvent) -> {
			Vec3 wall = parkourability.get(WallSlide.class).getLeanedWallDirection();
			
			if (wall == null) {
				return;
			}
			
			Vec3 bodyVec = VectorUtil.fromYawDegree(livingMotionUpdateEvent.getPlayerPatch().getOriginal().yBodyRot);
			Vec3 vec = new Vec3(bodyVec.x, 0, bodyVec.z).normalize();
			Vec3 dividedVec = new Vec3(vec.x * wall.x + vec.z * wall.z, 0, -vec.x * wall.z + vec.z * wall.x).normalize();
			
			if (dividedVec.z < 0) {
				livingMotionUpdateEvent.setMotion(ParcoolLivingMotions.WALL_SLIDING_RIGHT);
			} else {
				livingMotionUpdateEvent.setMotion(ParcoolLivingMotions.WALL_SLIDING_LEFT);
			}
		});
		
		PARCOOL_ANIMATOR_MAPPING.put(HorizontalWallRunAnimator.class, (animator, parkourability, livingMotionUpdateEvent) -> {
			if (((ParCoolMixinHorizontalWallRunAnimator)animator).getWallIsRightSide()) {
				livingMotionUpdateEvent.setMotion(ParcoolLivingMotions.WALL_RUNNING_RIGHT);
			} else {
				livingMotionUpdateEvent.setMotion(ParcoolLivingMotions.WALL_RUNNING_LEFT);
			}
		});
		
		PARCOOL_ANIMATOR_MAPPING.put(FastRunningAnimator.class, (animator, parkourability, livingMotionUpdateEvent) -> {
			livingMotionUpdateEvent.setMotion(ParcoolLivingMotions.FAST_RUN);
		});
		
		PARCOOL_ANIMATOR_MAPPING.put(HangAnimator.class, (animator, parkourability, livingMotionUpdateEvent) -> {
			HangDown hangDown = parkourability.get(HangDown.class);
			
			if (hangDown.isOrthogonalToBar()) {
				livingMotionUpdateEvent.setMotion(ParcoolLivingMotions.HANG_DOWN_ORTHOGONAL);
			} else {
				livingMotionUpdateEvent.setMotion(ParcoolLivingMotions.HANG_DOWN);
			}
		});
		
		PARCOOL_ANIMATOR_MAPPING.put(SlidingAnimator.class, (animator, parkourability, livingMotionUpdateEvent) -> {
			livingMotionUpdateEvent.setMotion(ParcoolLivingMotions.SLIDING);
		});
		/**
		PARCOOL_ANIMATOR_MAPPING.put(CrawlAnimator.class, (animator, parkourability, livingMotionUpdateEvent) -> {
			livingMotionUpdateEvent.setMotion(ParcoolLivingMotions.CRAWL);
		});
		**/
	}
	
	@Override
	public void onModEventBus(IEventBus eventBus) {
	}
	
	@Override
	public void onForgeEventBus(IEventBus eventBus) {
	}
	
	@Override
	public void onModEventBusClient(IEventBus eventBus) {
	}
	
	@Override
	public void onForgeEventBusClient(IEventBus eventBus) {
		eventBus.<UpdatePlayerMotionEvent.BaseLayer>addListener((event) -> {
			if (event.inaction()) {
				return;
			}
			
			event.getPlayerPatch().getOriginal().getCapability(Capabilities.ANIMATION_CAPABILITY).ifPresent((animation) -> {
				ParCoolMixinAnimation animationAccessor = (ParCoolMixinAnimation)animation;
				com.alrex.parcool.client.animation.Animator animator = animationAccessor.getAnimator();
				Parkourability parkourability = Parkourability.get(event.getPlayerPatch().getOriginal());
				
				if (parkourability != null && animator != null && PARCOOL_ANIMATOR_MAPPING.containsKey(animator.getClass())) {
					PARCOOL_ANIMATOR_MAPPING.get(animator.getClass()).accept(animator, parkourability, event);
				}
			});
		});
		
		eventBus.<MovementInputUpdateEvent>addListener((event) -> {
			LocalPlayerPatch playerpatch = EpicFightCapabilities.getEntityPatch(event.getEntity(), LocalPlayerPatch.class);
			
			if (!playerpatch.isBattleMode()) {
				return;
			}
			
			Parkourability parkourability = Parkourability.get(event.getEntity());
			HangDown hangDown;
			ClingToCliff clingToCliff;
			
			if ((hangDown = (HangDown)parkourability.get(HangDown.class)) != null && hangDown.isDoing()) {
				if (!playerpatch.getEntityState().inaction()) {
					float yRot = ParCoolUtils.idealYRotForHanging(hangDown, event.getEntity());
					BarAxis barAxis = hangDown.getHangingBarAxis();
					boolean axisMismatches = false;
					
					if (hangDown.isOrthogonalToBar()) {
						if (barAxis == ParCoolUtils.getLookBarAxis(yRot)) {
							axisMismatches = true;
						}
					} else {
						if (barAxis != ParCoolUtils.getLookBarAxis(yRot)) {
							axisMismatches = true;
						}
					}
					
					if (!axisMismatches) {
						playerpatch.setModelYRot(yRot, true);
						
						if (hangDown.isOrthogonalToBar()) {
							if (event.getInput().left) {
								playerpatch.playAnimationSynchronized(ParCoolCompat.BIPED_HANG_DOWN_MOVE_LEFT, 0.0F);
							} else if (event.getInput().right) {
								playerpatch.playAnimationSynchronized(ParCoolCompat.BIPED_HANG_DOWN_MOVE_RIGHT, 0.0F);
							}
						} else {
							if (event.getInput().up) {
								playerpatch.playAnimationSynchronized(ParCoolCompat.BIPED_HANG_DOWN_MOVE_FORWARD_START, 0.0F);
							} else if (event.getInput().down) {
								playerpatch.playAnimationSynchronized(ParCoolCompat.BIPED_HANG_DOWN_MOVE_BACKWARD, 0.0F);
							}
						}
					}
				}
				
				event.getInput().left = false;
				event.getInput().right = false;
				event.getInput().up = false;
				event.getInput().down = false;
				event.getInput().forwardImpulse = 0.0F;
				event.getInput().leftImpulse = 0.0F;
			}
			
			if ((clingToCliff = (ClingToCliff)parkourability.get(ClingToCliff.class)) != null && clingToCliff.isDoing()) {
				if (!playerpatch.getEntityState().inaction()) {
					if (event.getInput().left) {
						ParCoolUtils.scanTerrainAndStartClingAction(playerpatch, ParCoolUtils.WallMoveType.MOVE_LEFT);
					} else if (event.getInput().right) {
						ParCoolUtils.scanTerrainAndStartClingAction(playerpatch, ParCoolUtils.WallMoveType.MOVE_RIGHT);
					}
				}
				
				event.getInput().left = false;
				event.getInput().right = false;
				event.getInput().up = false;
				event.getInput().down = false;
				event.getInput().forwardImpulse = 0.0F;
				event.getInput().leftImpulse = 0.0F;
			}
		});
	}
}