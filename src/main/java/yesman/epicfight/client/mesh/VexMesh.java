package yesman.epicfight.client.mesh;

import java.util.List;
import java.util.Map;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.client.model.SkinnedMeshVertexBuilder;
import yesman.epicfight.api.client.model.MeshPartDefinition;
import yesman.epicfight.api.client.model.MeshProvider;

@OnlyIn(Dist.CLIENT)
public class VexMesh extends HumanoidMesh implements MeshProvider<VexMesh> {
	public final SkinnedMeshPart leftWing;
	public final SkinnedMeshPart rightWing;
	
	public VexMesh(Map<String, float[]> arrayMap, Map<MeshPartDefinition, List<SkinnedMeshVertexBuilder>> parts, SkinnedMesh parent, RenderProperties properties) {
		super(arrayMap, parts, parent, properties);
		
		this.leftWing = this.getOrLogException(this.parts, "leftWing");
		this.rightWing = this.getOrLogException(this.parts, "rightWing");
	}

	@Override
	public VexMesh get() {
		return this;
	}
}