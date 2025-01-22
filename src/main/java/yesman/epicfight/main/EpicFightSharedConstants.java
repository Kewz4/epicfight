package yesman.epicfight.main;

import java.util.function.Function;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.ServerAnimator;
import yesman.epicfight.api.client.animation.ClientAnimator;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class EpicFightSharedConstants {
	public static final float A_TICK = 0.05F;
	public static final float GENERAL_ANIMATION_TRANSITION_TIME = 0.15F;
	public static final boolean IS_DEV_ENV = !FMLEnvironment.production;
	
	private static final Function<LivingEntityPatch<?>, Animator> ANIMATOR_PROVIDER;
	
	static {
		ANIMATOR_PROVIDER = isPhysicalClient() ? ClientAnimator::getAnimator : ServerAnimator::getAnimator;
	}
	
	public static Animator getAnimator(LivingEntityPatch<?> entitypatch) {
		return ANIMATOR_PROVIDER.apply(entitypatch);
	}
	
	public static boolean isPhysicalClient() {
		return FMLEnvironment.dist == Dist.CLIENT;
	}
}