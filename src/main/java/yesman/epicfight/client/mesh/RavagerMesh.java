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
public class RavagerMesh extends SkinnedMesh implements MeshProvider<RavagerMesh> {
	public final SkinnedMeshPart head;
	public final SkinnedMeshPart body;
	public final SkinnedMeshPart leftFrontLeg;
	public final SkinnedMeshPart rightFrontLeg;
	public final SkinnedMeshPart leftBackLeg;
	public final SkinnedMeshPart rightBackLeg;
	
	public RavagerMesh(Map<String, float[]> arrayMap, Map<MeshPartDefinition, List<SkinnedMeshVertexBuilder>> parts, SkinnedMesh parent, RenderProperties properties) {
		super(arrayMap, parts, parent, properties);
		
		this.head = this.getOrLogException(this.parts, "head");
		this.body = this.getOrLogException(this.parts, "body");
		this.leftFrontLeg = this.getOrLogException(this.parts, "leftFrontLeg");
		this.rightFrontLeg = this.getOrLogException(this.parts, "rightFrontLeg");
		this.leftBackLeg = this.getOrLogException(this.parts, "leftBackLeg");
		this.rightBackLeg = this.getOrLogException(this.parts, "rightBackLeg");
	}

	@Override
	public RavagerMesh get() {
		return this;
	}
}