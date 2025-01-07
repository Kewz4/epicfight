package yesman.epicfight.api.client.model;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.asset.JsonAssetLoader;
import yesman.epicfight.api.client.model.Mesh.RenderProperties;
import yesman.epicfight.api.client.physics.cloth.ClothSimulatable;
import yesman.epicfight.api.client.physics.cloth.ClothSimulator.ClothObject;
import yesman.epicfight.api.client.physics.cloth.ClothSimulator.ClothObjectBuilder;
import yesman.epicfight.client.mesh.CreeperMesh;
import yesman.epicfight.client.mesh.DragonMesh;
import yesman.epicfight.client.mesh.EndermanMesh;
import yesman.epicfight.client.mesh.HoglinMesh;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.mesh.IronGolemMesh;
import yesman.epicfight.client.mesh.PiglinMesh;
import yesman.epicfight.client.mesh.RavagerMesh;
import yesman.epicfight.client.mesh.SpiderMesh;
import yesman.epicfight.client.mesh.VexMesh;
import yesman.epicfight.client.mesh.VillagerMesh;
import yesman.epicfight.client.mesh.WitherMesh;
import yesman.epicfight.main.EpicFightMod;

@OnlyIn(Dist.CLIENT)
public class Meshes implements PreparableReloadListener {
	private static final Map<MeshAccessor<? extends Mesh>, Mesh> MESHES = Maps.newHashMap();
	private static ResourceManager resourceManager = null;
	
	public static final Meshes INSTANCE = new Meshes();
	
	//Entities
	public static final MeshAccessor<HumanoidMesh> ALEX = MeshAccessor.create(EpicFightMod.MODID, "entity/biped_slim_arm", (jsonModelLoader) -> jsonModelLoader.loadSkinnedMesh(HumanoidMesh::new));
	public static final MeshAccessor<HumanoidMesh> BIPED = MeshAccessor.create(EpicFightMod.MODID, "entity/biped", (jsonModelLoader) -> jsonModelLoader.loadSkinnedMesh(HumanoidMesh::new));
	public static final MeshAccessor<HumanoidMesh> BIPED_OLD_TEX = MeshAccessor.create(EpicFightMod.MODID, "entity/biped_old_texture", (jsonModelLoader) -> jsonModelLoader.loadSkinnedMesh(HumanoidMesh::new));
	public static final MeshAccessor<HumanoidMesh> BIPED_OUTLAYER = MeshAccessor.create(EpicFightMod.MODID, "entity/biped_outlayer", (jsonModelLoader) -> jsonModelLoader.loadSkinnedMesh(HumanoidMesh::new));
	public static final MeshAccessor<VillagerMesh> VILLAGER_ZOMBIE = MeshAccessor.create(EpicFightMod.MODID, "entity/zombie_villager", (jsonModelLoader) -> jsonModelLoader.loadSkinnedMesh(VillagerMesh::new));
	public static final MeshAccessor<CreeperMesh> CREEPER = MeshAccessor.create(EpicFightMod.MODID, "entity/creeper", (jsonModelLoader) -> jsonModelLoader.loadSkinnedMesh(CreeperMesh::new));
	public static final MeshAccessor<EndermanMesh> ENDERMAN = MeshAccessor.create(EpicFightMod.MODID, "entity/enderman", (jsonModelLoader) -> jsonModelLoader.loadSkinnedMesh(EndermanMesh::new));
	public static final MeshAccessor<HumanoidMesh> SKELETON = MeshAccessor.create(EpicFightMod.MODID, "entity/skeleton", (jsonModelLoader) -> jsonModelLoader.loadSkinnedMesh(HumanoidMesh::new));
	public static final MeshAccessor<SpiderMesh> SPIDER = MeshAccessor.create(EpicFightMod.MODID, "entity/spider", (jsonModelLoader) -> jsonModelLoader.loadSkinnedMesh(SpiderMesh::new));
	public static final MeshAccessor<IronGolemMesh> IRON_GOLEM = MeshAccessor.create(EpicFightMod.MODID, "entity/iron_golem", (jsonModelLoader) -> jsonModelLoader.loadSkinnedMesh(IronGolemMesh::new));
	public static final MeshAccessor<HumanoidMesh> ILLAGER = MeshAccessor.create(EpicFightMod.MODID, "entity/illager", (jsonModelLoader) -> jsonModelLoader.loadSkinnedMesh(VillagerMesh::new));
	public static final MeshAccessor<VillagerMesh> WITCH = MeshAccessor.create(EpicFightMod.MODID, "entity/witch", (jsonModelLoader) -> jsonModelLoader.loadSkinnedMesh(VillagerMesh::new));
	public static final MeshAccessor<RavagerMesh> RAVAGER = MeshAccessor.create(EpicFightMod.MODID, "entity/ravager",(jsonModelLoader) -> jsonModelLoader.loadSkinnedMesh(RavagerMesh::new));
	public static final MeshAccessor<VexMesh> VEX = MeshAccessor.create(EpicFightMod.MODID, "entity/vex", (jsonModelLoader) -> jsonModelLoader.loadSkinnedMesh(VexMesh::new));
	public static final MeshAccessor<PiglinMesh> PIGLIN = MeshAccessor.create(EpicFightMod.MODID, "entity/piglin", (jsonModelLoader) -> jsonModelLoader.loadSkinnedMesh(PiglinMesh::new));
	public static final MeshAccessor<HoglinMesh> HOGLIN = MeshAccessor.create(EpicFightMod.MODID, "entity/hoglin", (jsonModelLoader) -> jsonModelLoader.loadSkinnedMesh(HoglinMesh::new));
	public static final MeshAccessor<DragonMesh> DRAGON = MeshAccessor.create(EpicFightMod.MODID, "entity/dragon", (jsonModelLoader) -> jsonModelLoader.loadSkinnedMesh(DragonMesh::new));
	public static final MeshAccessor<WitherMesh> WITHER = MeshAccessor.create(EpicFightMod.MODID, "entity/wither", (jsonModelLoader) -> jsonModelLoader.loadSkinnedMesh(WitherMesh::new));
	
	//Particles
	public static final MeshAccessor<SkinnedMesh> HELMET = MeshAccessor.create(EpicFightMod.MODID, "armor/helmet", (jsonModelLoader) -> jsonModelLoader.loadSkinnedMesh(SkinnedMesh::new));
	public static final MeshAccessor<SkinnedMesh> HELMET_PIGLIN = MeshAccessor.create(EpicFightMod.MODID, "armor/piglin_helmet", (jsonModelLoader) -> jsonModelLoader.loadSkinnedMesh(SkinnedMesh::new));
	public static final MeshAccessor<SkinnedMesh> HELMET_VILLAGER = MeshAccessor.create(EpicFightMod.MODID, "armor/villager_helmet", (jsonModelLoader) -> jsonModelLoader.loadSkinnedMesh(SkinnedMesh::new));
	public static final MeshAccessor<SkinnedMesh> CHESTPLATE = MeshAccessor.create(EpicFightMod.MODID, "armor/chestplate", (jsonModelLoader) -> jsonModelLoader.loadSkinnedMesh(SkinnedMesh::new));
	public static final MeshAccessor<SkinnedMesh> LEGGINS = MeshAccessor.create(EpicFightMod.MODID, "armor/leggins", (jsonModelLoader) -> jsonModelLoader.loadSkinnedMesh(SkinnedMesh::new));
	public static final MeshAccessor<SkinnedMesh> BOOTS = MeshAccessor.create(EpicFightMod.MODID, "armor/boots", (jsonModelLoader) -> jsonModelLoader.loadSkinnedMesh(SkinnedMesh::new));
	
	//Armors
	public static final MeshAccessor<ClassicMesh> AIR_BURST = MeshAccessor.create(EpicFightMod.MODID, "particle/air_burst", (jsonModelLoader) -> jsonModelLoader.loadClassicMesh(ClassicMesh::new));
	public static final MeshAccessor<ClassicMesh> FORCE_FIELD = MeshAccessor.create(EpicFightMod.MODID, "particle/force_field", (jsonModelLoader) -> jsonModelLoader.loadClassicMesh(ClassicMesh::new));
	public static final MeshAccessor<ClassicMesh> LASER = MeshAccessor.create(EpicFightMod.MODID, "particle/laser", (jsonModelLoader) -> jsonModelLoader.loadClassicMesh(ClassicMesh::new));
	
	//Layers
	public static MeshAccessor<SkinnedMesh> CLOAK = MeshAccessor.create(EpicFightMod.MODID, "layer/cloak", (jsonModelLoader) -> jsonModelLoader.loadSkinnedMesh(SkinnedMesh::new));
	
	public static void reload(ResourceManager resourceManager) {
		Meshes.resourceManager = resourceManager;
		MESHES.clear();
	}
	
	public static <M extends Mesh> AssetAccessor<M> getOrCreate(ResourceLocation id, Function<JsonAssetLoader, M> jsonLoader) {
		return MeshAccessor.create(id, jsonLoader);
	}
	
	@SuppressWarnings("unchecked")
	public static <M extends Mesh> Set<AssetAccessor<M>> entry() {
		Set<AssetAccessor<M>> newset = Sets.newHashSet();
		
		for (AssetAccessor<? extends Mesh> accessor : MESHES.keySet()) {
			try {
				AssetAccessor<M> casted = (AssetAccessor<M>)accessor;
				newset.add(casted);
			} catch(ClassCastException e) {
			}
		}
		
		return newset;
	}
	
	public static ResourceLocation wrapLocation(ResourceLocation rl) {
		return rl.getPath().matches("animmodels/.*\\.json") ? rl : new ResourceLocation(rl.getNamespace(), "animmodels/" + rl.getPath() + ".json");
	}
	
	@Override
	public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier stage, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
		return CompletableFuture.runAsync(() -> {
			Meshes.reload(resourceManager);
		}, gameExecutor).thenCompose(stage::wait);
	}
	
	@FunctionalInterface
	@OnlyIn(Dist.CLIENT)
	public interface MeshContructor<P extends MeshPart<V>, V extends VertexBuilder<?>, M extends StaticMesh<P, V>> {
		M invoke(Map<String, Number[]> arrayMap, Map<MeshPartDefinition, List<V>> parts, M parent, RenderProperties properties);
	}
	
	@OnlyIn(Dist.CLIENT)
	public static record MeshAccessor<M extends Mesh> (ResourceLocation registryName, Function<JsonAssetLoader, M> jsonLoader) implements AssetAccessor<M>, SoftBodyTranslatable {
		public static <M extends Mesh> MeshAccessor<M> create(String namespaceId, String path, Function<JsonAssetLoader, M> jsonLoader) {
			return create(new ResourceLocation(namespaceId, path), jsonLoader);
		}
		
		private static <M extends Mesh> MeshAccessor<M> create(ResourceLocation id, Function<JsonAssetLoader, M> jsonLoader) {
			return new MeshAccessor<M> (id, jsonLoader);
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public M get() {
			if (MESHES.get(this) == null) {
				JsonAssetLoader jsonModelLoader = new JsonAssetLoader(resourceManager, wrapLocation(this.registryName));
				MESHES.put(this, this.jsonLoader.apply(jsonModelLoader));
			}
			
			return (M)MESHES.get(this);
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
			} else if (obj instanceof MeshAccessor armatureAccessor) {
				return this.registryName.equals(armatureAccessor.registryName());
			} else if (obj instanceof ResourceLocation rl) {
				return this.registryName.equals(rl);
			} else if (obj instanceof String name) {
				return this.registryName.toString().equals(name);
			} else {
				return false;
			}
		}
		
		@Override
		public boolean canStartSoftBodySimulation() {
			Mesh mesh = this.get();
			
			if (mesh instanceof StaticMesh<?, ?> staticMesh) {
				return staticMesh.canStartSoftBodySimulation();
			} else if (mesh instanceof CompositeMesh compositeMesh) {
				return compositeMesh.canStartSoftBodySimulation();
			}
			
			return false;
		}
		
		@Override
		public ClothObject createSimulationData(SoftBodyTranslatable provider, ClothSimulatable simOwner, ClothObjectBuilder simBuilder) {
			Mesh mesh = this.get();
			
			if (mesh instanceof StaticMesh<?, ?> staticMesh) {
				return staticMesh.createSimulationData(provider, simOwner, simBuilder);
			} else if (mesh instanceof CompositeMesh compositeMesh) {
				return compositeMesh.createSimulationData(provider, simOwner, simBuilder);
			}
			
			return null;
		}
	}
}