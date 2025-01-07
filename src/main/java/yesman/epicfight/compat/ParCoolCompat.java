package yesman.epicfight.compat;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.function.BiFunction;

import javax.annotation.Nullable;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.alrex.parcool.ParCool;
import com.alrex.parcool.api.unstable.action.ParCoolActionEvent;
import com.alrex.parcool.client.input.KeyBindings;
import com.alrex.parcool.common.action.Action;
import com.alrex.parcool.common.action.impl.CatLeap;
import com.alrex.parcool.common.action.impl.ClimbUp;
import com.alrex.parcool.common.action.impl.ClingToCliff;
import com.alrex.parcool.common.action.impl.Crawl;
import com.alrex.parcool.common.action.impl.HangDown;
import com.alrex.parcool.common.action.impl.HangDown.BarAxis;
import com.alrex.parcool.common.action.impl.JumpFromBar;
import com.alrex.parcool.common.action.impl.Slide;
import com.alrex.parcool.common.action.impl.Vault;
import com.alrex.parcool.common.action.impl.WallJump;
import com.alrex.parcool.common.capability.IStamina;
import com.alrex.parcool.common.capability.Parkourability;
import com.alrex.parcool.config.ParCoolConfig;
import com.alrex.parcool.utilities.EntityUtil;
import com.alrex.parcool.utilities.VectorUtil;
import com.google.common.collect.Maps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationManager.AnimationRegistryEvent;
import yesman.epicfight.api.animation.AnimationVariables;
import yesman.epicfight.api.animation.AnimationVariables.IndependentAnimationVariableKey;
import yesman.epicfight.api.animation.AnimationVariables.SharedAnimationVariableKey;
import yesman.epicfight.api.animation.JointTransform;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.property.AnimationEvent;
import yesman.epicfight.api.animation.property.AnimationEvent.Side;
import yesman.epicfight.api.animation.property.AnimationEvent.SimpleEvent;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.StaticAnimationProperty;
import yesman.epicfight.api.animation.property.MoveCoordFunctions;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.animation.types.MovementAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.forgeevent.InitAnimatorEvent;
import yesman.epicfight.api.utils.TimePairList;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.Vec2i;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public class ParCoolCompat implements ICompatModule {
	public static AnimationAccessor<StaticAnimation> BIPED_CLING_TO_CLIFF;
	public static AnimationAccessor<StaticAnimation> BIPED_CLING_TO_CLIFF_LEFT;
	public static AnimationAccessor<StaticAnimation> BIPED_CLING_TO_CLIFF_RIGHT;
	public static AnimationAccessor<ActionAnimation> BIPED_WALL_JUMP_LEFT_START;
	public static AnimationAccessor<ActionAnimation> BIPED_WALL_JUMP_LEFT;
	public static AnimationAccessor<ActionAnimation> BIPED_WALL_JUMP_RIGHT_START;
	public static AnimationAccessor<ActionAnimation> BIPED_WALL_JUMP_RIGHT;
	
	public static AnimationAccessor<StaticAnimation> BIPED_DIVE;
	public static AnimationAccessor<StaticAnimation> BIPED_WALL_SLIDE_LEFT;
	public static AnimationAccessor<StaticAnimation> BIPED_WALL_SLIDE_RIGHT;
	public static AnimationAccessor<StaticAnimation> BIPED_WALL_RUN_LEFT;
	public static AnimationAccessor<StaticAnimation> BIPED_WALL_RUN_RIGHT;
	public static AnimationAccessor<StaticAnimation> BIPED_FAST_RUN;
	public static AnimationAccessor<StaticAnimation> BIPED_CAT_LEAP;
	public static AnimationAccessor<StaticAnimation> BIPED_HANG_DOWN;
	public static AnimationAccessor<StaticAnimation> BIPED_HANG_DOWN_ORTHOGONAL;
	public static AnimationAccessor<StaticAnimation> BIPED_JUMP_FROM_BAR;
	public static AnimationAccessor<StaticAnimation> BIPED_SLIDE;
	public static AnimationAccessor<MovementAnimation> BIPED_CRAWL;
	
	public static AnimationAccessor<ActionAnimation> BIPED_CLING_START;
	public static AnimationAccessor<ActionAnimation> BIPED_CLING_MOVE_LEFT;
	public static AnimationAccessor<ActionAnimation> BIPED_CLING_MOVE_LEFT_CORNER1;
	public static AnimationAccessor<ActionAnimation> BIPED_CLING_MOVE_LEFT_CORNER2;
	public static AnimationAccessor<ActionAnimation> BIPED_CLING_MOVE_RIGHT;
	public static AnimationAccessor<ActionAnimation> BIPED_CLING_MOVE_RIGHT_CORNER1;
	public static AnimationAccessor<ActionAnimation> BIPED_CLING_MOVE_RIGHT_CORNER2;
	
	public static AnimationAccessor<ActionAnimation> BIPED_CLIMB_UP;
	public static AnimationAccessor<ActionAnimation> BIPED_VAULT_FORWARD;
	public static AnimationAccessor<ActionAnimation> BIPED_VAULT_LEFT;
	public static AnimationAccessor<ActionAnimation> BIPED_VAULT_RIGHT;
	public static AnimationAccessor<ActionAnimation> BIPED_ROLL_LEFT;
	public static AnimationAccessor<ActionAnimation> BIPED_ROLL_RIGHT;
	public static AnimationAccessor<ActionAnimation> BIPED_HANG_DOWN_INERTIA_ORTHOGONAL;
	public static AnimationAccessor<ActionAnimation> BIPED_HANG_DOWN_INERTIA;
	public static AnimationAccessor<ActionAnimation> BIPED_JUMP_FROM_BAR_START_ORTHOGONAL;
	public static AnimationAccessor<ActionAnimation> BIPED_JUMP_FROM_BAR_START;
	public static AnimationAccessor<ActionAnimation> BIPED_HANG_DOWN_MOVE_FORWARD_START;
	public static AnimationAccessor<ActionAnimation> BIPED_HANG_DOWN_MOVE_FORWARD_CROSS1;
	public static AnimationAccessor<ActionAnimation> BIPED_HANG_DOWN_MOVE_FORWARD_CROSS2;
	public static AnimationAccessor<ActionAnimation> BIPED_HANG_DOWN_MOVE_FORWARD_END1;
	public static AnimationAccessor<ActionAnimation> BIPED_HANG_DOWN_MOVE_FORWARD_END2;
	public static AnimationAccessor<ActionAnimation> BIPED_HANG_DOWN_MOVE_BACKWARD;
	public static AnimationAccessor<ActionAnimation> BIPED_HANG_DOWN_MOVE_LEFT;
	public static AnimationAccessor<ActionAnimation> BIPED_HANG_DOWN_MOVE_RIGHT;
	
	public static final Map<Class<? extends com.alrex.parcool.common.action.Action>, BiFunction<PlayerPatch<?>, ParCoolActionEvent.StartEvent, AssetAccessor<? extends StaticAnimation>>> PARCOOL_ACTION_MAPPING = Maps.newHashMap();
	public static final Map<Class<? extends com.alrex.parcool.common.action.Action>, BiFunction<PlayerPatch<?>, Action, Boolean>> PARCOOL_ACTION_CANCEL_EVENTS = Maps.newHashMap();
	
	private static final ByteBuffer DUMMY_BUFFER = ByteBuffer.allocate(128);
	
	public static final IndependentAnimationVariableKey<Vec3> JUMP_DIRECTION = AnimationVariables.independent(() -> new Vec3(0.0D, 0.0D, 0.0D), true);
	public static final IndependentAnimationVariableKey<Vec3> WALL_DIRECTION = AnimationVariables.independent(() -> new Vec3(0.0D, 0.0D, 0.0D), true);
	
	public static final SharedAnimationVariableKey<Float> CLIFF_Y_ROT = AnimationVariables.shared(() -> 0.0F, true);
	public static final IndependentAnimationVariableKey<Float> CLIFF_START_Y_ROT = AnimationVariables.independent(() -> 0.0F, true);
	public static final IndependentAnimationVariableKey<Float> CLIFF_DEST_Y_ROT = AnimationVariables.independent(() -> 0.0F, true);
	public static final IndependentAnimationVariableKey<Vec3> CLING_DESTINATION = AnimationVariables.independent(() -> null, true);
	
	public static void registerAnimations(AnimationRegistryEvent event) {
		event.startsWith(ParCool.MOD_ID);
		
		BIPED_CLING_TO_CLIFF = event.nextAccessor("biped/cling_to_cliff", (accessor) ->
			new StaticAnimation(true, accessor, Armatures.BIPED)
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addProperty(StaticAnimationProperty.ON_ITEM_UPDATE_EVENT, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK_WHEN_ITEM_CHANGED, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT))
				.addEvents(SimpleEvent.create((entitypatch, animation, params) -> {
					entitypatch.setYRot(entitypatch.getAnimator().getVariables().getSharedVariable(CLIFF_Y_ROT));
				}, Side.LOCAL_CLIENT))
				.newTimePair(0.0F, Float.MAX_VALUE)
				.addStateRemoveOld(EntityState.TURNING_LOCKED, true));
		
		BIPED_CLING_TO_CLIFF_LEFT = event.nextAccessor("biped/cling_to_cliff_left", (accessor) ->
			new StaticAnimation(true, accessor, Armatures.BIPED)
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addProperty(StaticAnimationProperty.ON_ITEM_UPDATE_EVENT, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK_WHEN_ITEM_CHANGED, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));
		
		BIPED_CLING_TO_CLIFF_RIGHT = event.nextAccessor("biped/cling_to_cliff_right", (accessor) ->
			new StaticAnimation(true, accessor, Armatures.BIPED)
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addProperty(StaticAnimationProperty.ON_ITEM_UPDATE_EVENT, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK_WHEN_ITEM_CHANGED, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));
		
		BIPED_WALL_JUMP_LEFT_START = event.nextAccessor("biped/wall_jump_left_start", (accessor) ->
			new ActionAnimation(0.05F, accessor, Armatures.BIPED)
				.addProperty(ActionAnimationProperty.REMOVE_DELTA_MOVEMENT, true)
				.addProperty(ActionAnimationProperty.NO_GRAVITY_TIME, TimePairList.create(-1.0F, 10.0F))
				.addEvents(SimpleEvent.create((entitypatch, animation, param) -> {
					KeyBindings.getKeyGrabWall().setDown(false);
				}, Side.LOCAL_CLIENT))
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS,
					SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT),
					SimpleEvent.create(ParcoolUtils.WALL_JUMP, Side.CLIENT)));
		
		BIPED_WALL_JUMP_LEFT = event.nextAccessor("biped/wall_jump_left", (accessor) ->
			new ActionAnimation(0.15F, 0.7F, accessor, Armatures.BIPED)
				.addStateRemoveOld(EntityState.MOVEMENT_LOCKED, false)
				.addStateRemoveOld(EntityState.TURNING_LOCKED, false)
				.addEvents(SimpleEvent.create((entitypatch, animation, param) -> {
					KeyBindings.getKeyGrabWall().setDown(false);
				}, Side.LOCAL_CLIENT)));
		
		BIPED_WALL_JUMP_RIGHT_START = event.nextAccessor("biped/wall_jump_right_start", (accessor) ->
			new ActionAnimation(0.05F, accessor, Armatures.BIPED)
				.addProperty(ActionAnimationProperty.REMOVE_DELTA_MOVEMENT, true)
				.addProperty(ActionAnimationProperty.NO_GRAVITY_TIME, TimePairList.create(-1.0F, 10.0F))
				.addEvents(SimpleEvent.create((entitypatch, animation, param) -> {
					KeyBindings.getKeyGrabWall().setDown(false);
				}, Side.LOCAL_CLIENT))
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS,
					SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT),
					SimpleEvent.create(ParcoolUtils.WALL_JUMP, Side.CLIENT)
				));
		
		BIPED_WALL_JUMP_RIGHT = event.nextAccessor("biped/wall_jump_right", (accessor) ->
			new ActionAnimation(0.15F, 0.7F, accessor, Armatures.BIPED)
				.addStateRemoveOld(EntityState.MOVEMENT_LOCKED, false)
				.addStateRemoveOld(EntityState.TURNING_LOCKED, false)
				.addEvents(SimpleEvent.create((entitypatch, animation, param) -> {
					KeyBindings.getKeyGrabWall().setDown(false);
				}, Side.LOCAL_CLIENT)));
		
		BIPED_DIVE = event.nextAccessor("biped/dive", (accessor) ->
			new StaticAnimation(true, accessor, Armatures.BIPED).addProperty(StaticAnimationProperty.POSE_MODIFIER,
				new AnimationProperty.PoseModifier() {
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
				})
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addProperty(StaticAnimationProperty.ON_ITEM_UPDATE_EVENT, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK_WHEN_ITEM_CHANGED, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));
		
		BIPED_WALL_SLIDE_LEFT = event.nextAccessor("biped/wall_slide_left", (accessor) ->
			new StaticAnimation(true, accessor, Armatures.BIPED)
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addProperty(StaticAnimationProperty.ON_ITEM_UPDATE_EVENT, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK_WHEN_ITEM_CHANGED, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));
		
		BIPED_WALL_SLIDE_RIGHT = event.nextAccessor("biped/wall_slide_right", (accessor) ->
			new StaticAnimation(true, accessor, Armatures.BIPED)
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addProperty(StaticAnimationProperty.ON_ITEM_UPDATE_EVENT, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK_WHEN_ITEM_CHANGED, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));
		
		BIPED_WALL_RUN_LEFT = event.nextAccessor("biped/wall_run_left", (accessor) ->
			new StaticAnimation(true, accessor, Armatures.BIPED)
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addProperty(StaticAnimationProperty.ON_ITEM_UPDATE_EVENT, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK_WHEN_ITEM_CHANGED, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));
		
		BIPED_WALL_RUN_RIGHT = event.nextAccessor("biped/wall_run_right", (accessor) ->
			new StaticAnimation(true, accessor, Armatures.BIPED)
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addProperty(StaticAnimationProperty.ON_ITEM_UPDATE_EVENT, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK_WHEN_ITEM_CHANGED, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));
		
		BIPED_FAST_RUN = event.nextAccessor("biped/fast_run", (accessor) ->
			new StaticAnimation(true, accessor, Armatures.BIPED)
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addProperty(StaticAnimationProperty.ON_ITEM_UPDATE_EVENT, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK_WHEN_ITEM_CHANGED, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));
		
		BIPED_CAT_LEAP = event.nextAccessor("biped/cat_leap", (accessor) ->
			new StaticAnimation(0.05F, false, accessor, Armatures.BIPED)
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addProperty(StaticAnimationProperty.ON_ITEM_UPDATE_EVENT, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK_WHEN_ITEM_CHANGED, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT))
				.newTimePair(0.0F, Float.MAX_VALUE)
				.addState(EntityState.UPDATE_LIVING_MOTION, false));
		
		BIPED_HANG_DOWN = event.nextAccessor("biped/hang_down", (accessor) ->
			new StaticAnimation(true, accessor, Armatures.BIPED)
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addProperty(StaticAnimationProperty.ON_ITEM_UPDATE_EVENT, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK_WHEN_ITEM_CHANGED, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));
		
		BIPED_HANG_DOWN_ORTHOGONAL = event.nextAccessor("biped/hang_down_orthogonal", (accessor) ->
			new StaticAnimation(true, accessor, Armatures.BIPED)
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addProperty(StaticAnimationProperty.ON_ITEM_UPDATE_EVENT, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK_WHEN_ITEM_CHANGED, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));
		
		BIPED_JUMP_FROM_BAR = event.nextAccessor("biped/jump_from_bar", (accessor) ->
			new StaticAnimation(false, accessor, Armatures.BIPED)
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addProperty(StaticAnimationProperty.ON_ITEM_UPDATE_EVENT, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK_WHEN_ITEM_CHANGED, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT))
				.addEvents(SimpleEvent.create((entitypatch, animation, param) -> {
					KeyBindings.getKeyHangDown().setDown(false);
				}, Side.LOCAL_CLIENT)));
		
		BIPED_SLIDE = event.nextAccessor("biped/slide", (accessor) ->
			new StaticAnimation(true, accessor, Armatures.BIPED)
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addProperty(StaticAnimationProperty.ON_ITEM_UPDATE_EVENT, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK_WHEN_ITEM_CHANGED, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));
		
		BIPED_CRAWL = event.nextAccessor("biped/crawl", (accessor) ->
			new MovementAnimation(true, accessor, Armatures.BIPED)
				.addProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER, (self, entitypatch, speed, prevElapsedTime, elapsedTime) -> {
					return speed;
				}));
		
		BIPED_CLING_START = event.nextAccessor("biped/cling_start", (accessor) ->
			new ActionAnimation(0.05F, accessor, Armatures.BIPED)
				.addProperty(ActionAnimationProperty.COORD_SET_BEGIN, MoveCoordFunctions.TRACE_ORIGIN_AS_DESTINATION)
				.addProperty(ActionAnimationProperty.COORD_SET_TICK, null)
				.addProperty(ActionAnimationProperty.COORD_GET, MoveCoordFunctions.WORLD_COORD)
				.addProperty(ActionAnimationProperty.COORD_DEST_KEYFRAME_INDEX, 1)
				.addProperty(ActionAnimationProperty.DEST_COORD_YROT_PROVIDER, (self, entitypatch) -> {
					return entitypatch.getAnimator().getVariables().getSharedVariable(CLIFF_Y_ROT);
				})
				.addProperty(ActionAnimationProperty.DEST_LOCATION_PROVIDER, (self, entitypatch) -> {
					return entitypatch.getAnimator().getVariables().get(CLING_DESTINATION, BIPED_CLING_START);
				})
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));
		
		BIPED_CLING_MOVE_LEFT = event.nextAccessor("biped/cling_move_left", (accessor) ->
			new ActionAnimation(0.1F, accessor, Armatures.BIPED)
				.addProperty(ActionAnimationProperty.REMOVE_DELTA_MOVEMENT, true)
				.addProperty(ActionAnimationProperty.NO_GRAVITY_TIME, TimePairList.create(-1.0F, 10.0F))
				.addProperty(ActionAnimationProperty.COORD_SET_BEGIN, MoveCoordFunctions.TRACE_TARGET_DISTANCE)
				.addProperty(ActionAnimationProperty.COORD_SET_TICK, null)
				.addProperty(ActionAnimationProperty.DEST_LOCATION_PROVIDER, (self, entitypatch) -> {
					return entitypatch.getAnimator().getVariables().getOrDefault(CLING_DESTINATION, BIPED_CLING_MOVE_LEFT);
				})
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS,
					SimpleEvent.create(Animations.ReusableSources.PLAY_SOUND, AnimationEvent.Side.CLIENT).params(com.alrex.parcool.api.SoundEvents.CLING_TO_CLIFF.get()),
					SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT)
				)
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));
		
		BIPED_CLING_MOVE_LEFT_CORNER1 = event.nextAccessor("biped/cling_move_left_corner1", (accessor) ->
			new ActionAnimation(0.1F, accessor, Armatures.BIPED)
				.addProperty(ActionAnimationProperty.COORD_SET_BEGIN, MoveCoordFunctions.TRACE_ORIGIN_AS_DESTINATION)
				.addProperty(ActionAnimationProperty.COORD_SET_TICK, null)
				.addProperty(ActionAnimationProperty.COORD_GET, MoveCoordFunctions.WORLD_COORD)
				.addProperty(ActionAnimationProperty.COORD_DEST_KEYFRAME_INDEX, 2)
				.addProperty(ActionAnimationProperty.ENTITY_YROT_PROVIDER, (self, entitypatch) -> {
					if (self.isLinkAnimation()) {
						return entitypatch.getYRot();
					}
					
					entitypatch.getAnimator().getPlayerFor(self.getAccessor()).getElapsedTime();
					
					return 0.0F;
				})
				.addProperty(ActionAnimationProperty.DEST_COORD_YROT_PROVIDER, (self, entitypatch) -> {
					return entitypatch.getAnimator().getVariables().get(CLIFF_DEST_Y_ROT, BIPED_CLING_MOVE_LEFT_CORNER1);
				})
				.addProperty(ActionAnimationProperty.DEST_LOCATION_PROVIDER, (self, entitypatch) -> {
					return entitypatch.getAnimator().getVariables().get(CLING_DESTINATION, BIPED_CLING_MOVE_LEFT_CORNER1);
				})
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));
		
		BIPED_CLING_MOVE_LEFT_CORNER2 = event.nextAccessor("biped/cling_move_left_corner2", (accessor) ->
			new ActionAnimation(0.1F, accessor, Armatures.BIPED)
				.addProperty(ActionAnimationProperty.COORD_SET_BEGIN, MoveCoordFunctions.TRACE_ORIGIN_AS_DESTINATION)
				.addProperty(ActionAnimationProperty.COORD_SET_TICK, null)
				.addProperty(ActionAnimationProperty.COORD_GET, MoveCoordFunctions.WORLD_COORD)
				.addProperty(ActionAnimationProperty.COORD_DEST_KEYFRAME_INDEX, 2)
				.addProperty(ActionAnimationProperty.DEST_COORD_YROT_PROVIDER, (self, entitypatch) -> {
					return entitypatch.getAnimator().getVariables().get(CLIFF_DEST_Y_ROT, BIPED_CLING_MOVE_LEFT_CORNER1);
				})
				.addProperty(ActionAnimationProperty.DEST_LOCATION_PROVIDER, (self, entitypatch) -> {
					return entitypatch.getAnimator().getVariables().get(CLING_DESTINATION, BIPED_CLING_MOVE_LEFT_CORNER2);
				})
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));
		
		BIPED_CLING_MOVE_RIGHT = event.nextAccessor("biped/cling_move_right", (accessor) ->
			new ActionAnimation(0.1F, accessor, Armatures.BIPED)
				.addProperty(ActionAnimationProperty.REMOVE_DELTA_MOVEMENT, true)
				.addProperty(ActionAnimationProperty.NO_GRAVITY_TIME, TimePairList.create(-1.0F, 10.0F))
				.addProperty(ActionAnimationProperty.COORD_SET_BEGIN, MoveCoordFunctions.TRACE_TARGET_DISTANCE)
				.addProperty(ActionAnimationProperty.COORD_SET_TICK, null)
				.addProperty(ActionAnimationProperty.DEST_LOCATION_PROVIDER, (self, entitypatch) -> {
					return entitypatch.getAnimator().getVariables().getOrDefault(CLING_DESTINATION, BIPED_CLING_MOVE_RIGHT);
				})
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS,
					SimpleEvent.create(Animations.ReusableSources.PLAY_SOUND, AnimationEvent.Side.CLIENT).params(com.alrex.parcool.api.SoundEvents.CLING_TO_CLIFF.get()),
					SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT)
				)
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));
		
		BIPED_CLING_MOVE_RIGHT_CORNER1 = event.nextAccessor("biped/cling_move_right_corner1", (accessor) ->
			new ActionAnimation(0.05F, accessor, Armatures.BIPED)
				.addProperty(ActionAnimationProperty.COORD_SET_BEGIN, MoveCoordFunctions.TRACE_ORIGIN_AS_DESTINATION)
				.addProperty(ActionAnimationProperty.COORD_SET_TICK, null)
				.addProperty(ActionAnimationProperty.COORD_GET, MoveCoordFunctions.WORLD_COORD)
				.addProperty(ActionAnimationProperty.COORD_DEST_KEYFRAME_INDEX, 1)
				.addProperty(ActionAnimationProperty.ENTITY_YROT_PROVIDER, (self, entitypatch) -> {
					if (self.isLinkAnimation()) {
						return entitypatch.getYRot();
					}
					
					Quaternionf qInitRot = self.getCoord().getInterpolatedRotation(0.0F);
					Quaternionf qRot = self.getCoord().getInterpolatedRotation(entitypatch.getAnimator().getPlayerFor(self.getAccessor()).getElapsedTime());
					Vector3f initAngles = qInitRot.getEulerAnglesXYZ(new Vector3f());
					Vector3f angles = qRot.getEulerAnglesXYZ(new Vector3f());
					float initRot = entitypatch.getAnimator().getVariables().get(CLIFF_START_Y_ROT, self.getRealAnimation());
					
					return initRot + (float)(Math.toDegrees(angles.y) - Math.toDegrees(initAngles.y));
				})
				.addProperty(ActionAnimationProperty.DEST_COORD_YROT_PROVIDER, (self, entitypatch) -> {
					return entitypatch.getAnimator().getVariables().get(CLIFF_DEST_Y_ROT, BIPED_CLING_MOVE_RIGHT_CORNER1);
				})
				.addProperty(ActionAnimationProperty.DEST_LOCATION_PROVIDER, (self, entitypatch) -> {
					return entitypatch.getAnimator().getVariables().get(CLING_DESTINATION, BIPED_CLING_MOVE_RIGHT_CORNER1);
				})
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));
		
		BIPED_CLING_MOVE_RIGHT_CORNER2 = event.nextAccessor("biped/cling_move_right_corner2", (accessor) ->
			new ActionAnimation(0.05F, accessor, Armatures.BIPED)
				.addProperty(ActionAnimationProperty.COORD_SET_BEGIN, MoveCoordFunctions.TRACE_ORIGIN_AS_DESTINATION)
				.addProperty(ActionAnimationProperty.COORD_SET_TICK, null)
				.addProperty(ActionAnimationProperty.COORD_GET, MoveCoordFunctions.WORLD_COORD)
				.addProperty(ActionAnimationProperty.COORD_DEST_KEYFRAME_INDEX, 2) // Fix this to 1
				.addProperty(ActionAnimationProperty.DEST_COORD_YROT_PROVIDER, (self, entitypatch) -> {
					return entitypatch.getAnimator().getVariables().get(CLIFF_DEST_Y_ROT, BIPED_CLING_MOVE_RIGHT_CORNER2);
				})
				.addProperty(ActionAnimationProperty.DEST_LOCATION_PROVIDER, (self, entitypatch) -> {
					return entitypatch.getAnimator().getVariables().get(CLING_DESTINATION, BIPED_CLING_MOVE_RIGHT_CORNER2);
				})
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));
		
		BIPED_CLIMB_UP = event.nextAccessor("biped/climb_up", (accessor) ->
			new ActionAnimation(0.15F, accessor, Armatures.BIPED)
				.addProperty(ActionAnimationProperty.MOVE_VERTICAL, true)
				.addProperty(ActionAnimationProperty.STOP_MOVEMENT, true)
				.addProperty(ActionAnimationProperty.REMOVE_DELTA_MOVEMENT, true)
				.addProperty(ActionAnimationProperty.MOVE_ON_LINK, false)
				.addProperty(ActionAnimationProperty.COORD_SET_BEGIN, MoveCoordFunctions.TRACE_ORIGIN_AS_DESTINATION)
				.addProperty(ActionAnimationProperty.COORD_SET_TICK, null)
				.addProperty(ActionAnimationProperty.COORD_GET, MoveCoordFunctions.WORLD_COORD)
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create((entitypatch, animation, params) -> {
					entitypatch.setYRot(entitypatch.getOriginal().yBodyRot);
				}, AnimationEvent.Side.CLIENT), SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT))
				.addEvents(SimpleEvent.create((entitypatch, animation, param) -> {
					KeyBindings.getKeyGrabWall().setDown(false);
				}, Side.LOCAL_CLIENT)));
		
		BIPED_VAULT_FORWARD = event.nextAccessor("biped/vault_forward", (accessor) ->
			new ActionAnimation(0.1F, 0.3F, accessor, Armatures.BIPED)
				.addProperty(ActionAnimationProperty.MOVE_VERTICAL, true)
				.addProperty(ActionAnimationProperty.COORD_SET_BEGIN, MoveCoordFunctions.TRACE_ORIGIN_AS_DESTINATION)
				.addProperty(ActionAnimationProperty.COORD_SET_TICK, null)
				.addProperty(ActionAnimationProperty.COORD_GET, MoveCoordFunctions.WORLD_COORD)
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));
		
		BIPED_VAULT_LEFT = event.nextAccessor("biped/vault_left", (accessor) ->
			new ActionAnimation(0.1F, 0.3F, accessor, Armatures.BIPED)
				.addProperty(ActionAnimationProperty.MOVE_VERTICAL, true)
				.addProperty(ActionAnimationProperty.COORD_SET_BEGIN, MoveCoordFunctions.TRACE_ORIGIN_AS_DESTINATION)
				.addProperty(ActionAnimationProperty.COORD_SET_TICK, null)
				.addProperty(ActionAnimationProperty.COORD_GET, MoveCoordFunctions.WORLD_COORD)
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));
		
		BIPED_VAULT_RIGHT = event.nextAccessor("biped/vault_right", (accessor) ->
			new ActionAnimation(0.1F, 0.3F, accessor, Armatures.BIPED)
				.addProperty(ActionAnimationProperty.MOVE_VERTICAL, true)
				.addProperty(ActionAnimationProperty.COORD_SET_BEGIN, MoveCoordFunctions.TRACE_ORIGIN_AS_DESTINATION)
				.addProperty(ActionAnimationProperty.COORD_SET_TICK, null)
				.addProperty(ActionAnimationProperty.COORD_GET, MoveCoordFunctions.WORLD_COORD)
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));
		
		BIPED_HANG_DOWN_INERTIA = event.nextAccessor("biped/hang_down_inertia", (accessor) ->
			new ActionAnimation(0.05F, 1.35F, accessor, Armatures.BIPED)
				.addProperty(ActionAnimationProperty.COORD_SET_BEGIN, MoveCoordFunctions.TRACE_ORIGIN_AS_DESTINATION)
				.addProperty(ActionAnimationProperty.COORD_SET_TICK, null)
				.addProperty(ActionAnimationProperty.COORD_GET, MoveCoordFunctions.WORLD_COORD)
				.addProperty(ActionAnimationProperty.COORD_DEST_KEYFRAME_INDEX, 2)
				.addProperty(ActionAnimationProperty.DEST_LOCATION_PROVIDER, (self, entitypatch) -> {
					return ParcoolUtils.getHangableBars(entitypatch.getOriginal()); 
				})
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));
		
		BIPED_HANG_DOWN_INERTIA_ORTHOGONAL = event.nextAccessor("biped/hang_down_inertia_orthogonal", (accessor) ->
			new ActionAnimation(0.05F, 1.35F, accessor, Armatures.BIPED)
				.addProperty(ActionAnimationProperty.COORD_SET_BEGIN, MoveCoordFunctions.TRACE_ORIGIN_AS_DESTINATION)
				.addProperty(ActionAnimationProperty.COORD_SET_TICK, null)
				.addProperty(ActionAnimationProperty.COORD_GET, MoveCoordFunctions.WORLD_COORD)
				.addProperty(ActionAnimationProperty.COORD_DEST_KEYFRAME_INDEX, 2)
				.addProperty(ActionAnimationProperty.DEST_LOCATION_PROVIDER, (self, entitypatch) -> {
					Vec3 dest = ParcoolUtils.getHangableBars(entitypatch.getOriginal()); 
					
					if (dest != null) {
						return dest;
					} else {
						return null;
					}
				})
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));
		
		BIPED_JUMP_FROM_BAR_START_ORTHOGONAL = event.nextAccessor("biped/jump_from_bar_start_orthogonal", (accessor) ->
			new ActionAnimation(0.05F, accessor, Armatures.BIPED)
				.addProperty(ActionAnimationProperty.REMOVE_DELTA_MOVEMENT, true)
				.addProperty(ActionAnimationProperty.NO_GRAVITY_TIME, TimePairList.create(-1.0F, 10.0F))
				.addEvents(SimpleEvent.create((entitypatch, animation, param) -> {
					KeyBindings.getKeyHangDown().setDown(false);
				}, Side.LOCAL_CLIENT))
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS,
					SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT),
					SimpleEvent.create((entitypatch, animation, param) -> {
						if (entitypatch instanceof AbstractClientPlayerPatch<?> playerpatch && playerpatch.getOriginal().isLocalPlayer()) {
							EntityUtil.addVelocity(entitypatch.getOriginal(), entitypatch.getOriginal().getLookAngle().multiply(1, 0, 1).normalize().scale(entitypatch.getOriginal().getBbWidth() * 0.75));
						}
						
						if (ParCoolConfig.Client.Booleans.EnableActionSounds.get()) {
							entitypatch.getOriginal().playSound(com.alrex.parcool.api.SoundEvents.HANG_DOWN_JUMP.get(), 1f, 1f);
						}
						
						entitypatch.getAnimator().reserveAnimation(BIPED_JUMP_FROM_BAR);
					}
				, Side.CLIENT)));
		
		BIPED_JUMP_FROM_BAR_START = event.nextAccessor("biped/jump_from_bar_start", (accessor) ->
			new ActionAnimation(0.05F, accessor, Armatures.BIPED)
				.addProperty(ActionAnimationProperty.REMOVE_DELTA_MOVEMENT, true)
				.addProperty(ActionAnimationProperty.NO_GRAVITY_TIME, TimePairList.create(-1.0F, 10.0F))
				.addEvents(SimpleEvent.create((entitypatch, animation, param) -> {
					KeyBindings.getKeyHangDown().setDown(false);
				}, Side.LOCAL_CLIENT))
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS,
					SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT),
					SimpleEvent.create((entitypatch, animation, param) -> {
						if (entitypatch.isLogicalClient()) {
							if (entitypatch instanceof AbstractClientPlayerPatch<?> playerpatch && playerpatch.getOriginal().isLocalPlayer()) {
								EntityUtil.addVelocity(entitypatch.getOriginal(), entitypatch.getOriginal().getLookAngle().multiply(1, 0, 1).normalize().scale(entitypatch.getOriginal().getBbWidth() * 0.75));
							}
							
							if (ParCoolConfig.Client.Booleans.EnableActionSounds.get()) {
								entitypatch.getOriginal().playSound(com.alrex.parcool.api.SoundEvents.HANG_DOWN_JUMP.get(), 1f, 1f);
							}
							
							entitypatch.getAnimator().reserveAnimation(BIPED_JUMP_FROM_BAR);
						}
					}
				, Side.CLIENT)));
		
		BIPED_HANG_DOWN_MOVE_FORWARD_START = event.nextAccessor("biped/hang_down_move_start", (accessor) ->
			new ActionAnimation(0.05F, accessor, Armatures.BIPED)
				.addProperty(ActionAnimationProperty.REMOVE_DELTA_MOVEMENT, true)
				.addProperty(ActionAnimationProperty.NO_GRAVITY_TIME, TimePairList.create(-1.0F, 10.0F))
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.PLAY_SOUND, AnimationEvent.Side.CLIENT).params(SoundEvents.CHAIN_STEP), SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, AnimationEvent.SimpleEvent.<AnimationEvent.E1<Boolean>>create((entitypatch, animation, params) -> {
					if (params.first() && entitypatch instanceof PlayerPatch<?> playerpatch && playerpatch.isBattleMode()) {
						if (KeyBindings.getKeyForward().isDown() && KeyBindings.getKeyHangDown().isDown()) {
							playerpatch.reserveAnimation(BIPED_HANG_DOWN_MOVE_FORWARD_CROSS1);
						} else {
							playerpatch.reserveAnimation(BIPED_HANG_DOWN_MOVE_FORWARD_END1);
						}
					}
				}, Side.LOCAL_CLIENT), SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));
		
		BIPED_HANG_DOWN_MOVE_FORWARD_CROSS1 = event.nextAccessor("biped/hang_down_move_cross1", (accessor) ->
			new ActionAnimation(0.05F, accessor, Armatures.BIPED)
				.addProperty(ActionAnimationProperty.REMOVE_DELTA_MOVEMENT, true)
				.addProperty(ActionAnimationProperty.NO_GRAVITY_TIME, TimePairList.create(-1.0F, 10.0F))
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.PLAY_SOUND, AnimationEvent.Side.CLIENT).params(SoundEvents.CHAIN_STEP), SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, AnimationEvent.SimpleEvent.<AnimationEvent.E1<Boolean>>create((entitypatch, animation, params) -> {
					if (params.first() && entitypatch instanceof PlayerPatch<?> playerpatch && playerpatch.isBattleMode()) {
						if (KeyBindings.getKeyForward().isDown() && KeyBindings.getKeyHangDown().isDown()) {
							playerpatch.reserveAnimation(BIPED_HANG_DOWN_MOVE_FORWARD_CROSS2);
						} else {
							playerpatch.reserveAnimation(BIPED_HANG_DOWN_MOVE_FORWARD_END2);
						}
					}
				}, Side.LOCAL_CLIENT), SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));
		
		BIPED_HANG_DOWN_MOVE_FORWARD_CROSS2 = event.nextAccessor("biped/hang_down_move_cross2", (accessor) ->
			new ActionAnimation(0.05F, accessor, Armatures.BIPED)
				.addProperty(ActionAnimationProperty.REMOVE_DELTA_MOVEMENT, true)
				.addProperty(ActionAnimationProperty.NO_GRAVITY_TIME, TimePairList.create(-1.0F, 10.0F))
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.PLAY_SOUND, AnimationEvent.Side.CLIENT).params(SoundEvents.CHAIN_STEP), SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, AnimationEvent.SimpleEvent.<AnimationEvent.E1<Boolean>>create((entitypatch, animation, params) -> {
					if (params.first() && entitypatch instanceof PlayerPatch<?> playerpatch && playerpatch.isBattleMode()) {
						if (KeyBindings.getKeyForward().isDown() && KeyBindings.getKeyHangDown().isDown()) {
							playerpatch.reserveAnimation(BIPED_HANG_DOWN_MOVE_FORWARD_CROSS1);
						} else {
							playerpatch.reserveAnimation(BIPED_HANG_DOWN_MOVE_FORWARD_END1);
						}
					}
				}, Side.LOCAL_CLIENT), SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));
		
		BIPED_HANG_DOWN_MOVE_FORWARD_END1 = event.nextAccessor("biped/hang_down_move_end1", (accessor) ->
			new ActionAnimation(0.05F, accessor, Armatures.BIPED)
				.addProperty(ActionAnimationProperty.REMOVE_DELTA_MOVEMENT, true)
				.addProperty(ActionAnimationProperty.NO_GRAVITY_TIME, TimePairList.create(-1.0F, 10.0F))
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.PLAY_SOUND, AnimationEvent.Side.CLIENT).params(SoundEvents.CHAIN_STEP), SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));
		
		BIPED_HANG_DOWN_MOVE_FORWARD_END2 = event.nextAccessor("biped/hang_down_move_end2", (accessor) ->
			new ActionAnimation(0.05F, accessor, Armatures.BIPED)
				.addProperty(ActionAnimationProperty.REMOVE_DELTA_MOVEMENT, true)
				.addProperty(ActionAnimationProperty.NO_GRAVITY_TIME, TimePairList.create(-1.0F, 10.0F))
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.PLAY_SOUND, AnimationEvent.Side.CLIENT).params(SoundEvents.CHAIN_STEP), SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));
		
		BIPED_HANG_DOWN_MOVE_BACKWARD = event.nextAccessor("biped/hang_down_move_backward", (accessor) ->
			new ActionAnimation(0.15F, 0.6F, accessor, Armatures.BIPED)
				.addProperty(ActionAnimationProperty.REMOVE_DELTA_MOVEMENT, true)
				.addProperty(ActionAnimationProperty.NO_GRAVITY_TIME, TimePairList.create(-1.0F, 10.0F))
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.PLAY_SOUND, AnimationEvent.Side.CLIENT).params(SoundEvents.CHAIN_STEP), SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));
		
		BIPED_HANG_DOWN_MOVE_LEFT = event.nextAccessor("biped/hang_down_move_left", (accessor) ->
			new ActionAnimation(0.15F, 0.45F, accessor, Armatures.BIPED)
				.addProperty(ActionAnimationProperty.REMOVE_DELTA_MOVEMENT, true)
				.addProperty(ActionAnimationProperty.NO_GRAVITY_TIME, TimePairList.create(-1.0F, 10.0F))
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.PLAY_SOUND, AnimationEvent.Side.CLIENT).params(SoundEvents.CHAIN_STEP), SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));
		
		BIPED_HANG_DOWN_MOVE_RIGHT = event.nextAccessor("biped/hang_down_move_right", (accessor) ->
			new ActionAnimation(0.15F, 0.45F, accessor, Armatures.BIPED)
				.addProperty(ActionAnimationProperty.REMOVE_DELTA_MOVEMENT, true)
				.addProperty(ActionAnimationProperty.NO_GRAVITY_TIME, TimePairList.create(-1.0F, 10.0F))
				.addEvents(StaticAnimationProperty.ON_BEGIN_EVENTS, SimpleEvent.create(Animations.ReusableSources.PLAY_SOUND, AnimationEvent.Side.CLIENT).params(SoundEvents.CHAIN_STEP), SimpleEvent.create(Animations.ReusableSources.SET_TOOLS_BACK, Side.CLIENT))
				.addEvents(StaticAnimationProperty.ON_END_EVENTS, SimpleEvent.create(Animations.ReusableSources.REVERT_TO_HANDS, Side.CLIENT)));
		
		PARCOOL_ACTION_MAPPING.clear();
		PARCOOL_ACTION_CANCEL_EVENTS.clear();
		
		PARCOOL_ACTION_MAPPING.put(ClimbUp.class, (playerpatch, startEvent) -> BIPED_CLIMB_UP);
		PARCOOL_ACTION_MAPPING.put(CatLeap.class, (playerpatch, startEvent) -> {
			Parkourability parkourability = Parkourability.get(startEvent.getPlayer());
			
			if (parkourability.get(Slide.class).isDoing() || parkourability.get(Crawl.class).isDoing()) {
				return null;
			}
			
			return BIPED_CAT_LEAP;
		});
		
		PARCOOL_ACTION_MAPPING.put(HangDown.class, (playerpatch, startEvent) -> {
			HangDown action = ((HangDown)startEvent.getAction());
			float yRot = ParcoolUtils.idealYRotForHanging(action, startEvent.getPlayer());
			
			if (action.isOrthogonalToBar()) {
				playerpatch.setModelYRot(yRot, true);
				return BIPED_HANG_DOWN_INERTIA_ORTHOGONAL;
			} else {
				playerpatch.setModelYRot(yRot, true);
				return BIPED_HANG_DOWN_INERTIA;
			}
		});
		
		PARCOOL_ACTION_MAPPING.put(Vault.class, (playerpatch, startEvent) -> {
			Vault.AnimationType type = ((Vault)startEvent.getAction()).getCurrentAnimation();
			
			switch (type) {
			case KONG_VAULT -> {
				return BIPED_VAULT_FORWARD;
			}
			case SPEED_VAULT_LEFT -> {
				return BIPED_VAULT_LEFT;
			}
			case SPEED_VAULT_RIGHT -> {
				return BIPED_VAULT_RIGHT;
			}
			default -> {
				throw new UnsupportedOperationException("Invalid animation type");
			}
			}
		});
		
		PARCOOL_ACTION_MAPPING.put(ClingToCliff.class, (playerpatch, startEvent) -> {
			/**
			Vec3 wallVec = ParcoolUtils.getGrabbableWall(playerpatch.getOriginal());
			
			if (wallVec != null) {
				ClingToCliff clingToCliff = ((ClingToCliff)event.getAction());
				double yRot = VectorUtil.toYawDegree(((ParCoolMixinClingToCliff)clingToCliff).getClingWallDirection());
				playerpatch.getAnimator().getVariables().put(CLING_TO_CLIFF_Y_ROT, BIPED_CLING_START, (float)yRot);
				return BIPED_CLING_START;
			}
			**/
			return null;
		});
		
		PARCOOL_ACTION_CANCEL_EVENTS.put(JumpFromBar.class, (playerpatch, action) -> {
			Parkourability parkourability = Parkourability.get(playerpatch.getOriginal());
			IStamina stamina = IStamina.get(playerpatch.getOriginal());
			DUMMY_BUFFER.clear();
			
			if (parkourability.get(JumpFromBar.class).canStart(playerpatch.getOriginal(), parkourability, stamina, DUMMY_BUFFER)) {
				if (parkourability.get(HangDown.class).isOrthogonalToBar()) {
					playerpatch.playAnimationSynchronized(BIPED_JUMP_FROM_BAR_START_ORTHOGONAL, 0.0F);
				} else {
					playerpatch.playAnimationSynchronized(BIPED_JUMP_FROM_BAR_START, 0.0F);
				}
			}
			
			return true;
		});
		
		PARCOOL_ACTION_CANCEL_EVENTS.put(WallJump.class, (playerpatch, action) -> {
			Parkourability parkourability = Parkourability.get(playerpatch.getOriginal());
			IStamina stamina = IStamina.get(playerpatch.getOriginal());
			DUMMY_BUFFER.clear();
			
			if (parkourability.get(WallJump.class).canStart(playerpatch.getOriginal(), parkourability, stamina, DUMMY_BUFFER)) {
				DUMMY_BUFFER.flip();
				Vec3 jumpDirection = new Vec3(DUMMY_BUFFER.getDouble(), DUMMY_BUFFER.getDouble(), DUMMY_BUFFER.getDouble());
				Vec3 wallDirection = new Vec3(DUMMY_BUFFER.getDouble(), 0.0D, DUMMY_BUFFER.getDouble());
				byte animType = DUMMY_BUFFER.get();
				
				switch (animType) {
				case 0 -> {
					//entitypatch.playAnimationSynchronized(BIPED_JUMP_FROM_BAR_START, 0.0f);
				}
				case 1 -> {
					playerpatch.getAnimator().getVariables().put(JUMP_DIRECTION, BIPED_WALL_JUMP_LEFT_START, jumpDirection);
					playerpatch.getAnimator().getVariables().put(WALL_DIRECTION, BIPED_WALL_JUMP_LEFT_START, wallDirection);
					playerpatch.playAnimationSynchronized(BIPED_WALL_JUMP_LEFT_START, 0.0F);
				}
				case 2 -> {
					playerpatch.getAnimator().getVariables().put(JUMP_DIRECTION, BIPED_WALL_JUMP_RIGHT_START, jumpDirection);
					playerpatch.getAnimator().getVariables().put(WALL_DIRECTION, BIPED_WALL_JUMP_RIGHT_START, wallDirection);
					playerpatch.playAnimationSynchronized(BIPED_WALL_JUMP_RIGHT_START, 0.0F);
				}
				default -> {
					throw new UnsupportedOperationException("No matching wall jump animation type " + animType);
				}
				}
			}
			
			return true;
		});
		
		PARCOOL_ACTION_CANCEL_EVENTS.put(ClingToCliff.class, (playerpatch, action) -> {
			AssetAccessor<? extends StaticAnimation> nowPlaying = playerpatch.getAnimator().getPlayerFor(null).getAnimation().get().getRealAnimation();
			
			if (nowPlaying == BIPED_CLING_START || nowPlaying == BIPED_CLIMB_UP) {
				return true;
			}
			
			Parkourability parkourability = Parkourability.get(playerpatch.getOriginal());
			IStamina stamina = IStamina.get(playerpatch.getOriginal());
			DUMMY_BUFFER.clear();
			
			if (parkourability.get(ClingToCliff.class).canStart(playerpatch.getOriginal(), parkourability, stamina, DUMMY_BUFFER)) {
				if (!ParcoolUtils.scanTerrainAndStartClingAction(playerpatch, ParcoolUtils.ClingType.CLING_START)) {
					return true;
				}
			}
			
			return false;
		});
		
		/**
		PARCOOL_ACTION_CANCEL_EVENTS.put(WallSlide.class, (playerpatch, action) -> {
			DynamicAnimation nowPlaying = playerpatch.getAnimator().getPlayerFor(null).getAnimation().getRealAnimation();
			return nowPlaying == BIPED_CLIMB_UP;
		});
		**/
		
		if (EpicFightMod.isPhysicalClient()) {
			ParCoolClientCompat.buildClientStuff();
		}
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
				event.getAnimator().addLivingAnimation(ParcoolLivingMotions.WALL_SLIDING_RIGHT, BIPED_WALL_SLIDE_RIGHT);
				event.getAnimator().addLivingAnimation(ParcoolLivingMotions.WALL_RUNNING_LEFT, BIPED_WALL_RUN_LEFT);
				event.getAnimator().addLivingAnimation(ParcoolLivingMotions.WALL_RUNNING_RIGHT, BIPED_WALL_RUN_RIGHT);
				event.getAnimator().addLivingAnimation(ParcoolLivingMotions.FAST_RUN, BIPED_FAST_RUN);
				event.getAnimator().addLivingAnimation(ParcoolLivingMotions.HANG_DOWN_ORTHOGONAL, BIPED_HANG_DOWN_ORTHOGONAL);
				event.getAnimator().addLivingAnimation(ParcoolLivingMotions.HANG_DOWN, BIPED_HANG_DOWN);
				event.getAnimator().addLivingAnimation(ParcoolLivingMotions.SLIDING, BIPED_SLIDE);
				event.getAnimator().addLivingAnimation(ParcoolLivingMotions.CRAWL, BIPED_CRAWL);
			}
		});
		
		eventBus.<ParCoolActionEvent.TryToStartEvent>addListener((event) -> {
			if (PARCOOL_ACTION_CANCEL_EVENTS.containsKey(event.getAction().getClass())) {
				PlayerPatch<?> playerpatch = EpicFightCapabilities.getEntityPatch(event.getPlayer(), PlayerPatch.class);
				
				if (playerpatch != null && playerpatch.isBattleMode() && PARCOOL_ACTION_CANCEL_EVENTS.get(event.getAction().getClass()).apply(playerpatch, event.getAction())) {
					event.setCanceled(true);
				}
			}
		});
		
		eventBus.<ParCoolActionEvent.StartEvent>addListener((event) -> {
			PlayerPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(event.getPlayer(), PlayerPatch.class);
			
			if (entitypatch != null && entitypatch.isLogicalClient() && entitypatch.isBattleMode() && PARCOOL_ACTION_MAPPING.containsKey(event.getAction().getClass())) {
				AssetAccessor<? extends StaticAnimation> animation = PARCOOL_ACTION_MAPPING.get(event.getAction().getClass()).apply(entitypatch, event);
				
				if (animation != null) {
					entitypatch.getAnimator().playAnimation(animation, 0.0F);
				}
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
	}
	
	public enum ParcoolLivingMotions implements LivingMotion {
		CLING_TO_CLIFF, CLING_TO_CLIFF_LEFT, CLING_TO_CLIFF_RIGHT, DIVE, WALL_SLIDING_LEFT, WALL_SLIDING_RIGHT, WALL_RUNNING_LEFT, WALL_RUNNING_RIGHT, FAST_RUN, HANG_DOWN_ORTHOGONAL, HANG_DOWN, SLIDING, CRAWL;
		
		final int id;
		
		ParcoolLivingMotions() {
			this.id = LivingMotion.ENUM_MANAGER.assign(this);
		}
		
		public int universalOrdinal() {
			return this.id;
		}
	}
	
	public static class ParcoolUtils {
		public static final AnimationEvent.E0 WALL_JUMP = (entitypatch, animation, param) -> {
			Vec3 jumpDirection = entitypatch.getAnimator().getVariables().get(JUMP_DIRECTION, animation);
			Vec3 wallDirection = entitypatch.getAnimator().getVariables().get(WALL_DIRECTION, animation);
	        Vec3 jumpMotion = jumpDirection.scale(0.59);
			Vec3 motion = entitypatch.getOriginal().getDeltaMovement();
			
			BlockPos leanedBlock = new BlockPos(
					(int) (entitypatch.getOriginal().getX() + wallDirection.x()),
					(int) (entitypatch.getOriginal().getBoundingBox().minY + entitypatch.getOriginal().getBbHeight() * 0.25),
					(int) (entitypatch.getOriginal().getZ() + wallDirection.z())
			);
			
			float slipperiness = entitypatch.getOriginal().getCommandSenderWorld().isLoaded(leanedBlock) ?
					entitypatch.getOriginal().getCommandSenderWorld().getBlockState(leanedBlock).getFriction(entitypatch.getOriginal().getCommandSenderWorld(), leanedBlock, entitypatch.getOriginal())
					: 0.6f;

			double ySpeed;
			
			if (slipperiness > 0.9) {// icy blocks
				ySpeed = motion.y();
			} else {
	            ySpeed = motion.y() > jumpMotion.y() ? motion.y + jumpMotion.y() : jumpMotion.y();
	            ParcoolUtils.spawnJumpParticles(entitypatch.getOriginal(), wallDirection, jumpDirection);
			}
			
			if (entitypatch instanceof LocalPlayerPatch) {
				entitypatch.getOriginal().setDeltaMovement(motion.x() + jumpMotion.x(), ySpeed, motion.z() + jumpMotion.z());
			}
			
			if (ParCoolConfig.Client.Booleans.EnableActionSounds.get()) {
				entitypatch.getOriginal().playSound(com.alrex.parcool.api.SoundEvents.WALL_JUMP.get(), 1f, 1f);
			}
			
			entitypatch.getAnimator().reserveAnimation(BIPED_WALL_JUMP_LEFT);
		};
		
		public static float idealYRotForHanging(HangDown action, Player player) {
			Vec3 bodyVec = VectorUtil.fromYawDegree(player.yBodyRot).normalize();
			Vec3 lookVec = player.getLookAngle();
			Vec3 idealLookVec;
			
			if (Math.abs(lookVec.x) > Math.abs(lookVec.z)) {
				idealLookVec = new Vec3(lookVec.x > 0.0D ? 1 : -1, 0.0D, 0.0D);
			} else {
				idealLookVec = new Vec3(0.0D, 0.0D, lookVec.z > 0.0D ? 1 : -1);
			}
			
			double differenceAngle = Math.acos(bodyVec.dot(idealLookVec));
			differenceAngle /= 4.0D;
			
			return (float)VectorUtil.toYawDegree(idealLookVec.yRot((float) differenceAngle));
		};
		
		@SuppressWarnings("incomplete-switch")
		public static Vec3 getHangableBars(LivingEntity entity) {
			double bbWidth = entity.getBbWidth() / 4;
			double bbHeight = 0.35;
			AABB bb = new AABB(
				entity.getX() - bbWidth,
				entity.getY() + entity.getBbHeight(),
				entity.getZ() - bbWidth,
				entity.getX() + bbWidth,
				entity.getY() + entity.getBbHeight() + bbHeight,
				entity.getZ() + bbWidth
			);
			
			if (entity.getCommandSenderWorld().noCollision(entity, bb)) {
				return null;
			}
			
			BlockPos pos = new BlockPos(
				(int) Math.floor(entity.getX()),
				(int) Math.floor(entity.getY() + entity.getBbHeight() + 0.4),
				(int) Math.floor(entity.getZ())
			);
			
			if (!entity.getCommandSenderWorld().isLoaded(pos)) {
				return null;
			}
			
			BlockState state = entity.getCommandSenderWorld().getBlockState(pos);
			Block block = state.getBlock();
			HangDown.BarAxis axis = null;
			
			if (block instanceof RotatedPillarBlock) {
				if (state.isCollisionShapeFullBlock(entity.getCommandSenderWorld(), pos)) {
					return null;
				}
				
				Direction.Axis pillarAxis = state.getValue(RotatedPillarBlock.AXIS);
				switch (pillarAxis) {
					case X:
						axis = HangDown.BarAxis.X;
						break;
					case Z:
						axis = HangDown.BarAxis.Z;
						break;
				}
			} else if (block instanceof DirectionalBlock) {
				if (state.isCollisionShapeFullBlock(entity.getCommandSenderWorld(), pos)) {
					return null;
				}
				
				Direction direction = state.getValue(DirectionalBlock.FACING);
				switch (direction) {
					case EAST:
					case WEST:
						axis = HangDown.BarAxis.X;
						break;
					case NORTH:
					case SOUTH:
						axis = HangDown.BarAxis.Z;
				}
			} else if (block instanceof CrossCollisionBlock) {
				int zCount = 0;
				int xCount = 0;
				if (state.getValue(CrossCollisionBlock.NORTH)) zCount++;
				if (state.getValue(CrossCollisionBlock.SOUTH)) zCount++;
				if (state.getValue(CrossCollisionBlock.EAST)) xCount++;
				if (state.getValue(CrossCollisionBlock.WEST)) xCount++;
				if (zCount > 0 && xCount == 0) axis = HangDown.BarAxis.Z;
				if (xCount > 0 && zCount == 0) axis = HangDown.BarAxis.X;
			} else if (block instanceof WallBlock) {
				int zCount = 0;
				int xCount = 0;
				if (state.getValue(WallBlock.NORTH_WALL) != WallSide.NONE) zCount++;
				if (state.getValue(WallBlock.SOUTH_WALL) != WallSide.NONE) zCount++;
				if (state.getValue(WallBlock.EAST_WALL) != WallSide.NONE) xCount++;
				if (state.getValue(WallBlock.WEST_WALL) != WallSide.NONE) xCount++;
				if (zCount > 0 && xCount == 0) axis = HangDown.BarAxis.Z;
				if (xCount > 0 && zCount == 0) axis = HangDown.BarAxis.X;
			}
			
			switch (axis) {
			case X -> {
				return new Vec3(entity.getX(), pos.getY(), pos.getZ() + 0.5D);
			}
			case Z -> {
				return new Vec3(pos.getX() + 0.5D, pos.getY(), entity.getZ());
			}
			default -> {
				return null;
			}
			}
		}
		
	    private static void spawnJumpParticles(LivingEntity entity, Vec3 wallDirection, Vec3 jumpDirection) {
	        Level level = entity.level();
	        Vec3 pos = entity.position();
	        BlockPos leanedBlock = new BlockPos(
	                (int) Math.floor(pos.x() + wallDirection.x()),
	                (int) Math.floor(pos.y() + entity.getBbHeight() * 0.25),
	                (int) Math.floor(pos.z() + wallDirection.z())
	        );
	        if (!level.isLoaded(leanedBlock)) return;
	        float width = entity.getBbWidth();
	        BlockState blockstate = level.getBlockState(leanedBlock);

	        Vec3 horizontalJumpDirection = jumpDirection.multiply(1, 0, 1).normalize();

	        wallDirection = wallDirection.normalize();
	        Vec3 orthogonalToWallVec = wallDirection.yRot((float) (Math.PI / 2)).normalize();

	        //doing "Conjugate of (horizontalJumpDirection/-wallDirection)" as complex number(x + z i)
	        Vec3 differenceVec =
	                new Vec3(
	                        -wallDirection.x() * horizontalJumpDirection.x() - wallDirection.z() * horizontalJumpDirection.z(), 0,
	                        wallDirection.z() * horizontalJumpDirection.x() - wallDirection.x() * horizontalJumpDirection.z()
	                ).multiply(1, 0, -1).normalize();
	        Vec3 particleBaseDirection =
	                new Vec3(
	                        -wallDirection.x() * differenceVec.x() + wallDirection.z() * differenceVec.z(), 0,
	                        -wallDirection.x() * differenceVec.z() - wallDirection.z() * differenceVec.x()
	                );
	        if (blockstate.getRenderShape() != RenderShape.INVISIBLE) {
	            for (int i = 0; i < 10; i++) {
	                Vec3 particlePos = new Vec3(
	                        pos.x() + (wallDirection.x() * 0.4 + orthogonalToWallVec.x() * (entity.getRandom().nextDouble() - 0.5D)) * width,
	                        pos.y() + 0.1D + 0.3 * entity.getRandom().nextDouble(),
	                        pos.z() + (wallDirection.z() * 0.4 + orthogonalToWallVec.z() * (entity.getRandom().nextDouble() - 0.5D)) * width
	                );
	                Vec3 particleSpeed = particleBaseDirection
	                        .yRot((float) (Math.PI * 0.2 * (entity.getRandom().nextDouble() - 0.5)))
	                        .scale(3 + 9 * entity.getRandom().nextDouble())
	                        .add(0, -jumpDirection.y() * 3 * entity.getRandom().nextDouble(), 0);
	                level.addParticle(
	                        new BlockParticleOption(ParticleTypes.BLOCK, blockstate).setPos(leanedBlock),
	                        particlePos.x(),
	                        particlePos.y(),
	                        particlePos.z(),
	                        particleSpeed.x(),
	                        particleSpeed.y(),
	                        particleSpeed.z()
	                );
	            }
	        }
	    }
	    
	    public static BarAxis getLookAxis(float rotation) {
	    	rotation = Math.abs(Mth.wrapDegrees(rotation));
	    	return Math.abs(rotation - 90.0F) > 45.0F ? BarAxis.Z : BarAxis.X;
	    }
	    
	    public static Vec2i getAdjacentWallDirection(Entity entity, Level level, Vec3 start, double hangHeight, double xPositive, double xNegative, double zPositive, double zNegative) {
	    	double entityWidth = entity.getBbWidth() * 0.49D;
	    	
	    	AABB belowHangHeightX = new AABB(
    			start.x() - entityWidth,
    			start.y() + hangHeight - entity.getBbHeight() / 6,
    			start.z() - 0.01D,
    			start.x() + entityWidth,
    			start.y() + hangHeight,
    			start.z() + 0.01D
			);
			AABB belowHangHeightZ = new AABB(
				start.x() - 0.01D,
				start.y() + hangHeight - entity.getBbHeight() / 6,
				start.z() - entityWidth,
				start.x() + 0.01D,
				start.y() + hangHeight,
				start.z() + entityWidth
			);
			AABB aboveHangHeight = new AABB(
				start.x() - entityWidth,
				start.y() + hangHeight,
				start.z() - entityWidth,
				start.x() + entityWidth,
				start.y() + entity.getBbHeight(),
				start.z() + entityWidth
			);
			
			double entityBBHalfSize = entity.getBbWidth() * 0.5D;
			int xDirection = 0;
			int zDirection = 0;
			
			if (!level.noCollision(entity, belowHangHeightX.expandTowards(entityBBHalfSize + xPositive, 0.0D, 0.0D)) && level.noCollision(entity, aboveHangHeight.expandTowards(entityBBHalfSize + xPositive, 0.0D, 0.0D)))
				xDirection++;
			if (!level.noCollision(entity, belowHangHeightX.expandTowards(-entityBBHalfSize + xNegative, 0.0D, 0.0D)) && level.noCollision(entity, aboveHangHeight.expandTowards(-entityBBHalfSize + xNegative, 0.0D, 0.0D)))
				xDirection--;
			if (!level.noCollision(entity, belowHangHeightZ.expandTowards(0.0D, 0.0D, entityBBHalfSize + zPositive)) && level.noCollision(entity, aboveHangHeight.expandTowards(0.0D, 0.0D, entityBBHalfSize + zPositive)))
				zDirection++;
			if (!level.noCollision(entity, belowHangHeightZ.expandTowards(0.0D, 0.0D, -entityBBHalfSize + zNegative)) && level.noCollision(entity, aboveHangHeight.expandTowards(0.0D, 0.0D, -entityBBHalfSize + zNegative)))
				zDirection--;
			
			return new Vec2i(xDirection, zDirection);
	    }
	    
		@Nullable
		public static boolean scanTerrainAndStartClingAction(PlayerPatch<?> playerpatch, ClingType clingType) {
			LivingEntity entity = playerpatch.getOriginal();
			double hangHeight = entity.getBbHeight() + (entity.getBbHeight() - entity.getEyeHeight()) * 0.5D;
			Level world = entity.getCommandSenderWorld();
			Vec3 playerLocation = entity.position();
			
			switch (clingType) {
			case CLING_START -> {
				Vec2i currentAdjacentWall = getAdjacentWallDirection(entity, world, playerLocation, hangHeight, 0.0D, 0.0D, 0.0D, 0.0D);
				
				if (currentAdjacentWall.x == 0 && currentAdjacentWall.y == 0) {
					return false;
				}
				
				float slipperiness;
				BlockPos blockPos = new BlockPos(
					(int)(entity.getX() + currentAdjacentWall.x),
					(int)(entity.getBoundingBox().minY + hangHeight - 0.3D),
					(int)(entity.getZ() + currentAdjacentWall.y)
				);
				
				if (!entity.getCommandSenderWorld().isLoaded(blockPos)) {
					return false;
				}
				
				slipperiness = entity.getCommandSenderWorld().getBlockState(blockPos).getFriction(entity.getCommandSenderWorld(), blockPos, entity);
				
				if (slipperiness > 0.9D) {
					return false;
				}
				
				BlockPos meLocation = new BlockPos(
					(int)(entity.getX()),
					(int)(entity.getBoundingBox().minY + hangHeight - 0.3D),
					(int)(entity.getZ())
				);
				float destYRot = (float)Mth.wrapDegrees(MathUtils.getYRotOfVector(new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ()).subtract(new Vec3(meLocation.getX(), meLocation.getY(), meLocation.getZ()))));
				Vec3 coordDestination = new Vec3(currentAdjacentWall.x == 0 ? entity.getX() : blockPos.getX() - 0.5D, blockPos.getY(), currentAdjacentWall.y == 0 ? entity.getZ() : blockPos.getZ() - 0.5D);
				BlockPos destBlockPos = new BlockPos(
					(int)Math.floor(entity.getX() + currentAdjacentWall.x),
					(int)Math.floor(entity.getBoundingBox().minY + hangHeight - 0.3D),
					(int)Math.floor(entity.getZ() + currentAdjacentWall.y)
				);
				
				if (currentAdjacentWall.x == 0) {
					double xFloat = entity.getX() - Math.floor(entity.getX());
					
					if (xFloat > 0.5D) {
						if (!world.getBlockState(destBlockPos.east()).isCollisionShapeFullBlock(world, destBlockPos.east())) {
							coordDestination = new Vec3((int)coordDestination.x - 0.5D, coordDestination.y, coordDestination.z);
						}
					} else {
						if (!world.getBlockState(destBlockPos.west()).isCollisionShapeFullBlock(world, destBlockPos.west())) {
							coordDestination = new Vec3((int)coordDestination.x - 0.5D, coordDestination.y, coordDestination.z);
						}
					}
				} else if (currentAdjacentWall.y == 0) {
					double zFloat = entity.getZ() - Math.floor(entity.getZ());
					
					if (zFloat > 0.5D) {
						if (!world.getBlockState(destBlockPos.south()).isCollisionShapeFullBlock(world, destBlockPos.south())) {
							coordDestination = new Vec3(coordDestination.x, coordDestination.y, (int)coordDestination.z - 0.5D);
						}
					} else {
						if (!world.getBlockState(destBlockPos.north()).isCollisionShapeFullBlock(world, destBlockPos.north())) {
							coordDestination = new Vec3(coordDestination.x, coordDestination.y, (int)coordDestination.z - 0.5D);
						}
					}
				}
				
				playerpatch.setModelYRot(destYRot, true);
				playerpatch.getAnimator().getVariables().putSharedVariable(CLIFF_Y_ROT, destYRot);
				playerpatch.getAnimator().getVariables().put(CLING_DESTINATION, BIPED_CLING_START, coordDestination);
				playerpatch.playAnimationInClientSide(BIPED_CLING_START, 0.0F);
			}
			case MOVE_LEFT, MOVE_RIGHT -> {
				float moveYRot = Mth.wrapDegrees(playerpatch.getYRot() + (clingType == ClingType.MOVE_LEFT ? -90.0F : 90.0F));
				Vec3 moveVec = MathUtils.getVectorForRotation(0.0F, moveYRot);
				Vec3 coordVec = moveVec.scale(0.6735F);
				Vec2i currentAdjacentWall = getAdjacentWallDirection(entity, world, playerLocation, hangHeight, 0.0D, 0.0D, 0.0D, 0.0D);
				
				BlockPos turnInsideCheckPos = new BlockPos(
					(int)Math.floor(playerLocation.x() + moveVec.x),
					(int)Math.floor(entity.getBoundingBox().minY + hangHeight - 0.3D),
					(int)Math.floor(playerLocation.z() + moveVec.z)
				);
				
				BlockPos turnOutsideCheckPos = new BlockPos(
					(int)Math.floor(playerLocation.x() + moveVec.x + currentAdjacentWall.x),
					(int)Math.floor(entity.getBoundingBox().minY + hangHeight - 0.3D),
					(int)Math.floor(playerLocation.z() + moveVec.z + currentAdjacentWall.y)
				);
				
				//System.out.println("turn inside: " + turnInsideCheckPos +" turn outside: "+ turnOutsideCheckPos);
				
				boolean shouldTurnInside = world.getBlockState(turnInsideCheckPos).isCollisionShapeFullBlock(world, turnInsideCheckPos);
				boolean shouldTurnOutside = !shouldTurnInside && !world.getBlockState(turnOutsideCheckPos).isFaceSturdy(world, turnOutsideCheckPos, Direction.UP);
				boolean canKeepMoving = !shouldTurnOutside && world.getBlockState(turnInsideCheckPos).getCollisionShape(world, turnInsideCheckPos).isEmpty();
				
				//System.out.println("should turn inside: " + shouldTurnInside + " should turn outside: " + shouldTurnOutside + " can keep moving: " + canKeepMoving);
				
				Vec3 destination;
				
				if (!canKeepMoving) {
					destination = new Vec3(
						currentAdjacentWall.x == 0 ? Math.floor(playerLocation.x() + (canKeepMoving ? coordVec.x : 0.0F)) + 0.5D : playerLocation.x(),
						playerLocation.y(),
						currentAdjacentWall.y == 0 ? Math.floor(playerLocation.z() + (canKeepMoving ? coordVec.z : 0.0F)) + 0.5D : playerLocation.z()
					);
					
					AnimationAccessor<? extends StaticAnimation> moveAnimation = (clingType == ClingType.MOVE_LEFT ? BIPED_CLING_MOVE_LEFT : BIPED_CLING_MOVE_RIGHT);
					
					//System.out.println(destination +" "+ destination.subtract(playerLocation).horizontalDistance());
					
					double distance = destination.subtract(playerLocation).horizontalDistance();
					
					//System.out.println(destination +" "+ playerLocation + " distance: " + distance);
					
					if (distance < 0.1D) {
						AnimationAccessor<? extends StaticAnimation> cornerMoveAnimation = null;
						
						if (shouldTurnInside) {
							cornerMoveAnimation = (clingType == ClingType.MOVE_LEFT ? BIPED_CLING_MOVE_LEFT_CORNER1 : BIPED_CLING_MOVE_RIGHT_CORNER1);
						} else if (shouldTurnOutside) {
							//moveAnimationCorner = (clingType == ClingType.MOVE_LEFT ? BIPED_CLING_MOVE_LEFT : BIPED_CLING_MOVE_RIGHT);
						}
						
						if (cornerMoveAnimation != null) {
							float visualYRot = (float)Mth.wrapDegrees(VectorUtil.toYawDegree(new Vec3(currentAdjacentWall.x, 0.0D, currentAdjacentWall.y)));
							playerpatch.getAnimator().getVariables().put(CLIFF_START_Y_ROT, cornerMoveAnimation, playerpatch.getYRot());
							playerpatch.getAnimator().getVariables().put(CLIFF_DEST_Y_ROT, cornerMoveAnimation, moveYRot);
							playerpatch.getAnimator().getVariables().putSharedVariable(CLIFF_Y_ROT, visualYRot);
							
							BlockPos top = turnInsideCheckPos.above();
							playerpatch.getAnimator().getVariables().put(CLING_DESTINATION, cornerMoveAnimation, new Vec3(top.getX() + 0.5D, top.getY(), top.getZ() + 0.5D));
							playerpatch.playAnimationInClientSide(cornerMoveAnimation, 0.0F);
							
							return true;
						} else {
							return false;
						}
					} else if (distance < 0.6735D) {
						playerpatch.getAnimator().getVariables().put(CLING_DESTINATION, moveAnimation, destination);
					}
					
					playerpatch.setModelYRot(playerpatch.getYRot(), true);
					playerpatch.playAnimationInClientSide(moveAnimation, 0.0F);
					
					return true;
				} else {
					AnimationAccessor<? extends StaticAnimation> moveAnimation = (clingType == ClingType.MOVE_LEFT ? BIPED_CLING_MOVE_LEFT : BIPED_CLING_MOVE_RIGHT);
					
					playerpatch.setModelYRot(playerpatch.getYRot(), true);
					playerpatch.playAnimationInClientSide(moveAnimation, 0.0F);
					
					return true;
				}
			}
			default -> {
				throw new UnsupportedOperationException("Invalid Cling Type");
			}
			}
			
			return true;
		}
		
		public enum ClingType {
			CLING_START, MOVE_LEFT, MOVE_RIGHT;
		}
	}
}