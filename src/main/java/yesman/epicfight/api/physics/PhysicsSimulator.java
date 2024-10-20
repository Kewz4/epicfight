package yesman.epicfight.api.physics;

import java.util.Optional;
import java.util.function.BooleanSupplier;

import yesman.epicfight.api.physics.SimulationObject.SimulationBuilder;

public interface PhysicsSimulator<B extends SimulationBuilder, KEY extends SimulationProvider<O, T, B>, O, T extends SimulationObject<B, KEY, O>> {
	public void tick(O object);
	
	public boolean isRunning(KEY key);
	
	public void runWhen(KEY key, B builder, BooleanSupplier when);
	
	public void runWhenPermanent(KEY key, B builder, BooleanSupplier when);
	
	public Optional<T> getRunningSimulationData(KEY key);
}
