package yesman.epicfight.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PacketDistributor.PacketTarget;
import net.minecraftforge.network.simple.SimpleChannel;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.network.client.*;
import yesman.epicfight.network.server.*;

public class EpicFightNetworkManager {
	private static final String PROTOCOL_VERSION = "1";
	public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(EpicFightMod.MODID, "network_manager"),
			() -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

	public static <MSG> void sendToServer(MSG message) {
		INSTANCE.sendToServer(message);
	}
	
	public static <MSG> void sendToClient(MSG message, PacketTarget packetTarget) {
		INSTANCE.send(packetTarget, message);
	}
	
	public static <MSG> void sendToAll(MSG message) {
		sendToClient(message, PacketDistributor.ALL.noArg());
	}

	public static <MSG> void sendToAllPlayerTrackingThisEntity(MSG message, Entity entity) {
		sendToClient(message, PacketDistributor.TRACKING_ENTITY.with(() -> entity));
	}
	
	public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
		sendToClient(message, PacketDistributor.PLAYER.with(() -> player));
	}
	
	public static <MSG> void sendToAllPlayerTrackingThisEntityWithSelf(MSG message, ServerPlayer entity) {
		sendToClient(message, PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity));
	}
	
	public static <MSG> void sendToAllPlayerTrackingThisChunkWithSelf(MSG message, LevelChunk chunk) {
		sendToClient(message, PacketDistributor.TRACKING_CHUNK.with(() -> chunk));
	}
	
	public static void registerPackets() {
		int id = 0;
		
		INSTANCE.registerMessage(id++, CPExecuteSkill.class, CPExecuteSkill::toBytes, CPExecuteSkill::fromBytes, CPExecuteSkill::handle);
		INSTANCE.registerMessage(id++, CPAnimatorControl.class, CPAnimatorControl::toBytes, CPAnimatorControl::fromBytes, CPAnimatorControl::handle);
		INSTANCE.registerMessage(id++, CPModifyEntityModelYRot.class, CPModifyEntityModelYRot::toBytes, CPModifyEntityModelYRot::fromBytes, CPModifyEntityModelYRot::handle);
		INSTANCE.registerMessage(id++, CPChangePlayerMode.class, CPChangePlayerMode::toBytes, CPChangePlayerMode::fromBytes, CPChangePlayerMode::handle);
		INSTANCE.registerMessage(id++, CPUpdatePlayerInput.class, CPUpdatePlayerInput::toBytes, CPUpdatePlayerInput::fromBytes, CPUpdatePlayerInput::handle);
		INSTANCE.registerMessage(id++, CPSetPlayerTarget.class, CPSetPlayerTarget::toBytes, CPSetPlayerTarget::fromBytes, CPSetPlayerTarget::handle);
		INSTANCE.registerMessage(id++, CPChangeSkill.class, CPChangeSkill::toBytes, CPChangeSkill::fromBytes, CPChangeSkill::handle);
		INSTANCE.registerMessage(id++, CPModifySkillData.class, CPModifySkillData::toBytes, CPModifySkillData::fromBytes, CPModifySkillData::handle);
		INSTANCE.registerMessage(id++, CPCheckAnimationRegistrySync.class, CPCheckAnimationRegistrySync::toBytes, CPCheckAnimationRegistrySync::fromBytes, CPCheckAnimationRegistrySync::handle);
		INSTANCE.registerMessage(id++, CPAnimationVariablePacket.class, CPAnimationVariablePacket::toBytes, CPAnimationVariablePacket::fromBytes, CPAnimationVariablePacket::handle);
		INSTANCE.registerMessage(id++, CPSetStamina.class, CPSetStamina::toBytes, CPSetStamina::fromBytes, CPSetStamina::handle);
		
		INSTANCE.registerMessage(id++, SPChangeSkill.class, SPChangeSkill::toBytes, SPChangeSkill::fromBytes, SPChangeSkill::handle);
		INSTANCE.registerMessage(id++, SPSkillExecutionFeedback.class, SPSkillExecutionFeedback::toBytes, SPSkillExecutionFeedback::fromBytes, SPSkillExecutionFeedback::handle);
		INSTANCE.registerMessage(id++, SPSpawnData.class, SPSpawnData::toBytes, SPSpawnData::fromBytes, SPSpawnData::handle);
		INSTANCE.registerMessage(id++, SPChangeLivingMotion.class, SPChangeLivingMotion::toBytes, SPChangeLivingMotion::fromBytes, SPChangeLivingMotion::handle);
		INSTANCE.registerMessage(id++, SPSetSkillValue.class, SPSetSkillValue::toBytes, SPSetSkillValue::fromBytes, SPSetSkillValue::handle);
		INSTANCE.registerMessage(id++, SPModifyPlayerData.class, SPModifyPlayerData::toBytes, SPModifyPlayerData::fromBytes, SPModifyPlayerData::handle);
		INSTANCE.registerMessage(id++, SPAnimatorControl.class, SPAnimatorControl::toBytes, SPAnimatorControl::fromBytes, SPAnimatorControl::handle);
		INSTANCE.registerMessage(id++, SPPlayAnimationAndSetTarget.class, SPPlayAnimationAndSetTarget::toBytes, SPPlayAnimationAndSetTarget::fromBytes, SPPlayAnimationAndSetTarget::handle);
		INSTANCE.registerMessage(id++, SPMoveAndPlayAnimation.class, SPMoveAndPlayAnimation::toBytes, SPMoveAndPlayAnimation::fromBytes, SPMoveAndPlayAnimation::handle);
		INSTANCE.registerMessage(id++, SPPotion.class, SPPotion::toBytes, SPPotion::fromBytes, SPPotion::handle);
		INSTANCE.registerMessage(id++, SPModifySkillData.class, SPModifySkillData::toBytes, SPModifySkillData::fromBytes, SPModifySkillData::handle);
		INSTANCE.registerMessage(id++, SPChangeGamerule.class, SPChangeGamerule::toBytes, SPChangeGamerule::fromBytes, SPChangeGamerule::handle);
		INSTANCE.registerMessage(id++, SPChangePlayerMode.class, SPChangePlayerMode::toBytes, SPChangePlayerMode::fromBytes, SPChangePlayerMode::handle);
		INSTANCE.registerMessage(id++, SPAddLearnedSkill.class, SPAddLearnedSkill::toBytes, SPAddLearnedSkill::fromBytes, SPAddLearnedSkill::handle);
		INSTANCE.registerMessage(id++, SPDatapackSync.class, SPDatapackSync::toBytes, SPDatapackSync::fromBytes, SPDatapackSync::handle);
		INSTANCE.registerMessage(id++, SPDatapackSyncSkill.class, SPDatapackSyncSkill::toBytes, SPDatapackSyncSkill::fromBytes, SPDatapackSyncSkill::handle);
		INSTANCE.registerMessage(id++, SPSetAttackTarget.class, SPSetAttackTarget::toBytes, SPSetAttackTarget::fromBytes, SPSetAttackTarget::handle);
		INSTANCE.registerMessage(id++, SPClearSkills.class, SPClearSkills::toBytes, SPClearSkills::fromBytes, SPClearSkills::handle);
		INSTANCE.registerMessage(id++, SPRemoveSkill.class, SPRemoveSkill::toBytes, SPRemoveSkill::fromBytes, SPRemoveSkill::handle);
		INSTANCE.registerMessage(id++, SPFracture.class, SPFracture::toBytes, SPFracture::fromBytes, SPFracture::handle);
		INSTANCE.registerMessage(id++, SPUpdatePlayerInput.class, SPUpdatePlayerInput::toBytes, SPUpdatePlayerInput::fromBytes, SPUpdatePlayerInput::handle);
		INSTANCE.registerMessage(id++, SPAddOrRemoveSkillData.class, SPAddOrRemoveSkillData::toBytes, SPAddOrRemoveSkillData::fromBytes, SPAddOrRemoveSkillData::handle);
		INSTANCE.registerMessage(id++, SPAnimationVariablePacket.class, SPAnimationVariablePacket::toBytes, SPAnimationVariablePacket::fromBytes, SPAnimationVariablePacket::handle);
	}
}