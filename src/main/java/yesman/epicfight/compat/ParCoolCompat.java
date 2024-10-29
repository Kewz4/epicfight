package yesman.epicfight.compat;

import java.util.Map;

import com.alrex.parcool.ParCool;
import com.alrex.parcool.client.animation.impl.ClingToCliffAnimator;
import com.alrex.parcool.common.capability.capabilities.Capabilities;
import com.google.common.collect.Maps;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.client.forgeevent.UpdatePlayerMotionEvent;
import yesman.epicfight.api.forgeevent.AnimationRegistryEvent;
import yesman.epicfight.api.forgeevent.InitAnimatorEvent;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.mixin.ParCoolMixinAnimation;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public class ParCoolCompat implements ICompatModule {
	public static StaticAnimation BIPED_CLING_TO_CLIFF;
	
	public static void registerAnimations(AnimationRegistryEvent event) {
		event.getRegistryMap().put(ParCool.MOD_ID, ParCoolCompat::build);
	}
	
	private static void build() {
		HumanoidArmature biped = Armatures.BIPED;
		
		BIPED_CLING_TO_CLIFF = new StaticAnimation(true, "biped/cling_to_cliff", biped);
		
		PARCOOL_ANIMATION_MAPPING.put(ClingToCliffAnimator.class, ParcoolLivingMotions.CLING_TO_CLIFF);
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
			}
		});
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public void onModEventBusClient(IEventBus eventBus) {
	}
	
	public static final Map<Class<? extends com.alrex.parcool.client.animation.Animator>, ParcoolLivingMotions> PARCOOL_ANIMATION_MAPPING = Maps.newHashMap();
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public void onForgeEventBusClient(IEventBus eventBus) {
		eventBus.<UpdatePlayerMotionEvent.BaseLayer>addListener((event) -> {
			event.getPlayerPatch().getOriginal().getCapability(Capabilities.ANIMATION_CAPABILITY).ifPresent((animation) -> {
				ParCoolMixinAnimation animationAccessor = (ParCoolMixinAnimation)animation;
				com.alrex.parcool.client.animation.Animator animator = animationAccessor.getAnimator();
				
				if (animator != null && PARCOOL_ANIMATION_MAPPING.containsKey(animator.getClass())) {
					event.setMotion(PARCOOL_ANIMATION_MAPPING.get(animator.getClass()));
				}
			});
		});
	}
	
	public enum ParcoolLivingMotions implements LivingMotion {
		CLING_TO_CLIFF, WALL_SLIDING;
		
		final int id;
		
		ParcoolLivingMotions() {
			this.id = LivingMotion.ENUM_MANAGER.assign(this);
		}
		
		public int universalOrdinal() {
			return this.id;
		}
	}
}