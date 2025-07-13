package dev.thomasqtruong.veryscuffedcobblemonbreeding.config;

import dev.thomasqtruong.veryscuffedcobblemonbreeding.VeryScuffedCobblemonBreeding;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;

public class VeryScuffedCobblemonBreedingConfig {
    private static final Logger LOGGER = VeryScuffedCobblemonBreeding.INSTANCE.getLogger();

    Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    // Track if legacy integer booleans were found during load
    private boolean foundIntegerBooleans = false;

    // Default permission level for the /pokebreed command (default: 2).
    public static int COMMAND_POKEBREED_PERMISSION_LEVEL = 2;
    // Default permission level for the /pokebreed command for VIPs (default: 3).
    public static int VIP_COMMAND_POKEBREED_PERMISSION_LEVEL = 3;
    // Default cooldown in minutes for the /pokebreed command (default: 5).
    public static int COOLDOWN_IN_MINUTES = 5;
    // Default cooldown in minutes for the /pokebreed command for VIPs (default: 3).
    public static int VIP_COOLDOWN_IN_MINUTES = 3;
    // Whether breeding with a ditto is enabled (default: true).
    public static boolean DITTO_BREEDING = true;
    // Whether passing down hidden abilities is enabled (default: true).
    public static boolean HIDDEN_ABILITY = true;

    // Whether to use the custom shiny rate (default: false).
    public static boolean USE_CUSTOM_SHINY_RATE = false;
    // Custom shiny rate for breeding (default: 4096 [1 in 4096]).
    // CUSTOM_SHINY_RATE is only used if USE_CUSTOM_SHINY_RATE is set to true
    public static int CUSTOM_SHINY_RATE = 4096;

    // Whether breeding using the "Masuda" method is enabled (default: true).
    // The Masuda method is a breeding technique that increases the chance of getting a shiny Pokémon when one or both of the parent Pokémon are from foreign OTs (traded from other players).
    public static boolean MASUDA_BREEDING = true;
    // Default shiny rate modifier for the Masuda method (default: 6 [8192 / 6 = 1 in 1365]).
    public static int MASUDA_BREEDING_MODIFIER = 6;

    // Whether breeding using the "Crystal" method is enabled (default: true).
    // The crystal method is a breeding technique that increases the chance of getting a shiny Pokémon when one or both of the parent Pokémon are shiny.
    public static boolean CRYSTAL_BREEDING = true;
    // Default shiny rate modifier for the Crystal method (default: 2 [8192 / 2 = 1 in 4096]).
    public static int CRYSTAL_BREEDING_MODIFIER = 2;
    // Whether breeding two crystal (shiny) Pokémon increases the rate further (default: true).
    // When enabled, if both parents are shiny, the shiny rate is effectively doubled
    public static boolean CRYSTAL_BREEDING_DOUBLE_PARENT = true;

    public VeryScuffedCobblemonBreedingConfig() {
        init();
    }

    // Extracts data from the config file.
    public void init() {
        File configFolder = new File(System.getProperty("user.dir") + "/config/veryscuffedcobblemonbreeding");
        File configFile = new File(configFolder, "config.json");
        LOGGER.info("Config -> {}", configFolder.getAbsolutePath());

        if (!configFolder.exists()) {
            configFolder.mkdirs();
            createConfig(configFolder);
        } else if (!configFile.exists()) {
            createConfig(configFolder);
        }

        try {
            Type type = new TypeToken<HashMap<String, Object>>(){}.getType();
            JsonObject obj = GSON.fromJson(new FileReader(configFile), JsonObject.class);

            // Define JSON key structure
            HashMap<String, Object> permissionMap = new HashMap<>();
            HashMap<String, Object> cooldownsMap = new HashMap<>();
            HashMap<String, Object> shinyModifiersMap = new HashMap<>();
            HashMap<String, Object> otherFeaturesMap = new HashMap<>();

            // Load JSON objects with null checks in case of outdated format
            if (obj.get("permissionlevels") != null) {
                JsonObject permLevels = obj.get("permissionlevels").getAsJsonObject();
                permissionMap = GSON.fromJson(permLevels, type);
            }
            if (obj.get("cooldowns") != null) {
                JsonObject cooldowns = obj.get("cooldowns").getAsJsonObject();
                cooldownsMap = GSON.fromJson(cooldowns, type);
            }
            if (obj.get("shinyModifiers") != null) {
                JsonObject shinyModifiers = obj.get("shinyModifiers").getAsJsonObject();
                shinyModifiersMap = GSON.fromJson(shinyModifiers, type);
            }
            if (obj.get("otherFeatures") != null) {
                JsonObject otherFeatures = obj.get("otherFeatures").getAsJsonObject();
                otherFeaturesMap = GSON.fromJson(otherFeatures, type);
            }

            // Permissions map
            COMMAND_POKEBREED_PERMISSION_LEVEL = getInt(permissionMap, "command.pokebreed", 2);
            VIP_COMMAND_POKEBREED_PERMISSION_LEVEL = getInt(permissionMap, "command.vippokebreed", 3);

            // Cooldowns
            COOLDOWN_IN_MINUTES = getInt(cooldownsMap, "command.pokebreed.cooldown", 5);
            VIP_COOLDOWN_IN_MINUTES = getInt(cooldownsMap, "command.pokebreed.vipcooldown", 3);

            // Shiny modifiers
            USE_CUSTOM_SHINY_RATE = getBool(shinyModifiersMap, "shiny.rates.useCustomValue", false);
            CUSTOM_SHINY_RATE = getInt(shinyModifiersMap, "shiny.rates.customValue", 4096);
            MASUDA_BREEDING = getBool(shinyModifiersMap, "shiny.methods.masuda.enabled", true);
            MASUDA_BREEDING_MODIFIER = getInt(shinyModifiersMap, "shiny.methods.masuda.modifier", 6);
            CRYSTAL_BREEDING = getBool(shinyModifiersMap, "shiny.methods.crystal.enabled", true);
            CRYSTAL_BREEDING_MODIFIER = getInt(shinyModifiersMap, "shiny.methods.crystal.modifier", 2);
            CRYSTAL_BREEDING_DOUBLE_PARENT = getBool(shinyModifiersMap, "shiny.methods.crystal.doubleParent", true);

            // Other features
            DITTO_BREEDING = getBool(otherFeaturesMap, "ditto.breeding", true);
            HIDDEN_ABILITY = getBool(otherFeaturesMap, "hidden.ability", true);

            // Check if any new fields are missing, and update the config file if needed
            boolean configNeedsUpdate = false;

            // Check for any missing sections
            if (obj.get("shinyModifiers") == null || obj.get("permissionlevels") == null ||
                obj.get("cooldowns") == null || obj.get("otherFeatures") == null) {
                configNeedsUpdate = true;
            }

            // Check for specific missing fields in existing sections
            if (obj.get("shinyModifiers") != null) {
                JsonObject shinyMods = obj.get("shinyModifiers").getAsJsonObject();
                if (shinyMods.get("shiny.methods.masuda.modifier") == null) {
                    configNeedsUpdate = true;
                }
            }

            // Tracks if any integer booleans were found so they can be updated/converted
            if (foundIntegerBooleans) {
                configNeedsUpdate = true;
                LOGGER.info("Integer boolean values found, updating to booleans");
            }

            if (configNeedsUpdate) {
                LOGGER.info("Updating config file...");
                updateConfigFile(configFile);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void createConfig(File configFolder) {
        File file = new File(configFolder, "config.json");
        try {
            file.createNewFile();
            writeConfigJson(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateConfigFile(File configFile) {
        try {
            writeConfigJson(configFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeConfigJson(File file) throws IOException {
        JsonWriter writer = GSON.newJsonWriter(new FileWriter(file));
        writer.beginObject()
            .name("permissionlevels")
                .beginObject()
                    .name("command.pokebreed")
                    .value(COMMAND_POKEBREED_PERMISSION_LEVEL)
                    .name("command.vippokebreed")
                    .value(VIP_COMMAND_POKEBREED_PERMISSION_LEVEL)
                .endObject()
            .name("cooldowns")
                .beginObject()
                    .name("command.pokebreed.cooldown")
                    .value(COOLDOWN_IN_MINUTES)
                    .name("command.pokebreed.vipcooldown")
                    .value(VIP_COOLDOWN_IN_MINUTES)
                .endObject()
            .name("otherFeatures")
                .beginObject()
                    .name("ditto.breeding")
                    .value(DITTO_BREEDING)
                    .name("hidden.ability")
                    .value(HIDDEN_ABILITY)
                .endObject()
            .name("shinyModifiers")
                .beginObject()
                    .name("shiny.rates.useCustomValue")
                    .value(USE_CUSTOM_SHINY_RATE)
                    .name("shiny.rates.customValue")
                    .value(CUSTOM_SHINY_RATE)
                    .name("shiny.methods.masuda.enabled")
                    .value(MASUDA_BREEDING)
                    .name("shiny.methods.masuda.modifier")
                    .value(MASUDA_BREEDING_MODIFIER)
                    .name("shiny.methods.crystal.enabled")
                    .value(CRYSTAL_BREEDING)
                    .name("shiny.methods.crystal.modifier")
                    .value(CRYSTAL_BREEDING_MODIFIER)
                    .name("shiny.methods.crystal.doubleParent")
                    .value(CRYSTAL_BREEDING_DOUBLE_PARENT)
                .endObject()
            .endObject()
            .flush();
    }

    private boolean getBool(HashMap<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);

        if (value == null) return defaultValue;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Integer) {
            foundIntegerBooleans = true;
            return ((Integer) value) == 1;
        }
        if (value instanceof Double) {
            foundIntegerBooleans = true;
            return ((Double) value).intValue() == 1;
        }

        LOGGER.warn("Couldn't read boolean value for key: {}, using default value: {}", key, defaultValue);
        return defaultValue;
    }

    private int getInt(HashMap<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Double) return ((Double) value).intValue();

        LOGGER.warn("Couldn't read integer value for key: {}, using default value: {}", key, defaultValue);
        return defaultValue;
    }
}
