package yesman.epicfight.config;

import java.io.File;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.loading.FMLPaths;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.world.gamerule.EpicFightGameRules;

public class ConfigManager {
	public static final ForgeConfigSpec COMMON_CONFIG;
	public static final ForgeConfigSpec CLIENT_CONFIG;
	public static final ClientConfig INGAME_CONFIG;
	public static final ForgeConfigSpec.IntValue SKILL_BOOK_MOB_DROP_CHANCE_MODIFIER;
	public static final ForgeConfigSpec.IntValue SKILL_BOOK_CHEST_LOOT_MODIFYER;
	
	static {
		CommentedFileConfig file = CommentedFileConfig.builder(new File(FMLPaths.CONFIGDIR.get().resolve(EpicFightMod.CONFIG_FILE_PATH).toString())).sync().autosave().writingMode(WritingMode.REPLACE).build();
		file.load();
		ForgeConfigSpec.Builder client = new ForgeConfigSpec.Builder();
		ForgeConfigSpec.Builder commond = new ForgeConfigSpec.Builder();
		
		EpicFightGameRules.GAME_RULES.values().forEach(configurableGameRule -> configurableGameRule.defineConfig(commond));
		
		SKILL_BOOK_MOB_DROP_CHANCE_MODIFIER = commond.defineInRange("loot.skill_book_mob_drop_chance_modifier", 0, -100, 100);
		SKILL_BOOK_CHEST_LOOT_MODIFYER = commond.defineInRange("loot.skill_book_chest_drop_chance_modifier", 0, -100, 100);
		
		INGAME_CONFIG = new ClientConfig(client);
		CLIENT_CONFIG = client.build();
		COMMON_CONFIG = commond.build();
	}
	
	public static void loadConfig(ForgeConfigSpec config, String path) {
		CommentedFileConfig file = CommentedFileConfig.builder(new File(path)).sync().autosave().writingMode(WritingMode.REPLACE).build();
		file.load();
		config.setConfig(file);
	}
}