package com.franchee.reino;

import com.franchee.reino.ambiente.AmbienteManager;
import com.franchee.reino.boss.BossFlorencia;
import com.franchee.reino.estructuras.Ciudadela;
import com.franchee.reino.estructuras.MiniEstructuras;
import com.franchee.reino.gen.ReinoGenerator;
import com.franchee.reino.gen.Terreno;
import com.franchee.reino.listeners.CofresListener;
import com.franchee.reino.loot.LootService;
import com.franchee.reino.mobs.ModificadorMobs;
import com.franchee.reino.zonas.ZonaManager;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * El Reino Perdido de Florencia: una dimension (mundo de Multiverse) generada por este plugin,
 * con minerales ricos, mini estructuras, una ciudadela gigante, mobs modificados y un jefe final.
 */
public class ReinoPerdidoPlugin extends JavaPlugin implements Listener {

    private LootService loot;
    private ModificadorMobs mobs;
    private BossFlorencia boss;
    private ZonaManager zonas;
    private AmbienteManager ambiente;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        // Las estructuras se arman una vez, aca, en el hilo principal, antes de generar ningun chunk.
        Ciudadela.inicializar();
        MiniEstructuras.inicializar();

        loot = new LootService();
        mobs = new ModificadorMobs(this);
        boss = new BossFlorencia(this);
        zonas = new ZonaManager(this);
        ambiente = new AmbienteManager(this);

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(mobs, this);
        getServer().getPluginManager().registerEvents(boss, this);
        getServer().getPluginManager().registerEvents(ambiente, this);
        getServer().getPluginManager().registerEvents(new CofresListener(this), this);

        getServer().getScheduler().runTaskTimer(this, () -> zonas.tick(), 40L, 40L);
        getServer().getScheduler().runTaskTimer(this, () -> boss.tick(), 20L, 10L);
        getServer().getScheduler().runTaskTimer(this, () -> ambiente.tick(), 20L, 10L);

        // Si el mundo ya existe y Multiverse no lo cargo, lo cargamos nosotros.
        getServer().getScheduler().runTaskLater(this, this::autoCargar, 100L);

        getLogger().info("ReinoPerdidoPlugin habilitado.");
    }

    @Override
    public void onDisable() {
        getLogger().info("ReinoPerdidoPlugin deshabilitado.");
    }

    // ---------------------------------------------------------------- acceso

    public LootService getLoot() {
        return loot;
    }

    public ModificadorMobs getMobs() {
        return mobs;
    }

    public BossFlorencia getBoss() {
        return boss;
    }

    public String nombreMundo() {
        return getConfig().getString("mundo", "reino_perdido");
    }

    public World mundo() {
        return Bukkit.getWorld(nombreMundo());
    }

    public boolean esReino(World w) {
        return w != null && w.getName().equals(nombreMundo());
    }

    // ---------------------------------------------------------------- mundo

    /** Multiverse (y Bukkit) piden el generador por aca: /mv create reino_perdido normal -g ReinoPerdidoPlugin */
    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return new ReinoGenerator();
    }

    private World crearMundo() {
        WorldCreator creador = new WorldCreator(nombreMundo());
        creador.environment(World.Environment.NORMAL);
        creador.generator(new ReinoGenerator());
        long semilla = getConfig().getLong("semilla", 0);
        if (semilla != 0) creador.seed(semilla);
        return creador.createWorld();
    }

    private void autoCargar() {
        if (!getConfig().getBoolean("autoCargar", true)) return;
        if (mundo() != null) return;
        if (!new File(Bukkit.getWorldContainer(), nombreMundo()).exists()) return;
        getLogger().info("Cargando el mundo '" + nombreMundo() + "'...");
        crearMundo();
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        if (esReino(e.getWorld())) configurarMundo(e.getWorld());
    }

    private void configurarMundo(World w) {
        w.setTime(18000);
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        w.setGameRule(GameRule.DO_INSOMNIA, false);
        w.setStorm(false);
        w.setThundering(false);
        w.setDifficulty(Difficulty.HARD);
        w.setSpawnLocation(0, Terreno.MESETA_Y + 1, -100);

        int radio = getConfig().getInt("bordeMundo", 1200);
        w.getWorldBorder().setCenter(0, 0);
        w.getWorldBorder().setSize(radio * 2.0);
    }

    // ---------------------------------------------------------------- comandos

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("reino")) return false;

        if (args.length < 1) {
            sender.sendMessage("Uso: /reino <crear|tp|info|jefe respawn>");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "crear" -> {
                if (mundo() != null) {
                    sender.sendMessage("El mundo '" + nombreMundo() + "' ya existe y está cargado.");
                    return true;
                }
                sender.sendMessage("Creando el Reino Perdido (puede tardar unos segundos)...");
                World w = crearMundo();
                if (w == null) {
                    sender.sendMessage("No se pudo crear el mundo, mirá la consola.");
                    return true;
                }
                sender.sendMessage("Listo. Para que Multiverse lo recuerde: /mv import " + nombreMundo()
                        + " normal -g ReinoPerdidoPlugin");
                sender.sendMessage("Después andá con /reino tp y usá /libroflorencia setportal <nombre> destino.");
                return true;
            }
            case "tp" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Solo un jugador puede usar esto.");
                    return true;
                }
                World w = mundo();
                if (w == null) {
                    sender.sendMessage("El mundo todavía no existe (usá /reino crear).");
                    return true;
                }
                player.teleport(new Location(w, 0.5, Terreno.MESETA_Y + 1, -100.5));
                player.sendMessage("Bienvenido al Reino Perdido.");
                return true;
            }
            case "info" -> {
                World w = mundo();
                sender.sendMessage("Mundo '" + nombreMundo() + "': " + (w != null ? "cargado" : "NO cargado"));
                sender.sendMessage("Jefe: " + (boss.estaVivo() ? "vivo"
                        : (boss.msParaReaparecer() > 0
                        ? "reaparece en " + (boss.msParaReaparecer() / 60000 + 1) + " min"
                        : "listo para aparecer cuando alguien entre a la sala del trono")));
                return true;
            }
            case "jefe" -> {
                if (args.length >= 2 && args[1].equalsIgnoreCase("respawn")) {
                    World w = mundo();
                    if (w == null) {
                        sender.sendMessage("El mundo todavía no existe.");
                        return true;
                    }
                    boss.forzar(w);
                    sender.sendMessage("Jefe invocado en la sala del trono.");
                } else {
                    sender.sendMessage("Uso: /reino jefe respawn");
                }
                return true;
            }
            default -> {
                sender.sendMessage("Uso: /reino <crear|tp|info|jefe respawn>");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> resultado = new ArrayList<>();
        if (!command.getName().equalsIgnoreCase("reino")) return resultado;

        if (args.length == 1) {
            for (String o : List.of("crear", "tp", "info", "jefe")) {
                if (o.startsWith(args[0].toLowerCase(Locale.ROOT))) resultado.add(o);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("jefe")) {
            if ("respawn".startsWith(args[1].toLowerCase(Locale.ROOT))) resultado.add("respawn");
        }
        return resultado;
    }
}
