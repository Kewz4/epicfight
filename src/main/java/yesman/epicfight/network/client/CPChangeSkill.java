package yesman.epicfight.network.client;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import yesman.epicfight.api.data.reloader.SkillManager;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

public class CPChangeSkill {
	private final int skillSlotIndex;
	private final int itemSlotIndex;
	private final String skillName;
	private final boolean consumeXp;
	
	public CPChangeSkill() {
		this(0, -1, "", false);
	}
	
	public CPChangeSkill(int skillSlotIndex, int itemSlotIndex, String name, boolean consumeXp) {
		this.skillSlotIndex = skillSlotIndex;
		this.itemSlotIndex = itemSlotIndex;
		this.skillName = name;
		this.consumeXp = consumeXp;
	}
	
	public static CPChangeSkill fromBytes(FriendlyByteBuf buf) {
		CPChangeSkill msg = new CPChangeSkill(buf.readInt(), buf.readInt(), buf.readUtf(), buf.readBoolean());
		return msg;
	}
	
	public static void toBytes(CPChangeSkill msg, FriendlyByteBuf buf) {
		buf.writeInt(msg.skillSlotIndex);
		buf.writeInt(msg.itemSlotIndex);
		buf.writeUtf(msg.skillName);
		buf.writeBoolean(msg.consumeXp);
	}
	
	public static void handle(CPChangeSkill msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			EpicFightCapabilities.getUnparameterizedEntityPatch(ctx.get().getSender(), ServerPlayerPatch.class).ifPresent(playerpatch -> {
				Skill skill = SkillManager.getSkill(msg.skillName);
				playerpatch.getSkill(msg.skillSlotIndex).setSkill(skill);
				
				if (skill.getCategory().learnable()) {
					playerpatch.getSkillCapability().addLearnedSkill(skill);
				}
				
				if (msg.consumeXp) {
					playerpatch.getOriginal().giveExperienceLevels(-skill.getRequiredXp());
				} else {
					if (!playerpatch.getOriginal().isCreative()) {
						playerpatch.getOriginal().getInventory().removeItem(playerpatch.getOriginal().getInventory().getItem(msg.itemSlotIndex));
					}
				}
			});
		});
		ctx.get().setPacketHandled(true);
	}
}
