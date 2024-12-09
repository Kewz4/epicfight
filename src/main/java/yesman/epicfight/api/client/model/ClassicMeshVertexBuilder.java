package yesman.epicfight.api.client.model;

import java.util.List;

import javax.annotation.Nullable;

import org.joml.Vector3f;
import org.joml.Vector4f;

import com.google.common.collect.Lists;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.utils.math.OpenMatrix4f;

@OnlyIn(Dist.CLIENT)
public class ClassicMeshVertexBuilder extends VertexBuilder<ClassicMesh> {
	public static List<ClassicMeshVertexBuilder> create(int[] drawingIndices) {
		List<ClassicMeshVertexBuilder> vertexIndicators = Lists.newArrayList();
		
		for (int i = 0; i < drawingIndices.length / 3; i++) {
			int k = i * 3;
			int position = drawingIndices[k];
			int uv = drawingIndices[k + 1];
			int normal = drawingIndices[k + 2];
			ClassicMeshVertexBuilder vi = new ClassicMeshVertexBuilder(position, uv, normal);
			vertexIndicators.add(vi);
		}
		
		return vertexIndicators;
	}
	
	public ClassicMeshVertexBuilder(int position, int uv, int normal) {
		super(position, uv, normal);
	}
	
	@Override
	public void getVertexPosition(ClassicMesh mesh, Vector4f dest) {
		int index = this.position * 3;
		dest.set(mesh.positions[index], mesh.positions[index + 1], mesh.positions[index + 2], 1.0F);
	}
	
	@Override
	public void getVertexNormal(ClassicMesh mesh, Vector3f dest) {
		int index = this.normal * 3;
		dest.set(mesh.normals[index], mesh.normals[index + 1], mesh.normals[index + 2]);
	}
	
	@Override
	public void getVertexPosition(ClassicMesh mesh, Vector4f dest, @Nullable OpenMatrix4f[] poses) {
		this.getVertexPosition(mesh, dest);
	}
	
	@Override
	public void getVertexNormal(ClassicMesh mesh, Vector3f dest, @Nullable OpenMatrix4f[] poses) {
		this.getVertexNormal(mesh, dest);
	}
}
