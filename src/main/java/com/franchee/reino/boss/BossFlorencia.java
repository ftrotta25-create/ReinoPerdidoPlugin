package com.franchee.reino.boss;

import com.franchee.reino.ReinoPerdidoPlugin;
import com.franchee.reino.estructuras.Ciudadela;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Florencia, la Reina Perdida: jefe final de la ciudadela, en la sala del trono.
 *
 * Es un Evoker (conserva su IA de hechicero: colmillos y vex) con mucha vida y 3 habilidades
 * propias que se van sumando segun baja su vida:
 *   - Llamarada de amatista (siempre): dano en area cerca del jefe.
 *   - Guardias de la reina (<66%): invoca guardias modificados.
 *   - Eclipse (<33%): ceguera, lentitud y dano a todos los que estan cerca. Ademas entra en furia.
 * Reaparece despues de unos minutos (config jefe.minutosRespawn).
 */
public class BossFlorencia implements Listener {

    private static final double RADIO_CORREA = 32.0;

    private final ReinoPerdidoPlugin plugin;
    private final NamespacedKey keyBoss;
    private final File archivo;
    private final YamlConfiguration datos;
    private final Random random = new Random();

    private final BossBar barra = BossBar.bossBar(
            Component.text("Florencia, la Reina Perdida", NamedTextColor.LIGHT_PURPLE),
            1.0f, BossBar.Color.PURPLE, BossBar.Overlay.NOTCHED_10);

    private final Map<UUID, Double> danioPorJugador = new HashMap<>();
    private UUID bossId;
    private long tLlamarada;
    private long tGuardias;
    private long tEclipse;
    private boolean enFuria;
    private boolean avisoSkin;

    public BossFlorencia(ReinoPerdidoPlugin plugin) {
        this.plugin = plugin;
        this.keyBoss = new NamespacedKey(plugin, "reino_jefe");
        this.archivo = new File(plugin.getDataFolder(), "jefe.yml");
        this.datos = YamlConfiguration.loadConfiguration(archivo);
    }

    // ------------------------------------------------------------ estado

    private Location centroTrono(World w) {
        return new Location(w, 0.5, Ciudadela.Y0 + Ciudadela.JEFE[1], Ciudadela.JEFE[2] + 0.5);
    }

    private LivingEntity boss() {
        if (bossId == null) return null;
        Entity e = Bukkit.getEntity(bossId);
        return (e instanceof LivingEntity le && le.isValid() && !le.isDead()) ? le : null;
    }

    public boolean estaVivo() {
        return boss() != null;
    }

    public long msParaReaparecer() {
        return Math.max(0, datos.getLong("proximoRespawn", 0) - System.currentTimeMillis());
    }

    private void guardar() {
        try {
            datos.save(archivo);
        } catch (IOException ex) {
            plugin.getLogger().warning("No se pudo guardar jefe.yml: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------ invocacion

    /** Lo llama la zona del trono cuando hay un jugador adentro. */
    public void intentarInvocar(World w) {
        if (estaVivo()) return;
        if (adoptarSiExiste(w)) return;
        if (msParaReaparecer() > 0) return;
        invocar(w);
    }

    /** Si el jefe ya estaba en el mundo (por ejemplo, despues de reiniciar el server) lo vuelve a seguir. */
    private boolean adoptarSiExiste(World w) {
        for (Entity e : w.getNearbyEntities(centroTrono(w), 30, 30, 30)) {
            if (e instanceof LivingEntity le && le.getPersistentDataContainer().has(keyBoss, PersistentDataType.BYTE)) {
                bossId = le.getUniqueId();
                aplicarDisfraz(le);
                return true;
            }
        }
        return false;
    }

    /** Admin: reaparece ya, ignorando el tiempo de espera. */
    public void forzar(World w) {
        LivingEntity actual = boss();
        if (actual != null) actual.remove();
        bossId = null;
        invocar(w);
    }

    private void invocar(World w) {
        double vidaMax = plugin.getConfig().getDouble("jefe.vida", 900);
        Location loc = centroTrono(w);
        danioPorJugador.clear();
        enFuria = false;

        Evoker evoker = w.spawn(loc, Evoker.class, e -> {
            e.customName(Component.text("Florencia, la Reina Perdida", NamedTextColor.LIGHT_PURPLE));
            e.setCustomNameVisible(true);
            e.setPersistent(true);
            e.setRemoveWhenFarAway(false);
            e.setCanJoinRaid(false);
            e.getPersistentDataContainer().set(keyBoss, PersistentDataType.BYTE, (byte) 1);

            AttributeInstance vida = e.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (vida != null) vida.setBaseValue(vidaMax);
            e.setHealth(vidaMax);
            AttributeInstance armadura = e.getAttribute(Attribute.GENERIC_ARMOR);
            if (armadura != null) armadura.setBaseValue(10);
            AttributeInstance retroceso = e.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
            if (retroceso != null) retroceso.setBaseValue(0.8);
            AttributeInstance seguir = e.getAttribute(Attribute.GENERIC_FOLLOW_RANGE);
            if (seguir != null) seguir.setBaseValue(48);

            e.getEquipment().setHelmet(new ItemStack(Material.GOLDEN_HELMET));
            e.getEquipment().setHelmetDropChance(0f);
        });

        bossId = evoker.getUniqueId();
        aplicarDisfraz(evoker);
        long ahora = System.currentTimeMillis();
        tLlamarada = ahora + 6000;
        tGuardias = ahora + 12000;
        tEclipse = ahora + 10000;

        w.playSound(loc, Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.7f);
        w.spawnParticle(Particle.DRAGON_BREATH, loc.clone().add(0, 1, 0), 120, 1.5, 1.5, 1.5, 0.05);
        Bukkit.broadcast(Component.text(
                "Algo se despierta en la sala del trono de la Ciudadela del Reino Perdido...",
                NamedTextColor.DARK_PURPLE));

        for (Player p : w.getNearbyPlayers(loc, 40)) {
            p.showTitle(Title.title(
                    Component.text("FLORENCIA", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD),
                    Component.text("la Reina Perdida", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(800))));
        }
    }

    /**
     * Le pone la skin de mujer (via LibsDisguises) si esta instalado y configurado.
     * Si no, Florencia se ve como Evoker y todo lo demas funciona igual.
     */
    private void aplicarDisfraz(LivingEntity boss) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("LibsDisguises")) return;

        String value = plugin.getConfig().getString("skinFlorencia.value", "");
        String signature = plugin.getConfig().getString("skinFlorencia.signature", "");
        if (value == null || value.isBlank() || signature == null || signature.isBlank()) {
            if (!avisoSkin) {
                avisoSkin = true;
                plugin.getLogger().info("Florencia no tiene skin configurada (skinFlorencia.value/signature "
                        + "en config.yml): se ve como Evoker.");
            }
            return;
        }

        try {
            if (DisfrazFlorencia.estaDisfrazado(boss)) return;
            DisfrazFlorencia.aplicar(boss, plugin.getConfig().getString("skinFlorencia.nombre", "Florencia"),
                    value, signature);
        } catch (Throwable ex) {
            plugin.getLogger().warning("No se pudo aplicar el disfraz de Florencia: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------ tick

    /** Se llama cada 10 ticks. */
    public void tick() {
        World w = plugin.mundo();
        if (w == null) return;

        LivingEntity boss = boss();
        if (boss == null) {
            if (bossId != null) { // desaparecio sin morir (descarga, comando...)
                bossId = null;
                ocultarBarra(w);
            }
            return;
        }

        AttributeInstance atributoVida = boss.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double vidaMax = atributoVida != null ? atributoVida.getBaseValue() : 900;
        double fraccion = Math.max(0.0, Math.min(1.0, boss.getHealth() / vidaMax));
        Location loc = boss.getLocation();

        // Barra de vida para los que estan cerca
        barra.progress((float) fraccion);
        for (Player p : w.getPlayers()) {
            if (p.getLocation().distanceSquared(loc) < 70 * 70) barra.addViewer(p);
            else barra.removeViewer(p);
        }

        // Correa: no se aleja de la sala del trono
        Location trono = centroTrono(w);
        if (loc.distanceSquared(trono) > RADIO_CORREA * RADIO_CORREA) {
            boss.teleport(trono);
        }

        Collection<Player> cerca = w.getNearbyPlayers(loc, 40);
        if (cerca.isEmpty()) return;

        long ahora = System.currentTimeMillis();

        if (fraccion <= 0.33 && !enFuria) {
            enFuria = true;
            AttributeInstance velocidad = boss.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (velocidad != null) velocidad.setBaseValue(velocidad.getBaseValue() * 1.25);
            w.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.8f);
            Bukkit.broadcast(Component.text("¡Florencia entra en furia!", NamedTextColor.RED));
        }

        if (ahora >= tLlamarada) {
            llamarada(boss);
            tLlamarada = ahora + (fraccion > 0.33 ? 9000 : 6000);
        }
        if (fraccion <= 0.66 && ahora >= tGuardias) {
            guardias(boss);
            tGuardias = ahora + 22000;
        }
        if (fraccion <= 0.33 && ahora >= tEclipse) {
            eclipse(boss);
            tEclipse = ahora + 15000;
        }
    }

    private void ocultarBarra(World w) {
        for (Player p : w.getPlayers()) barra.removeViewer(p);
    }

    // ------------------------------------------------------------ habilidades

    private void llamarada(LivingEntity boss) {
        World w = boss.getWorld();
        Location loc = boss.getLocation();
        w.playSound(loc, Sound.ENTITY_EVOKER_PREPARE_ATTACK, 2.0f, 0.6f);
        w.spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 1, 0), 80, 3, 1, 3, 0.05);

        for (Player p : w.getNearbyPlayers(loc, 12)) {
            p.damage(8.0, boss);
            w.spawnParticle(Particle.DRAGON_BREATH, p.getLocation().add(0, 1, 0), 25, 0.4, 0.6, 0.4, 0.02);
        }
    }

    private void guardias(LivingEntity boss) {
        World w = boss.getWorld();
        Location base = boss.getLocation();

        int vivos = 0;
        for (Entity e : w.getNearbyEntities(base, 25, 20, 25)) {
            String zona = e.getPersistentDataContainer().get(plugin.getMobs().keyZona(), PersistentDataType.STRING);
            if ("trono".equals(zona)) vivos++;
        }
        if (vivos >= 6) return;

        EntityType[] tipos = {EntityType.ZOMBIE, EntityType.SKELETON, EntityType.VINDICATOR};
        for (int i = 0; i < 3; i++) {
            Location loc = base.clone().add(random.nextInt(9) - 4, 0, random.nextInt(9) - 4);
            Entity e = w.spawnEntity(loc, tipos[random.nextInt(tipos.length)]);
            if (e instanceof LivingEntity vivo) {
                plugin.getMobs().aplicar(vivo, 4, "trono");
            }
            w.spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.02);
        }
        w.playSound(base, Sound.ENTITY_EVOKER_CAST_SPELL, 2.0f, 0.7f);
        Bukkit.broadcast(Component.text("«Guardias, a mí.»", NamedTextColor.LIGHT_PURPLE));
    }

    private void eclipse(LivingEntity boss) {
        World w = boss.getWorld();
        Location loc = boss.getLocation();
        w.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 0.6f);
        w.spawnParticle(Particle.EXPLOSION_NORMAL, loc.clone().add(0, 1, 0), 40, 4, 1, 4, 0.05);

        for (Player p : w.getNearbyPlayers(loc, 30)) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 1));
            p.damage(6.0, boss);
        }
    }

    // ------------------------------------------------------------ eventos

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (bossId == null || !e.getEntity().getUniqueId().equals(bossId)) return;

        Player atacante = null;
        if (e.getDamager() instanceof Player p) {
            atacante = p;
        } else if (e.getDamager() instanceof Projectile proy && proy.getShooter() instanceof Player p) {
            atacante = p;
        }
        if (atacante != null) {
            danioPorJugador.merge(atacante.getUniqueId(), e.getFinalDamage(), Double::sum);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        LivingEntity ent = e.getEntity();
        if (!ent.getPersistentDataContainer().has(keyBoss, PersistentDataType.BYTE)) return;

        e.getDrops().clear();
        e.setDroppedExp(3000);
        entregarRecompensas();

        long minutos = plugin.getConfig().getLong("jefe.minutosRespawn", 45);
        datos.set("proximoRespawn", System.currentTimeMillis() + minutos * 60_000L);
        guardar();

        bossId = null;
        ocultarBarra(ent.getWorld());
        ent.getWorld().playSound(ent.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 2.0f, 0.8f);
        ent.getWorld().spawnParticle(Particle.TOTEM, ent.getLocation().add(0, 1, 0), 150, 1.5, 1.5, 1.5, 0.4);
        ent.getWorld().spawnParticle(Particle.DRAGON_BREATH, ent.getLocation().add(0, 1, 0), 120, 2, 1.5, 2, 0.05);
        Bukkit.broadcast(Component.text("Florencia, la Reina Perdida, se desvaneció... otra vez.", NamedTextColor.LIGHT_PURPLE));
    }

    private void entregarRecompensas() {
        UUID mvpId = null;
        double maximo = -1;
        for (Map.Entry<UUID, Double> entrada : danioPorJugador.entrySet()) {
            if (entrada.getValue() > maximo) {
                maximo = entrada.getValue();
                mvpId = entrada.getKey();
            }
        }

        for (UUID uuid : danioPorJugador.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;

            // 3 tiradas del tesoro de nivel 3 + premio fijo
            for (int i = 0; i < 3; i++) {
                ItemStack item = null;
                for (int intento = 0; intento < 5 && item == null; intento++) item = plugin.getLoot().elegir(3);
                if (item != null) dar(p, item);
            }
            dar(p, new ItemStack(Material.DIAMOND, 8));
            dar(p, new ItemStack(Material.NETHERITE_SCRAP, 3));
            dar(p, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 2));
            p.giveExp(1500);
            p.sendMessage(Component.text("Recibiste tu parte del tesoro de la Reina.", NamedTextColor.LIGHT_PURPLE));
            p.showTitle(Title.title(
                    Component.text("LA REINA CAYÓ", NamedTextColor.GOLD, TextDecoration.BOLD),
                    Component.text("Su tesoro es tuyo", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(400), Duration.ofMillis(3000), Duration.ofMillis(1000))));

            if (uuid.equals(mvpId)) {
                dar(p, plugin.getLoot().corona());
                dar(p, new ItemStack(Material.NETHER_STAR));
                p.sendMessage(Component.text("Fuiste el que más daño le hizo: la Corona de Florencia es tuya.",
                        NamedTextColor.GOLD));
            }
        }

        if (mvpId != null) {
            Player mvp = Bukkit.getPlayer(mvpId);
            String nombre = mvp != null ? mvp.getName() : "???";
            Bukkit.broadcast(Component.text("MVP: " + nombre + " con " + Math.round(maximo) + " de daño al jefe.",
                    NamedTextColor.GOLD));
        }
    }

    private void dar(Player p, ItemStack item) {
        Map<Integer, ItemStack> sobrante = p.getInventory().addItem(item);
        for (ItemStack resto : sobrante.values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), resto);
        }
    }
}
