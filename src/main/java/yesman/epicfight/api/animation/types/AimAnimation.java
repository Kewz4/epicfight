package yesman.epicfight.api.animation.types;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.JointTransform;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.client.animation.ClientAnimator;
import yesman.epicfight.api.client.animation.Layer;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.QuaternionUtils;
import yesman.epicfight.config.EpicFightOptions;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class AimAnimation extends StaticAnimation {
	public StaticAnimation lookForward;
	public StaticAnimation lookUp;
	public StaticAnimation lookDown;
	public StaticAnimation lying;
	
	public AimAnimation(boolean repeatPlay, AnimationAccessor<? extends AimAnimation> animationAccessor, String path1, String path2, String path3, String path4, Armature armature) {
		this(EpicFightOptions.GENERAL_ANIMATION_TRANSITION_TIME, repeatPlay, animationAccessor, path1, path2, path3, path4, armature);
	}
	
	public AimAnimation(float transitionTime, boolean repeatPlay, AnimationAccessor<? extends AimAnimation> animationAccessor, String path1, String path2, String path3, String path4, Armature armature) {
		super(transitionTime, repeatPlay, path1, armature);
		
		this.lookForward = new StaticAnimation(transitionTime, repeatPlay, path1, armature);
		this.lookUp = new StaticAnimation(transitionTime, repeatPlay, path2, armature);
		this.lookDown = new StaticAnimation(transitionTime, repeatPlay, path3, armature);
		this.lying = new StaticAnimation(transitionTime, repeatPlay, path4, armature);
	}
	
	@Override
	public void tick(LivingEntityPatch<?> entitypatch) {
		super.tick(entitypatch);
		
		ClientAnimator animator = entitypatch.getClientAnimator();
		Layer layer = animator.getCompositeLayer(this.getPriority());
		AnimationPlayer player = layer.animationPlayer;
		
		if (player.getElapsedTime() >= this.getTotalTime() - 0.06F) {
			layer.pause();
		}
	}
	
	@Override
	public Pose getPoseByTime(LivingEntityPatch<?> entitypatch, float time, float partialTicks) {
		if (!entitypatch.isFirstPerson()) {
			LivingMotion livingMotion = entitypatch.getCurrentLivingMotion();
			
			if (livingMotion == LivingMotions.SWIM || livingMotion == LivingMotions.FLY || livingMotion == LivingMotions.CREATIVE_FLY) {
				Pose pose = this.lying.getPoseByTime(entitypatch, time, partialTicks);
				this.modifyPose(this, pose, entitypatch, time, partialTicks);
				
				return pose;
			} else {
				float pitch = entitypatch.getOriginal().getViewXRot(Minecraft.getInstance().getFrameTime());
				StaticAnimation interpolateAnimation;
				interpolateAnimation = (pitch > 0) ? this.lookDown : this.lookUp;
				Pose pose1 = super.getPoseByTime(entitypatch, time, partialTicks);	
				Pose pose2 = interpolateAnimation.getPoseByTime(entitypatch, time, partialTicks);
				this.modifyPose(this, pose2, entitypatch, time, partialTicks);
				Pose interpolatedPose = Pose.interpolatePose(pose1, pose2, (Math.abs(pitch) / 90.0F));
				
				return interpolatedPose;
			}
		}
		
		return this.lookForward.getPoseByTime(entitypatch, time, partialTicks);
	}
	
	@Override
	public void modifyPose(DynamicAnimation animation, Pose pose, LivingEntityPatch<?> entitypatch, float time, float partialTicks) {
		super.modifyPose(animation, pose, entitypatch, time, partialTicks);
		
		if (!entitypatch.isFirstPerson() && !animation.isLinkAnimation()) {
			JointTransform chest = pose.getOrDefaultTransform("Chest");
			JointTransform head = pose.getOrDefaultTransform("Head");
			float f = 90.0F;
			float ratio = (f - Math.abs(entitypatch.getOriginal().getXRot())) / f;
			float yRotHead = entitypatch.getOriginal().yHeadRotO;
			float yRot = entitypatch.getOriginal().getVehicle() != null ? yRotHead : entitypatch.getYRot();
			MathUtils.mulQuaternion(QuaternionUtils.YP.rotationDegrees(Mth.wrapDegrees(yRot - yRotHead) * ratio), head.rotation(), head.rotation());
			chest.frontResult(JointTransform.rotation(QuaternionUtils.YP.rotationDegrees(Mth.wrapDegrees(yRotHead - yRot) * ratio)), OpenMatrix4f::mulAsOriginInverse);
		}
	}
	
	@Override
	public List<StaticAnimation> getSubAnimations() {
		return List.of(this.lookForward, this.lookUp, this.lookDown, this.lying);
	}
	
	@Override
	public boolean isClientAnimation() {
		return true;
	}
}