package yesman.epicfight.api.animation.property;

import org.joml.Quaternionf;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.JointTransform;
import yesman.epicfight.api.animation.Keyframe;
import yesman.epicfight.api.animation.TransformSheet;
import yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackAnimationProperty;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.grappling.GrapplingAttackAnimation;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.api.utils.math.Vec4f;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class MoveCoordFunctions {
	/**
	 * Defines a function that how to build the coordinate of {@link ActionAnimation}
	 */
	@FunctionalInterface
	public interface MoveCoordSetter {
		void set(DynamicAnimation animation, LivingEntityPatch<?> entitypatch, TransformSheet transformSheet);
	}
	
	/**
	 * Defines a function that how to interpret given coordinate and return the movement vector from entity's current position
	 */
	@FunctionalInterface
	public interface MoveCoordGetter {
		Vec3f get(DynamicAnimation animation, LivingEntityPatch<?> entitypatch, TransformSheet transformSheet);
	}
	
	/**
	 * MODEL_COORD
	 *  - Calculates the coordinate gap between previous and current elapsed time
	 *  - the coordinate doesn't reflect the entity's rotation
	 */
	public static final MoveCoordGetter MODEL_COORD = (animation, entitypatch, coord) -> {
		LivingEntity livingentity = entitypatch.getOriginal();
		AnimationPlayer player = entitypatch.getAnimator().getPlayerFor(animation);
		JointTransform oJt = coord.getInterpolatedTransform(player.getPrevElapsedTime());
		JointTransform jt = coord.getInterpolatedTransform(player.getElapsedTime());
		
		TransformSheet animCoord = animation.getCoord();
		JointTransform oAnimJt = animCoord.getInterpolatedTransform(player.getPrevElapsedTime());
		JointTransform animJt = animCoord.getInterpolatedTransform(player.getElapsedTime());
		
		Vec3f lastCoord = entitypatch.getAnimator().getAnimationVariable(ActionAnimation.LAST_MODEL_COORD);
		
		if (lastCoord == null || Math.abs(animJt.translation().z - oAnimJt.translation().z) < 0.01F) {
			lastCoord = oJt.translation();
		}
		
		Vec4f prevpos = new Vec4f(lastCoord);
		Vec4f currentpos = new Vec4f(jt.translation());
		entitypatch.getAnimator().putAnimationVariable(ActionAnimation.LAST_MODEL_COORD, jt.translation());
		
		OpenMatrix4f rotationTransform = OpenMatrix4f.createRotatorDeg(-entitypatch.getYRot(), Vec3f.Y_AXIS);
		OpenMatrix4f localTransform = entitypatch.getArmature().searchJointByName("Root").getLocalTrasnform().removeTranslation();
		rotationTransform.mulBack(localTransform);
		currentpos.transform(rotationTransform);
		prevpos.transform(rotationTransform);
		
		boolean hasNoGravity = entitypatch.getOriginal().isNoGravity();
		boolean moveVertical = animation.getProperty(ActionAnimationProperty.MOVE_VERTICAL).orElse(false) || animation.getProperty(ActionAnimationProperty.COORD).isPresent();
		float dx = prevpos.x - currentpos.x;
		float dy = (moveVertical || hasNoGravity) ? currentpos.y - prevpos.y : 0.0F;
		float dz = prevpos.z - currentpos.z;
		dx = Math.abs(dx) > 0.0001F ? dx : 0.0F;
		dz = Math.abs(dz) > 0.0001F ? dz : 0.0F;
		
		BlockPos blockpos = new BlockPos.MutableBlockPos(livingentity.getX(), livingentity.getBoundingBox().minY - 1.0D, livingentity.getZ());
		BlockState blockState = livingentity.level().getBlockState(blockpos);
		AttributeInstance movementSpeed = livingentity.getAttribute(Attributes.MOVEMENT_SPEED);
		boolean soulboost = blockState.is(BlockTags.SOUL_SPEED_BLOCKS) && EnchantmentHelper.getEnchantmentLevel(Enchantments.SOUL_SPEED, livingentity) > 0;
		float speedFactor = (float)(soulboost ? 1.0D : livingentity.level().getBlockState(blockpos).getBlock().getSpeedFactor());
		float moveMultiplier = (float)(animation.getProperty(ActionAnimationProperty.AFFECT_SPEED).orElse(false) ? (movementSpeed.getValue() / movementSpeed.getBaseValue()) : 1.0F);
		
		return new Vec3f(dx * moveMultiplier * speedFactor, dy, dz * moveMultiplier * speedFactor);
	};
	
	/**
	 * WORLD_COORD
	 * - Calculates the coordinate of current elapsed time
	 * - the coordinate is the world position
	 */
	public static final MoveCoordGetter WORLD_COORD = (animation, entitypatch, coord) -> {
		AnimationPlayer player = entitypatch.getAnimator().getPlayerFor(animation);
		JointTransform jt = coord.getInterpolatedTransform(player.getElapsedTime());
		Vec3 entityPos = entitypatch.getOriginal().position();
		
		return jt.translation().copy().sub(Vec3f.fromDoubleVector(entityPos));
	};
	
	/**
	 * ATTACHED
	 * Calculates the relative position of a grappling target entity.
	 *  - especially used by {@link GrapplingAttackAnimation}
	 *  - used with {@link MoveCoordFunctions#RAW_COORD}
	 */
	public static final MoveCoordGetter ATTACHED = (animation, entitypatch, coord) -> {
		LivingEntity target = entitypatch.getGrapplingTarget();
		
		if (target == null) {
			return MODEL_COORD.get(animation, entitypatch, coord);
		}
		
		TransformSheet rootCoord = animation.getCoord();
		LivingEntity livingentity = entitypatch.getOriginal();
		AnimationPlayer player = entitypatch.getAnimator().getPlayerFor(animation);
		Vec3f model = rootCoord.getInterpolatedTransform(player.getElapsedTime()).translation();
		Vec3f world = OpenMatrix4f.transform3v(OpenMatrix4f.createRotatorDeg(-target.getYRot(), Vec3f.Y_AXIS), model, null);
		Vec3f dst = Vec3f.fromDoubleVector(target.position()).add(world);
		entitypatch.setYRot(Mth.wrapDegrees(target.getYRot() + 180.0F));
		
		return dst.sub(Vec3f.fromDoubleVector(livingentity.position()));
	};
	
	/******************************************************
	 * MoveCoordSetters
	 * Consider that getAnimationPlayer(self) returns null at the beginning.
	 ******************************************************/
	
	/**
	 * Sets a raw animation coordinate as action animation's coord
	 *  - used with {@link MoveCoordFunctions#MODEL_COORD}
	 */
	public static final MoveCoordSetter RAW_COORD = (self, entitypatch, transformSheet) -> {
		transformSheet.readFrom(self.getCoord().copyAll());
	};
	
	/**
	 * Sets a raw animation coordinate multiplied by entity's pitch as action animation's coord
	 *  - used with {@link MoveCoordFunctions#MODEL_COORD}
	 */
	public static final MoveCoordSetter RAW_COORD_WITH_X_ROT = (self, entitypatch, transformSheet) -> {
		float xRot = entitypatch.getOriginal().getXRot();
		TransformSheet sheet = self.getCoord().copyAll();
		
		for (Keyframe kf : sheet.getKeyframes()) {
			kf.transform().translation().rotate(-xRot, Vec3f.X_AXIS);
		}
		
		transformSheet.readFrom(sheet);
	};
	
	/**
	 * Trace the last frame location of animation relative to the target entity's position
	 *  - zero vector is the target entity's position in animation
	 *  - rotation is the direction toward a target entity
	 *  - used with WORLD_COORD)
	 */
	public static final MoveCoordSetter TRACE_ORIGIN_AS_TARGET_POSITION_BEGIN = (self, entitypatch, transformSheet) -> {
		LivingEntity attackTarget = entitypatch.getTarget();
		TransformSheet transform = self.getCoord().copyAll();
		Keyframe[] rootKeyframes = transform.getKeyframes();
		
		if (attackTarget != null) {
			Vec3 start = entitypatch.getOriginal().position();
			Vec3 targetTracePosition = attackTarget.position();
			Vec3 toTarget = targetTracePosition.subtract(start);
			Vec3f modelDst = rootKeyframes[rootKeyframes.length - 1].transform().translation().copy().multiply(-1.0F, 1.0F, -1.0F);
			float yRot = (float)MathUtils.getYRotOfVector(toTarget);
			
			modelDst.rotate(-yRot, Vec3f.Y_AXIS);
			
			Vec3 dst = targetTracePosition.add(modelDst.x, modelDst.y, modelDst.z);
			float clampedXRot = MathUtils.rotlerp(entitypatch.getOriginal().getXRot(), (float)MathUtils.getXRotOfVector(toTarget), 20.0F);
			float clampedYRot = MathUtils.rotlerp(entitypatch.getYRot(), yRot, entitypatch.getYRotLimit());
			TransformSheet newTransform = transform.getCorrectedWorldCoord(entitypatch, start, dst, -clampedXRot, clampedYRot, 0, rootKeyframes.length, true);
			
			transformSheet.readFrom(newTransform);
		} else {
			transform.transform((jt) -> {
				Vec3f firstPos = self.getCoord().getKeyframes()[0].transform().translation().copy();
				jt.translation().sub(firstPos);
				
				LivingEntity original = entitypatch.getOriginal();
				Vec3 pos = original.position();
				
				jt.translation().rotate(-entitypatch.getYRot(), Vec3f.Y_AXIS);
				jt.translation().multiply(-1.0F, 1.0F, -1.0F);
				jt.translation().add(Vec3f.fromDoubleVector(pos));
			});
			
			transformSheet.readFrom(transform);
		}
	};
	
	/**
	 * Trace the last frame location of animation relative to the target entity's position
	 *  - zero vector is the target entity's position in animation
	 *  - rotation is the direction toward a target entity
	 *  - used with WORLD_COORD)
	 */
	public static final MoveCoordSetter TRACE_ORIGIN_AS_TARGET_POSITION = (self, entitypatch, transformSheet) -> {
		LivingEntity attackTarget = entitypatch.getTarget();
		
		if (attackTarget != null) {
			TransformSheet transform = self.getCoord().copyAll();
			Keyframe[] rootKeyframes = transform.getKeyframes();
			Vec3 start = entitypatch.getAnimator().getAnimationVariable(ActionAnimation.BEGINNING_LOCATION);
			Vec3 targetTracePosition = attackTarget.position();
			
			Vec3 toTarget = targetTracePosition.subtract(start);
			Vec3f destInAnimation = rootKeyframes[rootKeyframes.length - 1].transform().translation().copy().multiply(1.0F, 1.0F, -1.0F);
			float yRot = (float)Mth.wrapDegrees(MathUtils.getYRotOfVector(toTarget));
			destInAnimation.rotate(-yRot, Vec3f.Y_AXIS);
			
			Vec3 destInWorld = targetTracePosition.add(destInAnimation.toDoubleVector());
			float clampedXRot = (float)MathUtils.getXRotOfVector(toTarget);
			float clampedYRot = MathUtils.rotlerp(entitypatch.getYRot(), yRot, entitypatch.getYRotLimit());
			TransformSheet newTransform = transform.getCorrectedWorldCoord(entitypatch, start, destInWorld, -clampedXRot, clampedYRot, 0, rootKeyframes.length, true);
			
			entitypatch.setYRot(clampedYRot);
			transformSheet.readFrom(newTransform);
		}
	};
	
	/**
	 * Trace the target entity's position (use it with MODEL_COORD)
	 *  - the location of the last keyfram is basis to limit maximum distance
	 *  - rotation is where the entity is looking
	 */
	public static final MoveCoordSetter TRACE_TARGET_DISTANCE = (self, entitypatch, transformSheet) -> {
		LivingEntity attackTarget = entitypatch.getTarget();
		
		if (attackTarget != null) {
			TransformSheet transform = self.getCoord().copyAll();
			Keyframe[] coord = transform.getKeyframes();
			Keyframe[] realAnimationCoord = self.getRealAnimation().getCoord().getKeyframes();
			
			Vec3 start = entitypatch.getAnimator().getAnimationVariable(ActionAnimation.BEGINNING_LOCATION);
			Vec3 toDestWorld = attackTarget.position().subtract(start);
			Vec3f toDestAnim = realAnimationCoord[realAnimationCoord.length - 1].transform().translation();
			
			float entityRadius = (attackTarget.getBbWidth() + entitypatch.getOriginal().getBbWidth()) * 0.7F;
			float worldLength = Math.max((float)toDestWorld.length() - entityRadius, 0.0F);
			float animLength = toDestAnim.length();
			
			float dot = entitypatch.getAnimator().getAnimationVariable(ActionAnimation.INITIAL_DOT);
			float lookLength = Mth.lerp(dot, animLength, worldLength);
			float scale = Math.min(lookLength / animLength, 1.0F);
			
			if (self.isLinkAnimation()) {
				scale *= coord[coord.length - 1].transform().translation().length() / animLength;
			}
			
			int startFrame = 0; 
			int endFrame = coord.length - 1;
			
			for (int i = startFrame; i <= endFrame; i++) {
				Vec3f translation = coord[i].transform().translation();
				
				if (translation.z < 0.0F) {
					translation.z *= scale;
				}
			}
			
			transformSheet.readFrom(transform);
		} else {
			transformSheet.readFrom(self.getCoord().copyAll());
		}
	};
	
	/**
	 * Trace the target entity's position (use it MODEL_COORD)
	 *  - the location of the last keyframe is a basis to limit maximum distance
	 *  - rotation is the direction toward a target entity
	 */
	public static final MoveCoordSetter TRACE_TARGET_LOCATION_ROTATION = (self, entitypatch, transformSheet) -> {
		LivingEntity attackTarget = entitypatch.getTarget();
		
		if (attackTarget != null) {
			TransformSheet transform = self.getCoord().copyAll();
			Keyframe[] coord = transform.getKeyframes();
			Keyframe[] realAnimationCoord = self.getRealAnimation().getCoord().getKeyframes();
			
			Vec3 start = entitypatch.getAnimator().getAnimationVariable(ActionAnimation.BEGINNING_LOCATION);
			Vec3 toDestWorld = attackTarget.position().subtract(start);
			Vec3f toDestAnim = realAnimationCoord[realAnimationCoord.length - 1].transform().translation();
			
			float yRot = (float)Mth.wrapDegrees(MathUtils.getYRotOfVector(toDestWorld));
			float clampedYRot = MathUtils.rotlerp(entitypatch.getYRot(), yRot, entitypatch.getYRotLimit());
			float entityRadius = (attackTarget.getBbWidth() + entitypatch.getOriginal().getBbWidth()) * 0.7F;
			float worldLength = Math.max((float)toDestWorld.length() - entityRadius, 0.0F);
			float animLength = toDestAnim.length();
			float scale = Math.min(worldLength / animLength, 1.0F);
			
			if (self.isLinkAnimation()) {
				scale *= coord[coord.length - 1].transform().translation().length() / animLength;
			}
			
			int startFrame = 0; 
			int endFrame = coord.length - 1;
			
			for (int i = startFrame; i <= endFrame; i++) {
				Vec3f translation = coord[i].transform().translation();
				
				if (translation.z < 0.0F) {
					translation.z *= scale;
				}
			}
			
			entitypatch.setYRot(clampedYRot);
			transformSheet.readFrom(transform);
		} else {
			transformSheet.readFrom(self.getCoord().copyAll());
		}
	};
	
	/**
	 * OLD: Traces the location of a target
	 */
	@Deprecated
	public static final MoveCoordSetter TRACE_LOC_TARGET = (self, entitypatch, transformSheet) -> {
		LivingEntity attackTarget = entitypatch.getTarget();
		
		if (attackTarget != null && !self.getRealAnimation().getProperty(AttackAnimationProperty.FIXED_MOVE_DISTANCE).orElse(false)) {
			TransformSheet transform = self.getCoord().copyAll();
			Keyframe[] keyframes = transform.getKeyframes();
			int startFrame = 0; 
			int endFrame = keyframes.length - 1;
			Vec3f keyLast = keyframes[endFrame].transform().translation();
			Vec3 startpos = entitypatch.getAnimator().getAnimationVariable(ActionAnimation.BEGINNING_LOCATION);
			Vec3 targetpos = attackTarget.position();
			Vec3 toTarget = targetpos.subtract(startpos);
			Vec3 viewVec = entitypatch.getOriginal().getViewVector(1.0F);
			float horizontalDistance = Math.max((float)toTarget.horizontalDistance() - (attackTarget.getBbWidth() + entitypatch.getOriginal().getBbWidth()) * 0.3333F, 0.0F);
			Vec3f worldPosition = new Vec3f(keyLast.x, 0.0F, -horizontalDistance);
			float scale = worldPosition.length() / keyLast.length();
			
			if (scale > 1.0F) {
				float dot = (float)toTarget.normalize().dot(viewVec.normalize());
				scale = Math.max(scale * dot, 1.0F);
			}
			
			for (int i = startFrame; i <= endFrame; i++) {
				Vec3f translation = keyframes[i].transform().translation();
				
				if (translation.z < 0.0F) {
					translation.z *= scale;
				}
			}
			
			transformSheet.readFrom(transform);
		} else {
			transformSheet.readFrom(self.getCoord().copyAll());
		}
	};
	
	/**
	 * OLD: Traces the location of a target and turns the entity toward a direction facing target
	 */
	@Deprecated
	public static final MoveCoordSetter TRACE_LOCROT_TARGET = (self, entitypatch, transformSheet) -> {
		LivingEntity attackTarget = entitypatch.getTarget();
		
		if (attackTarget != null) {
			TransformSheet transform = self.getCoord().copyAll();
			Keyframe[] keyframes = transform.getKeyframes();
			int startFrame = 0; 
			int endFrame = keyframes.length - 1;
			Vec3f keyLast = keyframes[endFrame].transform().translation();
			Vec3 startpos = entitypatch.getAnimator().getAnimationVariable(ActionAnimation.BEGINNING_LOCATION);
			Vec3 targetpos = attackTarget.position();
			Vec3 toTarget = targetpos.subtract(startpos);
			float horizontalDistance = Math.max((float)toTarget.horizontalDistance() - (attackTarget.getBbWidth() + entitypatch.getOriginal().getBbWidth()) * 0.3333F, 0.0F);
			Vec3f worldPosition = new Vec3f(keyLast.x, 0.0F, -horizontalDistance);
			float scale = worldPosition.length() / keyLast.length();
			float yRot = (float)MathUtils.getYRotOfVector(toTarget);
			float clampedYRot = MathUtils.rotlerp(entitypatch.getYRot(), yRot, entitypatch.getYRotLimit());
			
			entitypatch.setYRot(clampedYRot);
			
			for (int i = startFrame; i <= endFrame; i++) {
				Vec3f translation = keyframes[i].transform().translation();
				
				if (translation.z < 0.0F) {
					translation.z *= scale;
				}
			}
			
			transformSheet.readFrom(transform);
		} else {
			transformSheet.readFrom(self.getCoord().copyAll());
		}
	};
	
	public static final MoveCoordSetter VEX_TRACE = (self, entitypatch, transformSheet) -> {
		TransformSheet transform = self.getCoord().copyAll();
		Keyframe[] keyframes = transform.getKeyframes();
		int startFrame = 0;
		int endFrame = 6;
		Vec3 pos = entitypatch.getOriginal().position();
		Vec3 targetpos = entitypatch.getTarget().position();
		float verticalDistance = (float) (targetpos.y - pos.y);
		Quaternionf rotator = Vec3f.getRotatorBetween(new Vec3f(0.0F, -verticalDistance, (float)targetpos.subtract(pos).horizontalDistance()), new Vec3f(0.0F, 0.0F, 1.0F));
		
		for (int i = startFrame; i <= endFrame; i++) {
			Vec3f translation = keyframes[i].transform().translation();
			OpenMatrix4f.transform3v(OpenMatrix4f.fromQuaternion(rotator), translation, translation);
		}
		
		transformSheet.readFrom(transform);
	};
}