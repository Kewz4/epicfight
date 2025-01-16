package yesman.epicfight.config;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.EnumValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.api.utils.math.Vec2i;
import yesman.epicfight.main.EpicFightMod;

@Mod.EventBusSubscriber(modid = EpicFightMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientConfig {
	private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
	
	public static final IntValue LONG_PRESS_COUNTER = BUILDER.defineInRange("ingame.long_press_count", 2, 1, 10);
	public static final IntValue MAX_STUCK_PROJECTILES = BUILDER.defineInRange("ingame.max_hit_projectiles", 30, 0, 30);
	public static final BooleanValue FILTER_ANIMATION = BUILDER.define("ingame.filter_animation", () -> false);
	public static final DoubleValue AIM_HELPER_COLOR = BUILDER.defineInRange("ingame.laser_pointer_color", 0.328125D, 0.0D, 1.0D);
	public static final BooleanValue ENABLE_AIM_HELPER = BUILDER.define("ingame.enable_laser_pointer", () -> true);
	public static final BooleanValue AUTO_SWITCH_CAMERA = BUILDER.define("ingame.camera_auto_switch", () -> false);
	public static final BooleanValue AUTO_PREPARATION = BUILDER.define("ingame.auto_preparation", () -> false);
	public static final BooleanValue BLOOD_EFFECTS = BUILDER.define("ingame.blood_effects", () -> true);
	public static final BooleanValue PREVENT_MINING_IN_COMBAT_MODE = BUILDER.define("ingame.no_mining_in_combat", () -> true);
	public static final BooleanValue AIMING_POV_CORRECTION = BUILDER.define("ingame.aiming_correction", () -> true);
	public static final BooleanValue SHOW_EPICFIGHT_ATTRIBUTES_IN_TOOLTIP = BUILDER.define("ingame.show_epicfight_attributes", () -> true);
	public static final BooleanValue ACTIVATE_ANIMATION_SHADER = BUILDER.define("ingame.use_animation_shader", () -> false);
	public static final BooleanValue ENABLE_ANIMATED_FIRST_PERSON_MODEL = BUILDER.define("ingame.first_person_model", () -> true);
	
	public static final ConfigValue<List<? extends String>> BATTLE_MODE_SWITCHING_ITEMS = BUILDER.defineList("ingame.battle_autoswitch_items", Lists.newArrayList(), (element) -> {
		if (element instanceof String str) {
			return str.contains(":");
		}
		
		return false;
	});
	
	public static final ConfigValue<List<? extends String>> MINING_MODE_SWITCHING_ITEMS = BUILDER.defineList("ingame.mining_autoswitch_items", Lists.newArrayList(), (element) -> {
		if (element instanceof String str) {
			return str.contains(":");
		}
		
		return false;
	});
	
	// UI configurations
	public static final BooleanValue SHOW_TARGET_INDICATOR = BUILDER.define("ingame.show_target_indicator", () -> true);
	public static final EnumValue<HealthBarType> HEALTH_BAR_TYPE = BUILDER.defineEnum("ingame.health_bar_show_option", HealthBarType.HURT);
	
	public static final ConfigValue<Integer> STAMINA_BAR_X = BUILDER.define("ingame.ui.stamina_bar_x", 120);
	public static final ConfigValue<Integer> STAMINA_BAR_Y = BUILDER.define("ingame.ui.stamina_bar_y", 10);
	public static final EnumValue<HorizontalBasis> STAMINA_BAR_BASE_X = BUILDER.defineEnum("ingame.ui.stamina_bar_x_base", HorizontalBasis.RIGHT);
	public static final EnumValue<VerticalBasis> STAMINA_BAR_BASE_Y = BUILDER.defineEnum("ingame.ui.stamina_bar_y_base", VerticalBasis.BOTTOM);
	
	public static final ConfigValue<Integer> WEAPON_INNATE_X = BUILDER.define("ingame.ui.weapon_innate_x", 42);
	public static final ConfigValue<Integer> WEAPON_INNATE_Y = BUILDER.define("ingame.ui.weapon_innate_y", 48);
	public static final EnumValue<HorizontalBasis> WEAPON_INNATE_BASE_X = BUILDER.defineEnum("ingame.ui.weapon_innate_x_base", HorizontalBasis.RIGHT);
	public static final EnumValue<VerticalBasis> WEAPON_INNATE_BASE_Y = BUILDER.defineEnum("ingame.ui.weapon_innate_y_base", VerticalBasis.BOTTOM);
	
	public static final ConfigValue<Integer> PASSIVE_X = BUILDER.define("ingame.ui.passives_x", 70);
	public static final ConfigValue<Integer> PASSIVE_Y = BUILDER.define("ingame.ui.passives_y", 36);
	public static final EnumValue<HorizontalBasis> PASSIVE_BASE_X = BUILDER.defineEnum("ingame.ui.passives_x_base", HorizontalBasis.RIGHT);
	public static final EnumValue<VerticalBasis> PASSIVE_BASE_Y = BUILDER.defineEnum("ingame.ui.passives_y_base", VerticalBasis.BOTTOM);
	public static final EnumValue<AlignDirection> PASSIVE_ALIGN_DIRECTION = BUILDER.defineEnum("ingame.ui.passives_align_direction", AlignDirection.HORIZONTAL);
	
	public static final ConfigValue<Integer> CHARGING_BAR_X = BUILDER.define("ingame.ui.charging_bar_x", -119);
	public static final ConfigValue<Integer> CHARGING_BAR_Y = BUILDER.define("ingame.ui.charging_bar_y", 60);
	public static final EnumValue<HorizontalBasis> CHARGING_BAR_BASE_X = BUILDER.defineEnum("ingame.ui.charging_bar_x_base", HorizontalBasis.CENTER);
	public static final EnumValue<VerticalBasis> CHARGING_BAR_BASE_Y = BUILDER.defineEnum("ingame.ui.charging_bar_y_base", VerticalBasis.CENTER);
	
	public static final ForgeConfigSpec.ConfigValue<String> ACCESS_TOKEN = BUILDER.comment("Web cache data for auto login to epic fight patron server").define("access_token", "");
	public static final ForgeConfigSpec.ConfigValue<String> REFRESH_TOKNE = BUILDER.define("refresh_token", "");
	//public static final ForgeConfigSpec.EnumValue<AuthenticationProvider> PROVIDER = BUILDER.defineEnum("provider", AuthenticationProvider.NULL);
	
	public static final ForgeConfigSpec SPEC = BUILDER.build();
	
	public static int longPressCounter;
	public static int maxStuckProjectiles;
	public static boolean filterAnimation;
	public static double aimHelperColor;
	public static int aimHelperPackedColor;
	public static boolean enableAimHelper;
	public static boolean authSwitchCamera;
	public static boolean autoPreparation;
	public static boolean bloodEffects;
	public static boolean preventMiningInCombatMode;
	public static boolean aimingPovCorrection;
	public static boolean showEpicFightAttributesInTooltip;
	public static boolean activateAnimationShader;
	public static boolean animationShaderLockedByException = false;
	public static boolean enableAnimatedFirstPersonModel;
	public static List<Item> battleModeSwitchingItems;
	public static List<Item> miningModeSwitchingItems;
	
	// UI configurations
	public static boolean showTargetIndicator;
	public static HealthBarType healthBarType;
	public static int staminaBarX;
	public static int staminaBarY;
	public static HorizontalBasis staminaBarBaseX;
	public static VerticalBasis staminaBarBaseY;
	public static int weaponInnateX;
	public static int weaponInnateY;
	public static HorizontalBasis weaponInnateBaseX;
	public static VerticalBasis weaponInnateBaseY;
	public static int passiveX;
	public static int passiveY;
	public static HorizontalBasis passiveBaseX;
	public static VerticalBasis passiveBaseY;
	public static AlignDirection passiveAlignDirection;
	public static int chargingBarX;
	public static int chargingBarY;
	public static HorizontalBasis chargingBarBaseX;
	public static VerticalBasis chargingBarBaseY;
	
	@SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
		longPressCounter = LONG_PRESS_COUNTER.get();
		maxStuckProjectiles = MAX_STUCK_PROJECTILES.get();
		aimHelperColor = AIM_HELPER_COLOR.get();
		enableAimHelper = ENABLE_AIM_HELPER.get();
		authSwitchCamera = AUTO_SWITCH_CAMERA.get();
		autoPreparation = AUTO_PREPARATION.get();
		bloodEffects = BLOOD_EFFECTS.get();
		preventMiningInCombatMode = PREVENT_MINING_IN_COMBAT_MODE.get();
		aimingPovCorrection = AIMING_POV_CORRECTION.get();
		showEpicFightAttributesInTooltip = SHOW_EPICFIGHT_ATTRIBUTES_IN_TOOLTIP.get();
		activateAnimationShader = ACTIVATE_ANIMATION_SHADER.get();
		enableAnimatedFirstPersonModel = ENABLE_ANIMATED_FIRST_PERSON_MODEL.get();
		
		battleModeSwitchingItems = BATTLE_MODE_SWITCHING_ITEMS.get().stream()
				.map(itemName -> ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(itemName)))
				.collect(Collectors.toList());
		miningModeSwitchingItems = MINING_MODE_SWITCHING_ITEMS.get().stream()
				.map(itemName -> ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(itemName)))
				.collect(Collectors.toList());
		
		showTargetIndicator = SHOW_TARGET_INDICATOR.get();
		healthBarType = HEALTH_BAR_TYPE.get();
		staminaBarX = STAMINA_BAR_X.get();
		staminaBarY = STAMINA_BAR_Y.get();
		staminaBarBaseX = STAMINA_BAR_BASE_X.get();
		staminaBarBaseY = STAMINA_BAR_BASE_Y.get();
		weaponInnateX = WEAPON_INNATE_X.get();
		weaponInnateY = WEAPON_INNATE_Y.get();
		weaponInnateBaseX = WEAPON_INNATE_BASE_X.get();
		weaponInnateBaseY = WEAPON_INNATE_BASE_Y.get();
		passiveX = PASSIVE_X.get();
		passiveY = PASSIVE_Y.get();
		passiveBaseX = PASSIVE_BASE_X.get();
		passiveBaseY = PASSIVE_BASE_Y.get();
		passiveAlignDirection = PASSIVE_ALIGN_DIRECTION.get();
		chargingBarX = CHARGING_BAR_X.get();
		chargingBarY = CHARGING_BAR_Y.get();
		chargingBarBaseX = CHARGING_BAR_BASE_X.get();
		chargingBarBaseY = CHARGING_BAR_BASE_Y.get();
    }
	
	public static void saveChanges() {
		LONG_PRESS_COUNTER.set(longPressCounter);
		MAX_STUCK_PROJECTILES.set(maxStuckProjectiles);
		AIM_HELPER_COLOR.set(aimHelperColor);
		ENABLE_AIM_HELPER.set(enableAimHelper);
		AUTO_SWITCH_CAMERA.set(authSwitchCamera);
		AUTO_PREPARATION.set(autoPreparation);
		BLOOD_EFFECTS.set(bloodEffects);
		PREVENT_MINING_IN_COMBAT_MODE.set(preventMiningInCombatMode);
		AIMING_POV_CORRECTION.set(aimingPovCorrection);
		SHOW_EPICFIGHT_ATTRIBUTES_IN_TOOLTIP.set(showEpicFightAttributesInTooltip);
		ACTIVATE_ANIMATION_SHADER.set(activateAnimationShader);
		ENABLE_ANIMATED_FIRST_PERSON_MODEL.set(enableAnimatedFirstPersonModel);
		BATTLE_MODE_SWITCHING_ITEMS.set(Lists.newArrayList(battleModeSwitchingItems.stream().map((item) -> ForgeRegistries.ITEMS.getKey(item).toString()).iterator()));
		MINING_MODE_SWITCHING_ITEMS.set(Lists.newArrayList(miningModeSwitchingItems.stream().map((item) -> ForgeRegistries.ITEMS.getKey(item).toString()).iterator()));
		SHOW_TARGET_INDICATOR.set(showTargetIndicator);
		HEALTH_BAR_TYPE.set(healthBarType);
		STAMINA_BAR_X.set(staminaBarX);
		STAMINA_BAR_Y.set(staminaBarY);
		STAMINA_BAR_BASE_X.set(staminaBarBaseX);
		STAMINA_BAR_BASE_Y.set(staminaBarBaseY);
		WEAPON_INNATE_X.set(weaponInnateX);
		WEAPON_INNATE_Y.set(weaponInnateY);
		WEAPON_INNATE_BASE_X.set(weaponInnateBaseX);
		WEAPON_INNATE_BASE_Y.set(weaponInnateBaseY);
		PASSIVE_X.set(passiveX);
		PASSIVE_Y.set(passiveY);
		PASSIVE_BASE_X.set(passiveBaseX);
		PASSIVE_BASE_Y.set(passiveBaseY);
		PASSIVE_ALIGN_DIRECTION.set(passiveAlignDirection);
		CHARGING_BAR_X.set(chargingBarX);
		CHARGING_BAR_Y.set(chargingBarY);
		CHARGING_BAR_BASE_X.set(chargingBarBaseX);
		CHARGING_BAR_BASE_Y.set(chargingBarBaseY);
	}
	
	public static Vec2i getStaminaPosition(int width, int height) {
		int posX = staminaBarBaseX.positionGetter.apply(width, staminaBarX);
		int posY = staminaBarBaseY.positionGetter.apply(height, staminaBarY);
		return new Vec2i(posX, posY);
	}
	
	public static Vec2i getWeaponInnatePosition(int width, int height) {
		int posX = weaponInnateBaseX.positionGetter.apply(width, weaponInnateX);
		int posY = weaponInnateBaseY.positionGetter.apply(height, weaponInnateY);
		return new Vec2i(posX, posY);
	}
	
	public static Vec2i getChargingBarPosition(int width, int height) {
		int posX = chargingBarBaseX.positionGetter.apply(width, chargingBarX);
		int posY = chargingBarBaseY.positionGetter.apply(height, chargingBarY);
		return new Vec2i(posX, posY);
	}
	
	/**
	 * Screen position calculations
	 */
	private static final BiFunction<Integer, Integer, Integer> ORIGIN = ((screenLength, value) -> value);
	private static final BiFunction<Integer, Integer, Integer> SCREEN_EDGE = ((screenLength, value) -> screenLength - value);
	private static final BiFunction<Integer, Integer, Integer> CENTER = ((screenLength, value) -> screenLength / 2 + value);
	private static final BiFunction<Integer, Integer, Integer> CENTER_SAVE = ((screenLength, value) -> value - screenLength / 2);
	
	public enum HorizontalBasis {
		LEFT(ClientConfig.ORIGIN, ClientConfig.ORIGIN), RIGHT(ClientConfig.SCREEN_EDGE, ClientConfig.SCREEN_EDGE), CENTER(ClientConfig.CENTER, ClientConfig.CENTER_SAVE);
		
		public BiFunction<Integer, Integer, Integer> positionGetter;
		public BiFunction<Integer, Integer, Integer> saveCoordGetter;
		
		HorizontalBasis(BiFunction<Integer, Integer, Integer> positionGetter, BiFunction<Integer, Integer, Integer> saveCoordGetter) {
			this.positionGetter = positionGetter;
			this.saveCoordGetter = saveCoordGetter;
		}
	}
	
	public enum VerticalBasis {
		TOP(ClientConfig.ORIGIN, ClientConfig.ORIGIN), BOTTOM(ClientConfig.SCREEN_EDGE, ClientConfig.SCREEN_EDGE), CENTER(ClientConfig.CENTER, ClientConfig.CENTER_SAVE);
		
		public BiFunction<Integer, Integer, Integer> positionGetter;
		public BiFunction<Integer, Integer, Integer> saveCoordGetter;
		
		VerticalBasis(BiFunction<Integer, Integer, Integer> positionGetter, BiFunction<Integer, Integer, Integer> saveCoordGetter) {
			this.positionGetter = positionGetter;
			this.saveCoordGetter = saveCoordGetter;
		}
	}
	
	@FunctionalInterface
	public interface StartCoordGetter {
		Vec2i get(int x, int y, int width, int height, int icons, HorizontalBasis horBasis, VerticalBasis verBasis);
	}
	
	private static final StartCoordGetter START_HORIZONTAL = (x, y, width, height, icons, horBasis, verBasis) -> {
		if (horBasis == HorizontalBasis.CENTER) {
			return new Vec2i(x - width * (icons - 1) / 2, y);
		} else {
			return new Vec2i(x, y);
		}
	};
	
	private static final StartCoordGetter START_VERTICAL = (x, y, width, height, icons, horBasis, verBasis) -> {
		if (verBasis == VerticalBasis.CENTER) {
			return new Vec2i(x, y - height * (icons - 1) / 2);
		} else {
			return new Vec2i(x, y);
		}
	};
	
	@FunctionalInterface
	public interface NextCoordGetter {
		Vec2i getNext(HorizontalBasis horBasis, VerticalBasis verBasis, Vec2i prevCoord, int width, int height);
	}
	
	private static final NextCoordGetter NEXT_HORIZONTAL = (horBasis, verBasis, oldPos, width, height) -> {
		if (horBasis == HorizontalBasis.LEFT || horBasis == HorizontalBasis.CENTER) {
			return new Vec2i(oldPos.x + width, oldPos.y);
		} else {
			return new Vec2i(oldPos.x - width, oldPos.y);
		}
	};
	
	private static final NextCoordGetter NEXT_VERTICAL = (horBasis, verBasis, oldPos, width, height) -> {
		if (verBasis == VerticalBasis.TOP || verBasis == VerticalBasis.CENTER) {
			return new Vec2i(oldPos.x, oldPos.y + height);
		} else {
			return new Vec2i(oldPos.x, oldPos.y - height);
		}
	};
	
	public enum AlignDirection {
		HORIZONTAL(START_HORIZONTAL, NEXT_HORIZONTAL), VERTICAL(START_VERTICAL, NEXT_VERTICAL);
		
		public StartCoordGetter startCoordGetter;
		public NextCoordGetter nextPositionGetter;
		
		AlignDirection(StartCoordGetter startCoordGetter, NextCoordGetter nextPositionGetter) {
			this.startCoordGetter = startCoordGetter;
			this.nextPositionGetter = nextPositionGetter;
		}
	}
	
	public enum HealthBarType {
		NONE, HURT, TARGET;
		
		@Override
		public String toString() {
			return ParseUtil.toLowerCase(this.name());
		}
		
		public HealthBarType nextEnum() {
			return HealthBarType.values()[(this.ordinal() + 1) % 3];
		}
	}
}