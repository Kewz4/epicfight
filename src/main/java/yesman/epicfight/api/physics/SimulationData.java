package yesman.epicfight.api.physics;

public interface SimulationData<SP extends SimulationProvider<O, ?>, O> {
	SP getProvider();
	void tick(O simulatable);
}