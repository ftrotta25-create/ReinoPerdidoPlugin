package com.franchee.reino.boss;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;
import org.bukkit.entity.LivingEntity;

import java.util.UUID;

/**
 * Disfraz de jugadora para Florencia. Esta aislado en su propia clase a proposito: solo se carga
 * si LibsDisguises esta instalado, asi el plugin arranca igual aunque no lo este.
 */
final class DisfrazFlorencia {

    private DisfrazFlorencia() {
    }

    static boolean estaDisfrazado(LivingEntity entidad) {
        return DisguiseAPI.isDisguised(entidad);
    }

    static void aplicar(LivingEntity entidad, String nombre, String value, String signature) {
        String perfilJson = "{\"id\":\"" + UUID.randomUUID().toString().replace("-", "")
                + "\",\"name\":\"" + nombre + "\",\"properties\":[{\"name\":\"textures\",\"value\":\""
                + value + "\",\"signature\":\"" + signature + "\"}]}";

        PlayerDisguise disfraz = new PlayerDisguise(nombre);
        disfraz.setSkin(perfilJson);
        disfraz.setHearSelfDisguise(false);
        DisguiseAPI.disguiseToAll(entidad, disfraz);
    }
}
