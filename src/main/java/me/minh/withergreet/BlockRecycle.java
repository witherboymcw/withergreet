package me.minh.withergreet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

public class BlockRecycle implements Listener {

    private final WitherGreet plugin;
    private final Map<Material, RecipeValue> recipeValues = new HashMap<>();

    public BlockRecycle(WitherGreet plugin) {
        this.plugin = plugin;
        loadTags();
    }

    private void loadTags() {
        recipeValues.clear();

        for (String resourcePath : getCraftTagResources()) {
            String fileName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
            String materialKey = fileName.substring(0, fileName.length() - 4);

            try (InputStream inputStream = plugin.getResource(resourcePath)) {
                if (inputStream == null) {
                    continue;
                }

                YamlConfiguration config = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8)
                );
                loadMaterialConfig(materialKey, config);
            } catch (IOException exception) {
                plugin.getLogger().log(
                    Level.WARNING,
                    "Failed to read craft tag file {0}: {1}",
                    new Object[] { resourcePath, exception.getMessage() }
                );
            }
        }
    }

    private Set<String> getCraftTagResources() {
        Set<String> resources = new HashSet<>();

        File craftagsFolder = new File(plugin.getDataFolder(), "craftags");
        File[] dataFiles = craftagsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (dataFiles != null) {
            for (File file : dataFiles) {
                resources.add("craftags/" + file.getName());
            }
        }

        try (JarFile jarFile = new JarFile(plugin.getPluginJar())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!entry.isDirectory() && name.startsWith("craftags/") && name.endsWith(".yml")) {
                    resources.add(name);
                }
            }
        } catch (IOException exception) {
            plugin.getLogger().log(
                Level.WARNING,
                "Failed to scan bundled craft tags: {0}",
                exception.getMessage()
            );
        }

        return resources;
    }

    private void loadMaterialConfig(String materialKey, YamlConfiguration config) {
        for (String sectionName : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(sectionName);
            if (section == null) {
                continue;
            }

            Material resultMaterial = resolveResultMaterial(materialKey, sectionName);
            if (resultMaterial == null) {
                continue;
            }

            for (String listName : section.getKeys(false)) {
                int amount = parseAmount(listName);
                if (amount <= 0) {
                    continue;
                }

                List<String> items = section.getStringList(listName);
                for (String itemId : items) {
                    Material ingredient = parseMaterial(itemId);
                    if (ingredient == null || ingredient == Material.AIR) {
                        continue;
                    }

                    recipeValues.put(ingredient, new RecipeValue(materialKey, sectionName, amount, resultMaterial));
                }
            }
        }
    }

    private Material resolveResultMaterial(String materialKey, String sectionName) {
        String normalizedSection = sectionName.toLowerCase();
        String normalizedMaterial = materialKey.toLowerCase();

        if ("blocks".equals(normalizedSection)) {
            return switch (normalizedMaterial) {
                case "iron" -> Material.IRON_BLOCK;
                case "copper" -> Material.COPPER_BLOCK;
                case "diamond" -> Material.DIAMOND_BLOCK;
                case "gold" -> Material.GOLD_BLOCK;
                case "lapis" -> Material.LAPIS_BLOCK;
                case "redstone" -> Material.REDSTONE_BLOCK;
                default -> null;
            };
        }

        if ("ingots".equals(normalizedSection)) {
            return switch (normalizedMaterial) {
                case "iron" -> Material.IRON_INGOT;
                case "copper" -> Material.COPPER_INGOT;
                case "gold" -> Material.GOLD_INGOT;
                case "diamond" -> Material.DIAMOND;
                case "lapis" -> Material.LAPIS_LAZULI;
                case "redstone" -> Material.REDSTONE;
                case "leather" -> Material.LEATHER;
                default -> null;
            };
        }

        return null;
    }

    private Material parseMaterial(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }

        return Material.matchMaterial(itemId, true);
    }

    private int parseAmount(String key) {
        if (key == null || key.isBlank()) {
            return 0;
        }

        try {
            int numericAmount = Integer.parseInt(key);
            return numericAmount >= 1 && numericAmount <= 64 ? numericAmount : 0;
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inventory = event.getInventory();
        ItemStack[] matrix = inventory.getMatrix();

        if (matrix.length != 9) {
            return;
        }

        RecipeValue expectedValue = null;
        boolean hasRecognizedIngredient = false;

        for (ItemStack item : matrix) {
            if (item == null || item.getType() == Material.AIR) {
                if (hasRecognizedIngredient) {
                    inventory.setResult(null);
                }
                return;
            }

            RecipeValue currentValue = recipeValues.get(item.getType());
            if (currentValue == null) {
                if (hasRecognizedIngredient) {
                    inventory.setResult(null);
                }
                return;
            }

            hasRecognizedIngredient = true;

            if (expectedValue == null) {
                expectedValue = currentValue;
                continue;
            }

            if (!expectedValue.matchesGroup(currentValue)) {
                inventory.setResult(null);
                return;
            }
        }

        if (expectedValue == null) {
            return;
        }

        RecipeValue resolvedValue = Objects.requireNonNull(expectedValue);
        inventory.setResult(new ItemStack(resolvedValue.resultMaterial(), resolvedValue.amount()));
    }

    private record RecipeValue(String materialKey, String resultGroup, int amount, Material resultMaterial) {
        private boolean matchesGroup(RecipeValue other) {
            if (other == null) {
                return false;
            }

            return materialKey.equals(other.materialKey)
                && resultGroup.equals(other.resultGroup)
                && amount == other.amount
                && resultMaterial == other.resultMaterial;
        }
    }
}
