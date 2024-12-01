package yesman.epicfight.client.mesh;

import java.util.List;
import java.util.Map;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.client.model.SkinnedMeshVertexBuilder;
import yesman.epicfight.api.client.model.MeshPartDefinition;
import yesman.epicfight.api.client.model.MeshProvider;
import yesman.epicfight.api.client.model.Meshes;

@OnlyIn(Dist.CLIENT)
public class VillagerMesh extends HumanoidMesh implements MeshProvider<VillagerMesh> {
	public VillagerMesh(Map<String, float[]> arrayMap, Map<MeshPartDefinition, List<SkinnedMeshVertexBuilder>> parts, SkinnedMesh parent, RenderProperties properties) {
		super(arrayMap, parts, parent, properties);
	}
	
	@Override
	public SkinnedMesh getHumanoidArmorModel(EquipmentSlot slot) {
		switch (slot) {
		case HEAD:
			return Meshes.HELMET_VILLAGER;
		case CHEST:
			return Meshes.CHESTPLATE;
		case LEGS:
			return Meshes.LEGGINS;
		case FEET:
			return Meshes.BOOTS;
		default:
			return null;
		}
	}
	
	@Override
	public VillagerMesh get() {
		return this;
	}
}