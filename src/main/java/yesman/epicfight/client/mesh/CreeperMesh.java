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
public class CreeperMesh extends SkinnedMesh implements MeshProvider<CreeperMesh> {
	public final SkinnedMeshPart head;
	public final SkinnedMeshPart torso;
	public final SkinnedMeshPart legRF;
	public final SkinnedMeshPart legLF;
	public final SkinnedMeshPart legRB;
	public final SkinnedMeshPart legLB;
	
	public CreeperMesh(Map<String, float[]> arrayMap, Map<MeshPartDefinition, List<SkinnedMeshVertexBuilder>> parts, SkinnedMesh parent, RenderProperties properties) {
		super(arrayMap, parts, parent, properties);
		
		this.head = this.getOrLogException(this.parts, "head");
		this.torso = this.getOrLogException(this.parts, "torso");
		this.legRF = this.getOrLogException(this.parts, "legRF");
		this.legLF = this.getOrLogException(this.parts, "legLF");
		this.legRB = this.getOrLogException(this.parts, "legRB");
		this.legLB = this.getOrLogException(this.parts, "legLB");
	}

	@Override
	public CreeperMesh get() {
		return this;
	}
}