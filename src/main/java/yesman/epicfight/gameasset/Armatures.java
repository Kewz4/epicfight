package yesman.epicfight.gameasset;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.asset.JsonAssetLoader;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.model.armature.CreeperArmature;
import yesman.epicfight.model.armature.DragonArmature;
import yesman.epicfight.model.armature.EndermanArmature;
import yesman.epicfight.model.armature.HoglinArmature;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.model.armature.IronGolemArmature;
import yesman.epicfight.model.armature.PiglinArmature;
import yesman.epicfight.model.armature.RavagerArmature;
import yesman.epicfight.model.armature.SpiderArmature;
import yesman.epicfight.model.armature.VexArmature;
import yesman.epicfight.model.armature.WitherArmature;
import yesman.epicfight.world.capabilities.entitypatch.EntityPatch;
import yesman.epicfight.world.entity.EpicFightEntities;

public class Armatures {
	public static final Armatures INSTANCE = new Armatures();
	private static ResourceManager resourceManager = null;
	
	@FunctionalInterface
	public interface ArmatureContructor<T extends Armature> {
		T invoke(String name, int jointNumber, Joint joint, Map<String, Joint> jointMap);
	}
	
	private static final Map<AssetAccessor<? extends Armature>, Armature> ARMATURES = Maps.newHashMap();
	private static final Map<EntityType<?>, AssetAccessor<? extends Armature>> ENTITY_TYPE_ARMATURE_MAPPER = Maps.newHashMap();
	
	public static final AssetAccessor<HumanoidArmature> BIPED = ArmatureAccessor.create(EpicFightMod.MODID, "entity/biped", HumanoidArmature::new);
	public static final AssetAccessor<CreeperArmature> CREEPER = ArmatureAccessor.create(EpicFightMod.MODID, "entity/creeper", CreeperArmature::new);
	public static final AssetAccessor<EndermanArmature> ENDERMAN = ArmatureAccessor.create(EpicFightMod.MODID, "entity/enderman", EndermanArmature::new);
	public static final AssetAccessor<HumanoidArmature> SKELETON = ArmatureAccessor.create(EpicFightMod.MODID, "entity/skeleton", HumanoidArmature::new);
	public static final AssetAccessor<SpiderArmature> SPIDER = ArmatureAccessor.create(EpicFightMod.MODID, "entity/spider", SpiderArmature::new);
	public static final AssetAccessor<IronGolemArmature> IRON_GOLEM = ArmatureAccessor.create(EpicFightMod.MODID, "entity/iron_golem", IronGolemArmature::new);
	public static final AssetAccessor<RavagerArmature> RAVAGER = ArmatureAccessor.create(EpicFightMod.MODID, "entity/ravager", RavagerArmature::new);
	public static final AssetAccessor<VexArmature> VEX = ArmatureAccessor.create(EpicFightMod.MODID, "entity/vex", VexArmature::new);
	public static final AssetAccessor<PiglinArmature> PIGLIN = ArmatureAccessor.create(EpicFightMod.MODID, "entity/piglin", PiglinArmature::new);
	public static final AssetAccessor<HoglinArmature> HOGLIN = ArmatureAccessor.create(EpicFightMod.MODID, "entity/hoglin", HoglinArmature::new);
	public static final AssetAccessor<DragonArmature> DRAGON = ArmatureAccessor.create(EpicFightMod.MODID, "entity/dragon", DragonArmature::new);
	public static final AssetAccessor<WitherArmature> WITHER = ArmatureAccessor.create(EpicFightMod.MODID, "entity/wither", WitherArmature::new);
	
	static {
		registerEntityTypeArmature(EntityType.CAVE_SPIDER, SPIDER);
		registerEntityTypeArmature(EntityType.CREEPER, CREEPER);
		registerEntityTypeArmature(EntityType.DROWNED, BIPED);
		registerEntityTypeArmature(EntityType.ENDERMAN, ENDERMAN);
		registerEntityTypeArmature(EntityType.EVOKER, BIPED);
		registerEntityTypeArmature(EntityType.HOGLIN, HOGLIN);
		registerEntityTypeArmature(EntityType.HUSK, BIPED);
		registerEntityTypeArmature(EntityType.IRON_GOLEM, IRON_GOLEM);
		registerEntityTypeArmature(EntityType.PIGLIN_BRUTE, PIGLIN);
		registerEntityTypeArmature(EntityType.PIGLIN, PIGLIN);
		registerEntityTypeArmature(EntityType.PILLAGER, BIPED);
		registerEntityTypeArmature(EntityType.RAVAGER, RAVAGER);
		registerEntityTypeArmature(EntityType.SKELETON, SKELETON);
		registerEntityTypeArmature(EntityType.SPIDER, SPIDER);
		registerEntityTypeArmature(EntityType.STRAY, SKELETON);
		registerEntityTypeArmature(EntityType.VEX, VEX);
		registerEntityTypeArmature(EntityType.VINDICATOR, BIPED);
		registerEntityTypeArmature(EntityType.WITCH, BIPED);
		registerEntityTypeArmature(EntityType.WITHER_SKELETON, SKELETON);
		registerEntityTypeArmature(EntityType.ZOGLIN, HOGLIN);
		registerEntityTypeArmature(EntityType.ZOMBIE, BIPED);
		registerEntityTypeArmature(EntityType.ZOMBIE_VILLAGER, BIPED);
		registerEntityTypeArmature(EntityType.ZOMBIFIED_PIGLIN, PIGLIN);
		registerEntityTypeArmature(EntityType.PLAYER, BIPED);
		registerEntityTypeArmature(EntityType.ENDER_DRAGON, DRAGON);
		registerEntityTypeArmature(EntityType.WITHER, WITHER);
		registerEntityTypeArmature(EpicFightEntities.WITHER_SKELETON_MINION.get(), SKELETON);
		registerEntityTypeArmature(EpicFightEntities.WITHER_GHOST_CLONE.get(), WITHER);
	}
	
	public static void reload(ResourceManager resourceManager) {
		Armatures.resourceManager = resourceManager;
		ARMATURES.clear();
	}
	
	public static void registerEntityTypeArmature(EntityType<?> entityType, AssetAccessor<? extends Armature> armatureAccessor) {
		ENTITY_TYPE_ARMATURE_MAPPER.put(entityType, armatureAccessor);
	}
	
	//For presets
	public static void registerEntityTypeArmatureByPreset(EntityType<?> entityType, String presetName) {
		EntityType<?> presetEntityType = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.tryParse(presetName));
		ENTITY_TYPE_ARMATURE_MAPPER.put(entityType, ENTITY_TYPE_ARMATURE_MAPPER.get(presetEntityType));
	}
	
	@SuppressWarnings("unchecked")
	public static <A extends Armature> A getArmatureFor(EntityPatch<?> entitypatch) {
		return (A)ENTITY_TYPE_ARMATURE_MAPPER.get(entitypatch.getOriginal().getType()).get().deepCopy();
	}
	
	public static <A extends Armature> AssetAccessor<A> getOrCreate(ResourceLocation id, ArmatureContructor<A> armatureConstructor) {
		return ArmatureAccessor.create(id, armatureConstructor);
	}
	
	@SuppressWarnings("unchecked")
	public static <A extends Armature> Set<AssetAccessor<A>> entry() {
		Set<AssetAccessor<A>> newset = Sets.newHashSet();
		
		for (AssetAccessor<? extends Armature> accessor : ARMATURES.keySet()) {
			try {
				AssetAccessor<A> casted = (AssetAccessor<A>)accessor;
				newset.add(casted);
			} catch(ClassCastException e) {
			}
		}
		
		return newset;
	}
	
	public static ResourceLocation wrapLocation(ResourceLocation rl) {
		return rl.getPath().matches("animmodels/.*\\.json") ? rl : new ResourceLocation(rl.getNamespace(), "animmodels/" + rl.getPath() + ".json");
	}
	
	public static record ArmatureAccessor<A extends Armature> (ResourceLocation registryName, ArmatureContructor<A> armatureConstructor) implements AssetAccessor<A> {
		public static <A extends Armature> AssetAccessor<A> create(String namespaceId, String path, ArmatureContructor<A> armatureConstructor) {
			return create(new ResourceLocation(namespaceId, path), armatureConstructor);
		}
		
		public static <A extends Armature> ArmatureAccessor<A> create(ResourceLocation id, ArmatureContructor<A> armatureConstructor) {
			return new ArmatureAccessor<A> (id, armatureConstructor);
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public A get() {
			if (ARMATURES.get(this) == null) {
				JsonAssetLoader jsonAssetLoader = new JsonAssetLoader(resourceManager, wrapLocation(this.registryName()));
				ARMATURES.put(this, jsonAssetLoader.loadArmature(this.armatureConstructor));
			}
			
			return (A)ARMATURES.get(this);
		}
		
		public boolean isPresent() {
			return true;
		}
		
		public String toString() {
			return this.registryName.toString();
		}
		
		public int hashCode() {
			return this.registryName.hashCode();
		}
		
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			} else if (obj instanceof ArmatureAccessor armatureAccessor) {
				return this.registryName.equals(armatureAccessor.registryName());
			} else if (obj instanceof ResourceLocation rl) {
				return this.registryName.equals(rl);
			} else if (obj instanceof String name) {
				return this.registryName.toString().equals(name);
			} else {
				return false;
			}
		}
	}
}