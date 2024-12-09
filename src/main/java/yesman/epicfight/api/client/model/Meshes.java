package yesman.epicfight.api.client.model;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.datafixers.util.Pair;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModLoader;
import yesman.epicfight.api.client.model.ClassicMesh.ClassicMeshPart;
import yesman.epicfight.api.client.model.Mesh.RenderProperties;
import yesman.epicfight.api.client.model.SkinnedMesh.SkinnedMeshPart;
import yesman.epicfight.api.forgeevent.ModelBuildEvent;
import yesman.epicfight.api.model.JsonAssetLoader;
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
import yesman.epicfight.client.renderer.patched.layer.WearableItemLayer;
import yesman.epicfight.main.EpicFightMod;

@OnlyIn(Dist.CLIENT)
public class Meshes implements PreparableReloadListener {
	public static final Meshes INSTANCE = new Meshes();
	
	@FunctionalInterface
	public interface MeshContructor<P extends MeshPart<V>, V extends VertexBuilder<?>, M extends StaticMesh<P, V>> {
		M invoke(Map<String, Number[]> arrayMap, Map<MeshPartDefinition, List<V>> parts, M parent, RenderProperties properties);
	}
	
	private static final BiMap<ResourceLocation, Mesh> MESHES = HashBiMap.create();
	
	public static HumanoidMesh ALEX;
	public static HumanoidMesh BIPED;
	public static HumanoidMesh BIPED_OLD_TEX;
	public static HumanoidMesh BIPED_OUTLAYER;
	public static HumanoidMesh VILLAGER_ZOMBIE;
	public static CreeperMesh CREEPER;
	public static EndermanMesh ENDERMAN;
	public static HumanoidMesh SKELETON;
	public static SpiderMesh SPIDER;
	public static IronGolemMesh IRON_GOLEM;
	public static HumanoidMesh ILLAGER;
	public static VillagerMesh WITCH;
	public static RavagerMesh RAVAGER;
	public static VexMesh VEX;
	public static PiglinMesh PIGLIN;
	public static HoglinMesh HOGLIN;
	public static DragonMesh DRAGON;
	public static WitherMesh WITHER;
	
	public static SkinnedMesh HELMET;
	public static SkinnedMesh HELMET_PIGLIN;
	public static SkinnedMesh HELMET_VILLAGER;
	public static SkinnedMesh CHESTPLATE;
	public static SkinnedMesh LEGGINS;
	public static SkinnedMesh BOOTS;
	
	public static ClassicMesh AIR_BURST;
	public static ClassicMesh FORCE_FIELD;
	public static ClassicMesh LASER;
	
	public static SkinnedMesh CLOAK;
	
	public static void build(ResourceManager resourceManager) {
		ModelBuildEvent.MeshBuild event = new ModelBuildEvent.MeshBuild(resourceManager, MESHES);
		
		MESHES.values().stream().filter((mesh) -> mesh instanceof SkinnedMesh).map((mesh) -> (SkinnedMesh)mesh).forEach(SkinnedMesh::destroy);
		MESHES.clear();
		WearableItemLayer.clearModels();
		
		//Entities
		ALEX = event.getSkinned(EpicFightMod.MODID, "entity/biped_slim_arm", HumanoidMesh::new);
		BIPED = event.getSkinned(EpicFightMod.MODID, "entity/biped", HumanoidMesh::new);
		BIPED_OLD_TEX = event.getSkinned(EpicFightMod.MODID, "entity/biped_old_texture", HumanoidMesh::new);
		BIPED_OUTLAYER = event.getSkinned(EpicFightMod.MODID, "entity/biped_outlayer", HumanoidMesh::new);
		VILLAGER_ZOMBIE = event.getSkinned(EpicFightMod.MODID, "entity/zombie_villager", VillagerMesh::new);
		CREEPER = event.getSkinned(EpicFightMod.MODID, "entity/creeper", CreeperMesh::new);
		ENDERMAN = event.getSkinned(EpicFightMod.MODID, "entity/enderman", EndermanMesh::new);
		SKELETON = event.getSkinned(EpicFightMod.MODID, "entity/skeleton", HumanoidMesh::new);
		SPIDER = event.getSkinned(EpicFightMod.MODID, "entity/spider", SpiderMesh::new);
		IRON_GOLEM = event.getSkinned(EpicFightMod.MODID, "entity/iron_golem", IronGolemMesh::new);
		ILLAGER = event.getSkinned(EpicFightMod.MODID, "entity/illager", VillagerMesh::new);
		WITCH = event.getSkinned(EpicFightMod.MODID, "entity/witch", VillagerMesh::new);
		RAVAGER = event.getSkinned(EpicFightMod.MODID, "entity/ravager", RavagerMesh::new);
		VEX = event.getSkinned(EpicFightMod.MODID, "entity/vex", VexMesh::new);
		PIGLIN = event.getSkinned(EpicFightMod.MODID, "entity/piglin", PiglinMesh::new);
		HOGLIN = event.getSkinned(EpicFightMod.MODID, "entity/hoglin", HoglinMesh::new);
		DRAGON = event.getSkinned(EpicFightMod.MODID, "entity/dragon", DragonMesh::new);
		WITHER = event.getSkinned(EpicFightMod.MODID, "entity/wither", WitherMesh::new);
		
		//Particles
		AIR_BURST = event.getClassic(EpicFightMod.MODID, "particle/air_burst", ClassicMesh::new);
		FORCE_FIELD = event.getClassic(EpicFightMod.MODID, "particle/force_field", ClassicMesh::new);
		LASER = event.getClassic(EpicFightMod.MODID, "particle/laser", ClassicMesh::new);
		
		//Armors
		HELMET = event.getSkinned(EpicFightMod.MODID, "armor/helmet", SkinnedMesh::new);
		HELMET_PIGLIN = event.getSkinned(EpicFightMod.MODID, "armor/piglin_helmet", SkinnedMesh::new);
		HELMET_VILLAGER = event.getSkinned(EpicFightMod.MODID, "armor/villager_helmet", SkinnedMesh::new);
		CHESTPLATE = event.getSkinned(EpicFightMod.MODID, "armor/chestplate", SkinnedMesh::new);
		LEGGINS = event.getSkinned(EpicFightMod.MODID, "armor/leggins", SkinnedMesh::new);
		BOOTS = event.getSkinned(EpicFightMod.MODID, "armor/boots", SkinnedMesh::new);
		
		//Cloths
		CLOAK = event.getSkinned(EpicFightMod.MODID, "layer/cloak", SkinnedMesh::new);
		
		ModLoader.get().postEvent(event);
	}
	
	@SuppressWarnings("unchecked")
	public static <M extends ClassicMesh> M getOrCreateClasicMesh(ResourceManager rm, ResourceLocation rl, MeshContructor<ClassicMeshPart, ClassicMeshVertexBuilder, M> constructor) {
		return (M) MESHES.computeIfAbsent(rl, (key) -> {
			JsonAssetLoader jsonModelLoader = new JsonAssetLoader(rm, wrapLocation(rl));
			return jsonModelLoader.loadClassicMesh(constructor);
		});
	}
	
	@SuppressWarnings("unchecked")
	public static <M extends SkinnedMesh> M getOrCreateSkinnedMesh(ResourceManager rm, ResourceLocation rl, MeshContructor<SkinnedMeshPart, SkinnedMeshVertexBuilder, M> constructor) {
		return (M) MESHES.computeIfAbsent(rl, (key) -> {
			JsonAssetLoader jsonModelLoader = new JsonAssetLoader(rm, wrapLocation(rl));
			return jsonModelLoader.loadSkinnedMesh(constructor);
		});
	}
	
	@SuppressWarnings("unchecked")
	public static <M extends CompositeMesh> M getOrCreateCompositeMesh(ResourceManager rm, ResourceLocation rl) {
		return (M) MESHES.computeIfAbsent(rl, (key) -> {
			JsonAssetLoader jsonModelLoader = new JsonAssetLoader(rm, wrapLocation(rl));
			return jsonModelLoader.loadCompositeMesh();
		});
	}
	
	public static ResourceLocation getKey(Mesh mesh) {
		return MESHES.inverse().get(mesh);
	}
	
	public static Mesh getMeshOrNull(ResourceLocation rl) {
		return MESHES.get(rl);
	}
	
	public static void addMesh(ResourceLocation rl, Mesh mesh) {
		MESHES.put(rl, mesh);
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Mesh> Set<Pair<ResourceLocation, MeshProvider<T>>> entries(Class<T> filterInstance) {
		return MESHES.entrySet().stream().filter((entry) -> filterInstance.isAssignableFrom(entry.getValue().getClass())).map((entry) -> Pair.of(entry.getKey(), (MeshProvider<T>)() -> (T)MESHES.get(entry.getKey()))).collect(Collectors.toSet());
	}
	
	public static ResourceLocation wrapLocation(ResourceLocation rl) {
		return rl.getPath().matches("animmodels/.*\\.json") ? rl : new ResourceLocation(rl.getNamespace(), "animmodels/" + rl.getPath() + ".json");
	}
	
	@Override
	public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier stage, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
		return CompletableFuture.runAsync(() -> {
			Meshes.build(resourceManager);
		}, gameExecutor).thenCompose(stage::wait);
	}
}