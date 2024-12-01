package yesman.epicfight.api.physics;

import yesman.epicfight.api.physics.SimulationObject.SimulationBuilder;

public interface SimulationProvider<OWN, OBJ extends SimulationObject<?, ?, ?>, BUILDER extends SimulationBuilder> {
	public OBJ createSimulationData(OWN simOwner, BUILDER simBuilder);
}