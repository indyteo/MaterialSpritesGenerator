package fr.theoszanto.mc.materialspritesgenerator;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import fr.theoszanto.mc.materialspritesgenerator.rules.MappingRule;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fr.theoszanto.mc.materialspritesgenerator.rules.MappingRule.rule;

public class MaterialSpritesGenerator extends JavaPlugin {
	public static final Map<Material, Component> SPRITES = new EnumMap<>(Material.class);
	public static final Map<String, List<String>> ATLASES = new HashMap<>();

	@Override
	public void onEnable() {
		this.saveDefaultConfig();
		FileConfiguration config = this.getConfig();
		String templateFileName = config.getString("template", "MaterialSprites.template.java");
		String packageOverride = config.getString("package");
		this.saveResource(templateFileName, false);
		for (String atlas : config.getStringList("atlases")) {
			try {
				this.saveResource("atlases/" + atlas + ".txt", false);
			} catch (IllegalArgumentException ignored) {}
		}

		File dataFolder = this.getDataFolder();
		loadAtlases(dataFolder);

		String template;
		try {
			template = Files.readString(new File(dataFolder, templateFileName).toPath());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		StringBuilder code = new StringBuilder();
		for (Material material : Registry.MATERIAL) {
			String resourceName = material.key().value();
			for (MappingRule rule : RULES) {
				if (rule.matches(resourceName)) {
					code.append("SPRITES.put(Material.")
							.append(material)
							.append(", ")
							.append(rule.componentCode(resourceName))
							.append(");\n");
					SPRITES.put(material, rule.component(resourceName));
					break;
				}
			}
		}

		try {
			Files.writeString(new File(dataFolder, config.getString("output", "MaterialSprites.java")).toPath(), template.lines()
					.map(line -> {
						if (packageOverride != null) {
							Matcher matcher = Pattern.compile("^package [^;]+;(.*)$").matcher(line);
							if (matcher.matches())
								return "package " + packageOverride + ";" + matcher.group(1);
						}
						int n = line.indexOf("// GENERATE HERE");
						if (n == -1)
							return line;
						String prefix = line.substring(0, n);
						return prefix + "// Auto-generated mappings\n" + code.toString().lines()
								.map(l -> prefix + l)
								.collect(Collectors.joining("\n"));
					})
					.collect(Collectors.joining("\n")) + "\n");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		int spritesFound = SPRITES.size();
		int spritesNotFound = 0;
		for (Material material : Registry.MATERIAL) {
			if (!SPRITES.containsKey(material)) {
				this.getLogger().warning("No sprite found for material: " + material);
				spritesNotFound++;
			}
		}
		this.getLogger().info("Sprites found: " + spritesFound);
		this.getLogger().info("Sprites not found: " + spritesNotFound);

		if (config.getBoolean("stop-server-after-generation", false)) {
			this.getLogger().info("Material sprites mappings generation done, stopping server as requested!");
			Bukkit.shutdown();
			return;
		}

		Objects.requireNonNull(this.getCommand("sprites")).setExecutor(new SpritesCommand());
	}

	private static final String MINECRAFT = "minecraft:";
	private static final String ITEM = MINECRAFT + "item/";
	private static final String BLOCK = MINECRAFT + "block/";

	private static final String ITEMS_ATLAS = "items";

	private static final List<MappingRule> RULES = List.of(
			//<editor-fold desc="Mapping rules">
			rule().suffix("air").overwrite("NOTHING").code().resolved(Component.text("  ")),

			rule().exact(Material.HEAVY_CORE).overwrite("5069a0f8bdd8c08693e60b93bdfe800abdf96eb1dc9e7efc38987311da4e0f34").head(),
			rule().exact(Material.DRIED_GHAST).overwrite("4650955792174fc48143643f2d9ddcedaabc822d651631a81c9f014a0c6181f7").head(),
			rule().either(Material.END_GATEWAY, Material.END_PORTAL).overwrite("5639d4079d6b7c0a913cff608ec43fa39a72fca5d64fb22700b1cc6c46cc69c2").head(),
			rule().exact(Material.CHEST).overwrite("758305e527fcf08525fdff07e4cd6e79774acdd426cc27327d45df8a5ff86419").head(),
			rule().exact(Material.TRAPPED_CHEST).overwrite("275bcff2e74deed37a319a1f404e70d06a5f360cacee99c71346f38560cbd72a").head(),
			rule().exact(Material.ENDER_CHEST).overwrite("87acb8b99d478ba35053e9f212acb5c55cc144840468fd0242b39f5bd75acb41").head(),
			rule().suffix("oxidized_copper_chest").overwrite("33af3bfdf486eeff78c49da400907b45a4c4db4d6fb159909a3217e069c640b1").head(),
			rule().suffix("weathered_copper_chest").overwrite("7b4b972335ae434b004ec7ad2e3706c757c14f210b2b1132d08e384ea722e45b").head(),
			rule().suffix("exposed_copper_chest").overwrite("5c092ddd68df26a30afbab8247a581cb981fb3ec8114ab95519e15083c480139").head(),
			rule().suffix("copper_chest").overwrite("2a7848dfe597995d04e822ac53878a29ed81e1f152848f1823cb3529c47e1cef").head(),
			rule().suffix("oxidized_copper_golem_statue").overwrite("d1e2791f850af92271763f32e3d440573952135c73083715665d7adcff9b06c9").head(),
			rule().suffix("weathered_copper_golem_statue").overwrite("ff401456e0b4da388e6f21a8683608501ecb4988af76dc4c4ace5c92fe34e9f1").head(),
			rule().suffix("exposed_copper_golem_statue").overwrite("ad8cf824534a3e63cb2278c3a6ec5b3b17d7c8e178ac67d793e63d0eea9b048c").head(),
			rule().suffix("copper_golem_statue").overwrite("d998651718b318a2ca6a4a21ed201f0c65a14d0fcde7dd044bad33116cd5e026").head(),

			rule().regex("creeper_(wall_)?head").overwrite("ba5e95735a3f3772b1b485e1502807ae396a72c61bfd36ab41fa71bec2f64aa2").head(),
			rule().regex("dragon_(wall_)?head").overwrite("f2191029c8ccd1ebd95207720ceae6944e5e20d8848bdf3d67e0d0e1101b78a1").head(),
			rule().regex("piglin_(wall_)?head").overwrite("90bc9dbb4404b800f8cf0256220ff74b0b71dba8b66600b6734f4d63361618f5").head(),
			rule().regex("player_(wall_)?head").overwrite("d5c4ee5ce20aed9e33e866c66caa37178606234b3721084bf01d13320fb2eb3f").head(),
			rule().regex("skeleton_(wall_)?skull").overwrite("22795c3c6f36d67decf9a3195e128040bec5226b055f2b16d46fa19a9180e023").head(),
			rule().regex("wither_skeleton_(wall_)?skull").overwrite("ba96e9d76bed30090ce6e2d8425996594eec6d68ac88cf07356e9814834243ec").head(),
			rule().regex("zombie_(wall_)?head").overwrite("d97e4259379a06f24843c1bb42f2df35c13f801ad079f715bded488db8f57c3").head(),

			rule().exact(Material.WATER).prepend(BLOCK).append("_still").sprite().color(TextColor.color(0x3F76E4)),
			rule().exact(Material.LAVA).prepend(BLOCK).append("_still").sprite(),
			rule().exact(Material.TRIPWIRE).overwrite(ITEM + "string").sprite().atlas(ITEMS_ATLAS),
			rule().exact(Material.LIGHT).prepend(ITEM).append("_15").sprite().atlas(ITEMS_ATLAS),
			rule().either(Material.FIRE, Material.SOUL_FIRE, Material.SUSPICIOUS_GRAVEL, Material.SUSPICIOUS_SAND).prepend(BLOCK).append("_0").sprite(),
			rule().suffix("anvil").prepend(BLOCK).append("_top").sprite(),
			rule().suffix("_dripleaf").prepend(BLOCK).append("_top").sprite(),
			rule().either(Material.DAYLIGHT_DETECTOR, Material.SCAFFOLDING, Material.LILAC, Material.ROSE_BUSH, Material.PEONY, Material.TALL_SEAGRASS, Material.CARTOGRAPHY_TABLE).prepend(BLOCK).append("_top").sprite(),
			rule().exact(Material.GRINDSTONE).prepend(BLOCK).append("_round").sprite(),
			rule().exact(Material.CRAFTER).prepend(BLOCK).append("_north").sprite(),
			rule().exact(Material.WHEAT).prepend(BLOCK).append("_stage7").sprite(),
			rule().exact(Material.TORCHFLOWER_CROP).prepend(BLOCK).append("_stage1").sprite(),
			rule().exact(Material.FROSTED_ICE).prepend(BLOCK).append("_3").sprite(),
			rule().exact(Material.TRIAL_SPAWNER).prepend(BLOCK).append("_side_inactive").sprite(),
			rule().exact(Material.VAULT).prepend(BLOCK).append("_front_off").sprite(),
			rule().exact(Material.TEST_BLOCK).prepend(BLOCK).append("_start").sprite(),
			rule().exact(Material.COCOA).prepend(ITEM).append("_beans").sprite().atlas(ITEMS_ATLAS),
			rule().exact(Material.CROSSBOW).prepend(ITEM).append("_standby").sprite().atlas(ITEMS_ATLAS),
			rule().exact(Material.CHISELED_BOOKSHELF).prepend(BLOCK).append("_occupied").sprite(),
			rule().exact(Material.CALIBRATED_SCULK_SENSOR).prepend(BLOCK).append("_input_side").sprite(),
			rule().either(Material.SHELF_MUSHROOM, Material.STRAW_BED).prepend(BLOCK).append("_particle").sprite(),
			rule().regex("smooth_quartz(_slab|_stairs)?").overwrite(BLOCK + "quartz_block_bottom").sprite(),
			rule().regex("smooth_sandstone(_slab|_stairs)?").overwrite(BLOCK + "sandstone_top").sprite(),
			rule().regex("smooth_red_sandstone(_slab|_stairs)?").overwrite(BLOCK + "red_sandstone_top").sprite(),
			rule().exact(Material.HEAVY_WEIGHTED_PRESSURE_PLATE).overwrite(BLOCK + "iron_block").sprite(),
			rule().exact(Material.LIGHT_WEIGHTED_PRESSURE_PLATE).overwrite(BLOCK + "gold_block").sprite(),
			rule().exact(Material.POTTED_CACTUS).overwrite(BLOCK + "cactus_side").sprite(),
			rule().exact(Material.NETHER_BRICK_FENCE).overwrite(BLOCK + "nether_bricks").sprite(),
			rule().exact(Material.MOSS_CARPET).overwrite(BLOCK + "moss_block").sprite(),
			rule().either(Material.PISTON_HEAD, Material.MOVING_PISTON).overwrite(BLOCK + "piston_top").sprite(),
			rule().exact(Material.STICKY_PISTON).overwrite(BLOCK + "piston_top_sticky").sprite(),
			rule().exact(Material.PETRIFIED_OAK_SLAB).overwrite(BLOCK + "oak_planks").sprite(),
			rule().exact(Material.DEBUG_STICK).overwrite(ITEM + "stick").sprite().atlas(ITEMS_ATLAS),
			rule().exact(Material.ENCHANTED_GOLDEN_APPLE).overwrite(ITEM + "golden_apple").sprite().atlas(ITEMS_ATLAS),
			rule().exact(Material.TIPPED_ARROW).overwrite(ITEM + "arrow").sprite().atlas(ITEMS_ATLAS),
			rule().exact(Material.BAMBOO_SAPLING).overwrite(BLOCK + "bamboo_stage0").sprite(),
			rule().exact(Material.DRIED_KELP_BLOCK).overwrite(BLOCK + "dried_kelp_side").sprite(),
			rule().exact(Material.LECTERN).overwrite(BLOCK + "bookshelf").sprite(),
			rule().either(Material.LAVA_CAULDRON, Material.POWDER_SNOW_CAULDRON, Material.WATER_CAULDRON).overwrite(ITEM + "cauldron").sprite().atlas(ITEMS_ATLAS),
			rule().exact(Material.DECORATED_POT).overwrite(MINECRAFT + "entity/decorated_pot/danger_pottery_pattern").sprite().atlas("decorated_pot"),
			rule().exact(Material.BUBBLE_COLUMN).overwrite(MINECRAFT + "bubble").sprite().atlas("particles"),
			rule().exact(Material.SHIELD).prepend(MINECRAFT + "container/slot/").sprite().atlas("gui"),

			rule().exact(Material.REDSTONE_WIRE).overwrite(BLOCK + "redstone_dust_line0").sprite().color(NamedTextColor.DARK_RED),
			rule().exact(Material.VINE).prepend(BLOCK).sprite().color(TextColor.color(0x48b518)),
			rule().exact(Material.LILY_PAD).prepend(BLOCK).sprite().color(TextColor.color(0x71c35c)),
			rule().either(Material.SHORT_GRASS, Material.FERN, Material.BUSH).prepend(BLOCK).sprite().color(TextColor.color(0x91bd59)),
			rule().either(Material.TALL_GRASS, Material.LARGE_FERN).prepend(BLOCK).append("_top").sprite().color(TextColor.color(0x91bd59)),
			rule().exact(Material.POTTED_FERN).overwrite(BLOCK + "fern").sprite().color(TextColor.color(0x91bd59)),
			rule().regex("(attached_)?(melon|pumpkin)_stem").prepend(BLOCK).sprite().color(TextColor.color(0xa9960a)),
			rule().regex("((dark_)?oak|jungle|acacia)_leaves").prepend(BLOCK).sprite().color(TextColor.color(0x48b518)),
			rule().exact(Material.MANGROVE_LEAVES).prepend(BLOCK).sprite().color(TextColor.color(0x92c648)),
			rule().exact(Material.SPRUCE_LEAVES).prepend(BLOCK).sprite().color(TextColor.color(0x619961)),
			rule().exact(Material.BIRCH_LEAVES).prepend(BLOCK).sprite().color(TextColor.color(0x80a755)),
			rule().prefix("leather_").prepend(ITEM).sprite().atlas(ITEMS_ATLAS).color(TextColor.color(Bukkit.getItemFactory().getDefaultLeatherColor().asRGB())),

			rule().suffix("_fence", true).prepend(BLOCK).append("_planks").sprite(),
			rule().suffix("_shelf", true).prepend(BLOCK).append("_planks").sprite(),

			rule().prepend(ITEM).sprite().atlas(ITEMS_ATLAS),
			rule().prepend(ITEM).append("_00").sprite().atlas(ITEMS_ATLAS),
			rule().prepend(BLOCK).sprite(),
			rule().prepend(BLOCK).append("_front").sprite(),
			rule().prepend(BLOCK).append("_side").sprite(),
			rule().prepend(BLOCK).append("_side0").sprite(),
			rule().prepend(BLOCK).append("_stage3").sprite(),

			rule().prefix("waxed_", true).prepend(ITEM).sprite().atlas(ITEMS_ATLAS),
			rule().prefix("waxed_", true).prepend(BLOCK).sprite(),
			rule().prefix("potted_", true).prepend(ITEM).sprite().atlas(ITEMS_ATLAS),
			rule().prefix("potted_", true).prepend(BLOCK).sprite(),
			rule().prefix("infested_", true).prepend(BLOCK).sprite(),

			rule().suffix("_block", true).prepend(BLOCK).sprite(),
			rule().suffix("_wall", true).prepend(BLOCK).append("s").sprite(),
			rule().suffix("_wall", true).prepend(BLOCK).sprite(),
			rule().suffix("_slab", true).prepend(BLOCK).multi(rule -> List.of(
					rule.append("_planks").sprite(),
					rule.append("_block").sprite(),
					rule.append("_block_side").sprite(),
					rule.append("s").sprite(),
					rule.sprite()
			)),
			rule().suffix("_stairs", true).prepend(BLOCK).multi(rule -> List.of(
					rule.append("_planks").sprite(),
					rule.append("_block").sprite(),
					rule.append("_block_side").sprite(),
					rule.append("s").sprite(),
					rule.sprite()
			)),
			rule().suffix("_pressure_plate", true).prepend(BLOCK).append("_planks").sprite(),
			rule().suffix("_pressure_plate", true).prepend(BLOCK).sprite(),
			rule().suffix("_button", true).prepend(BLOCK).append("_planks").sprite(),
			rule().suffix("_button", true).prepend(BLOCK).sprite(),
			rule().suffix("_fence_gate", true).prepend(BLOCK).append("_planks").sprite(),
			rule().suffix("_wood", true).prepend(BLOCK).append("_log").sprite(),
			rule().suffix("_hyphae", true).prepend(BLOCK).append("_stem").sprite(),
			rule().suffix("_cake", true).prepend(ITEM).sprite().atlas(ITEMS_ATLAS),
			rule().suffix("_pane", true).prepend(BLOCK).sprite(),
			rule().suffix("_carpet", true).prepend(BLOCK).append("_wool").sprite(),
			rule().suffix("_bed", true).prepend(BLOCK).append("_wool").sprite(),

			rule().suffix("wall_torch", true).prepend(BLOCK).append("torch").sprite(),
			rule().suffix("_wall_sign", true).prepend(ITEM).append("_sign").sprite().atlas(ITEMS_ATLAS),
			rule().suffix("_wall_hanging_sign", true).prepend(ITEM).append("_hanging_sign").sprite().atlas(ITEMS_ATLAS),
			rule().suffix("_wall_fan", true).prepend(BLOCK).append("_fan").sprite(),

			rule().prefix("waxed_", true).suffix("_slab", true).prepend(BLOCK).sprite(),
			rule().prefix("waxed_", true).suffix("_stairs", true).prepend(BLOCK).sprite(),

			rule().suffix("_wall_banner", true).prepend(MINECRAFT).append("_banner").sprite().atlas("map_decorations"),
			rule().suffix("_banner").prepend(MINECRAFT).sprite().atlas("map_decorations")
			//</editor-fold>
	);

	private static void loadAtlases(File dataFolder) {
		try (Stream<Path> list = Files.list(new File(dataFolder, "atlases").toPath())) {
			ATLASES.putAll(list.collect(Collectors.toMap(
					path -> {
						String name = path.toFile().getName();
						return name.endsWith(".txt") ? name.substring(0, name.length() - ".txt".length()) : name;
					},
					path -> {
						try {
							return Files.readAllLines(path).stream()
									.map(String::trim)
									.map(s -> s.split("\t")[0])
									.filter(Predicate.not(String::isEmpty))
									.toList();
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					}
			)));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
