package yesman.epicfight.api.physics;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.client.model.ClothMesh;
import yesman.epicfight.api.client.physics.cloth.ClothSimulatable;
import yesman.epicfight.api.client.physics.cloth.ClothSimulator;

public interface SimulationTypes<O, KEY extends SimulationProvider<O, DATA, B>, B extends SimulationObject.SimulationBuilder, DATA extends SimulationObject<B, KEY, O>, SIM extends PhysicsSimulator<B, KEY, O, DATA>> {
	@OnlyIn(Dist.CLIENT)
	public static final SimulationTypes<ClothSimulatable, ClothMesh, ClothSimulator.ClothObjectBuilder, ClothSimulator.ClothObject, ClothSimulator> CLOTH = new SimulationTypes<> () {};
	
	//public static final SimulationTypes<ProceduralAnimation, IKInfo, IKInfo> INVERSE_KINEMATICS = new SimulationTypes<> () {};
}