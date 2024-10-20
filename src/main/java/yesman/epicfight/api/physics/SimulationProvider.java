package yesman.epicfight.api.physics;

import yesman.epicfight.api.physics.SimulationObject.SimulationBuilder;

public interface SimulationProvider<O, T extends SimulationObject<?, ?, ?>, B extends SimulationBuilder> {
	public T createSimulationData(O simObject, B simBuilder);
}