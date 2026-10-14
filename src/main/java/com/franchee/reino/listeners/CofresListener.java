package com.franchee.reino.listeners;

import com.franchee.reino.ReinoPerdidoPlugin;
import com.franchee.reino.estructuras.Ciudadela;
import com.franchee.reino.estructuras.MiniEstructuras;
import com.franchee.reino.gen.Terreno;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.TileState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Random;

/**
 * Llena los cofres generados la PRIMERA vez que alguien los abre. Solo los cofres que el
 * generador puso en su lugar exacto reciben loot (un cofre puesto por un jugador no).
 */
public class CofresListener implements Listener {

    private final ReinoPerdidoPlugin plugin;
    private final NamespacedKey keyLlenado;
    private final Random random = new Random();

    public CofresListener(ReinoPerdidoPlugin plugin) {
        this.plugin = plugin;
        this.keyLlenado = new NamespacedKey(plugin, "cofre_llenado");
    }

    @EventHandler
    public void onAbrir(InventoryOpenEvent e) {
        if (!(e.getInventory().getHolder() instanceof Chest chest)) return;

        Location l = chest.getLocation();
        if (l.getWorld() == null || !plugin.esReino(l.getWorld())) return;

        int tier = tierDelCofre(l);
        if (tier <= 0) return;

        Block bloque = l.getBlock();
        if (!(bloque.getState() instanceof TileState estado)) return;

        PersistentDataContainer pdc = estado.getPersistentDataContainer();
        if (pdc.has(keyLlenado, PersistentDataType.BYTE)) return;

        pdc.set(keyLlenado, PersistentDataType.BYTE, (byte) 1);
        estado.update();

        plugin.getLoot().llenar(e.getInventory(), tier, random);
    }

    private int tierDelCofre(Location l) {
        int x = l.getBlockX();
        int y = l.getBlockY();
        int z = l.getBlockZ();

        int tier = Ciudadela.tierCofre(x, y, z);
        if (tier > 0) return tier;

        long semilla = l.getWorld().getSeed();
        return MiniEstructuras.tierCofre(semilla, Terreno.de(semilla), x, y, z);
    }
}
