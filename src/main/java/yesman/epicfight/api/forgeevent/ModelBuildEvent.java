package yesman.epicfight.api.forgeevent;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.client.model.SkinnedMesh.SkinnedMeshPart;
import yesman.epicfight.api.client.model.SkinnedMeshVertexBuilder;
import yesman.epicfight.api.client.model.ClothMesh;
import yesman.epicfight.api.client.model.ClothMesh.ClothMeshPart;
import yesman.epicfight.api.client.model.Mesh;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.client.model.Meshes.MeshContructor;
import yesman.epicfight.api.client.model.ClassicMesh;
import yesman.epicfight.api.client.model.ClassicMesh.ClassicMeshPart;
import yesman.epicfight.api.client.model.VertexBuilder;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.gameasset.Armatures.ArmatureContructor;

public abstract class ModelBuildEvent<T> extends Event implements IModBusEvent {
	protected final ResourceManager resourceManager;
	protected final BiMap<ResourceLocation, T> oldRegistry;
	
	public ModelBuildEvent(ResourceManager resourceManager, BiMap<ResourceLocation, T> oldRegistry) {
		this.resourceManager = resourceManager;
		this.oldRegistry = HashBiMap.create();
		this.oldRegistry.putAll(oldRegistry);
	}
	
	public BiMap<ResourceLocation, T> getOldRegistry() {
		return this.oldRegistry;
	}
	
	public static class ArmatureBuild extends ModelBuildEvent<Armature> {
		public ArmatureBuild(ResourceManager resourceManager, BiMap<ResourceLocation, Armature> oldRegistry) {
			super(resourceManager, oldRegistry);
		}
		
		public <T extends Armature> T get(String modid, String path, ArmatureContructor<T> constructor) {
			return Armatures.getOrCreateArmature(this.resourceManager, new ResourceLocation(modid, path), constructor);
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	public static class MeshBuild extends ModelBuildEvent<Mesh> {
		public MeshBuild(ResourceManager resourceManager, BiMap<ResourceLocation, Mesh> oldRegistry) {
			super(resourceManager, oldRegistry);
		}
		
		public <M extends ClassicMesh> M getRaw(String modid, String path, MeshContructor<ClassicMeshPart, VertexBuilder, M> constructor) {
			return Meshes.getOrCreateClasicMesh(this.resourceManager, new ResourceLocation(modid, path), constructor);
		}
		
		public <M extends SkinnedMesh> M getAnimated(String modid, String path, MeshContructor<SkinnedMeshPart, SkinnedMeshVertexBuilder, M> constructor) {
			return Meshes.getOrCreateSkinnedMesh(this.resourceManager, new ResourceLocation(modid, path), constructor);
		}
		
		public <M extends ClothMesh> M getCloth(String modid, String path, MeshContructor<ClothMeshPart, VertexBuilder, M> constructor) {
			return Meshes.getOrCreateClothMesh(this.resourceManager, new ResourceLocation(modid, path), constructor);
		}
	}
}