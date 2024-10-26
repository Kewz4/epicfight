package yesman.epicfight.api.client.physics;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import com.google.common.collect.Maps;

import yesman.epicfight.api.physics.PhysicsSimulator;
import yesman.epicfight.api.physics.SimulationObject;
import yesman.epicfight.api.physics.SimulationObject.SimulationBuilder;
import yesman.epicfight.api.physics.SimulationProvider;

public abstract class AbstractSimulator<B extends SimulationBuilder, KEY extends SimulationProvider<O, SD, B>, O, SD extends SimulationObject<B, KEY, O>> implements PhysicsSimulator<B, KEY, O, SD> {
	protected Map<KEY, KeyWrapper> simulationObjects = Maps.newHashMap();
	
	@Override
	public void tick(O simObject) {
		for (KeyWrapper keyWrapper : this.simulationObjects.values()) {
			if (keyWrapper.isRunning()) {
				if (!keyWrapper.runWhen.getAsBoolean()) {
					keyWrapper.stopRunning();
					
					if (!keyWrapper.permanent) {
						this.simulationObjects.remove(keyWrapper.key);
					}
				}
			} else {
				if (keyWrapper.runWhen.getAsBoolean()) {
					keyWrapper.startRunning(simObject);
				}
			}
		}
	}
	
	@Override
	public boolean isRunning(KEY key) {
		return this.simulationObjects.get(key).isRunning();
	}
	
	@Override
	public void runWhen(KEY key, B builder, BooleanSupplier when) {
		this.simulationObjects.put(key, new KeyWrapper(key, when, false, builder));
	}
	
	@Override
	public void runWhenPermanent(KEY key, B builder, BooleanSupplier when) {
		this.simulationObjects.put(key, new KeyWrapper(key, when, true, builder));
	}
	
	@Override
	public void stop(KEY key) {
		this.simulationObjects.remove(key);
	}
	
	@Override
	public void restart(KEY key, KEY newKey) {
		KeyWrapper kwrap = this.simulationObjects.get(key);
		
		this.stop(key);
		this.simulationObjects.put(newKey, new KeyWrapper(newKey, kwrap.runWhen, kwrap.permanent, kwrap.builder));
	}
	
	@Override
	public Optional<SD> getRunningSimulationData(KEY key) {
		if (!this.simulationObjects.containsKey(key)) {
			return Optional.empty();
		}
		
		return Optional.ofNullable(this.simulationObjects.get(key).simulationData);
	}
	
	public List<SD> getRunningObjects() {
		return this.simulationObjects.values().stream().filter((wrapper) -> wrapper.isRunning()).map((wrapper) -> wrapper.simulationData).toList();
	}
	
	protected class KeyWrapper {
		final KEY key;
		final BooleanSupplier runWhen;
		final boolean permanent;
		final B builder;
		
		SD simulationData;
		boolean isRunning;
		
		KeyWrapper(KEY key, BooleanSupplier runWhen, boolean permanent, B builder) {
			this.key = key;
			this.runWhen = runWhen;
			this.permanent = permanent;
			this.builder = builder;
		}
		
		public void startRunning(O simObject) {
			this.isRunning = true;
			this.simulationData = this.key.createSimulationData(simObject, this.builder);
		}
		
		public void stopRunning() {
			this.isRunning = false;
			this.simulationData = null;
		}
		
		public boolean isRunning() {
			return this.isRunning;
		}
	}
}