package yesman.epicfight.api.animation.types;

import java.util.List;

import net.minecraft.world.InteractionHand;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class MirrorAnimation extends StaticAnimation {
	public DirectStaticAnimation original;
	public DirectStaticAnimation mirror;
	
	public MirrorAnimation(float transitionTime, boolean repeatPlay, AnimationAccessor<? extends MirrorAnimation> accessor, String path1, String path2, Armature armature) {
		super(0.0F, false, accessor, armature);
		
		this.original = new DirectStaticAnimation(transitionTime, repeatPlay, path1, armature);
		this.mirror = new DirectStaticAnimation(transitionTime, repeatPlay, path2, armature);
	}
	
	@Override
	public void begin(LivingEntityPatch<?> entitypatch) {
		super.begin(entitypatch);
		
		if (entitypatch.isLogicalClient()) {
			DirectStaticAnimation animation = this.checkHandAndReturnAnimation(entitypatch.getOriginal().getUsedItemHand());
			entitypatch.getClientAnimator().playAnimation(animation, 0.0F);
		}
	}
	
	@Override
	public List<StaticAnimation> getSubAnimations() {
		return List.of(this.original, this.mirror);
	}
	
	@Override
	public boolean isMetaAnimation() {
		return true;
	}
	
	@Override
	public boolean isClientAnimation() {
		return true;
	}
	
	private DirectStaticAnimation checkHandAndReturnAnimation(InteractionHand hand) {
		if (hand == InteractionHand.OFF_HAND) {
			return this.mirror;
		}
		
		return this.original;
	}
}