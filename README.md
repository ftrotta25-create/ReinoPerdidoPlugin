# ReinoPerdidoPlugin

Plugin para Paper **1.20.1**. Crea una dimensión nueva (un mundo de Multiverse): **El Reino Perdido
de Florencia**. Todo con bloques vanilla, de noche eterna, con minerales mucho más ricos que el
overworld, mini estructuras, una **ciudadela gigante**, mobs modificados y un **jefe final**.
Es difícil a propósito, pero la recompensa está en todos lados, así que conviene ir.

## Cómo ponerlo en marcha

1. Poné `ReinoPerdidoPlugin.jar` en `plugins/` y reiniciá el server.
2. Creá el mundo, de cualquiera de estas dos formas:
   - `/reino crear`, y después `/mv import reino_perdido normal -g ReinoPerdidoPlugin` para que Multiverse lo recuerde.
   - O directo con Multiverse: `/mv create reino_perdido normal -g ReinoPerdidoPlugin`.
3. `/reino tp` te lleva al punto de entrada (delante de la puerta de la ciudadela).
4. Para que el **altar del Libro de Florencia** lleve ahí: parado en ese punto, corré
   `/libroflorencia setportal ida destino` (y armá el altar de vuelta como ya sabés).

**Recomendación:** el mundo se genera con código y la primera vez que alguien explora tarda un poco más
que el overworld normal. Pre-generá la zona de la ciudadela y alrededores (con Chunky, por ejemplo)
para que no haya tirones.

## Comandos (`reino.admin`, op por defecto)

| Comando | Qué hace |
|---|---|
| `/reino crear` | Crea el mundo con el generador del plugin |
| `/reino tp` | Te lleva al punto de entrada |
| `/reino info` | Estado del mundo y del jefe |
| `/reino jefe respawn` | Fuerza que el jefe aparezca ya en la sala del trono |

## Qué hay en la dimensión

**Terreno:** colinas, montañas, lagos y cuevas (con lava abajo de todo), bosque oscuro y pantano.
Geodas de amatista bajo tierra, árboles muertos, pilares en ruinas y plantas.
**Noche eterna**, sin clima, dificultad difícil y borde del mundo (radio configurable).

**Minerales ricos:** diamante, esmeralda, oro, redstone, lapis, cobre, hierro y carbón en
muchísima más cantidad que en el overworld, más **ancient debris** suelto bajo tierra.

**Mini estructuras** (alrededor de una cada 40 chunks, lejos de la ciudadela), cada una con cofres de loot:
torre en ruinas, santuario de amatista, campamento abandonado y cripta subterránea.
Cada una tiene guardias modificados que aparecen cuando te acercás.

**La Ciudadela de Florencia** (centro del mundo, sobre una meseta plana):
- Muralla con almenas, puerta sur y varias brechas, con 4 torres de esquina (escalera de mano, pisos y cofres).
- Torreón central de 3 pisos: gran salón, galerías con biblioteca, y la sala del trono.
- Catacumbas: laberinto subterráneo (entrada con escalera en el patio, a la derecha de la avenida) con
  cofres en los callejones sin salida y la cámara del tesoro en la celda más lejana.
- Guardias más fuertes en el patio, el torreón y las catacumbas.

**Jefe final: Florencia, la Reina Perdida** (sala del trono). Aparece cuando alguien entra a la sala.
Tiene tres habilidades que se van sumando al bajar su vida: llamarada de amatista, invocación de
guardias (bajo 66%) y eclipse (bajo 33%, además entra en furia). Reaparece a los 45 minutos de morir.
- Todos los que le hicieron daño reciben: 3 tiradas del tesoro de nivel 3, diamantes, netherite scrap,
  manzanas doradas encantadas y experiencia.
- El que más daño hizo recibe además la **Corona de Florencia** (casco con +4 armadura, +3 dureza,
  +3 corazones de vida y +8% velocidad) y una Nether Star.

## Mobs modificados

Todos los monstruos de la dimensión se modifican con 5 niveles, y cuanto más cerca de la ciudadela, más fuertes:

| Nivel | Dónde | Vida | Daño | Equipo |
|---|---|---|---|---|
| 1 Errante | lejos (>450 bloques) | x1.8 | x1.4 | cuero (a veces) |
| 2 Corrupto | 200 a 450 | x2.5 | x1.8 | cota de malla |
| 3 Centinela | 90 a 200 | x3.5 | x2.2 | hierro |
| 4 Guardián Real | ciudadela | x5 | x2.8 | diamante |
| 5 Campeón de la Reina | catacumbas | x7 | x3.3 | netherite |

Los zombis ralentizan, las arañas envenenan y los esqueletos (nivel 3+) debilitan. Los creepers
nivel 3+ salen cargados pero **no rompen bloques**. No spawnean animales (configurable).
Los mobs modificados sueltan **drops raros** (25% a 80% según el nivel): diamantes, esmeraldas,
libros encantados, manzanas doradas, netherite scrap, tótems...

## Loot de los cofres

Los cofres generados se llenan **la primera vez que alguien los abre** (los que pone un jugador no).
- **Nivel 1** (mini estructuras): diamantes, esmeraldas, lingotes, libros encantados, herramientas de diamante, perlas...
- **Nivel 2** (ciudadela): bloques de diamante y esmeralda, netherite, armadura de diamante encantada, tótems, élitros, shulkers...
- **Nivel 3** (tesoro del trono y cámara del tesoro): lingotes de netherite, armadura y armas de netherite
  encantadas con Mending, Nether Stars, beacons, élitros, y los items de tus otros plugins
  (Libro de Florencia, Cuerno de Cabra, Vestigio del Aparecido, Pava) si están instalados.

## Configuración (`config.yml`)

`mundo`, `semilla`, `bordeMundo`, `autoCargar`, `bloquearAnimales`, `jefe.vida` y `jefe.minutosRespawn`.
Los multiplicadores de los mobs están al principio de `ModificadorMobs.java` y las tablas de loot en `LootService.java`.

## Cosas para tener en cuenta

- Las estructuras las genera el código, así que se ven más "procedurales" que algo construido a mano.
  Si querés reemplazar o retocar la ciudadela, podés hacerlo en el juego (los cofres se reconocen por su posición).
- Los jugadores pueden romper y construir en toda la dimensión, incluida la ciudadela.
- El jefe es un Evoker con atributos y habilidades propias: conserva su IA de hechicero (colmillos y vex).
