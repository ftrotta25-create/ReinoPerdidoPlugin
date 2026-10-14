package com.franchee.reino.zonas;

import com.franchee.reino.ReinoPerdidoPlugin;
import com.franchee.reino.estructuras.Ciudadela;
import com.franchee.reino.estructuras.MiniEstructuras;
import com.franchee.reino.gen.Terreno;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Zonas "vigiladas": mientras haya un jugador adentro, la zona mantiene una cantidad de guardias
 * modificados vivos cerca de el. Hay zonas fijas en la ciudadela (patio, torreon, sala del
 * trono, catacumbas) y una zona chica alrededor de cada mini estructura cercana.
 * La sala del trono ademas despierta al jefe.
 */
public class ZonaManager {

    private record Zona(String id, int x1, int y1, int z1, int x2, int y2, int z2,
                        int tier, int max, EntityType[] tipos) {

        boolean contiene(Location l, int margenY) {
            int x = l.getBlockX();
            int y = l.getBlockY();
            int z = l.getBlockZ();
            return x >= x1 && x <= x2 && z >= z1 && z <= z2 && y >= y1 - margenY && y <= y2 + margenY;
        }
    }

    private final ReinoPerdidoPlugin plugin;
    private final List<Zona> fijas = new ArrayList<>();

    public ZonaManager(ReinoPerdidoPlugin plugin) {
        this.plugin = plugin;
        int y0 = Ciudadela.Y0;

        fijas.add(new Zona("patio", -68, y0 + 1, -68, 68, y0 + 15, 68, 4, 10,
                new EntityType[]{EntityType.ZOMBIE, EntityType.SKELETON, EntityType.VINDICATOR,
                        EntityType.SPIDER, EntityType.WITCH}));
        fijas.add(new Zona("torreon", -19, y0 + 1, -19, 19, y0 + 23, 19, 4, 10,
                new EntityType[]{EntityType.ZOMBIE, EntityType.SKELETON, EntityType.VINDICATOR, EntityType.WITCH}));
        fijas.add(new Zona("trono", -19, y0 + 25, -19, 19, y0 + 43, 19, 4, 5,
                new EntityType[]{EntityType.SKELETON, EntityType.VINDICATOR, EntityType.ZOMBIE}));
        fijas.add(new Zona("catacumbas", -26, y0 - 11, -48, 26, y0 - 8, 26, 5, 12,
                new EntityType[]{EntityType.WITHER_SKELETON, EntityType.SKELETON, EntityType.ZOMBIE,
                        EntityType.VINDICATOR, EntityType.CAVE_SPIDER}));
    }

    /** Se llama cada 2 segundos. */
    public void tick() {
        World w = plugin.mundo();
        if (w == null) return;

        long semilla = w.getSeed();
        Terreno terreno = Terreno.de(semilla);

        for (Player p : w.getPlayers()) {
            if (p.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
            Location pl = p.getLocation();

            List<Zona> zonas = new ArrayList<>(fijas);
            int cx = Math.floorDiv(pl.getBlockX(), 16);
            int cz = Math.floorDiv(pl.getBlockZ(), 16);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    MiniEstructuras.Sitio s = MiniEstructuras.sitio(semilla, terreno, cx + dx, cz + dz);
                    if (s == null) continue;
                    zonas.add(new Zona("mini:" + s.cx() + ":" + s.cz(),
                            s.centroX() - 9, s.baseY() - 12, s.centroZ() - 9,
                            s.centroX() + 9, s.baseY() + 16, s.centroZ() + 9,
                            2, 4,
                            new EntityType[]{EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER}));
                }
            }

            for (Zona z : zonas) {
                if (!z.contiene(pl, 2)) continue;
                if (z.id().equals("trono")) {
                    plugin.getBoss().intentarInvocar(w);
                }
                mantenerGuardias(w, p, z);
            }
        }
    }

    private void mantenerGuardias(World w, Player p, Zona z) {
        int actuales = 0;
        for (Entity e : p.getNearbyEntities(48, 30, 48)) {
            if (!(e instanceof LivingEntity)) continue;
            String id = e.getPersistentDataContainer().get(plugin.getMobs().keyZona(), PersistentDataType.STRING);
            if (z.id().equals(id)) actuales++;
        }
        if (actuales >= z.max()) return;

        int porCiclo = Math.min(2, z.max() - actuales);
        for (int i = 0; i < porCiclo; i++) {
            Location loc = buscarPosicion(w, p, z);
            if (loc == null) continue;

            EntityType tipo = z.tipos()[ThreadLocalRandom.current().nextInt(z.tipos().length)];
            Entity e = w.spawnEntity(loc, tipo);
            if (e instanceof LivingEntity vivo) {
                plugin.getMobs().aplicar(vivo, z.tier(), z.id());
            }
        }
    }

    private Location buscarPosicion(World w, Player p, Zona z) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Location pl = p.getLocation();

        for (int intento = 0; intento < 12; intento++) {
            int x = pl.getBlockX() + r.nextInt(-18, 19);
            int zz = pl.getBlockZ() + r.nextInt(-18, 19);
            if (x < z.x1() || x > z.x2() || zz < z.z1() || zz > z.z2()) continue;
            if (z.id().equals("patio") && Math.abs(x) <= 23 && Math.abs(zz) <= 23) continue; // no adentro del torreon

            int yMax = Math.min(z.y2(), pl.getBlockY() + 6);
            int yMin = Math.max(z.y1(), pl.getBlockY() - 8);
            for (int y = yMax; y >= yMin; y--) {
                if (!w.getBlockAt(x, y - 1, zz).getType().isSolid()) continue;
                if (!w.getBlockAt(x, y, zz).getType().isAir()) continue;
                if (!w.getBlockAt(x, y + 1, zz).getType().isAir()) continue;

                Location loc = new Location(w, x + 0.5, y, zz + 0.5);
                if (loc.distanceSquared(pl) < 49) break; // muy cerca del jugador
                return loc;
            }
        }
        return null;
    }
}
