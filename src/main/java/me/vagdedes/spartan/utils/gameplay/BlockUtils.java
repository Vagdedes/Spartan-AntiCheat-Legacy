package me.vagdedes.spartan.utils.gameplay;

import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.objects.replicates.SpartanBlock;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.server.MaterialUtils;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlockUtils {

    private static final Material
            water = MaterialUtils.get("water"),
            lava = MaterialUtils.get("lava"),
            web = MaterialUtils.get("web"),
            head = MaterialUtils.get("head"),
            iron_bars = MaterialUtils.get("iron_bars"),
            cake = MaterialUtils.get("cake"),
            magma = MaterialUtils.get("magma"),
            beetroot_block = MaterialUtils.get("beetroot_block");

    private static Method getBreakSpeed = null;
    private static final Set<Material> air, solid, sensitive, full_solid, editable, chest, plate, ice, glass, glass_pane, step, climbable,
            door, entity_blocks, trap_door, liquid, banner, carpet, bed, shulker_box, stairs, fence, fence_gate, heads, leaves, egg,
            coral_fan, pot, anvil, cobble_walls, terracotta, concrete, candle, candleCake, dripleaf, ores, wood, wool, wire;

    public static final double
            hitbox_extra = 0.35,
            hitbox_max = 0.3,
            hitbox = 0.298,
            hitbox_safe = 0.25,
            hitbox_small = 0.1;

    public static boolean endsWith(String s, String ending) {
        return s.endsWith(ending) && !s.contains("LEGACY_");
    }

    private static boolean startsWith(String s, String start) {
        return s.startsWith(start) && !s.contains("LEGACY_");
    }

    private static boolean contains(String s, String start) {
        return s.contains(start) && !s.contains("LEGACY_");
    }

    // Separator

    static {
        try {
            getBreakSpeed = Block.class.getMethod("getBreakSpeed", Player.class);
        } catch (Exception ignored) {
        }

        // Separator
        Set<Material> builder = new HashSet<>();
        Material[] materials = Material.values();

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            builder.add(Material.BUBBLE_COLUMN);
            builder.add(Material.KELP);
            builder.add(Material.KELP_PLANT);
            builder.add(Material.SEAGRASS);
            builder.add(Material.TALL_SEAGRASS);
            builder.add(Material.SEA_PICKLE);
            builder.add(Material.WATER);
            builder.add(Material.LAVA);
        } else {
            builder.add(Material.getMaterial("STATIONARY_WATER"));
            builder.add(Material.getMaterial("STATIONARY_LAVA"));
        }
        liquid = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            builder.add(Material.CHORUS_PLANT);
            builder.add(Material.END_CRYSTAL);
        }
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14)) {
                if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16)) {
                    if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
                        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_19)) {
                            //todo calibrated sensor
                            builder.add(Material.SCULK_SENSOR);
                            builder.add(Material.SCULK_SHRIEKER);
                            builder.add(Material.MUD);
                        }
                        builder.add(Material.LIGHTNING_ROD);
                        builder.add(Material.POINTED_DRIPSTONE);
                        builder.add(Material.SMALL_AMETHYST_BUD);
                        builder.add(Material.MEDIUM_AMETHYST_BUD);
                        builder.add(Material.LARGE_AMETHYST_BUD);
                        builder.add(Material.AMETHYST_CLUSTER);
                        builder.add(Material.POWDER_SNOW_CAULDRON);
                        builder.add(Material.LAVA_CAULDRON);
                        builder.add(Material.WATER_CAULDRON);
                        builder.add(Material.CAVE_VINES);
                        builder.add(Material.CAVE_VINES_PLANT);
                    }
                    builder.add(Material.SOUL_CAMPFIRE);
                    builder.add(Material.TWISTING_VINES);
                    builder.add(Material.TWISTING_VINES_PLANT);
                    builder.add(Material.WEEPING_VINES);
                    builder.add(Material.WEEPING_VINES_PLANT);
                    builder.add(Material.CHAIN);
                    builder.add(Material.SOUL_LANTERN);
                }
                builder.add(Material.BELL);
                builder.add(Material.LANTERN);
                builder.add(Material.CAMPFIRE);
                builder.add(Material.COMPOSTER);
                builder.add(Material.LECTERN);
                builder.add(Material.GRINDSTONE);
                builder.add(Material.STONECUTTER);
                builder.add(Material.BAMBOO);
            }
            builder.add(Material.PISTON_HEAD);
            builder.add(Material.COBBLESTONE_WALL);
            builder.add(Material.MOSSY_COBBLESTONE_WALL);
            builder.add(Material.IRON_BARS);
            builder.add(Material.PISTON);
            builder.add(Material.STICKY_PISTON);
            builder.add(Material.COBWEB);
            builder.add(Material.FARMLAND);
            builder.add(Material.TURTLE_EGG);
            builder.add(Material.CONDUIT);
        } else {
            builder.add(Material.getMaterial("PISTON_EXTENSION"));
            builder.add(Material.getMaterial("COBBLE_WALL"));
            builder.add(Material.getMaterial("IRON_FENCE"));
            builder.add(Material.getMaterial("SOIL"));
            builder.add(Material.getMaterial("PISTON_BASE"));
            builder.add(Material.getMaterial("PISTON_STICKY_BASE"));
            builder.add(Material.getMaterial("WEB"));
        }

        for (Material m : materials) {
            String s = m.toString();

            if (endsWith(s, "_HANGING_SIGN")) {
                builder.add(m);
            }
        }
        builder.add(Material.COCOA);
        builder.add(Material.DRAGON_EGG);
        builder.add(Material.HOPPER);
        builder.add(Material.FLOWER_POT);
        builder.add(Material.BREWING_STAND);
        builder.add(Material.CAULDRON);
        builder.add(Material.LADDER);
        builder.add(Material.VINE);
        builder.add(Material.SNOW);
        builder.add(Material.SNOW_BLOCK);
        builder.add(Material.CACTUS);
        builder.add(Material.SOUL_SAND);
        builder.add(MaterialUtils.get("daylight_detector_1"));
        builder.add(MaterialUtils.get("daylight_detector_2"));
        builder.add(MaterialUtils.get("repeater_on"));
        builder.add(MaterialUtils.get("repeater_off"));
        builder.add(MaterialUtils.get("comparator_on"));
        builder.add(MaterialUtils.get("comparator_off"));
        builder.add(MaterialUtils.get("enchanting_table"));
        builder.add(MaterialUtils.get("lily_pad"));
        builder.add(MaterialUtils.get("end_portal_frame"));
        builder.add(MaterialUtils.get("cake"));
        full_solid = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            builder.add(Material.END_CRYSTAL);
            builder.add(Material.CHORUS_PLANT);
        }
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14)) {
                if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15)) {
                    if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16)) {
                        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
                            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_19)) {
                                builder.add(Material.MUD);
                            }
                            builder.add(Material.GLOW_LICHEN);
                            builder.add(Material.GLOW_ITEM_FRAME);
                            builder.add(Material.AZALEA);
                            builder.add(Material.FLOWERING_AZALEA);
                            builder.add(Material.TINTED_GLASS);
                            builder.add(Material.CALCITE);
                        }
                        builder.add(Material.WARPED_WART_BLOCK);
                        builder.add(Material.NETHER_WART_BLOCK);
                        builder.add(Material.WARPED_NYLIUM);
                        builder.add(Material.CRIMSON_NYLIUM);
                    }
                    builder.add(Material.HONEY_BLOCK);
                }
                builder.add(Material.BAMBOO);
            }
            builder.add(Material.TURTLE_EGG);
            builder.add(Material.BROWN_MUSHROOM);
            builder.add(Material.RED_MUSHROOM);
            builder.add(Material.BROWN_MUSHROOM_BLOCK);
            builder.add(Material.RED_MUSHROOM_BLOCK);
            builder.add(Material.MUSHROOM_STEM);
            builder.add(Material.CUT_SANDSTONE);
            builder.add(Material.CUT_RED_SANDSTONE);
            builder.add(Material.PODZOL);

            for (Material m : materials) {
                String s = m.toString();

                if (startsWith(s, "INFESTED_")) {
                    builder.add(m);
                }
            }
        } else {
            builder.add(Material.getMaterial("HUGE_MUSHROOM_1"));
            builder.add(Material.getMaterial("HUGE_MUSHROOM_2"));
        }
        builder.add(Material.ITEM_FRAME);
        builder.add(Material.LADDER);
        builder.add(Material.TNT);
        builder.add(Material.COCOA);
        builder.add(Material.NETHERRACK);
        builder.add(Material.GLASS);
        builder.add(Material.SANDSTONE);
        builder.add(Material.SANDSTONE_STAIRS);
        builder.add(Material.QUARTZ_BLOCK);
        builder.add(Material.QUARTZ_STAIRS);
        builder.add(Material.SNOW);
        builder.add(Material.SNOW_BLOCK);
        builder.add(Material.SOUL_SAND);
        builder.add(Material.PUMPKIN);
        builder.add(MaterialUtils.get("lily_pad"));
        builder.add(MaterialUtils.get("daylight_detector_1"));
        builder.add(MaterialUtils.get("daylight_detector_2"));
        builder.add(MaterialUtils.get("repeater_on"));
        builder.add(MaterialUtils.get("repeater_off"));
        builder.add(MaterialUtils.get("comparator_on"));
        builder.add(MaterialUtils.get("comparator_off"));
        builder.add(MaterialUtils.get("enchanting_table"));
        sensitive = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14)) {
            builder.add(Material.SCAFFOLDING);
            builder.add(Material.WITHER_ROSE);
            builder.add(Material.SWEET_BERRY_BUSH);
            builder.add(Material.CORNFLOWER);
            builder.add(Material.LILY_OF_THE_VALLEY);
        } else {
            builder.add(Material.getMaterial("SIGN"));
        }
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16)) {
                if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
                    if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_19)) {
                        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_20)) {
                            builder.add(Material.PINK_PETALS);
                        }
                        builder.add(Material.SCULK_VEIN);
                    }
                    builder.add(Material.GLOW_LICHEN);
                    builder.add(Material.SPORE_BLOSSOM);
                    builder.add(Material.HANGING_ROOTS);
                    builder.add(Material.CAVE_VINES);
                    builder.add(Material.CAVE_VINES_PLANT);
                    builder.add(Material.POWDER_SNOW);
                    builder.add(Material.LIGHT);
                }
                builder.add(Material.SOUL_TORCH);
                builder.add(Material.SOUL_WALL_TORCH);
                builder.add(Material.SOUL_FIRE);
                builder.add(Material.TWISTING_VINES);
                builder.add(Material.TWISTING_VINES_PLANT);
                builder.add(Material.WEEPING_VINES);
                builder.add(Material.WEEPING_VINES_PLANT);
                builder.add(Material.WARPED_ROOTS);
                builder.add(Material.CRIMSON_ROOTS);
                builder.add(Material.WARPED_FUNGUS);
                builder.add(Material.CRIMSON_FUNGUS);
                builder.add(Material.NETHER_SPROUTS);
            }
            builder.add(Material.REDSTONE_TORCH);
            builder.add(Material.REDSTONE_WALL_TORCH);
            builder.add(Material.WALL_TORCH);
            builder.add(Material.RAIL);
            builder.add(Material.END_PORTAL);
            builder.add(Material.NETHER_PORTAL);
            builder.add(Material.MOVING_PISTON);
            builder.add(Material.DANDELION);
            builder.add(Material.POPPY);
            builder.add(Material.SUGAR_CANE);
            builder.add(Material.TALL_GRASS);
            builder.add(Material.POTATOES);
            builder.add(Material.CARROTS);
            builder.add(Material.NETHER_WART);
            builder.add(Material.ACTIVATOR_RAIL);
            builder.add(Material.DETECTOR_RAIL);
            builder.add(Material.POWERED_RAIL);
            builder.add(Material.BEETROOT_SEEDS);
            builder.add(Material.MELON_SEEDS);
            builder.add(Material.PUMPKIN_SEEDS);
            builder.add(Material.WHEAT);
            builder.add(Material.WHEAT_SEEDS);
            builder.add(Material.BUBBLE_COLUMN);
            builder.add(Material.CAVE_AIR);
            builder.add(Material.VOID_AIR);
            builder.add(Material.KELP);
            builder.add(Material.KELP_PLANT);
            builder.add(Material.BEETROOTS);
            builder.add(Material.GRASS);
            builder.add(Material.FERN);
            builder.add(Material.LARGE_FERN);
            builder.add(Material.SUNFLOWER);
            builder.add(Material.AZURE_BLUET);
            builder.add(Material.ATTACHED_MELON_STEM);
            builder.add(Material.ATTACHED_PUMPKIN_STEM);
            builder.add(Material.ROSE_BUSH);
            builder.add(Material.ALLIUM);
            builder.add(Material.OXEYE_DAISY);
            builder.add(Material.BLUE_ORCHID);
            builder.add(Material.LILAC);
            builder.add(Material.PEONY);
            builder.add(Material.COBWEB);

            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "_BUTTON") || !startsWith(s, "POTTED_") && (endsWith(s, "_SAPLING") || endsWith(s, "_TULIP"))) {
                    builder.add(m);
                }
            }
        } else {
            builder.add(Material.getMaterial("REDSTONE_TORCH_ON"));
            builder.add(Material.getMaterial("REDSTONE_TORCH_OFF"));
            builder.add(Material.getMaterial("SEEDS"));
            builder.add(Material.getMaterial("RAILS"));
            builder.add(Material.getMaterial("ENDER_PORTAL"));
            builder.add(Material.getMaterial("PORTAL"));
            builder.add(Material.getMaterial("PISTON_MOVING_PIECE"));
            builder.add(Material.getMaterial("CROPS"));
            builder.add(Material.getMaterial("SIGN_POST"));
            builder.add(Material.getMaterial("RED_ROSE"));
            builder.add(Material.getMaterial("FLOWER"));
            builder.add(Material.getMaterial("YELLOW_FLOWER"));
            builder.add(Material.getMaterial("SUGAR_CANE_BLOCK"));
            builder.add(Material.getMaterial("SAPLING"));
            builder.add(Material.getMaterial("LONG_GRASS"));
            builder.add(Material.getMaterial("NETHER_WARTS"));
            builder.add(Material.getMaterial("DOUBLE_PLANT"));
            builder.add(Material.getMaterial("NETHER_WARTS"));
            builder.add(Material.getMaterial("WOOD_BUTTON"));
            builder.add(Material.STONE_BUTTON);
            builder.add(Material.getMaterial("WEB"));
        }
        for (Material m : materials) {
            String s = m.toString();

            if (endsWith(s, "_SIGN") && !contains(s, "_HANGING_")) {
                builder.add(m);
            }
        }
        builder.add(Material.SUGAR_CANE);
        builder.add(Material.AIR);
        builder.add(Material.BROWN_MUSHROOM);
        builder.add(Material.RED_MUSHROOM);
        builder.add(Material.TORCH);
        builder.add(Material.TRIPWIRE);
        builder.add(Material.TRIPWIRE_HOOK);
        builder.add(Material.REDSTONE_WIRE);
        builder.add(Material.ACTIVATOR_RAIL);
        builder.add(Material.DETECTOR_RAIL);
        builder.add(Material.POWERED_RAIL);
        builder.add(Material.MELON_SEEDS);
        builder.add(Material.PUMPKIN_SEEDS);
        builder.add(Material.PUMPKIN_STEM);
        builder.add(Material.MELON_STEM);
        builder.add(Material.CARROT);
        builder.add(Material.FIRE);
        builder.add(Material.POTATO);
        builder.add(Material.LEVER);
        builder.add(Material.DEAD_BUSH);
        builder.add(Material.VINE);
        solid = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14)) {
                if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
                    builder.add(Material.GLOW_ITEM_FRAME);
                }
                builder.add(Material.SMITHING_TABLE);
                builder.add(Material.GRINDSTONE);
                builder.add(Material.FLETCHING_TABLE);
                builder.add(Material.STONECUTTER);
                builder.add(Material.CARTOGRAPHY_TABLE);
                builder.add(Material.BLAST_FURNACE);
                builder.add(Material.SMOKER);
                builder.add(Material.LOOM);
                builder.add(Material.BARREL);
                builder.add(Material.BELL);
            }
            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "_BUTTON")) {
                    builder.add(m);
                }
            }
        } else {
            builder.add(Material.getMaterial("WOOD_BUTTON"));
            builder.add(Material.STONE_BUTTON);
        }
        builder.add(Material.ITEM_FRAME);
        builder.add(Material.HOPPER);
        builder.add(Material.JUKEBOX);
        builder.add(Material.NOTE_BLOCK);
        builder.add(Material.DROPPER);
        builder.add(Material.BREWING_STAND);
        builder.add(Material.LEVER);
        builder.add(Material.ANVIL);
        builder.add(MaterialUtils.get("crafting_table"));
        builder.add(MaterialUtils.get("repeater_on"));
        builder.add(MaterialUtils.get("repeater_off"));
        builder.add(MaterialUtils.get("comparator_on"));
        builder.add(MaterialUtils.get("comparator_off"));
        builder.add(MaterialUtils.get("enchanting_table"));
        builder.add(MaterialUtils.get("end_portal_frame"));
        builder.add(MaterialUtils.get("furnace"));
        editable = new HashSet<>(builder);

        // Separator

        builder.clear();
        builder.add(Material.CHEST);
        builder.add(Material.TRAPPED_CHEST);
        builder.add(Material.ENDER_CHEST);
        chest = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            for (Material m : materials) {
                String s = m.toString();

                if (s.contains("_PLATE")) {
                    builder.add(m);
                }
            }
        } else {
            builder.add(Material.getMaterial("GOLD_PLATE"));
            builder.add(Material.getMaterial("IRON_PLATE"));
            builder.add(Material.getMaterial("STONE_PLATE"));
            builder.add(Material.getMaterial("WOOD_PLATE"));
        }
        plate = new HashSet<>(builder);

        // Separator

        builder.clear();
        for (Material m : materials) {
            String s = m.toString();

            if (contains(s, "WIRE")) {
                builder.add(m);
            }
        }
        wire = new HashSet<>(builder);

        // Separator

        builder.clear();
        for (Material m : materials) {
            String s = m.toString();

            if (endsWith(s, "_ORE")) {
                builder.add(m);
            }
        }
        ores = new HashSet<>(builder);

        // Separator

        builder.clear();
        builder.add(Material.ICE);
        builder.add(Material.PACKED_ICE);

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            builder.add(Material.FROSTED_ICE);

            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
                builder.add(Material.BLUE_ICE);
            }
        }
        ice = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            builder.add(Material.GLASS_PANE);

            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "_STAINED_GLASS_PANE")) {
                    builder.add(m);
                }
            }
        } else {
            builder.add(Material.getMaterial("THIN_GLASS"));
            builder.add(Material.getMaterial("STAINED_GLASS_PANE"));
        }
        glass_pane = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_20)) {
                builder.add(Material.BAMBOO_BLOCK);
                builder.add(Material.STRIPPED_BAMBOO_BLOCK);
                builder.add(Material.BAMBOO_MOSAIC);
                builder.add(Material.BAMBOO_PLANKS);
            }
            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "_LOG") || endsWith(s, "_WOOD")) {
                    builder.add(m);
                }
            }
        } else {
            builder.add(Material.getMaterial("LOG"));
            builder.add(Material.getMaterial("LOG_2"));
        }
        wood = new HashSet<>(builder);

        // Separator

        builder.clear();
        builder.add(Material.AIR);

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "_AIR")) {
                    builder.add(m);
                }
            }
        }
        air = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) { //
            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "_STAINED_GLASS")) {
                    builder.add(m);
                }
            }
        } else {
            builder.add(Material.getMaterial("STAINED_GLASS"));
        }
        builder.add(Material.GLASS);
        glass = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            builder.add(Material.getMaterial("STEP"));
            builder.add(Material.getMaterial("WOOD_STEP"));
        }
        for (Material m : materials) {
            String s = m.toString();

            if (contains(s, "_SLAB")) {
                builder.add(m);
            }
        }
        step = new HashSet<>(builder);

        // Separator

        builder.clear();
        builder.add(Material.LADDER);
        builder.add(Material.VINE);

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14)) {
            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15)) {
                if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16)) {
                    if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
                        builder.add(Material.CAVE_VINES);
                        builder.add(Material.CAVE_VINES_PLANT);
                    }
                    builder.add(Material.TWISTING_VINES);
                    builder.add(Material.TWISTING_VINES_PLANT);
                    builder.add(Material.WEEPING_VINES);
                    builder.add(Material.WEEPING_VINES_PLANT);
                }
                builder.add(Material.HONEY_BLOCK);
            }
            builder.add(Material.SCAFFOLDING);
        }
        climbable = new HashSet<>(builder);

        // Separator

        builder.clear();
        builder.add(Material.SAND);
        builder.add(Material.GRAVEL);

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_12)) {
            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
                for (Material m : materials) {
                    String s = m.toString();

                    if (endsWith(s, "_CONCRETE_POWDER")) {
                        builder.add(m);
                    }
                }
            } else {
                builder.add(Material.getMaterial("CONCRETE_POWDER"));
            }
        }
        entity_blocks = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            builder.add(Material.TERRACOTTA);

            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "_TERRACOTTA")) {
                    builder.add(m);
                }
            }
        } else {
            builder.add(Material.getMaterial("STAINED_CLAY"));
        }
        terracotta = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "_WOOL")) {
                    builder.add(m);
                }
            }
        } else {
            builder.add(Material.getMaterial("WOOL"));
        }
        wool = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_12)) {
            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "_CONCRETE")) {
                    builder.add(m);
                }
            }
        }
        concrete = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16)) {
                if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_19)) {
                    if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_20)) {
                        builder.add(Material.BAMBOO_DOOR);
                    }
                    builder.add(Material.MANGROVE_DOOR);
                }
                builder.add(Material.WARPED_DOOR);
                builder.add(Material.CRIMSON_DOOR);
            }
            builder.add(Material.IRON_DOOR);
            builder.add(Material.OAK_DOOR);
        } else {
            builder.add(Material.getMaterial("IRON_DOOR_BLOCK"));
            builder.add(Material.getMaterial("WOODEN_DOOR"));
        }
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)) {
            builder.add(Material.ACACIA_DOOR);
            builder.add(Material.BIRCH_DOOR);
            builder.add(Material.DARK_OAK_DOOR);
            builder.add(Material.JUNGLE_DOOR);
            builder.add(Material.SPRUCE_DOOR);
        }
        door = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "_TRAPDOOR")) {
                    builder.add(m);
                }
            }
        } else {
            builder.add(Material.getMaterial("TRAP_DOOR"));

            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)) {
                builder.add(Material.IRON_TRAPDOOR);
            }
        }
        trap_door = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "_CARPET")) {
                    builder.add(m);
                }
            }
        } else {
            builder.add(Material.getMaterial("CARPET"));
        }
        carpet = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "_BED")) {
                    builder.add(m);
                }
            }
        } else {
            builder.add(Material.getMaterial("BED_BLOCK"));
        }
        bed = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "_BANNER")) {
                    builder.add(m);
                }
            }
        } else {
            builder.add(Material.getMaterial("STANDING_BANNER"));
            builder.add(Material.getMaterial("WALL_BANNER"));
        }
        banner = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_11)) {
            for (Material m : materials) {
                String s = m.toString();

                if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
                    builder.add(Material.SHULKER_BOX);
                }
                if (endsWith(s, "_SHULKER_BOX")) {
                    builder.add(m);
                }
            }
        }
        shulker_box = new HashSet<>(builder);

        // Separator

        builder.clear();
        for (Material m : materials) {
            String s = m.toString();

            if (endsWith(s, "_STAIRS")) {
                builder.add(m);
            }
        }
        stairs = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            builder.add(Material.getMaterial("FENCE"));
        }
        for (Material m : materials) {
            String s = m.toString();

            if (endsWith(s, "_FENCE")) {
                builder.add(m);
            }
        }
        fence = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "CANDLE")) {
                    builder.add(m);
                }
            }
        }
        candle = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "CANDLE_CAKE")) {
                    builder.add(m);
                }
            }
        }
        candleCake = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
            for (Material m : materials) {
                String s = m.toString();

                if (contains(s, "DRIPLEAF")) {
                    builder.add(m);
                }
            }
        }
        dripleaf = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            builder.add(Material.getMaterial("FENCE_GATE"));
        }
        for (Material m : materials) {
            String s = m.toString();

            if (endsWith(s, "_FENCE_GATE")) {
                builder.add(m);
            }
        }
        fence_gate = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            builder.add(Material.SKELETON_SKULL);
            builder.add(Material.SKELETON_WALL_SKULL);
            builder.add(Material.WITHER_SKELETON_SKULL);
            builder.add(Material.WITHER_SKELETON_WALL_SKULL);
            builder.add(Material.CREEPER_HEAD);
            builder.add(Material.CREEPER_WALL_HEAD);
            builder.add(Material.DRAGON_HEAD);
            builder.add(Material.DRAGON_WALL_HEAD);
            builder.add(Material.PLAYER_HEAD);
            builder.add(Material.PLAYER_WALL_HEAD);
            builder.add(Material.ZOMBIE_HEAD);
            builder.add(Material.ZOMBIE_WALL_HEAD);
        }
        heads = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "_LEAVES")) {
                    builder.add(m);
                }
            }
        } else {
            builder.add(Material.getMaterial("LEAVES"));
            builder.add(Material.getMaterial("LEAVES_2"));
        }
        leaves = new HashSet<>(builder);

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "_SPAWN_EGG")) {
                    builder.add(m);
                }
            }
        }
        egg = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            builder.add(Material.BRAIN_CORAL_FAN);
            builder.add(Material.BRAIN_CORAL);
            builder.add(Material.DEAD_BRAIN_CORAL_FAN);
            builder.add(Material.DEAD_BRAIN_CORAL);
            builder.add(Material.DEAD_BRAIN_CORAL_WALL_FAN);

            builder.add(Material.BUBBLE_CORAL_FAN);
            builder.add(Material.BUBBLE_CORAL);
            builder.add(Material.DEAD_BUBBLE_CORAL_FAN);
            builder.add(Material.DEAD_BUBBLE_CORAL);
            builder.add(Material.DEAD_BUBBLE_CORAL_WALL_FAN);

            builder.add(Material.FIRE_CORAL_FAN);
            builder.add(Material.FIRE_CORAL);
            builder.add(Material.DEAD_FIRE_CORAL_FAN);
            builder.add(Material.DEAD_FIRE_CORAL);
            builder.add(Material.DEAD_FIRE_CORAL_WALL_FAN);

            builder.add(Material.HORN_CORAL_FAN);
            builder.add(Material.HORN_CORAL);
            builder.add(Material.DEAD_HORN_CORAL_FAN);
            builder.add(Material.DEAD_HORN_CORAL);
            builder.add(Material.DEAD_HORN_CORAL_WALL_FAN);

            builder.add(Material.TUBE_CORAL_FAN);
            builder.add(Material.TUBE_CORAL);
            builder.add(Material.DEAD_TUBE_CORAL_FAN);
            builder.add(Material.DEAD_TUBE_CORAL);
            builder.add(Material.DEAD_TUBE_CORAL_WALL_FAN);
        }
        coral_fan = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            builder.add(Material.FLOWER_POT);

            for (Material m : materials) {
                String s = m.toString();

                if (startsWith(s, "POTTED_")) {
                    builder.add(m);
                }
            }
        }
        pot = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            builder.add(Material.ANVIL);
            builder.add(Material.CHIPPED_ANVIL);
            builder.add(Material.DAMAGED_ANVIL);
        } else {
            builder.add(Material.ANVIL);
        }
        anvil = new HashSet<>(builder);

        // Separator

        builder.clear();
        for (Material m : materials) {
            String s = m.toString();

            if (endsWith(s, "_WALL")) {
                builder.add(m);
            }
        }
        cobble_walls = new HashSet<>(builder);

        // Separator

        builder.clear();
    }

    public static float getBreakSpeed(SpartanBlock block, SpartanPlayer player) {
        if (getBreakSpeed != null) {
            try {
                // block.getBreakSpeed(player)
                return (float) getBreakSpeed.invoke(block.getBlock(), player.getPlayer());
            } catch (Exception ignored) {
            }
        }
        return -1.0f;
    }

    public static int getMinHeight(World world) {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) ? world.getMinHeight() : 0;
    }

    public static boolean areCandles(Material m) {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && candle.contains(m);
    }

    public static boolean areCandleCakes(Material m) {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && candleCake.contains(m);
    }

    public static boolean isScaffoldingBlock(Material m) {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14) && m == Material.SCAFFOLDING;
    }

    public static boolean areWalls(Material m) {
        return areStainedGlasses(m)
                || areDoors(m)
                || areTrapdoors(m)
                || areChests(m)
                || areFences(m)
                || areFenceGates(m)
                || areCobbleWalls(m)
                || m == iron_bars
                || areAnvils(m)
                || m == Material.CACTUS
                || m == cake
                || m == Material.COCOA
                || m == head
                || areFlowerPots(m)
                || m == Material.DRAGON_EGG
                || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15) && m == Material.HONEY_BLOCK
                || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && (m == Material.CHORUS_PLANT || m == Material.END_ROD)
                || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16) && m == Material.CHAIN
                || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && m == Material.LIGHTNING_ROD;
    }

    public static boolean areHeads(Material m) {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? heads.contains(m) : m == Material.getMaterial("SKULL");
    }

    public static boolean areFlowerPots(Material m) {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? pot.contains(m) : m == Material.FLOWER_POT;
    }

    // Local due to Waterlogged block incompatibilities
    public static boolean isLiquid(Block block) {
        return liquid.contains(block.getType()) || block.isLiquid();
    }

    public static boolean isLiquid(Material m) {
        return liquid.contains(m);
    }

    public static boolean isBaseLiquid(Material m) {
        return m == water || m == lava;
    }

    public static boolean isInteractiveAndPassable(Material m) {
        return areDripLeafs(m) || areWebs(m) || isPowderSnow(m) || isScaffoldingBlock(m);
    }

    public static boolean areStairs(Material m) {
        return stairs.contains(m);
    }

    public static boolean areCobbleWalls(Material m) {
        return cobble_walls.contains(m);
    }

    public static boolean areSlabs(Material m) {
        return step.contains(m);
    }

    public static boolean areLeaves(Material m) {
        return leaves.contains(m);
    }

    public static boolean areCarpets(Material m) {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? carpet.contains(m) : m == Material.getMaterial("CARPET");
    }

    public static boolean areBeds(Material m) {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? bed.contains(m) : m == Material.getMaterial("BED_BLOCK");
    }

    public static boolean areEggs(Material m) {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? egg.contains(m) : m == Material.getMaterial("MONSTER_EGG");
    }

    public static boolean areTrapdoors(Material m) {
        return trap_door.contains(m);
    }

    public static boolean areFences(Material m) {
        return fence.contains(m);
    }

    public static boolean areFenceGates(Material m) {
        return fence_gate.contains(m);
    }

    public static boolean isPowderSnow(Material m) {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && m == Material.POWDER_SNOW;
    }

    public static boolean areDripLeafs(Material m) {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && dripleaf.contains(m);
    }

    public static boolean areAnvils(Material m) {
        return anvil.contains(m);
    }

    public static boolean areWires(Material m) {
        return wire.contains(m);
    }

    public static boolean areChests(Material m) {
        return chest.contains(m);
    }

    public static boolean areDoors(Material m) {
        return door.contains(m);
    }

    public static boolean areGlasses(Material m) {
        return glass.contains(m);
    }

    public static boolean areStainedGlasses(Material m) {
        return glass_pane.contains(m);
    }

    public static boolean arePlates(Material m) {
        return plate.contains(m);
    }

    public static boolean areIceBlocks(Material m) {
        return ice.contains(m);
    }

    public static boolean areWools(Material m) {
        return wool.contains(m);
    }

    public static boolean areEntityBlocks(Material m) {
        return entity_blocks.contains(m) || areAnvils(m);
    }

    public static boolean areWebs(Material m) {
        return m == web;
    }

    public static boolean areShulkerBoxes(Material m) {
        return shulker_box.contains(m);
    }

    public static boolean areCoralFans(Material m) {
        return coral_fan.contains(m);
    }

    public static boolean areTerracotta(Material m) {
        return terracotta.contains(m);
    }

    public static boolean areOres(Material m) {
        return ores.contains(m);
    }

    public static boolean areAir(Material m) {
        return air.contains(m);
    }

    public static boolean areWoods(Material m) {
        return wood.contains(m);
    }

    public static boolean areConcrete(Material m) {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_12) && concrete.contains(m);
    }

    public static boolean areInteractiveBushes(Material m) {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14) && m == Material.SWEET_BERRY_BUSH;
    }

    public static boolean canClimb(Material m) {
        return climbable.contains(m);
    }

    public static boolean isSensitive(Material m, long time) {
        if (time >= 0L && time <= 250L) {
            return true;
        }
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)) {
            if (m == Material.SLIME_BLOCK || m == Material.RED_SANDSTONE || m == Material.RED_SANDSTONE_STAIRS) {
                return true;
            }
            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && (m == Material.END_ROD || m == beetroot_block || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_10) && m == magma)) {
                return true;
            }
        }
        return !isSolid(m)
                || areGlasses(m)
                || areStainedGlasses(m)
                || sensitive.contains(m)
                || areIceBlocks(m)
                || areCoralFans(m)
                || areLeaves(m)
                || areHeads(m)
                || areBeds(m)
                || areCarpets(m)
                || areFlowerPots(m)
                || isLiquid(m)
                || isScaffoldingBlock(m)
                || isPowderSnow(m)
                || areDripLeafs(m)
                || areCandles(m)
                || areCandleCakes(m);
    }

    public static boolean isSensitive(SpartanPlayer p, Material m) {
        return isSensitive(m, MaterialUtils.getBlockBreakTime(p, p.getItemInHand(), m));
    }

    public static boolean isSensitive(Material m) {
        return isSensitive(m, -1L);
    }

    public static boolean isSolid(Material m) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)) {
            if (m == Material.ARMOR_STAND || banner.contains(m)
                    || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && (m == Material.BEETROOT_SEEDS || m == Material.END_GATEWAY
                    || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_10) && m == Material.STRUCTURE_VOID)) {
                return false;
            }
        }
        return !(areFenceGates(m) || areTrapdoors(m) || arePlates(m) || isLiquid(m) || areDripLeafs(m) || solid.contains(m));
    }

    // Separator

    public static String blockToString(SpartanBlock b) {
        return materialToString(b.getBlock().getType());
    }

    public static String getIllegalLocationInfo(SpartanPlayer p, SpartanLocation entityLocation, double minDistance) {
        SpartanLocation playerLocation = p.getLocation();
        double realDistance = playerLocation.distance(entityLocation);

        if (realDistance <= 6.0) {
            SpartanBlock realBlock = entityLocation.getBlock();
            double eyeHeight = p.getEyeHeight();

            for (double i = 0.0; i <= realDistance; i++) {
                SpartanLocation targetLocation = playerLocation.clone().add(0, eyeHeight, 0).add(playerLocation.getDirection().multiply(i));
                double targetDistance = playerLocation.distance(targetLocation);

                if (targetDistance >= minDistance) {
                    SpartanBlock targetBlock = targetLocation.getBlock();
                    double distanceDifference = playerLocation.distance(realBlock.getLocation()) - playerLocation.distance(targetBlock.getLocation());

                    if (targetDistance < realDistance && Math.abs(targetDistance - eyeHeight) >= 0.01
                            && distanceDifference >= 0.4
                            && targetLocation.getBlockY() >= playerLocation.getY()
                            && isFullSolid(targetLocation, false)) {
                        return "real-dis: " + realDistance + ", tar-dis: " + targetDistance + ", dis-diff: " + distanceDifference + ", tar-b: " + blockToString(targetBlock);
                    }
                }
            }
        }
        return null;
    }

    public static String materialToString(Material m) {
        return m.toString().toLowerCase().replace("_", "-");
    }

    // Separator

    public static boolean areCandles(SpartanLocation loc) {
        return areCandles(loc.getBlock().getType());
    }

    public static boolean areCandleCakes(SpartanLocation loc) {
        return areCandleCakes(loc.getBlock().getType());
    }

    public static boolean isScaffoldingBlock(SpartanLocation loc) {
        return isScaffoldingBlock(loc.getBlock().getType());
    }

    public static boolean areIceBlocks(SpartanLocation loc) {
        return areIceBlocks(loc.getBlock().getType());
    }

    public static boolean areWools(SpartanLocation loc) {
        return areWools(loc.getBlock().getType());
    }

    public static boolean areChests(SpartanLocation loc) {
        return areChests(loc.getBlock().getType());
    }

    public static boolean arePlates(SpartanLocation loc) {
        return arePlates(loc.getBlock().getType());
    }

    public static boolean areGlasses(SpartanLocation loc) {
        return areGlasses(loc.getBlock().getType());
    }

    public static boolean areStainedGlasses(SpartanLocation loc) {
        return areStainedGlasses(loc.getBlock().getType());
    }

    public static boolean areSlabs(SpartanLocation loc) {
        return areSlabs(loc.getBlock().getType());
    }

    public static boolean areTrapdoors(SpartanLocation loc) {
        return areTrapdoors(loc.getBlock().getType());
    }

    public static boolean isPowderSnow(SpartanLocation loc) {
        return isPowderSnow(loc.getBlock().getType());
    }

    public static boolean areDripLeafs(SpartanLocation loc) {
        return areDripLeafs(loc.getBlock().getType());
    }

    public static boolean areHeads(SpartanLocation loc) {
        return areHeads(loc.getBlock().getType());
    }

    public static boolean areCoralFans(SpartanLocation loc) {
        return areCoralFans(loc.getBlock().getType());
    }

    public static boolean areInteractiveBushes(SpartanLocation loc) {
        return areInteractiveBushes(loc.getBlock().getType());
    }

    public static boolean areWebs(SpartanLocation loc) {
        return areWebs(loc.getBlock().getType());
    }

    public static boolean areStairs(SpartanLocation loc) {
        return areStairs(loc.getBlock().getType());
    }

    public static boolean areFences(SpartanLocation loc) {
        return areFences(loc.getBlock().getType());
    }

    public static boolean areFenceGates(SpartanLocation loc) {
        return areFenceGates(loc.getBlock().getType());
    }

    public static boolean areDoors(SpartanLocation loc) {
        return areDoors(loc.getBlock().getType());
    }

    public static boolean areEntityBlocks(SpartanLocation loc) {
        return areEntityBlocks(loc.getBlock().getType());
    }

    public static boolean areCobbleWalls(SpartanLocation loc) {
        return areWalls(loc.getBlock().getType());
    }

    public static boolean areShulkerBoxes(SpartanLocation loc) {
        return areShulkerBoxes(loc.getBlock().getType());
    }

    public static boolean areBeds(SpartanLocation loc) {
        return areBeds(loc.getBlock().getType());
    }

    public static boolean areCarpets(SpartanLocation loc) {
        return areCarpets(loc.getBlock().getType());
    }

    public static boolean areAnvils(SpartanLocation loc) {
        return areAnvils(loc.getBlock().getType());
    }

    public static boolean areWires(SpartanLocation loc) {
        return areWires(loc.getBlock().getType());
    }

    public static boolean areLeaves(SpartanLocation loc) {
        return areLeaves(loc.getBlock().getType());
    }

    public static boolean areOres(SpartanLocation loc) {
        return areOres(loc.getBlock().getType());
    }

    public static boolean areAir(SpartanLocation loc) {
        return areAir(loc.getBlock().getType());
    }

    public static boolean areWoods(SpartanLocation loc) {
        return areWoods(loc.getBlock().getType());
    }

    public static boolean isInteractiveAndPassable(SpartanPlayer p, SpartanLocation loc) {
        return p.isFrozen() || isInteractiveAndPassable(loc.getBlock().getType());
    }

    public static boolean areTerracotta(SpartanLocation loc) {
        return areTerracotta(loc.getBlock().getType());
    }

    public static boolean areConcrete(SpartanLocation loc) {
        return areConcrete(loc.getBlock().getType());
    }

    public static boolean isSensitive(SpartanLocation loc, long time) {
        return isSensitive(loc.getBlock().getType(), time);
    }

    public static boolean isSensitive(SpartanPlayer p, SpartanLocation loc) {
        return isSensitive(p, loc.getBlock().getType());
    }

    public static boolean isSensitive(SpartanLocation loc) {
        SpartanPlayer p = loc.getPlayer();
        return p != null ? isSensitive(p, loc.getBlock().getType()) : isSensitive(loc.getBlock().getType());
    }

    public static boolean isFullSolid(Material m, boolean util) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8) && m == Material.SLIME_BLOCK
                || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && (m == Material.END_ROD || m == Material.getMaterial("GRASS_PATH") || m == beetroot_block)
                || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15) && m == Material.HONEY_BLOCK
                || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && m == Material.DIRT_PATH) {
            return false;
        }
        return (!util || isSolid(m)) && !isSemiSolid(m);
    }

    public static boolean isSemiSolid(Material m) {
        return areFenceGates(m)
                || areFences(m)
                || areStairs(m)
                || areSlabs(m)
                || areTrapdoors(m)
                || areDoors(m)
                || areStainedGlasses(m)
                || areChests(m)
                || arePlates(m)
                || areShulkerBoxes(m)
                || areLeaves(m)
                || areHeads(m)
                || areCoralFans(m)
                || areAnvils(m)
                || areCarpets(m)
                || areBeds(m)
                || areFlowerPots(m)
                || areCobbleWalls(m)
                || areCandles(m)
                || areCandleCakes(m)
                || isInteractiveAndPassable(m)
                || full_solid.contains(m);
    }

    public static boolean isFullSolid(SpartanLocation loc, boolean util) {
        return isFullSolid(loc.getBlock().getType(), util);
    }

    public static boolean isChangeable(SpartanLocation loc) {
        Material m = loc.getBlock().getType();
        return editable.contains(m)
                || areFenceGates(m)
                || areTrapdoors(m)
                || areChests(m)
                || areDoors(m)
                || areShulkerBoxes(m)
                || areBeds(m)
                || areAnvils(m);
    }

    public static boolean isAnvilBugged(SpartanLocation loc) {
        for (Material m : anvil) {
            if (loc.isBlock(m, hitbox)) {
                return true;
            }
        }
        return false;
    }

    public static boolean canClimb(SpartanLocation loc) {
        return canClimb(loc.getBlock().getType());
    }

    public static boolean isLiquid(SpartanLocation loc) {
        return loc.getBlock().isLiquid();
    }

    public static boolean isLiquidFree(SpartanLocation loc, double x, double y, double z) {
        for (SpartanLocation locs : loc.getSurroundingLocations(x, y, z)) {
            if (isLiquid(locs)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isLiquidSurrounded(SpartanLocation loc, double x, double y, double z) {
        for (SpartanLocation locs : loc.getSurroundingLocations(x, y, z, true)) {
            if (!isLiquid(locs)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isGroundFree(SpartanPlayer p, SpartanLocation loc, double x, double y, double z) {
        if (p.isOnGroundCustom()) {
            return false;
        }
        for (SpartanLocation locs : loc.getSurroundingLocations(x, y, z)) {
            if (PlayerData.isOnGround(p, locs, 0.0)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isSolidFree(SpartanLocation loc, double x, double y, double z) {
        for (SpartanLocation locs : loc.getSurroundingLocations(x, y, z)) {
            if (isSolid(locs)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isWaterLogFree(SpartanLocation loc, double x, double y, double z) {
        for (SpartanLocation locs : loc.getSurroundingLocations(x, y, z)) {
            if (locs.getBlock().isWaterLogged()) {
                return false;
            }
        }
        return true;
    }

    public static boolean isCoralFanFree(SpartanLocation loc, double x, double y, double z) {
        for (SpartanLocation locs : loc.getSurroundingLocations(x, y, z)) {
            if (areCoralFans(locs)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isHoneyBlockFree(SpartanLocation loc, double x, double y, double z) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15)) {
            for (SpartanLocation locs : loc.getSurroundingLocations(x, y, z, true)) {
                if (locs.getBlock().getType() == Material.HONEY_BLOCK) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isInteractiveAndPassThruFree(SpartanLocation loc, double x, double y, double z) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15)) {
            for (SpartanLocation locs : loc.getSurroundingLocations(x, y, z, true)) {
                if (isInteractiveAndPassable(locs.getBlock().getType())) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isInteractiveBushFree(SpartanLocation loc, double x, double y, double z) {
        for (SpartanLocation locs : loc.getSurroundingLocations(x, y, z)) {
            if (areInteractiveBushes(locs)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isWebFree(SpartanLocation loc, double x, double y, double z) {
        for (SpartanLocation locs : loc.getSurroundingLocations(x, y, z, true)) {
            if (areWebs(locs)) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasNonSolidSide(SpartanLocation loc, double x, double y, double z, boolean center) {
        if (center && !isSolid(loc.clone().add(0, y, 0))) {
            return true;
        }
        List<SpartanLocation> list = loc.getSurroundingLocations(x, y, z, true);
        list.remove(0);

        for (SpartanLocation locs : list) {
            if (!isSolid(locs)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNonSolidFree(SpartanLocation loc, double x, double y, double z) {
        for (SpartanLocation locs : loc.getSurroundingLocations(x, y, z)) {
            if (!isFullSolid(locs, false)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNonSolidAndAirFree(SpartanLocation loc, double x, double y, double z) {
        for (SpartanLocation locs : loc.getSurroundingLocations(x, y, z)) {
            if (!isFullSolid(locs, true)) {
                return false;
            }
        }
        return true;
    }

    public static boolean canClimbAround(SpartanLocation loc, double x, double y, double z) {
        for (SpartanLocation locs : loc.getSurroundingLocations(x, y, z, true)) {
            if (canClimb(locs)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isIceBlockFree(SpartanLocation loc, double x, double y, double z) {
        for (SpartanLocation locs : loc.getSurroundingLocations(x, y, z)) {
            if (areIceBlocks(locs)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isBedFree(SpartanLocation loc, double x, double y, double z) {
        for (SpartanLocation locs : loc.getSurroundingLocations(x, y, z)) {
            if (areBeds(locs)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isCarpetFree(SpartanLocation loc, double x, double y, double z) {
        for (SpartanLocation locs : loc.getSurroundingLocations(x, y, z)) {
            if (areCarpets(locs)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isHeadFree(SpartanPlayer p, SpartanLocation loc, double x, double y, double z) {
        for (SpartanLocation locs : loc.getSurroundingLocations(x, y, z)) {
            if (areHeads(locs)) {
                return false;
            }
        }
        return true;
    }

    public static boolean areWallsAround(SpartanLocation loc, double x, double y, double z) {
        for (SpartanLocation locs : loc.getSurroundingLocations(x, y, z)) {
            if (areWalls(locs)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAnvilFree(SpartanLocation loc, double x, double y, double z) {
        for (SpartanLocation locs : loc.getSurroundingLocations(x, y, z)) {
            if (areAnvils(locs)) {
                return false;
            }
        }
        return true;
    }

    public static boolean areWalls(SpartanLocation loc) {
        return areWalls(loc.getBlock().getType());
    }

    public static boolean isSolid(SpartanLocation loc) {
        Material m = loc.getBlock().getType();

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)) {
            if (m == Material.ARMOR_STAND || banner.contains(m)
                    || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && (m == Material.BEETROOT_SEEDS || m == Material.END_GATEWAY
                    || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_10) && m == Material.STRUCTURE_VOID)) {
                return false;
            }
        }
        return !(areFenceGates(loc) || isLiquid(loc) || solid.contains(m));
    }

    public static boolean isSemiSolid(SpartanLocation loc) {
        return isSemiSolid(loc.getBlock().getType());
    }

    // Separator

    public static boolean isSlime(SpartanPlayer p, SpartanLocation loc, int blocks) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)) {
            int y = loc.getBlockY();
            SpartanLocation loopLocation = loc.clone();

            for (int i = y; i > Math.max(getMinHeight(p.getWorld()), y - blocks); i--) {
                loopLocation.setY(i);
                Material m = loopLocation.getBlock().getType();

                if (m == Material.SLIME_BLOCK) {
                    return true;
                } else if (isSolid(m)
                        && m != Material.SNOW
                        && !areCarpets(m)
                        && !areSlabs(m)
                        && !areStairs(m)
                        && !areWalls(m)
                        && !canClimb(m)) {
                    break;
                }
            }
        }
        return false;
    }

    public static boolean isBed(SpartanPlayer p, SpartanLocation loc, int blocks) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_12)) {
            int y = loc.getBlockY();
            SpartanLocation loopLocation = loc.clone();

            for (int i = y; i > Math.max(getMinHeight(p.getWorld()), y - blocks); i--) {
                loopLocation.setY(i);
                Material m = loopLocation.getBlock().getType();

                if (areBeds(m)) {
                    return true;
                } else if (isSolid(m)) {
                    break;
                }
            }
        }
        return false;
    }

    public static boolean isBouncingBlock(SpartanPlayer p, SpartanLocation loc, int blocks) {
        return isSlime(p, loc, blocks) || isBed(p, loc, blocks);
    }

    public static boolean isClearLiquid(SpartanPlayer p, SpartanLocation loc) {
        SpartanBlock b = loc.getBlock();

        if (!b.isLiquid()) {
            return true;
        }
        int data = b.getData();
        return data == 0 || data == 8 || p != null && p.getLocation().getBlock() != b;
    }
}
