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
public class IronGolemMesh extends SkinnedMesh implements MeshProvider<IronGolemMesh> {
	public final SkinnedMeshPart head;
	public final SkinnedMeshPart chest;
	public final SkinnedMeshPart core;
	public final SkinnedMeshPart leftArm;
	public final SkinnedMeshPart rightArm;
	public final SkinnedMeshPart leftLeg;
	public final SkinnedMeshPart rightLeg;
	
	public IronGolemMesh(Map<String, float[]> arrayMap, Map<MeshPartDefinition, List<SkinnedMeshVertexBuilder>> parts, SkinnedMesh parent, RenderProperties properties) {
		super(arrayMap, parts, parent, properties);
		
		this.head = this.getOrLogException(this.parts, "head");
		this.chest = this.getOrLogException(this.parts, "chest");
		this.core = this.getOrLogException(this.parts, "core");
		this.leftArm = this.getOrLogException(this.parts, "leftArm");
		this.rightArm = this.getOrLogException(this.parts, "rightArm");
		this.leftLeg = this.getOrLogException(this.parts, "leftLeg");
		this.rightLeg = this.getOrLogException(this.parts, "rightLeg");
	}

	@Override
	public IronGolemMesh get() {
		return this;
	}
}