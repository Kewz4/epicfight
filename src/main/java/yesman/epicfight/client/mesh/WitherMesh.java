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
public class WitherMesh extends SkinnedMesh implements MeshProvider<WitherMesh> {
	public final SkinnedMeshPart centerHead;
	public final SkinnedMeshPart leftHead;
	public final SkinnedMeshPart rightHead;
	public final SkinnedMeshPart ribcage;
	public final SkinnedMeshPart tail;
	
	public WitherMesh(Map<String, float[]> arrayMap, Map<MeshPartDefinition, List<SkinnedMeshVertexBuilder>> parts, SkinnedMesh parent, RenderProperties properties) {
		super(arrayMap, parts, parent, properties);
		
		this.centerHead = this.getOrLogException(this.parts, "centerHead");
		this.leftHead = this.getOrLogException(this.parts, "leftHead");
		this.rightHead = this.getOrLogException(this.parts, "rightHead");
		this.ribcage = this.getOrLogException(this.parts, "ribcage");
		this.tail = this.getOrLogException(this.parts, "tail");
	}

	@Override
	public WitherMesh get() {
		return this;
	}
}