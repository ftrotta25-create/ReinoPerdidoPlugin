package com.franchee.reino.loot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Todo el loot del Reino Perdido.
 *  - Cofres: tier 1 (mini estructuras), 2 (ciudadela) y 3 (tesoro del trono y camara del tesoro).
 *  - Drops raros de los mobs modificados (por tier de mob, 1 a 5).
 *  - La Corona de Florencia (recompensa especial del jefe).
 */
public class LootService {

    private record Entrada(int peso, Supplier<ItemStack> gen) {
    }

    private record Drop(int tierMin, int peso, Supplier<ItemStack> gen) {
    }

    private static final UUID UUID_CORONA_ARMADURA = UUID.fromString("f10c1a00-0001-0000-0000-000000000001");
    private static final UUID UUID_CORONA_DUREZA = UUID.fromString("f10c1a00-0001-0000-0000-000000000002");
    private static final UUID UUID_CORONA_VIDA = UUID.fromString("f10c1a00-0001-0000-0000-000000000003");
    private static final UUID UUID_CORONA_VELOCIDAD = UUID.fromString("f10c1a00-0001-0000-0000-000000000004");

    private final List<Entrada> tier1 = new ArrayList<>();
    private final List<Entrada> tier2 = new ArrayList<>();
    private final List<Entrada> tier3 = new ArrayList<>();
    private final List<Drop> drops = new ArrayList<>();

    public LootService() {
        armarTier1();
        armarTier2();
        armarTier3();
        armarDrops();
    }

    // ---------------------------------------------------------- helpers

    private static int entre(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private static ItemStack cant(Material m, int min, int max) {
        return new ItemStack(m, entre(min, max));
    }

    private static ItemStack libro(Enchantment e, int nivel) {
        ItemStack libro = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) libro.getItemMeta();
        meta.addStoredEnchant(e, nivel, true);
        libro.setItemMeta(meta);
        return libro;
    }

    /** Item con encantamientos: pares (Enchantment, Integer). */
    private static ItemStack equipo(Material m, Object... pares) {
        ItemStack item = new ItemStack(m);
        ItemMeta meta = item.getItemMeta();
        for (int i = 0; i + 1 < pares.length; i += 2) {
            meta.addEnchant((Enchantment) pares[i], (Integer) pares[i + 1], true);
        }
        item.setItemMeta(meta);
        return item;
    }

    private static Supplier<ItemStack> externo(String token) {
        return () -> ItemsExternos.crear(token);
    }

    private static void add(List<Entrada> lista, int peso, Supplier<ItemStack> gen) {
        lista.add(new Entrada(peso, gen));
    }

    // ---------------------------------------------------------- tablas

    private void armarTier1() {
        add(tier1, 10, () -> cant(Material.DIAMOND, 2, 4));
        add(tier1, 10, () -> cant(Material.EMERALD, 3, 8));
        add(tier1, 10, () -> cant(Material.GOLD_INGOT, 4, 10));
        add(tier1, 10, () -> cant(Material.IRON_INGOT, 8, 16));
        add(tier1, 8, () -> cant(Material.GOLDEN_APPLE, 2, 3));
        add(tier1, 8, () -> cant(Material.ENDER_PEARL, 4, 8));
        add(tier1, 10, () -> cant(Material.EXPERIENCE_BOTTLE, 8, 16));
        add(tier1, 6, () -> cant(Material.ARROW, 16, 32));
        add(tier1, 8, () -> cant(Material.COOKED_BEEF, 8, 16));
        add(tier1, 4, () -> libro(Enchantment.DAMAGE_ALL, 4));
        add(tier1, 4, () -> libro(Enchantment.PROTECTION_ENVIRONMENTAL, 4));
        add(tier1, 4, () -> libro(Enchantment.DIG_SPEED, 4));
        add(tier1, 4, () -> libro(Enchantment.DURABILITY, 3));
        add(tier1, 4, () -> libro(Enchantment.LOOT_BONUS_BLOCKS, 3));
        add(tier1, 4, () -> libro(Enchantment.LOOT_BONUS_MOBS, 3));
        add(tier1, 3, () -> libro(Enchantment.MENDING, 1));
        add(tier1, 3, () -> equipo(Material.DIAMOND_PICKAXE, Enchantment.DIG_SPEED, 4, Enchantment.DURABILITY, 3));
        add(tier1, 3, () -> equipo(Material.DIAMOND_SWORD, Enchantment.DAMAGE_ALL, 4, Enchantment.DURABILITY, 3));
        add(tier1, 4, () -> cant(Material.NETHERITE_SCRAP, 1, 2));
        add(tier1, 2, () -> new ItemStack(Material.TOTEM_OF_UNDYING));
    }

    private void armarTier2() {
        add(tier2, 10, () -> cant(Material.DIAMOND, 4, 8));
        add(tier2, 5, () -> cant(Material.DIAMOND_BLOCK, 1, 2));
        add(tier2, 5, () -> cant(Material.EMERALD_BLOCK, 1, 2));
        add(tier2, 8, () -> cant(Material.NETHERITE_SCRAP, 2, 4));
        add(tier2, 3, () -> new ItemStack(Material.NETHERITE_INGOT));
        add(tier2, 4, () -> cant(Material.ENCHANTED_GOLDEN_APPLE, 1, 2));
        add(tier2, 4, () -> new ItemStack(Material.TOTEM_OF_UNDYING));
        add(tier2, 8, () -> cant(Material.EXPERIENCE_BOTTLE, 16, 32));
        add(tier2, 5, () -> cant(Material.ENDER_PEARL, 8, 16));
        add(tier2, 5, () -> cant(Material.GOLDEN_CARROT, 16, 32));
        add(tier2, 4, () -> new ItemStack(Material.SHULKER_BOX));
        add(tier2, 2, () -> new ItemStack(Material.ELYTRA));
        add(tier2, 1, () -> new ItemStack(Material.BEACON));
        add(tier2, 2, () -> libro(Enchantment.DAMAGE_ALL, 5));
        add(tier2, 6, () -> libro(Enchantment.PROTECTION_ENVIRONMENTAL, 4));
        add(tier2, 5, () -> libro(Enchantment.MENDING, 1));
        add(tier2, 5, () -> libro(Enchantment.LOOT_BONUS_BLOCKS, 3));
        add(tier2, 5, () -> libro(Enchantment.LOOT_BONUS_MOBS, 3));
        add(tier2, 4, () -> libro(Enchantment.DIG_SPEED, 5));
        add(tier2, 3, () -> libro(Enchantment.THORNS, 3));
        add(tier2, 3, () -> libro(Enchantment.FIRE_ASPECT, 2));
        for (Material pieza : new Material[]{Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE,
                Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS}) {
            add(tier2, 3, () -> equipo(pieza, Enchantment.PROTECTION_ENVIRONMENTAL, 4, Enchantment.DURABILITY, 3));
        }
        add(tier2, 3, () -> equipo(Material.DIAMOND_SWORD, Enchantment.DAMAGE_ALL, 5,
                Enchantment.LOOT_BONUS_MOBS, 3, Enchantment.DURABILITY, 3));
        add(tier2, 3, () -> equipo(Material.BOW, Enchantment.ARROW_DAMAGE, 5, Enchantment.DURABILITY, 3));
    }

    private void armarTier3() {
        add(tier3, 10, () -> cant(Material.NETHERITE_INGOT, 2, 4));
        add(tier3, 8, () -> cant(Material.NETHERITE_SCRAP, 4, 8));
        add(tier3, 3, () -> new ItemStack(Material.NETHER_STAR));
        add(tier3, 8, () -> cant(Material.ENCHANTED_GOLDEN_APPLE, 3, 6));
        add(tier3, 6, () -> cant(Material.TOTEM_OF_UNDYING, 1, 2));
        add(tier3, 6, () -> new ItemStack(Material.ELYTRA));
        add(tier3, 4, () -> new ItemStack(Material.BEACON));
        add(tier3, 6, () -> new ItemStack(Material.SHULKER_BOX));
        add(tier3, 6, () -> cant(Material.DIAMOND_BLOCK, 3, 6));
        add(tier3, 4, () -> cant(Material.EMERALD_BLOCK, 2, 4));
        add(tier3, 4, () -> libro(Enchantment.DAMAGE_ALL, 5));
        add(tier3, 4, () -> libro(Enchantment.PROTECTION_ENVIRONMENTAL, 4));
        add(tier3, 4, () -> libro(Enchantment.MENDING, 1));
        for (Material pieza : new Material[]{Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE,
                Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS}) {
            add(tier3, 4, () -> equipo(pieza, Enchantment.PROTECTION_ENVIRONMENTAL, 4,
                    Enchantment.DURABILITY, 3, Enchantment.MENDING, 1));
        }
        add(tier3, 5, () -> equipo(Material.NETHERITE_SWORD, Enchantment.DAMAGE_ALL, 5, Enchantment.FIRE_ASPECT, 2,
                Enchantment.LOOT_BONUS_MOBS, 3, Enchantment.DURABILITY, 3, Enchantment.MENDING, 1));
        add(tier3, 5, () -> equipo(Material.NETHERITE_PICKAXE, Enchantment.DIG_SPEED, 5,
                Enchantment.LOOT_BONUS_BLOCKS, 3, Enchantment.DURABILITY, 3, Enchantment.MENDING, 1));
        // Items de tus otros plugins (si estan instalados)
        add(tier3, 2, externo("libro_florencia"));
        add(tier3, 2, externo("cuerno_cabra"));
        add(tier3, 2, externo("vestigio_aparecido"));
        add(tier3, 2, externo("pava"));
    }

    private void armarDrops() {
        drops.add(new Drop(1, 10, () -> cant(Material.DIAMOND, 1, 2)));
        drops.add(new Drop(1, 10, () -> cant(Material.EMERALD, 2, 4)));
        drops.add(new Drop(1, 8, () -> cant(Material.GOLD_INGOT, 2, 5)));
        drops.add(new Drop(1, 8, () -> cant(Material.EXPERIENCE_BOTTLE, 4, 8)));
        drops.add(new Drop(2, 6, () -> cant(Material.GOLDEN_APPLE, 1, 2)));
        drops.add(new Drop(2, 4, () -> cant(Material.ENDER_PEARL, 2, 4)));
        drops.add(new Drop(2, 4, () -> libro(Enchantment.DAMAGE_ALL, 3)));
        drops.add(new Drop(2, 4, () -> libro(Enchantment.PROTECTION_ENVIRONMENTAL, 3)));
        drops.add(new Drop(2, 4, () -> libro(Enchantment.DURABILITY, 3)));
        drops.add(new Drop(3, 5, () -> new ItemStack(Material.NETHERITE_SCRAP)));
        drops.add(new Drop(4, 2, () -> new ItemStack(Material.ENCHANTED_GOLDEN_APPLE)));
        drops.add(new Drop(4, 2, () -> new ItemStack(Material.DIAMOND_BLOCK)));
        drops.add(new Drop(4, 1, () -> new ItemStack(Material.TOTEM_OF_UNDYING)));
        drops.add(new Drop(5, 1, externo("vestigio_aparecido")));
    }

    // ---------------------------------------------------------- uso

    private List<Entrada> tabla(int tier) {
        return tier >= 3 ? tier3 : (tier == 2 ? tier2 : tier1);
    }

    /** Un item al azar de la tabla de cofres de ese tier (null si justo salio un item externo no disponible). */
    public ItemStack elegir(int tier) {
        List<Entrada> lista = tabla(tier);
        int total = 0;
        for (Entrada e : lista) total += e.peso();
        int tiro = ThreadLocalRandom.current().nextInt(total);
        int acumulado = 0;
        for (Entrada e : lista) {
            acumulado += e.peso();
            if (tiro < acumulado) return e.gen().get();
        }
        return null;
    }

    /** Llena un cofre con varios items de ese tier, en casilleros al azar. */
    public void llenar(Inventory inv, int tier, Random r) {
        int tiradas = tier >= 3 ? 10 + r.nextInt(4) : (tier == 2 ? 7 + r.nextInt(4) : 4 + r.nextInt(3));
        for (int i = 0; i < tiradas; i++) {
            ItemStack item = null;
            for (int intento = 0; intento < 5 && item == null; intento++) item = elegir(tier);
            if (item == null) continue;
            for (int intento = 0; intento < 12; intento++) {
                int slot = r.nextInt(inv.getSize());
                if (inv.getItem(slot) == null) {
                    inv.setItem(slot, item);
                    break;
                }
            }
        }
    }

    /** Drops raros para un mob modificado de ese tier (1 a 5). Lista vacia si no toco nada. */
    public List<ItemStack> dropsDeMob(int tier, Random r) {
        List<ItemStack> resultado = new ArrayList<>();
        double[] probabilidad = {0, 0.25, 0.35, 0.45, 0.60, 0.80};
        int t = Math.max(1, Math.min(5, tier));
        if (r.nextDouble() >= probabilidad[t]) return resultado;

        int tiradas = (t >= 4 && r.nextDouble() < 0.4) ? 2 : 1;
        for (int k = 0; k < tiradas; k++) {
            int total = 0;
            for (Drop d : drops) if (d.tierMin() <= t) total += d.peso();
            int tiro = r.nextInt(total);
            int acumulado = 0;
            for (Drop d : drops) {
                if (d.tierMin() > t) continue;
                acumulado += d.peso();
                if (tiro < acumulado) {
                    ItemStack item = d.gen().get();
                    if (item != null) resultado.add(item);
                    break;
                }
            }
        }
        return resultado;
    }

    /** La recompensa especial del jefe: un casco dorado con atributos. */
    public ItemStack corona() {
        ItemStack item = new ItemStack(Material.GOLDEN_HELMET);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Corona de Florencia", NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Flotaba siempre un poco por encima de su cabeza.", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Ahora es tuya.", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Al llevarla puesta:", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("+4 Armadura", NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false),
                Component.text("+3 Dureza de armadura", NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false),
                Component.text("+3 Corazones de vida máxima", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false),
                Component.text("+8% Velocidad de movimiento", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)
        ));
        meta.setUnbreakable(true);
        meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
        meta.addEnchant(Enchantment.OXYGEN, 3, true);
        meta.addAttributeModifier(Attribute.GENERIC_ARMOR, new AttributeModifier(
                UUID_CORONA_ARMADURA, "corona_armadura", 4.0,
                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HEAD));
        meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, new AttributeModifier(
                UUID_CORONA_DUREZA, "corona_dureza", 3.0,
                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HEAD));
        meta.addAttributeModifier(Attribute.GENERIC_MAX_HEALTH, new AttributeModifier(
                UUID_CORONA_VIDA, "corona_vida", 6.0,
                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HEAD));
        meta.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED, new AttributeModifier(
                UUID_CORONA_VELOCIDAD, "corona_velocidad", 0.08,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlot.HEAD));
        item.setItemMeta(meta);
        return item;
    }
}
