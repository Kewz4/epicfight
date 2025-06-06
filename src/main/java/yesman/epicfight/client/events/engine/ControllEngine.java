package yesman.epicfight.client.events.engine;

import java.util.Set;

import org.lwjgl.glfw.GLFW;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.gui.screen.SkillEditScreen;
import yesman.epicfight.client.gui.screen.config.IngameConfigurationScreen;
import yesman.epicfight.client.input.EpicFightKeyMappings;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.skill.ChargeableSkill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlot;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.entity.eventlistener.MovementInputEvent;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;
import yesman.epicfight.world.entity.eventlistener.SkillExecuteEvent;
import yesman.epicfight.world.gamerule.EpicFightGameRules;

@OnlyIn(Dist.CLIENT)
public class ControllEngine {
	private final Set<Object> packets = Sets.newHashSet();
	private final Minecraft minecraft;
	private LocalPlayer player;
	private LocalPlayerPatch playerpatch;
	private int weaponInnatePressCounter = 0;
	private int sneakPressCounter = 0;
	private int moverPressCounter = 0;
	private int lastHotbarLockedTime;
	private boolean weaponInnatePressToggle = false;
	private boolean sneakPressToggle = false;
	private boolean moverPressToggle = false;
	private boolean attackLightPressToggle = false;
	private boolean hotbarLocked;
	private boolean chargeKeyUnpressed;
	private int reserveCounter;
	private KeyMapping reservedKey;
	private SkillSlot reservedOrChargingSkillSlot;
	private KeyMapping currentChargingKey;
	
	public Options options;
	
	public ControllEngine() {
		Events.controllEngine = this;
		this.minecraft = Minecraft.getInstance();
		this.options = this.minecraft.options;
	}
	
	public void setPlayerPatch(LocalPlayerPatch playerpatch) {
		this.weaponInnatePressCounter = 0;
		this.weaponInnatePressToggle = false;
		this.sneakPressCounter = 0;
		this.sneakPressToggle = false;
		this.attackLightPressToggle = false;
		this.player = playerpatch.getOriginal();
		this.playerpatch = playerpatch;
	}
	
	public LocalPlayerPatch getPlayerPatch() {
		return this.playerpatch;
	}
	
	public boolean canPlayerMove(EntityState playerState) {
		return !playerState.movementLocked() || this.player.jumpableVehicle() != null;
	}
	
	public boolean canPlayerRotate(EntityState playerState) {
		return !playerState.turningLocked() || this.player.jumpableVehicle() != null;
	}
	
	public void handleEpicFightKeyMappings() {
		// Pause here if playerpatch is null
		if (this.playerpatch == null) {
			return;
		}
		
		if (isKeyPressed(EpicFightKeyMappings.SKILL_EDIT, false)) {
			if (this.playerpatch.getSkillCapability() != null) {
				Minecraft.getInstance().setScreen(new SkillEditScreen(this.player, this.playerpatch.getSkillCapability()));
			}
		}
		
		if (isKeyPressed(EpicFightKeyMappings.OPEN_CONFIG_SCREEN, false)) {
			Minecraft.getInstance().setScreen(new IngameConfigurationScreen(null));
		}
		
		if (isKeyPressed(EpicFightKeyMappings.SWITCH_VANILLA_MODEL_DEBUGGING, false)) {
			boolean flag = ClientEngine.getInstance().switchVanillaModelDebuggingMode();
			this.minecraft.keyboardHandler.debugFeedbackTranslated(flag ? "debug.vanilla_model_debugging.on" : "debug.vanilla_model_debugging.off");
		}
		
		while (isKeyPressed(EpicFightKeyMappings.ATTACK, true)) {
			if (this.playerpatch.isEpicFightMode() && this.currentChargingKey != EpicFightKeyMappings.ATTACK) {
				boolean shouldPlayAttackAnimation = true;
				
				if (this.options.keyAttack.getKey() == EpicFightKeyMappings.ATTACK.getKey()) {
					if (this.minecraft.hitResult.getType() == HitResult.Type.ENTITY) {
						if (!(((EntityHitResult)this.minecraft.hitResult).getEntity() instanceof LivingEntity)) {
							shouldPlayAttackAnimation = false;
						}
					}
					
					if (shouldPlayAttackAnimation) {
						if (this.playerpatch.isTargetLockedOn()) {
							shouldPlayAttackAnimation = true;
						} else if (ClientConfig.combatPreferredItems.contains(this.player.getMainHandItem().getItem())) {
							if (this.playerpatch.getTarget() != null) {
								shouldPlayAttackAnimation = true;
							} else {
								if (this.minecraft.hitResult.getType() == HitResult.Type.BLOCK) {
									BlockPos bp = ((BlockHitResult)this.minecraft.hitResult).getBlockPos();
									BlockState bs = this.minecraft.level.getBlockState(bp);
									boolean isSuitableTool = this.player.getMainHandItem().getDestroySpeed(bs) <= 5.0F || this.player.getMainHandItem().isCorrectToolForDrops(bs);
									
									shouldPlayAttackAnimation = !this.player.getMainHandItem().getItem().canAttackBlock(bs, this.player.level(), bp, this.player) || isSuitableTool;
								} else {
									shouldPlayAttackAnimation = true;
								}
							}
						} else {
							if (this.minecraft.hitResult.getType() == HitResult.Type.BLOCK) {
								shouldPlayAttackAnimation = false;
							} else {
								shouldPlayAttackAnimation = true;
							}
						}
					}
					
					// Disable vanilla attack key
					if (shouldPlayAttackAnimation) {
						makeUnpressed(this.options.keyAttack);
					}
				}
				
				if (shouldPlayAttackAnimation) {
					if (!EpicFightKeyMappings.ATTACK.getKey().equals(EpicFightKeyMappings.WEAPON_INNATE_SKILL.getKey())) {
						SkillSlot slot = (!this.player.onGround() && !this.player.isInWater() && this.player.getDeltaMovement().y > 0.03D) ? SkillSlots.AIR_ATTACK : SkillSlots.BASIC_ATTACK;
						
						if (this.playerpatch.getSkill(slot).sendExecuteRequest(this.playerpatch, this).isExecutable()) {
							this.player.resetAttackStrengthTicker();
							this.attackLightPressToggle = false;
							this.releaseAllServedKeys();
						} else {
							if (!this.player.isSpectator() && slot == SkillSlots.BASIC_ATTACK) {
								this.reserveKey(slot, EpicFightKeyMappings.ATTACK);
							}
						}
						
						this.lockHotkeys();
						this.attackLightPressToggle = false;
						this.weaponInnatePressToggle = false;
						this.weaponInnatePressCounter = 0;
					} else {
						if (!this.weaponInnatePressToggle) {
							this.weaponInnatePressToggle = true;
						}
					}
				}
			}
		}
		
		while (isKeyPressed(EpicFightKeyMappings.DODGE, true)) {
			if (this.playerpatch.isEpicFightMode() && this.currentChargingKey != EpicFightKeyMappings.DODGE) {
				if (EpicFightKeyMappings.DODGE.getKey().getValue() == this.options.keyShift.getKey().getValue()) {
					if (this.player.getVehicle() == null) {
						if (!this.sneakPressToggle) {
							this.sneakPressToggle = true;
						}
					}
				} else {
					SkillSlot skillCategory = (this.playerpatch.getEntityState().knockDown()) ? SkillSlots.KNOCKDOWN_WAKEUP : SkillSlots.DODGE;
					SkillContainer skill = this.playerpatch.getSkill(skillCategory);
					
					if (skill.sendExecuteRequest(this.playerpatch, this).shouldReserverKey()) {
						this.reserveKey(SkillSlots.DODGE, EpicFightKeyMappings.DODGE);
					}
				}
			}
		}
		
		while (isKeyPressed(EpicFightKeyMappings.GUARD, true)) {
		}
		
		while (isKeyPressed(EpicFightKeyMappings.WEAPON_INNATE_SKILL, true)) {
			if (this.playerpatch.isEpicFightMode() && this.currentChargingKey != EpicFightKeyMappings.WEAPON_INNATE_SKILL) {
				if (!EpicFightKeyMappings.ATTACK.getKey().equals(EpicFightKeyMappings.WEAPON_INNATE_SKILL.getKey())) {
					if (this.playerpatch.getSkill(SkillSlots.WEAPON_INNATE).sendExecuteRequest(this.playerpatch, this).shouldReserverKey()) {
						if (!this.player.isSpectator()) {
							this.reserveKey(SkillSlots.WEAPON_INNATE, EpicFightKeyMappings.WEAPON_INNATE_SKILL);
						}
					} else {
						this.lockHotkeys();
					}
				}
			}
		}
		
		while (isKeyPressed(EpicFightKeyMappings.MOVER_SKILL, true)) {
			if (this.playerpatch.isEpicFightMode() && !this.playerpatch.isChargingSkill()) {
				if (EpicFightKeyMappings.MOVER_SKILL.getKey().getValue() == this.options.keyJump.getKey().getValue()) {
					SkillContainer skillContainer = this.playerpatch.getSkill(SkillSlots.MOVER);
					SkillExecuteEvent event = new SkillExecuteEvent(this.playerpatch, skillContainer);
					
					if (skillContainer.canExecute(this.playerpatch, event) && this.player.getVehicle() == null) {
						if (!this.moverPressToggle) {
							this.moverPressToggle = true;
						}
					}
				} else {
					SkillContainer skill = this.playerpatch.getSkill(SkillSlots.MOVER);
					skill.sendExecuteRequest(this.playerpatch, this);
				}
			}
		}
		
		while (isKeyPressed(EpicFightKeyMappings.SWITCH_MODE, false)) {
			if (EpicFightGameRules.CAN_SWITCH_PLAYER_MODE.getRuleValue(this.playerpatch.getOriginal().level())) {
				this.playerpatch.toggleMode();
			} else {
				this.minecraft.gui.getChat().addMessage(Component.translatable("epicfight.messages.mode_switching_disabled").withStyle(ChatFormatting.RED));
			}
		}
		
		while (isKeyPressed(EpicFightKeyMappings.LOCK_ON, false)) {
			this.playerpatch.toggleLockOn();
		}
		
		// Disable swap hand items
		if (this.playerpatch.getEntityState().inaction() || (!this.playerpatch.getHoldingItemCapability(InteractionHand.MAIN_HAND).canBePlacedOffhand())) {
			makeUnpressed(this.minecraft.options.keySwapOffhand);
		}
		
		// Pause here if player is not in battle mode
		if (!this.playerpatch.isEpicFightMode() || Minecraft.getInstance().isPaused()) {
			return;
		}
		
		if (this.player.tickCount - this.lastHotbarLockedTime > 20 && this.hotbarLocked) {
			this.unlockHotkeys();
		}
		
		if (this.weaponInnatePressToggle) {
			if (!isKeyDown(EpicFightKeyMappings.WEAPON_INNATE_SKILL)) {
				this.attackLightPressToggle = true;
				this.weaponInnatePressToggle = false;
				this.weaponInnatePressCounter = 0;
			} else {
				if (EpicFightKeyMappings.WEAPON_INNATE_SKILL.getKey().equals(EpicFightKeyMappings.ATTACK.getKey())) {
					if (this.weaponInnatePressCounter > ClientConfig.longPressCounter) {
						if (this.playerpatch.getSkill(SkillSlots.WEAPON_INNATE).sendExecuteRequest(this.playerpatch, this).shouldReserverKey()) {
							if (!this.player.isSpectator()) {
								this.reserveKey(SkillSlots.WEAPON_INNATE, EpicFightKeyMappings.WEAPON_INNATE_SKILL);
							}
						} else {
							this.lockHotkeys();
						}
						
						this.weaponInnatePressToggle = false;
						this.weaponInnatePressCounter = 0;
					} else {
						this.weaponInnatePressCounter++;
					}
				}
			}
		}
		
		if (this.attackLightPressToggle) {
			SkillSlot slot = (!this.player.onGround() && !this.player.isInWater() && this.player.getDeltaMovement().y > 0.03D) ? SkillSlots.AIR_ATTACK : SkillSlots.BASIC_ATTACK;
			
			if (this.playerpatch.getSkill(slot).sendExecuteRequest(this.playerpatch, this).isExecutable()) {
				this.player.resetAttackStrengthTicker();
				this.releaseAllServedKeys();
			} else {
				if (!this.player.isSpectator() && slot == SkillSlots.BASIC_ATTACK) {
					this.reserveKey(slot, EpicFightKeyMappings.ATTACK);
				}
			}
			
			this.lockHotkeys();
			
			this.attackLightPressToggle = false;
			this.weaponInnatePressToggle = false;
			this.weaponInnatePressCounter = 0;
		}
		
		if (this.sneakPressToggle) {
			if (!isKeyDown(this.options.keyShift)) {
				SkillSlot skillSlot = (this.playerpatch.getEntityState().knockDown()) ? SkillSlots.KNOCKDOWN_WAKEUP : SkillSlots.DODGE;
				SkillContainer skill = this.playerpatch.getSkill(skillSlot);
				
				if (skill.sendExecuteRequest(this.playerpatch, this).shouldReserverKey()) {
					this.reserveKey(skillSlot, this.options.keyShift);
				}
				
				this.sneakPressToggle = false;
				this.sneakPressCounter = 0;
			} else {
				if (this.sneakPressCounter > ClientConfig.longPressCounter) {
					this.sneakPressToggle = false;
					this.sneakPressCounter = 0;
				} else {
					this.sneakPressCounter++;
				}
			}
		}
		
		if (this.currentChargingKey != null) {
			SkillContainer skill = this.playerpatch.getSkill(this.reservedOrChargingSkillSlot);
			
			if (skill.getSkill() instanceof ChargeableSkill chargingSkill) {
				if (!isKeyDown(this.currentChargingKey)) {
					this.chargeKeyUnpressed = true;
				}
				
				if (this.chargeKeyUnpressed) {
					if (this.playerpatch.getSkillChargingTicks() > chargingSkill.getMinChargingTicks()) {
						if (skill.getSkill() != null) {
							skill.sendExecuteRequest(this.playerpatch, this);
						}

						this.releaseAllServedKeys();
					}
				}

				if (this.playerpatch.getSkillChargingTicks() >= chargingSkill.getAllowedMaxChargingTicks()) {
					this.releaseAllServedKeys();
				}
			} else {
				this.releaseAllServedKeys();
			}
		}
		
		if (this.reservedKey != null) {
			if (this.reserveCounter > 0) {
				SkillContainer skill = this.playerpatch.getSkill(this.reservedOrChargingSkillSlot);
				this.reserveCounter--;
				
				if (skill.getSkill() != null) {
					if (skill.sendExecuteRequest(this.playerpatch, this).isExecutable()) {
						this.releaseAllServedKeys();
						this.lockHotkeys();
					}
				}
			} else {
				this.releaseAllServedKeys();
			}
		}
		
		if (!this.playerpatch.getEntityState().canSwitchHoldingItem() || this.hotbarLocked) {
			for (int i = 0; i < 9; ++i) {
				while (this.options.keyHotbarSlots[i].consumeClick());
			}
			
			while (this.options.keyDrop.consumeClick());
		}
	}
	
	private void inputTick(Input input) {
		if (this.moverPressToggle) {
			if (!isKeyDown(this.options.keyJump)) {
				this.moverPressToggle = false;
				this.moverPressCounter = 0;
				
				if (this.player.onGround()) {
					this.player.noJumpDelay = 0;
					input.jumping = true;
				}
			} else {
				if (this.moverPressCounter > ClientConfig.longPressCounter) {
					SkillContainer skill = this.playerpatch.getSkill(SkillSlots.MOVER);
					skill.sendExecuteRequest(this.playerpatch, this);
					
					this.moverPressToggle = false;
					this.moverPressCounter = 0;
				} else {
					this.player.noJumpDelay = 2;
					this.moverPressCounter++;
				}
			}
		}
		
		if (!this.canPlayerMove(this.playerpatch.getEntityState())) {
			input.forwardImpulse = 0F;
			input.leftImpulse = 0F;
			input.up = false;
			input.down = false;
			input.left = false;
			input.right = false;
			input.jumping = false;
			input.shiftKeyDown = false;
			this.player.sprintTriggerTime = -1;
			this.player.setSprinting(false);
		}
		
		if (this.player.isAlive()) {
			this.playerpatch.getEventListener().triggerEvents(EventType.MOVEMENT_INPUT_EVENT, new MovementInputEvent(this.playerpatch, input));
		}
	}
	
	private void reserveKey(SkillSlot slot, KeyMapping keyMapping) {
		this.reservedKey = keyMapping;
		this.reservedOrChargingSkillSlot = slot;
		this.reserveCounter = 8;
	}
	
	private void releaseAllServedKeys() {
		this.chargeKeyUnpressed = true;
		this.currentChargingKey = null;
		this.reservedOrChargingSkillSlot = null;
		this.reserveCounter = -1;
		this.reservedKey = null;
	}
	
	public void setChargingKey(SkillSlot chargingSkillSlot, KeyMapping keyMapping) {
		this.chargeKeyUnpressed = false;
		this.currentChargingKey = keyMapping;
		this.reservedOrChargingSkillSlot = chargingSkillSlot;
		this.reserveCounter = -1;
		this.reservedKey = null;
	}
	
	public void lockHotkeys() {
		this.hotbarLocked = true;
		this.lastHotbarLockedTime = this.player.tickCount;
		
		for (int i = 0; i < 9; ++i) {
			while (this.options.keyHotbarSlots[i].consumeClick());
		}
	}
	
	public void unlockHotkeys() {
		this.hotbarLocked = false;
	}
	
	public void addPacketToSend(Object packet) {
		this.packets.add(packet);
	}
	
	public static boolean isKeyDown(KeyMapping key) {
		if (key.getKey().getType() == InputConstants.Type.KEYSYM) {
			return key.isDown() || GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), key.getKey().getValue()) > 0;
		} else if(key.getKey().getType() == InputConstants.Type.MOUSE) {
			return key.isDown() || GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), key.getKey().getValue()) > 0;
		} else {
			return false;
		}
	}
	
	private static boolean isKeyPressed(KeyMapping key, boolean eventCheck) {
		boolean consumes = key.consumeClick();
		
		if (consumes && eventCheck) {
			int mouseButton = InputConstants.Type.MOUSE == key.getKey().getType() ? key.getKey().getValue() : -1;
			InputEvent.InteractionKeyMappingTriggered inputEvent = net.minecraftforge.client.ForgeHooksClient.onClickInput(mouseButton, key, InteractionHand.MAIN_HAND);
			
	        if (inputEvent.isCanceled()) {
	        	return false;
	        }
		}
        
    	return consumes;
	}
	
	public static void makeUnpressed(KeyMapping keyMapping) {
		while (keyMapping.consumeClick()) {}
		setKeyBind(keyMapping, false);
	}
	
	public static void setKeyBind(KeyMapping key, boolean setter) {
		KeyMapping.set(key.getKey(), setter);
	}
	
	public boolean moverToggling() {
		return this.moverPressToggle;
	}
	
	public boolean sneakToggling() {
		return this.sneakPressToggle;
	}
	
	public boolean attackToggling() {
		return this.attackLightPressToggle;
	}
	
	public boolean weaponInnateToggling() {
		return this.weaponInnatePressToggle;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Mod.EventBusSubscriber(modid = EpicFightMod.MODID, value = Dist.CLIENT)
	public static class Events {
		static ControllEngine controllEngine;
		
		@SubscribeEvent
		public static void mouseScrollEvent(InputEvent.MouseScrollingEvent event) {
			// Disable item switching
			if (controllEngine.minecraft.player != null && controllEngine.playerpatch != null && !controllEngine.playerpatch.getEntityState().canSwitchHoldingItem() && controllEngine.minecraft.screen == null) {
				event.setCanceled(true);
			}
		}
		
		@SubscribeEvent
		public static void moveInputEvent(MovementInputUpdateEvent event) {
			if (controllEngine.playerpatch == null) {
				return;
			}
			
			controllEngine.inputTick(event.getInput());
		}
		
		@SubscribeEvent
		public static void clientTickEndEvent(TickEvent.ClientTickEvent event) {
			if (controllEngine.minecraft.player == null) {
				return;
			}
			
			if (event.phase == TickEvent.Phase.END) {
				for (Object packet : controllEngine.packets) {
					EpicFightNetworkManager.sendToServer(packet);
				}
				
				controllEngine.packets.clear();
			}
		}
		
		@SubscribeEvent
		public static void interactionEvent(InteractionKeyMappingTriggered event) {
			if (controllEngine.minecraft.player == null) {
				return;
			}
			
			if (
				event.getKeyMapping() == controllEngine.minecraft.options.keyAttack &&
				EpicFightKeyMappings.ATTACK.getKey() == controllEngine.minecraft.options.keyAttack.getKey() &&
				controllEngine.minecraft.hitResult.getType() == HitResult.Type.BLOCK &&
				ClientConfig.combatPreferredItems.contains(controllEngine.player.getMainHandItem().getItem())
			) {
				BlockPos bp = ((BlockHitResult)controllEngine.minecraft.hitResult).getBlockPos();
				BlockState bs = controllEngine.minecraft.level.getBlockState(bp);
				
				if (!controllEngine.player.getMainHandItem().getItem().canAttackBlock(bs, controllEngine.player.level(), bp, controllEngine.player) || controllEngine.player.getMainHandItem().getDestroySpeed(bs) <= 1.0F) {
					event.setSwingHand(false);
					event.setCanceled(true);
				}
			}
		}
	}
}