package yesman.epicfight.api.physics;

import yesman.epicfight.api.physics.SimulationObject.SimulationBuilder;

public interface SimulationObject<B extends SimulationBuilder, SP extends SimulationProvider<O, ?, B>, O> {
	SP getProvider();
	void tick(O simulatable);
	
	public static abstract class SimulationBuilder {
		//public abstract SimulationObject<?, ?, ?> create();
	}
}