package yesman.epicfight.api.client.physics;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import com.google.common.collect.Maps;

import yesman.epicfight.api.physics.PhysicsSimulator;
import yesman.epicfight.api.physics.SimulationData;
import yesman.epicfight.api.physics.SimulationProvider;

public abstract class AbstractSimulator<KEY extends SimulationProvider<O, SD>, O, SD extends SimulationData<KEY, O>> implements PhysicsSimulator<KEY, O, SD> {
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
	public void runWhen(KEY key, BooleanSupplier when) {
		this.simulationObjects.put(key, new KeyWrapper(key, when, false));
	}
	
	@Override
	public void runWhenPermanent(KEY key, BooleanSupplier when) {
		this.simulationObjects.put(key, new KeyWrapper(key, when, true));
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
		
		SD simulationData;
		boolean isRunning;
		
		KeyWrapper(KEY key, BooleanSupplier runWhen, boolean permanent) {
			this.key = key;
			this.runWhen = runWhen;
			this.permanent = permanent;
		}
		
		public void startRunning(O simObject) {
			this.isRunning = true;
			this.simulationData = this.key.createSimulationData(simObject);
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