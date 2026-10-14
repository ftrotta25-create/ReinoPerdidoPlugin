package com.franchee.reino.estructuras;

import com.franchee.reino.gen.Hash;
import com.franchee.reino.gen.Terreno;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * La Ciudadela de Florencia, centrada en (0, 0) sobre la meseta plana (altura MESETA_Y).
 * Se dibuja una sola vez en un Buffer y despues se copia al mundo chunk por chunk.
 *
 * Alturas relativas (ry): 0 = suelo del patio, + hacia arriba, - hacia abajo.
 *  - Muralla: 68..72 de radio, 14 de alto, con almenas y varias brechas.
 *  - 4 torres de esquina (15x15, 34 de alto) con escalera de mano y pisos.
 *  - Torreon central (45x45): salon (ry 1..11), galerias (13..23), sala del trono (25..43).
 *  - Catacumbas: laberinto de 13x13 celdas bajo el patio (ry -12..-7), con camara del tesoro.
 */
public final class Ciudadela {

    public static final int RADIO = 78;
    public static final int PROF = 14;
    public static final int ALTURA = 64;
    public static final int Y0 = Terreno.MESETA_Y;

    // Posicion de los pies del jefe en la sala del trono (x, ry, z)
    public static final int[] JEFE = {0, 27, 17};

    private static Buffer buffer;

    private Ciudadela() {
    }

    public static synchronized void inicializar() {
        if (buffer == null) buffer = construir();
    }

    public static Buffer buffer() {
        return buffer;
    }

    /** Nivel de loot del cofre en esa posicion ABSOLUTA, o 0 si no es un cofre de la ciudadela. */
    public static int tierCofre(int x, int y, int z) {
        return buffer == null ? 0 : buffer.tierCofre(x, y - Y0, z);
    }

    private static Buffer construir() {
        Buffer b = new Buffer(RADIO, PROF, ALTURA);
        Random r = new Random(0xF10C1A5L);

        // Despeja todo lo que haya arriba del suelo (arboles, plantas)
        b.fill(-RADIO, 1, -RADIO, RADIO, ALTURA, RADIO, Paleta.AIRE);

        patio(b, r);
        muralla(b, r);
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                torre(b, r, sx * 69, sz * 69, sx, sz);
            }
        }
        torreon(b, r);
        catacumbas(b, r);
        return b;
    }

    // ------------------------------------------------------------------ PATIO

    private static void patio(Buffer b, Random r) {
        // Avenida desde la puerta hasta el torreon
        for (int z = -71; z <= -22; z++) {
            for (int x = -3; x <= 3; x++) {
                b.set(x, 0, z, ((x + z) & 1) == 0 ? Paleta.NEGRO_LADRILLO : Paleta.PIEDRA_LADRILLO);
            }
        }
        // Anillo alrededor del torreon
        for (int x = -30; x <= 30; x++) {
            for (int z = -30; z <= 30; z++) {
                int d = Math.max(Math.abs(x), Math.abs(z));
                if (d >= 26 && d <= 28) b.set(x, 0, z, Paleta.TEJAS);
            }
        }
        // Pilares con cristales llorones a los lados de la avenida
        for (int z = -66; z <= -30; z += 9) {
            for (int sx : new int[]{-1, 1}) {
                int x = sx * 6;
                for (int ry = 1; ry <= 6; ry++) b.set(x, ry, z, Paleta.LADRILLO_PROF);
                b.set(x, 7, z, Paleta.LLORON);
            }
        }
    }

    // ---------------------------------------------------------------- MURALLA

    private static void muralla(Buffer b, Random r) {
        // {lado (0=norte,1=este,2=oeste,3=sur), centro, ancho}
        List<int[]> brechas = new ArrayList<>();
        brechas.add(new int[]{0, -35, 9});
        brechas.add(new int[]{0, 28, 8});
        brechas.add(new int[]{1, 10, 10});
        brechas.add(new int[]{1, -38, 8});
        brechas.add(new int[]{2, -30, 9});
        brechas.add(new int[]{3, 38, 8});
        brechas.add(new int[]{3, -42, 7});

        for (int x = -RADIO; x <= RADIO; x++) {
            for (int z = -RADIO; z <= RADIO; z++) {
                int d = Math.max(Math.abs(x), Math.abs(z));
                if (d < 68 || d > 72) continue;

                int lado = Math.abs(z) >= Math.abs(x) ? (z > 0 ? 0 : 3) : (x > 0 ? 1 : 2);
                int along = (lado == 0 || lado == 3) ? x : z;

                int corte = 99;
                for (int[] br : brechas) {
                    if (br[0] == lado && Math.abs(along - br[1]) <= br[2] / 2) {
                        corte = 2 + (int) (Hash.unit(99L, x, 0, z, 3) * 5);
                        break;
                    }
                }

                boolean puerta = z <= -68 && z >= -72 && Math.abs(x) <= 5;
                boolean posterna = z >= 68 && z <= 72 && Math.abs(x) <= 2;

                for (int ry = 0; ry <= 14; ry++) {
                    if (ry >= corte) break;
                    if (puerta && ry >= 1 && ry <= 11) continue;
                    if (posterna && ry >= 1 && ry <= 5) continue;
                    if (ry >= 9 && r.nextDouble() < (ry - 8) * 0.045) continue;
                    b.set(x, ry, z, Paleta.muro(r));
                }

                if (corte == 99) {
                    if (d == 72) {
                        b.set(x, 15, z, Paleta.muro(r));
                        if (((x + z) & 1) == 0) b.set(x, 16, z, Paleta.muro(r));
                    } else if (d == 68 && ((x + z) & 1) == 0) {
                        b.set(x, 15, z, Paleta.muro(r));
                    }
                }
            }
        }
        // Faroles colgando en la puerta
        for (int sx : new int[]{-4, 4}) {
            b.set(sx, 11, -70, Paleta.CADENA);
            b.set(sx, 10, -70, Paleta.FAROL_COLGANTE);
        }
    }

    // ----------------------------------------------------------------- TORRES

    private static void torre(Buffer b, Random r, int cx, int cz, int sx, int sz) {
        for (int x = cx - 7; x <= cx + 7; x++) {
            for (int z = cz - 7; z <= cz + 7; z++) {
                boolean borde = Math.abs(x - cx) >= 6 || Math.abs(z - cz) >= 6;
                for (int ry = 0; ry <= 34; ry++) {
                    if (borde) {
                        if (ry >= 28 && r.nextDouble() < (ry - 27) * 0.10) continue;
                        if (ry == 34 && ((x + z) & 1) != 0) continue;
                        b.set(x, ry, z, Paleta.muro(r));
                    } else if (ry == 0) {
                        b.set(x, 0, z, Paleta.piso(r));
                    } else {
                        b.set(x, ry, z, Paleta.AIRE);
                    }
                }
            }
        }

        // Pisos (14 = altura de la muralla, 23 y 32 = azotea). Hueco en la escalera de mano.
        for (int ry : new int[]{14, 23, 32}) {
            for (int x = cx - 5; x <= cx + 5; x++) {
                for (int z = cz - 5; z <= cz + 5; z++) {
                    if (x == cx + 5 && z == cz + 3) continue;
                    b.set(x, ry, z, Paleta.piso(r));
                }
            }
        }
        // Escalera de mano (pegada a la pared este de la torre)
        for (int ry = 1; ry <= 33; ry++) {
            b.set(cx + 5, ry, cz + 3, Paleta.ESCALERA_OESTE);
        }

        // Puerta al patio (cara que mira al centro, sobre el eje x)
        for (int ry = 1; ry <= 3; ry++) {
            for (int z = cz - 1; z <= cz + 1; z++) {
                b.set(cx - sx * 6, ry, z, Paleta.AIRE);
                b.set(cx - sx * 7, ry, z, Paleta.AIRE);
            }
        }
        // Puertas hacia el camino de ronda de la muralla (ry 15..17)
        int zMin = Math.min(sz * 69, sz * 71);
        int zMax = Math.max(sz * 69, sz * 71);
        int xMin = Math.min(sx * 69, sx * 71);
        int xMax = Math.max(sx * 69, sx * 71);
        for (int ry = 15; ry <= 17; ry++) {
            for (int z = zMin; z <= zMax; z++) {
                b.set(cx - sx * 6, ry, z, Paleta.AIRE);
                b.set(cx - sx * 7, ry, z, Paleta.AIRE);
            }
            for (int x = xMin; x <= xMax; x++) {
                b.set(x, ry, cz - sz * 6, Paleta.AIRE);
                b.set(x, ry, cz - sz * 7, Paleta.AIRE);
            }
        }

        // Cofres y faroles
        b.cofre(cx - 3, 24, cz - 3, 2, "south");
        b.cofre(cx - 3, 33, cz - 3, 2, "south");
        b.set(cx - 3, 15, cz + 3, Paleta.FAROL);
        b.set(cx + 3, 24, cz + 3, Paleta.FAROL);
    }

    // ---------------------------------------------------------------- TORREON

    private static void torreon(Buffer b, Random r) {
        // Cascaron, pisos y techo
        for (int x = -22; x <= 22; x++) {
            for (int z = -22; z <= 22; z++) {
                boolean borde = Math.abs(x) >= 20 || Math.abs(z) >= 20;
                for (int ry = 0; ry <= 44; ry++) {
                    if (borde) {
                        b.set(x, ry, z, Paleta.muro(r));
                    } else if (ry == 44) {
                        b.set(x, ry, z, Paleta.muro(r));
                    } else if (ry == 0 || ry == 12 || ry == 24) {
                        b.set(x, ry, z, Paleta.piso(r));
                    } else {
                        b.set(x, ry, z, Paleta.AIRE);
                    }
                }
                if (Math.max(Math.abs(x), Math.abs(z)) == 22) {
                    b.set(x, 45, z, Paleta.muro(r));
                    if (((x + z) & 1) == 0) b.set(x, 46, z, Paleta.muro(r));
                }
            }
        }

        // Ventanas de vidrio violeta (3 pisos)
        int[][] niveles = {{5, 7}, {17, 19}, {29, 38}};
        for (int[] nv : niveles) {
            for (int p = -16; p <= 16; p += 8) {
                for (int ry = nv[0]; ry <= nv[1]; ry++) {
                    for (int k = 20; k <= 22; k++) {
                        b.set(p, ry, -k, Paleta.VIDRIO);
                        b.set(p, ry, k, Paleta.VIDRIO);
                        b.set(-k, ry, p, Paleta.VIDRIO);
                        b.set(k, ry, p, Paleta.VIDRIO);
                    }
                }
            }
        }
        // Puerta principal (cara sur)
        b.fill(-3, 1, -22, 3, 8, -20, Paleta.AIRE);

        // ----- Gran salon (ry 1..11) -----
        for (int px : new int[]{-12, -4, 4, 12}) {
            for (int pz : new int[]{-12, -4, 4, 12}) {
                for (int ry = 1; ry <= 10; ry++) b.set(px, ry, pz, Paleta.LADRILLO_PROF);
                b.set(px, 11, pz, Paleta.LLORON);
            }
        }
        b.fill(-1, 1, -19, 1, 1, 19, Paleta.ALFOMBRA);
        for (int[] c : new int[][]{{0, -8}, {0, 8}, {-8, 0}, {8, 0}}) {
            for (int ry = 9; ry <= 11; ry++) b.set(c[0], ry, c[1], Paleta.CADENA);
            b.set(c[0], 8, c[1], Paleta.FAROL_COLGANTE);
        }
        b.cofre(-17, 1, 0, 2, "east");
        b.cofre(17, 1, 0, 2, "west");

        // Escalera A (oeste): del salon a las galerias, sube hacia +z
        for (int i = 0; i <= 11; i++) {
            int z = -18 + i;
            for (int x = -19; x <= -17; x++) {
                if (i <= 10) b.set(x, 12, z, Paleta.AIRE);
                b.set(x, 1 + i, z, Paleta.PIEDRA_ESC_SUR);
            }
        }

        // ----- Galerias (ry 13..23) -----
        for (int x = -14; x <= 14; x++) {
            for (int ry = 13; ry <= 16; ry++) b.set(x, ry, 19, Paleta.ESTANTERIA);
        }
        b.cofre(-17, 13, 17, 2, "east");
        b.cofre(17, 13, -17, 2, "west");
        b.cofre(-8, 13, -18, 2, "south");
        b.cofre(8, 13, 18, 2, "north");
        for (int[] c : new int[][]{{-10, -10}, {10, 10}, {-10, 10}, {10, -10}}) {
            b.set(c[0], 13, c[1], Paleta.FAROL);
        }

        // Escalera B (este): de las galerias a la sala del trono, sube hacia -z
        for (int i = 0; i <= 11; i++) {
            int z = 18 - i;
            for (int x = 17; x <= 19; x++) {
                if (i <= 10) b.set(x, 24, z, Paleta.AIRE);
                b.set(x, 13 + i, z, Paleta.PIEDRA_ESC_NORTE);
            }
        }

        // ----- Sala del trono (ry 25..43) -----
        for (int sx : new int[]{-1, 1}) {
            for (int pz : new int[]{-12, -4, 4, 12}) {
                for (int ry = 25; ry <= 43; ry++) b.set(sx * 12, ry, pz, Paleta.PURPUR_PILAR);
                b.set(sx * 12, 25, pz, Paleta.LLORON);
            }
        }
        b.fill(-1, 25, -18, 1, 25, 11, Paleta.ALFOMBRA);
        // Estrado en dos niveles
        b.fill(-10, 25, 12, 10, 25, 20, Paleta.PURPUR);
        b.fill(-6, 25, 16, 6, 26, 20, Paleta.PURPUR);
        // Trono
        b.set(0, 27, 18, Paleta.PURPUR_ESC_NORTE);
        b.fill(0, 27, 19, 0, 31, 19, Paleta.PURPUR_PILAR);
        for (int sx : new int[]{-3, 3}) {
            b.set(sx, 27, 19, Paleta.AMATISTA);
            b.set(sx, 28, 19, Paleta.CLUSTER);
        }
        // Candelabros colgantes
        for (int[] c : new int[][]{{-8, -8}, {8, -8}, {-8, 8}, {8, 8}, {0, 0}}) {
            for (int ry = 40; ry <= 43; ry++) b.set(c[0], ry, c[1], Paleta.CADENA);
            b.set(c[0], 39, c[1], Paleta.FAROL_COLGANTE);
        }
        // Tesoro del trono (a los costados del estrado)
        b.cofre(-10, 26, 18, 3, "south");
        b.cofre(10, 26, 18, 3, "south");

        // ----- Torretas en las esquinas -----
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                int cx = sx * 20;
                int cz = sz * 20;
                for (int x = cx - 3; x <= cx + 3; x++) {
                    for (int z = cz - 3; z <= cz + 3; z++) {
                        for (int ry = 45; ry <= 53; ry++) b.set(x, ry, z, Paleta.muro(r));
                    }
                }
                for (int ry = 54; ry <= 58; ry++) b.set(cx, ry, cz, Paleta.PURPUR_PILAR);
                b.set(cx, 59, cz, Paleta.LLORON);
                b.set(cx, 60, cz, Paleta.CLUSTER);
            }
        }
    }

    // ------------------------------------------------------------- CATACUMBAS

    private static void catacumbas(Buffer b, Random r) {
        final int N = 13;
        final int P = 4;
        final int X0 = -26;
        final int Z0 = -26;
        final int ENTRADA = 8; // columna de celdas donde entra la escalera (x = 7..9)

        // Masa solida del laberinto
        for (int x = -27; x <= 27; x++) {
            for (int z = -27; z <= 27; z++) {
                for (int ry = -12; ry <= -7; ry++) {
                    b.set(x, ry, z, ry == -12 ? Paleta.piso(r) : Paleta.muro(r));
                }
            }
        }

        // Laberinto por busqueda en profundidad
        boolean[][] visitada = new boolean[N][N];
        boolean[][] pasoEste = new boolean[N][N];
        boolean[][] pasoSur = new boolean[N][N]; // hacia +z
        List<int[]> pila = new ArrayList<>();
        pila.add(new int[]{ENTRADA, 0});
        visitada[ENTRADA][0] = true;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        while (!pila.isEmpty()) {
            int[] c = pila.get(pila.size() - 1);
            List<int[]> opciones = new ArrayList<>();
            for (int[] dd : dirs) {
                int ni = c[0] + dd[0];
                int nj = c[1] + dd[1];
                if (ni >= 0 && ni < N && nj >= 0 && nj < N && !visitada[ni][nj]) {
                    opciones.add(new int[]{ni, nj, dd[0], dd[1]});
                }
            }
            if (opciones.isEmpty()) {
                pila.remove(pila.size() - 1);
                continue;
            }
            int[] o = opciones.get(r.nextInt(opciones.size()));
            if (o[2] == 1) pasoEste[c[0]][c[1]] = true;
            else if (o[2] == -1) pasoEste[o[0]][o[1]] = true;
            else if (o[3] == 1) pasoSur[c[0]][c[1]] = true;
            else pasoSur[o[0]][o[1]] = true;
            visitada[o[0]][o[1]] = true;
            pila.add(new int[]{o[0], o[1]});
        }

        // Cavar celdas y pasillos
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                int xi = X0 + P * i + 1;
                int zi = Z0 + P * j + 1;
                b.fill(xi, -11, zi, xi + 2, -8, zi + 2, Paleta.AIRE);
                if (pasoEste[i][j]) b.fill(xi + 3, -11, zi, xi + 3, -8, zi + 2, Paleta.AIRE);
                if (pasoSur[i][j]) b.fill(xi, -11, zi + 3, xi + 2, -8, zi + 3, Paleta.AIRE);
            }
        }

        // Distancias desde la entrada (para ubicar la camara del tesoro) y grado de cada celda
        int[][] dist = new int[N][N];
        for (int[] fila : dist) java.util.Arrays.fill(fila, -1);
        List<int[]> cola = new ArrayList<>();
        cola.add(new int[]{ENTRADA, 0});
        dist[ENTRADA][0] = 0;
        int[][] grado = new int[N][N];
        int mejorI = ENTRADA;
        int mejorJ = 0;
        for (int head = 0; head < cola.size(); head++) {
            int[] c = cola.get(head);
            int i = c[0];
            int j = c[1];
            List<int[]> vecinos = new ArrayList<>();
            if (pasoEste[i][j]) vecinos.add(new int[]{i + 1, j});
            if (i > 0 && pasoEste[i - 1][j]) vecinos.add(new int[]{i - 1, j});
            if (pasoSur[i][j]) vecinos.add(new int[]{i, j + 1});
            if (j > 0 && pasoSur[i][j - 1]) vecinos.add(new int[]{i, j - 1});
            grado[i][j] = vecinos.size();
            for (int[] v : vecinos) {
                if (dist[v[0]][v[1]] < 0) {
                    dist[v[0]][v[1]] = dist[i][j] + 1;
                    cola.add(v);
                    if (dist[v[0]][v[1]] > dist[mejorI][mejorJ]) {
                        mejorI = v[0];
                        mejorJ = v[1];
                    }
                }
            }
        }

        // Camara del tesoro: 4 cofres de nivel 3 en las esquinas de la celda mas lejana
        int tx = X0 + P * mejorI + 2;
        int tz = Z0 + P * mejorJ + 2;
        b.set(tx, -12, tz, Paleta.LLORON);
        b.set(tx, -11, tz, Paleta.CLUSTER);
        b.cofre(tx - 1, -11, tz - 1, 3, "east");
        b.cofre(tx + 1, -11, tz - 1, 3, "west");
        b.cofre(tx - 1, -11, tz + 1, 3, "east");
        b.cofre(tx + 1, -11, tz + 1, 3, "west");

        // Cofres de nivel 2 en callejones sin salida (hasta 8), faroles y telaranas
        List<int[]> callejones = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                boolean esEntrada = i == ENTRADA && j == 0;
                boolean esTesoro = i == mejorI && j == mejorJ;
                if (grado[i][j] == 1 && !esEntrada && !esTesoro) callejones.add(new int[]{i, j});
            }
        }
        Collections.shuffle(callejones, r);
        for (int k = 0; k < Math.min(8, callejones.size()); k++) {
            int[] c = callejones.get(k);
            b.cofre(X0 + P * c[0] + 2, -11, Z0 + P * c[1] + 2, 2, "south");
        }
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                int cx = X0 + P * i + 2;
                int cz = Z0 + P * j + 2;
                if ((i + j) % 3 == 0) b.set(cx, -8, cz, Paleta.FAROL_COLGANTE);
                if (r.nextDouble() < 0.15) b.set(cx + 1, -9, cz + 1, Paleta.TELARANA);
            }
        }

        // Entrada: escalera de 12 escalones hacia abajo (x = 7..9), tunel y apertura al laberinto
        for (int i = 0; i <= 11; i++) {
            int z = -60 + i;
            for (int x = 7; x <= 9; x++) {
                b.fill(x, -i + 1, z, x, -i + 3, z, Paleta.AIRE);
                b.set(x, -i, z, Paleta.PIEDRA_ESC_NORTE);
            }
        }
        for (int x = 6; x <= 10; x++) {
            for (int z = -48; z <= -27; z++) {
                for (int ry = -12; ry <= -7; ry++) {
                    b.set(x, ry, z, ry == -12 ? Paleta.piso(r) : Paleta.muro(r));
                }
            }
        }
        b.fill(7, -11, -48, 9, -8, -27, Paleta.AIRE);
        b.fill(7, -11, -27, 9, -8, -26, Paleta.AIRE);
        for (int z = -46; z <= -30; z += 8) b.set(8, -8, z, Paleta.FAROL_COLGANTE);

        // Portico sobre la entrada
        for (int[] p : new int[][]{{5, -62}, {11, -62}, {5, -58}, {11, -58}}) {
            for (int ry = 1; ry <= 4; ry++) b.set(p[0], ry, p[1], Paleta.LADRILLO_PROF);
        }
        for (int x = 5; x <= 11; x++) {
            for (int z = -62; z <= -58; z++) b.set(x, 5, z, Paleta.muro(r));
        }
        b.set(8, 4, -60, Paleta.FAROL_COLGANTE);
    }
}
