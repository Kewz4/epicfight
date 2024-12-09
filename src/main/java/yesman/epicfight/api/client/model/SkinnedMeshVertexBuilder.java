package yesman.epicfight.api.client.model;

import java.util.List;

import javax.annotation.Nullable;

import org.joml.Vector3f;
import org.joml.Vector4f;

import com.google.common.collect.Lists;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec4f;

@OnlyIn(Dist.CLIENT)
public class SkinnedMeshVertexBuilder extends VertexBuilder<SkinnedMesh> {
	public static List<SkinnedMeshVertexBuilder> create(int[] drawingIndices) {
		List<SkinnedMeshVertexBuilder> vertexIndicators = Lists.newArrayList();
		
		for (int i = 0; i < drawingIndices.length / 3; i++) {
			int k = i * 3;
			int position = drawingIndices[k];
			int uv = drawingIndices[k + 1];
			int normal = drawingIndices[k + 2];
			SkinnedMeshVertexBuilder vi = new SkinnedMeshVertexBuilder(position, uv, normal);
			vertexIndicators.add(vi);
		}
		
		return vertexIndicators;
	}
	
	public SkinnedMeshVertexBuilder(int position, int uv, int normal) {
		super(position, uv, normal);
	}
	
	@Override
	public void getVertexPosition(SkinnedMesh mesh, Vector4f dest) {
		int index = this.position * 3;
		dest.set(mesh.positions[index], mesh.positions[index + 1], mesh.positions[index + 2], 1.0F);
	}
	
	@Override
	public void getVertexNormal(SkinnedMesh mesh, Vector3f dest) {
		int index = this.normal * 3;
		dest.set(mesh.normals[index], mesh.normals[index + 1], mesh.normals[index + 2]);
	}
	
	private static final Vec4f TRANSFORM = new Vec4f();
	private static final Vec4f POSITION = new Vec4f();
	private static final Vec4f TOTAL_POS = new Vec4f();
	
	@Override
	public void getVertexPosition(SkinnedMesh mesh, Vector4f dest, @Nullable OpenMatrix4f[] poses) {
		int index = this.position * 3;
		
		POSITION.set(mesh.positions[index], mesh.positions[index + 1], mesh.positions[index + 2], 1.0F);
		TOTAL_POS.set(0.0F, 0.0F, 0.0F, 0.0F);
		
		for (int i = 0; i < mesh.affectingJointCounts[this.position]; i++) {
			int jointIndex = mesh.affectingJointIndices[this.position][i];
			int weightIndex = mesh.affectingWeightIndices[this.position][i];
			float weight = mesh.weights[weightIndex];
			Vec4f.add(OpenMatrix4f.transform(poses[jointIndex], POSITION, TRANSFORM).scale(weight), TOTAL_POS, TOTAL_POS);
		}
		
		dest.set(TOTAL_POS.x, TOTAL_POS.y, TOTAL_POS.z, 1.0F);
	}
	
	private static final Vec4f NORMAL = new Vec4f();
	private static final Vec4f TOTAL_NORMAL = new Vec4f();
	
	@Override
	public void getVertexNormal(SkinnedMesh mesh, Vector3f dest, @Nullable OpenMatrix4f[] poses) {
		int index = this.normal * 3;
		NORMAL.set(mesh.normals[index], mesh.normals[index + 1], mesh.normals[index + 2], 1.0F);
		TOTAL_NORMAL.set(0.0F, 0.0F, 0.0F, 0.0F);
		
		for (int i = 0; i < mesh.affectingJointCounts[this.position]; i++) {
			int jointIndex = mesh.affectingJointIndices[this.position][i];
			int weightIndex = mesh.affectingWeightIndices[this.position][i];
			float weight = mesh.weights[weightIndex];
			Vec4f.add(OpenMatrix4f.transform(poses[jointIndex], NORMAL, TRANSFORM).scale(weight), TOTAL_NORMAL, TOTAL_NORMAL);
		}
		
		dest.set(TOTAL_NORMAL.x, TOTAL_NORMAL.y, TOTAL_NORMAL.z);
	}
}