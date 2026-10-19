package com.franchee.reino.ambiente;

import com.franchee.reino.ReinoPerdidoPlugin;
import com.franchee.reino.estructuras.Ciudadela;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Hace que el Reino Perdido se sienta vivo:
 *  - Titulo + sonido + espiral de particulas al entrar a la dimension.
 *  - Ceniza flotando, destellos violetas y bruma cerca de la ciudadela.
 *  - Sonidos ambientales a lo lejos y latidos dentro del torreon y las catacumbas.
 *  - Un titulo al entrar a cada zona de la ciudadela.
 * Se puede apagar desde config.yml (ambiente.efectos / ambiente.titulos).
 */
public class AmbienteManager implements Listener {

    private enum Area { NINGUNA, CIUDADELA, TORREON, TRONO, CATACUMBAS }

    private final ReinoPerdidoPlugin plugin;
    private final Random random = new Random();
    private final Map<UUID, Area> areaActual = new HashMap<>();
    private final Map<UUID, Long> proximoSonido = new HashMap<>();
    private final Map<UUID, Long> proximoLatido = new HashMap<>();
    private final Map<UUID, Long> ultimoTituloArea = new HashMap<>();

    public AmbienteManager(ReinoPerdidoPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean efectos() {
        return plugin.getConfig().getBoolean("ambiente.efectos", true);
    }

    private boolean titulos() {
        return plugin.getConfig().getBoolean("ambiente.titulos", true);
    }

    // ---------------------------------------------------------- entrada al mundo

    @EventHandler
    public void onCambioDeMundo(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();
        if (plugin.esReino(p.getWorld())) entrar(p);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!plugin.esReino(p.getWorld())) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline() && plugin.esReino(p.getWorld())) entrar(p);
        }, 30L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID u = e.getPlayer().getUniqueId();
        areaActual.remove(u);
        proximoSonido.remove(u);
        proximoLatido.remove(u);
        ultimoTituloArea.remove(u);
    }

    private void entrar(Player p) {
        if (titulos()) {
            p.showTitle(Title.title(
                    Component.text("EL REINO PERDIDO", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD),
                    Component.text("Aquí gobernó Florencia, la Reina", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(800), Duration.ofMillis(3500), Duration.ofMillis(1500))));
        }
        if (!efectos()) return;

        p.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.5f, 0.9f);
        float[] tonos = {0.8f, 1.0f, 1.3f};
        for (int i = 0; i < tonos.length; i++) {
            final float tono = tonos[i];
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, tono);
            }, 15L + i * 12L);
        }
        espiral(p);
    }

    /** Dos hilos de particulas que giran y suben alrededor del jugador durante ~3 segundos. */
    private void espiral(Player p) {
        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!p.isOnline() || t > 30) {
                    if (p.isOnline()) {
                        Location l = p.getLocation().add(0, 1, 0);
                        p.getWorld().spawnParticle(Particle.DRAGON_BREATH, l, 60, 0.6, 0.8, 0.6, 0.03);
                    }
                    cancel();
                    return;
                }
                Location l = p.getLocation();
                for (int k = 0; k < 2; k++) {
                    double angulo = t * 0.5 + k * Math.PI;
                    double radio = 1.4 - t * 0.02;
                    double x = l.getX() + Math.cos(angulo) * radio;
                    double y = l.getY() + t * 0.07;
                    double z = l.getZ() + Math.sin(angulo) * radio;
                    p.getWorld().spawnParticle(Particle.PORTAL, x, y, z, 4, 0.1, 0.1, 0.1, 0.1);
                    p.getWorld().spawnParticle(Particle.END_ROD, x, y, z, 1, 0, 0, 0, 0.01);
                }
                t++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    // ---------------------------------------------------------- tick

    /** Se llama cada 10 ticks (medio segundo). */
    public void tick() {
        if (!efectos() && !titulos()) return;
        World w = plugin.mundo();
        if (w == null) return;

        long ahora = System.currentTimeMillis();
        for (Player p : w.getPlayers()) {
            if (p.getGameMode() == GameMode.SPECTATOR) continue;
            Location l = p.getLocation();

            Area area = detectarArea(l);
            cambiarArea(p, area, ahora);

            if (!efectos()) continue;
            particulas(p, l, area);
            sonidos(p, area, ahora);
        }
    }

    private Area detectarArea(Location l) {
        int x = l.getBlockX();
        int y = l.getBlockY();
        int z = l.getBlockZ();
        int y0 = Ciudadela.Y0;

        if (x >= -26 && x <= 26 && z >= -48 && z <= 26 && y >= y0 - 12 && y <= y0 - 7) return Area.CATACUMBAS;
        if (Math.abs(x) <= 19 && Math.abs(z) <= 19) {
            if (y >= y0 + 25 && y <= y0 + 44) return Area.TRONO;
            if (y >= y0 + 1 && y <= y0 + 24) return Area.TORREON;
        }
        if (Math.max(Math.abs(x), Math.abs(z)) <= 74 && y >= y0 - 2) return Area.CIUDADELA;
        return Area.NINGUNA;
    }

    private void cambiarArea(Player p, Area nueva, long ahora) {
        UUID u = p.getUniqueId();
        Area anterior = areaActual.getOrDefault(u, Area.NINGUNA);
        if (anterior == nueva) return;
        areaActual.put(u, nueva);

        if (nueva == Area.NINGUNA || !titulos()) return;
        Long ultimo = ultimoTituloArea.get(u);
        if (ultimo != null && ahora - ultimo < 8000) return; // evita repetir en los bordes
        ultimoTituloArea.put(u, ahora);

        switch (nueva) {
            case CIUDADELA -> mostrar(p, "Ciudadela de Florencia", "Los muros todavía recuerdan a la reina");
            case TORREON -> mostrar(p, "El Torreón", "Algo se mueve en las sombras");
            case TRONO -> mostrar(p, "La Sala del Trono", "Ella te estaba esperando");
            case CATACUMBAS -> mostrar(p, "Las Catacumbas", "Aquí abajo, hasta el silencio pesa");
            default -> {
            }
        }
        if (efectos()) {
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 0.6f);
        }
    }

    private void mostrar(Player p, String principal, String subtitulo) {
        p.showTitle(Title.title(
                Component.text(principal, NamedTextColor.LIGHT_PURPLE),
                Component.text(subtitulo, NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2500), Duration.ofMillis(1000))));
    }

    // ---------------------------------------------------------- particulas

    private void particulas(Player p, Location l, Area area) {
        double x = l.getX();
        double y = l.getY();
        double z = l.getZ();

        // Ceniza flotando alrededor
        for (int i = 0; i < 5; i++) {
            p.spawnParticle(Particle.ASH,
                    x + random.nextGaussian() * 5, y + random.nextDouble() * 5, z + random.nextGaussian() * 5,
                    1, 0, 0, 0, 0);
        }
        // Destellos violetas de vez en cuando
        if (random.nextDouble() < 0.6) {
            p.spawnParticle(Particle.END_ROD,
                    x + random.nextGaussian() * 4, y + 0.5 + random.nextDouble() * 3, z + random.nextGaussian() * 4,
                    1, 0, 0.03, 0, 0.01);
        }
        // Bruma violeta cerca de la ciudadela
        int distancia = Math.max(Math.abs(l.getBlockX()), Math.abs(l.getBlockZ()));
        if (distancia <= 100 || area != Area.NINGUNA) {
            p.spawnParticle(Particle.DRAGON_BREATH,
                    x + random.nextGaussian() * 4, y + 0.3 + random.nextDouble() * 2, z + random.nextGaussian() * 4,
                    2, 0.3, 0.2, 0.3, 0.01);
        }
        // Glifos flotando adentro de los edificios
        if (area == Area.TORREON || area == Area.TRONO || area == Area.CATACUMBAS) {
            p.spawnParticle(Particle.ENCHANTMENT_TABLE, x, y + 2, z, 8, 2.0, 0.8, 2.0, 0.6);
        }
    }

    // ---------------------------------------------------------- sonidos

    private void sonidos(Player p, Area area, long ahora) {
        UUID u = p.getUniqueId();

        Long siguiente = proximoSonido.get(u);
        if (siguiente == null) {
            proximoSonido.put(u, ahora + 5000 + random.nextInt(10000));
        } else if (ahora >= siguiente) {
            ambiental(p);
            proximoSonido.put(u, ahora + 14000 + random.nextInt(16000));
        }

        if (area == Area.TORREON || area == Area.TRONO || area == Area.CATACUMBAS) {
            long intervalo = area == Area.CATACUMBAS ? 1800 : 2600;
            Long latido = proximoLatido.get(u);
            if (latido == null || ahora >= latido) {
                p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 1.0f);
                proximoLatido.put(u, ahora + intervalo);
            }
        }
    }

    /** Un sonido lejano, desde una direccion al azar. */
    private void ambiental(Player p) {
        Location origen = p.getLocation().add(
                random.nextGaussian() * 8, random.nextGaussian() * 2, random.nextGaussian() * 8);
        switch (random.nextInt(4)) {
            case 0 -> p.playSound(origen, Sound.AMBIENT_CAVE, 0.7f, 0.7f + random.nextFloat() * 0.3f);
            case 1 -> p.playSound(origen, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 0.7f, 0.8f);
            case 2 -> p.playSound(origen, Sound.ENTITY_VEX_AMBIENT, 0.25f, 0.5f);
            default -> p.playSound(origen, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.6f, 0.5f + random.nextFloat() * 0.4f);
        }
    }
}
