package yesman.epicfight.world.entity.eventlistener;

import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public class SkillExecuteEvent extends PlayerEvent<PlayerPatch<?>> {
	private final SkillContainer skillContainer;
	private boolean skillExecutable;
	private boolean stateExecutable;
	
	public SkillExecuteEvent(PlayerPatch<?> playerpatch, SkillContainer skillContainer) {
		super(playerpatch, true);
		
		this.skillContainer = skillContainer;
	}
	
	public SkillContainer getSkillContainer() {
		return this.skillContainer;
	}
	
	public boolean isSkillExecutable() {
		return this.skillExecutable;
	}
	
	public boolean isStateExecutable() {
		return this.stateExecutable;
	}
	
	public void setSkillExecutable(boolean skillExecutable) {
		this.skillExecutable = skillExecutable;
	}
	
	public void setStateExecutable(boolean stateExecutable) {
		this.stateExecutable = stateExecutable;
	}
	
	public boolean isExecutable() {
		return this.skillExecutable && this.stateExecutable;
	}
	
	public boolean shouldReserverKey() {
		return !this.isExecutable() && !this.isCanceled();
	}
}