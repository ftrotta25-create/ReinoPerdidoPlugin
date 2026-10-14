package com.franchee.reino.mobs;

import com.franchee.reino.ReinoPerdidoPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.CaveSpider;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Vex;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

/**
 * Convierte a los mobs hostiles del Reino Perdido en versiones "modificadas": mas vida, mas
 * dano, equipo, nombre con rango y drops raros. Hay 5 niveles (tier): cuanto mas cerca de la
 * ciudadela, mas fuertes. Los guardias que spawnean las zonas se modifican a mano (CUSTOM).
 */
public class ModificadorMobs implements Listener {

    private static final double[] VIDA = {1, 1.8, 2.5, 3.5, 5.0, 7.0};
    private static final double[] DANIO = {1, 1.4, 1.8, 2.2, 2.8, 3.3};
    private static final double[] VELOCIDAD = {1, 1.0, 1.08, 1.12, 1.15, 1.2};
    private static final NamedTextColor[] COLOR = {
            NamedTextColor.WHITE, NamedTextColor.GRAY, NamedTextColor.YELLOW,
            NamedTextColor.GOLD, NamedTextColor.RED, NamedTextColor.LIGHT_PURPLE};
    private static final String[] RANGO = {
            "", "Errante", "Corrupto", "Centinela", "Guardián Real", "Campeón de la Reina"};

    private final ReinoPerdidoPlugin plugin;
    private final NamespacedKey keyTier;
    private final NamespacedKey keyZona;
    private final Random random = new Random();

    public ModificadorMobs(ReinoPerdidoPlugin plugin) {
        this.plugin = plugin;
        this.keyTier = new NamespacedKey(plugin, "reino_tier");
        this.keyZona = new NamespacedKey(plugin, "reino_zona");
    }

    public NamespacedKey keyZona() {
        return keyZona;
    }

    public NamespacedKey keyTier() {
        return keyTier;
    }

    // ------------------------------------------------------------ modificar

    public void aplicar(LivingEntity e, int tierPedido, String zonaId) {
        if (e.getPersistentDataContainer().has(keyTier, PersistentDataType.INTEGER)) return;
        int tier = Math.max(1, Math.min(5, tierPedido));

        e.getPersistentDataContainer().set(keyTier, PersistentDataType.INTEGER, tier);
        if (zonaId != null) {
            e.getPersistentDataContainer().set(keyZona, PersistentDataType.STRING, zonaId);
        }

        escalar(e, Attribute.GENERIC_MAX_HEALTH, VIDA[tier]);
        AttributeInstance vida = e.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (vida != null) e.setHealth(vida.getBaseValue());
        escalar(e, Attribute.GENERIC_ATTACK_DAMAGE, DANIO[tier]);
        escalar(e, Attribute.GENERIC_MOVEMENT_SPEED, VELOCIDAD[tier]);

        AttributeInstance seguir = e.getAttribute(Attribute.GENERIC_FOLLOW_RANGE);
        if (seguir != null) seguir.setBaseValue(40);

        equipar(e, tier);

        if (e instanceof Creeper creeper && tier >= 3) {
            creeper.setPowered(true);
        }

        e.customName(Component.text(nombreBase(e.getType()) + " " + RANGO[tier], COLOR[tier]));
        e.setCustomNameVisible(true);
    }

    private void escalar(LivingEntity e, Attribute atributo, double factor) {
        AttributeInstance a = e.getAttribute(atributo);
        if (a != null) a.setBaseValue(a.getBaseValue() * factor);
    }

    private String nombreBase(EntityType tipo) {
        return switch (tipo) {
            case ZOMBIE -> "Zombi";
            case HUSK -> "Husk";
            case DROWNED -> "Ahogado";
            case SKELETON -> "Esqueleto";
            case STRAY -> "Esqueleto Helado";
            case WITHER_SKELETON -> "Esqueleto Wither";
            case SPIDER -> "Araña";
            case CAVE_SPIDER -> "Araña Cavernaria";
            case CREEPER -> "Creeper";
            case WITCH -> "Bruja";
            case VINDICATOR -> "Vindicador";
            case PILLAGER -> "Saqueador";
            case ENDERMAN -> "Enderman";
            default -> {
                String n = tipo.name().toLowerCase().replace('_', ' ');
                yield Character.toUpperCase(n.charAt(0)) + n.substring(1);
            }
        };
    }

    // ------------------------------------------------------------ equipo

    private ItemStack encantado(Material material, Object... pares) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        for (int i = 0; i + 1 < pares.length; i += 2) {
            meta.addEnchant((Enchantment) pares[i], (Integer) pares[i + 1], true);
        }
        item.setItemMeta(meta);
        return item;
    }

    private void equipar(LivingEntity e, int tier) {
        EntityEquipment eq = e.getEquipment();
        if (eq == null) return;

        boolean usaArmadura = e instanceof Zombie || e instanceof AbstractSkeleton;
        if (usaArmadura) {
            Material[] set = switch (tier) {
                case 1 -> new Material[]{Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS};
                case 2 -> new Material[]{Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS};
                case 3 -> new Material[]{Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS};
                case 4 -> new Material[]{Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS};
                default -> new Material[]{Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS};
            };
            double probabilidad = tier == 1 ? 0.35 : (tier == 2 ? 0.55 : 0.9);
            int proteccion = tier >= 5 ? 4 : (tier == 4 ? 3 : 0);

            ItemStack[] piezas = new ItemStack[4];
            for (int i = 0; i < 4; i++) {
                if (random.nextDouble() < probabilidad) {
                    piezas[i] = proteccion > 0
                            ? encantado(set[i], Enchantment.PROTECTION_ENVIRONMENTAL, proteccion)
                            : new ItemStack(set[i]);
                }
            }
            eq.setHelmet(piezas[0]);
            eq.setChestplate(piezas[1]);
            eq.setLeggings(piezas[2]);
            eq.setBoots(piezas[3]);
        }

        if (e instanceof WitherSkeleton) {
            if (tier >= 4) eq.setItemInMainHand(encantado(Material.NETHERITE_SWORD, Enchantment.DAMAGE_ALL, 3));
        } else if (e instanceof AbstractSkeleton) {
            int poder = tier == 1 ? 0 : (tier == 2 ? 1 : (tier == 3 ? 2 : (tier == 4 ? 4 : 5)));
            eq.setItemInMainHand(poder > 0
                    ? encantado(Material.BOW, Enchantment.ARROW_DAMAGE, poder)
                    : new ItemStack(Material.BOW));
        } else if (e instanceof Zombie) {
            switch (tier) {
                case 2 -> eq.setItemInMainHand(new ItemStack(Material.IRON_SWORD));
                case 3 -> eq.setItemInMainHand(encantado(Material.IRON_SWORD, Enchantment.DAMAGE_ALL, 2));
                case 4 -> eq.setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));
                case 5 -> eq.setItemInMainHand(encantado(Material.NETHERITE_SWORD, Enchantment.DAMAGE_ALL, 3));
                default -> {
                }
            }
        }

        // Nada del equipo se dropea: los premios salen de la tabla de loot del Reino
        eq.setHelmetDropChance(0f);
        eq.setChestplateDropChance(0f);
        eq.setLeggingsDropChance(0f);
        eq.setBootsDropChance(0f);
        eq.setItemInMainHandDropChance(0f);
        eq.setItemInOffHandDropChance(0f);
    }

    // ------------------------------------------------------------ eventos

    private int tierPorDistancia(Location l) {
        int d = Math.max(Math.abs(l.getBlockX()), Math.abs(l.getBlockZ()));
        if (d <= 90) return 4;
        if (d <= 200) return 3;
        if (d <= 450) return 2;
        return 1;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        LivingEntity ent = e.getEntity();
        if (!plugin.esReino(ent.getWorld())) return;

        CreatureSpawnEvent.SpawnReason motivo = e.getSpawnReason();

        if (!(ent instanceof Monster)) {
            boolean natural = motivo == CreatureSpawnEvent.SpawnReason.NATURAL
                    || motivo == CreatureSpawnEvent.SpawnReason.CHUNK_GEN
                    || motivo == CreatureSpawnEvent.SpawnReason.VILLAGE_INVASION;
            boolean animal = !(ent instanceof org.bukkit.entity.Enemy) && !(ent instanceof Player);
            if (natural && animal && plugin.getConfig().getBoolean("bloquearAnimales", true)) {
                e.setCancelled(true);
            }
            return;
        }

        if (motivo == CreatureSpawnEvent.SpawnReason.CUSTOM) return; // los de zona/jefe se modifican a mano
        if (ent instanceof Vex) return;                               // los vex del jefe quedan vanilla
        aplicar(ent, tierPorDistancia(ent.getLocation()), null);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        LivingEntity ent = e.getEntity();
        Integer tier = ent.getPersistentDataContainer().get(keyTier, PersistentDataType.INTEGER);
        if (tier == null || ent.getKiller() == null) return;

        e.setDroppedExp(e.getDroppedExp() * (1 + tier));
        e.getDrops().addAll(plugin.getLoot().dropsDeMob(tier, random));
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victima)) return;

        LivingEntity atacante = null;
        if (e.getDamager() instanceof LivingEntity le) {
            atacante = le;
        } else if (e.getDamager() instanceof Projectile p && p.getShooter() instanceof LivingEntity le2) {
            atacante = le2;
        }
        if (atacante == null) return;

        Integer tier = atacante.getPersistentDataContainer().get(keyTier, PersistentDataType.INTEGER);
        if (tier == null || tier < 2) return;

        if (atacante instanceof Spider || atacante instanceof CaveSpider) {
            victima.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 80 + tier * 20, tier >= 4 ? 1 : 0));
        } else if (atacante instanceof Zombie) {
            victima.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60 + tier * 10, tier >= 4 ? 1 : 0));
        } else if (atacante instanceof Skeleton && tier >= 3) {
            victima.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 80, 0));
        }
    }

    /** Los creepers del Reino no rompen bloques: asi no se destruyen las estructuras. */
    @EventHandler
    public void onExplosion(EntityExplodeEvent e) {
        if (plugin.esReino(e.getEntity().getWorld()) && e.getEntity() instanceof Creeper) {
            e.blockList().clear();
        }
    }
}
