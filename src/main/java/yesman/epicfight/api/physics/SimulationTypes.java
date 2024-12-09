package yesman.epicfight.api.physics;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.client.model.SoftBodyMesh;
import yesman.epicfight.api.client.physics.cloth.ClothSimulatable;
import yesman.epicfight.api.client.physics.cloth.ClothSimulator;

public interface SimulationTypes<O, PV extends SimulationProvider<O, DATA, B, PV>, B extends SimulationObject.SimulationBuilder, DATA extends SimulationObject<B, PV, O>, SIM extends PhysicsSimulator<B, PV, O, DATA>> {
	@OnlyIn(Dist.CLIENT)
	public static final SimulationTypes<ClothSimulatable, SoftBodyMesh, ClothSimulator.ClothObjectBuilder, ClothSimulator.ClothObject, ClothSimulator> CLOTH = new SimulationTypes<> () {};
	
	//public static final SimulationTypes<ProceduralAnimation, IKInfo, IKInfo> INVERSE_KINEMATICS = new SimulationTypes<> () {};
}