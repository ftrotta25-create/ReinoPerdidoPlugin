package com.franchee.reino.loot;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.Map;

/**
 * Pide items de tus otros plugins por REFLEXION, asi este plugin no depende de ellos para
 * arrancar. Si el plugin no esta (o tiene un jar viejo), simplemente devuelve null.
 */
public final class ItemsExternos {

    private static final Map<String, String[]> RUTAS = Map.of(
            "libro_florencia", new String[]{"LibroFlorenciaPlugin", "getItemFactory", "crearLibro"},
            "cuerno_cabra", new String[]{"JefeCabraPlugin", "getItemFactory", "crearCuernoInvocador"},
            "yelmo_testarazo", new String[]{"JefeCabraPlugin", "getItemFactory", "crearYelmoTestarazo"},
            "pava", new String[]{"MateSystemPlugin", "getItemFactory", "crearPava"},
            "sudario_aparecido", new String[]{"EventoPlugin", "getEventoManager", "getItemFactory", "crearSudario"},
            "vestigio_aparecido", new String[]{"EventoPlugin", "getEventoManager", "getItemFactory", "crearVestigioDelAparecido"}
    );

    private ItemsExternos() {
    }

    public static ItemStack crear(String token) {
        String[] ruta = RUTAS.get(token.toLowerCase(Locale.ROOT));
        if (ruta == null) return null;

        Plugin otro = Bukkit.getPluginManager().getPlugin(ruta[0]);
        if (otro == null || !otro.isEnabled()) return null;

        try {
            Object actual = otro;
            for (int i = 1; i < ruta.length; i++) {
                actual = actual.getClass().getMethod(ruta[i]).invoke(actual);
                if (actual == null) return null;
            }
            return actual instanceof ItemStack item ? item : null;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null; // jar viejo o metodo que no existe: se ignora este item
        }
    }
}
