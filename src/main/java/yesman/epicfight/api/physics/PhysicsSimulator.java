package yesman.epicfight.api.physics;

import java.util.Optional;
import java.util.function.BooleanSupplier;

import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.physics.SimulationObject.SimulationObjectBuilder;

public interface PhysicsSimulator<B extends SimulationObjectBuilder, PV extends SimulationProvider<O, T, B, PV>, O, T extends SimulationObject<B, PV, O>> {
	public void tick(O object);
	
	public boolean isRunning(ResourceLocation key);
	
	public void runWhen(ResourceLocation key,PV provider, B builder, BooleanSupplier when);
	
	public void runWhenPermanent(ResourceLocation key,PV provider, B builder, BooleanSupplier when);
	
	public void restart(ResourceLocation key);
	
	public void stop(ResourceLocation key);
	
	public Optional<T> getRunningObject(ResourceLocation key);
}
