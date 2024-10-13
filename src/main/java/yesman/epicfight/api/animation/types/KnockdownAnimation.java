package yesman.epicfight.api.animation.types;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.EpicFightDamageType;
import yesman.epicfight.world.damagesource.StunType;

public class KnockdownAnimation extends LongHitAnimation {
	public KnockdownAnimation(float convertTime, String path, Armature armature) {
		this(convertTime, path, armature, false);
	}
	
	public KnockdownAnimation(float convertTime, String path, Armature armature, boolean noRegister) {
		super(convertTime, path, armature, noRegister);
		
		this.stateSpectrumBlueprint
			.addState(EntityState.KNOCKDOWN, true)
			.addState(EntityState.ATTACK_RESULT, (damagesource) -> {
				if (damagesource.getEntity() != null && !damagesource.is(DamageTypeTags.IS_EXPLOSION) && !damagesource.is(DamageTypes.MAGIC) && !damagesource.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
					if (damagesource instanceof EpicFightDamageSource epicfight$damagesource) {
						if (epicfight$damagesource.is(EpicFightDamageType.FINISHER)) {
							epicfight$damagesource.setStunType(StunType.NONE);
							return AttackResult.ResultType.SUCCESS;
						}
						
						return AttackResult.ResultType.BLOCKED;
					} else {
						return AttackResult.ResultType.BLOCKED;
					}
				}
				
				return AttackResult.ResultType.SUCCESS;
			});
	}
}