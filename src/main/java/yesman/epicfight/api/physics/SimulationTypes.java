package yesman.epicfight.api.physics;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.client.model.ClothMesh;
import yesman.epicfight.api.client.physics.ClothSimulatable;
import yesman.epicfight.api.client.physics.ClothSimulator;

public interface SimulationTypes<O, KEY extends SimulationProvider<O, DATA>, DATA extends SimulationData<KEY, O>, SIM extends PhysicsSimulator<KEY, O, DATA>> {
	@OnlyIn(Dist.CLIENT)
	public static final SimulationTypes<ClothSimulatable, ClothMesh, ClothSimulator.ClothObject, ClothSimulator> CLOTH = new SimulationTypes<> () {};
	
	//public static final SimulationTypes<ProceduralAnimation, IKInfo, IKInfo> INVERSE_KINEMATICS = new SimulationTypes<> () {};
}