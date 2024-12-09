package yesman.epicfight.api.client.physics;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Maps;

import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.physics.PhysicsSimulator;
import yesman.epicfight.api.physics.SimulationObject;
import yesman.epicfight.api.physics.SimulationObject.SimulationBuilder;
import yesman.epicfight.api.physics.SimulationProvider;

public abstract class AbstractSimulator<B extends SimulationBuilder, PV extends SimulationProvider<O, SO, B, PV>, O, SO extends SimulationObject<B, PV, O>> implements PhysicsSimulator<B, PV, O, SO> {
	protected Map<ResourceLocation, ObjectWrapper> simulationObjects = Maps.newHashMap();
	
	@Override
	public void tick(O simObject) {
		this.simulationObjects.values().removeIf((keyWrapper) -> {
			if (keyWrapper.isRunning()) {
				if (!keyWrapper.runWhen.getAsBoolean()) {
					keyWrapper.stopRunning();
					
					if (!keyWrapper.permanent) {
						return true;
					}
				}
			} else {
				if (keyWrapper.runWhen.getAsBoolean()) {
					keyWrapper.startRunning(simObject);
				}
			}
			
			return false;
		});
		
	}
	
	/**
	 * Run until the condition fails
	 */
	@Override
	public void runWhen(ResourceLocation key, PV provider, B builder, BooleanSupplier when) {
		this.simulationObjects.put(key, new ObjectWrapper(provider, when, false, builder));
	}
	
	/**
	 * Run permanently and pause simulation when condition fails
	 */
	@Override
	public void runWhenPermanent(ResourceLocation key, PV provider, B builder, BooleanSupplier when) {
		this.simulationObjects.put(key, new ObjectWrapper(provider, when, true, builder));
	}
	
	/**
	 * Stop simulation
	 */
	@Override
	public void stop(ResourceLocation key) {
		this.simulationObjects.remove(key);
	}
	
	/**
	 * Restart with the same condition but with another provider
	 */
	@Override
	public void restart(ResourceLocation key, PV newProvider) {
		ObjectWrapper kwrap = this.simulationObjects.get(key);
		
		if (kwrap != null) {
			this.stop(key);
			this.simulationObjects.put(key, new ObjectWrapper(kwrap.provider, kwrap.runWhen, kwrap.permanent, kwrap.builder));
		}
	}
	
	@Override
	public boolean isRunning(ResourceLocation key) {
		return this.simulationObjects.get(key).isRunning();
	}
	
	@Override
	public Optional<SO> getRunningObject(ResourceLocation key) {
		if (!this.simulationObjects.containsKey(key)) {
			return Optional.empty();
		}
		
		return Optional.ofNullable(this.simulationObjects.get(key).simulationObject);
	}
	
	public List<Pair<ResourceLocation, SO>> getAllRunningObjects() {
		return this.simulationObjects.entrySet().stream().filter((entry) -> entry.getValue().isRunning()).map((entry) -> Pair.of(entry.getKey(), entry.getValue().simulationObject)).toList();
	}
	
	protected class ObjectWrapper {
		final PV provider;
		final B builder;
		final BooleanSupplier runWhen;
		final boolean permanent;
		
		SO simulationObject;
		boolean isRunning;
		
		ObjectWrapper(PV key, BooleanSupplier runWhen, boolean permanent, B builder) {
			this.provider = key;
			this.runWhen = runWhen;
			this.permanent = permanent;
			this.builder = builder;
		}
		
		public void startRunning(O simObject) {
			this.isRunning = true;
			this.simulationObject = this.provider.createSimulationData(this.provider, simObject, this.builder);
		}
		
		public void stopRunning() {
			this.isRunning = false;
			this.simulationObject = null;
		}
		
		public boolean isRunning() {
			return this.isRunning;
		}
	}
}