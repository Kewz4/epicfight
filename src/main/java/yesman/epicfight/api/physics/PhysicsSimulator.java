package yesman.epicfight.api.physics;

import java.util.Optional;
import java.util.function.BooleanSupplier;

public interface PhysicsSimulator<KEY extends SimulationProvider<O, T>, O, T extends SimulationData<KEY, O>> {
	public void tick(O object);
	
	public boolean isRunning(KEY key);
	
	public void runWhen(KEY key, BooleanSupplier when);
	
	public void runWhenPermanent(KEY key, BooleanSupplier when);
	
	public Optional<T> getRunningSimulationData(KEY key);
}
