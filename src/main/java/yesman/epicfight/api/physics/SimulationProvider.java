package yesman.epicfight.api.physics;

public interface SimulationProvider<O, T extends SimulationData<?, ?>> {
	public T createSimulationData(O simObject);
}