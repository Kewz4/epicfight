package yesman.epicfight.api.client.model;

import javax.annotation.Nullable;

import org.joml.Vector3f;
import org.joml.Vector4f;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.utils.math.OpenMatrix4f;

@OnlyIn(Dist.CLIENT)
public abstract class VertexBuilder<M extends StaticMesh<?, ?>> {
	public final int position;
	public final int uv;
	public final int normal;
	
	public VertexBuilder(int position, int uv, int normal) {
		this.position = position;
		this.uv = uv;
		this.normal = normal;
	}
	
	public abstract void getVertexPosition(M mesh, Vector4f dest);
	public abstract void getVertexNormal(M mesh, Vector3f dest);
	public abstract void getVertexPosition(M mesh, Vector4f dest, @Nullable OpenMatrix4f[] poses);
	public abstract void getVertexNormal(M mesh, Vector3f dest, @Nullable OpenMatrix4f[] poses);
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof VertexBuilder vb) {
			return this.position == vb.position && this.uv == vb.uv && this.normal == vb.normal;
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
        final int prime = 31;
        int result = 1;
        
        result = prime * result + this.position;
        result = prime * result + this.uv;
        result = prime * result + this.normal;
        
        return result;
    }
}