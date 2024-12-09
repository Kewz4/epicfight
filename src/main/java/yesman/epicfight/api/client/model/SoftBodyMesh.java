package yesman.epicfight.api.client.model;

import java.util.List;

import org.apache.commons.compress.utils.Lists;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.client.physics.cloth.ClothSimulatable;
import yesman.epicfight.api.client.physics.cloth.ClothSimulator;
import yesman.epicfight.api.physics.SimulationProvider;

@OnlyIn(Dist.CLIENT)
public interface SoftBodyMesh extends SimulationProvider<ClothSimulatable, ClothSimulator.ClothObject, ClothSimulator.ClothObjectBuilder, SoftBodyMesh> {
	public static final List<ClothSimulatable> TRACKING_SIMULATABLE_OBJECTS = Lists.newArrayList();
	
	boolean canStartSoftBodySimulation();
	
	default StaticMesh<?, ?> getSoftBodyMesh() {
		return (StaticMesh<?, ?>)this;
	}
	
	default Mesh getAsMesh() {
		return (Mesh)this;
	}
	
	@OnlyIn(Dist.CLIENT)
	public static record ClothSimulationInfo(List<int[]> constraints, ConstraintType[] constraintTypes, float[] compliances, int[] particles, float[] rootDistance) {
	}
	
	@OnlyIn(Dist.CLIENT)
	public enum ConstraintType {
		DISTANCE, VOLUME
	}
}
