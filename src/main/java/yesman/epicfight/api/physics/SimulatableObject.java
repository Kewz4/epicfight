package yesman.epicfight.api.physics;

import java.util.Optional;

public interface SimulatableObject {
	<SIM extends PhysicsSimulator<?, ?, ?>> Optional<SIM> getSimulator(SimulationTypes<?, ?, ?, SIM> simulationType);
	
	@SuppressWarnings("unchecked")
	default <O extends SimulatableObject> O cast(Class<O> type) {
		if (type.isAssignableFrom(this.getClass())) {
			return (O)this;
		}
		
		return null;
	}
}
