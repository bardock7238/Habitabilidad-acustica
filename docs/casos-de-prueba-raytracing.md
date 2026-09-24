# Casos de prueba — Ray tracing (Tarea 4.3)

Diseño de escenarios de prueba para la propagación del sonido por rayos
(RayTracing + CalculoAcustico, Tareas 4.1 y 4.2), con su **resultado esperado
calculado a mano** y la verificación automática (VerificacionRayTracing.java).

| Escenario | Qué comprueba |
|---|---|
| 1. Edificio de dos habitaciones | Reparto de rayos entre superficies y nivel por ray tracing en la sala de origen; propagación directa a la habitación contigua (grafo/Dijkstra). |
| 2. Edificio con pasillo | Propagación **a través de un nodo intermedio**: la atenuación se acumula restando las dos puertas. |
| 3. Edificio con fachada a una vía | Ruido **exterior** que entra por la fachada (Dijkstra desde el nodo EXTERIOR) y comparación con la normativa. |

---

## Modelo de cálculo (resumen)

1. **Ray tracing en la habitación de origen** (`RayTracing.emitirRayos`):
   - Se emiten `N` rayos uniformes en 360°. El paso angular es `360 / N`.
   - Cada superficie "ocupa" una porción del círculo proporcional a su área:
     `porción_k = (área_k / áreaTotal) · 360°`.
   - Cada rayo pierde la atenuación de la superficie que atraviesa:
     `atenuación = área · coeficienteMaterial`, luego `nivelRayo = nivelInicial − atenuación`.
   - Los aportes se suman energéticamente (incoherente):
     `L = 10 · log10( Σ 10^(Lᵢ/10) )`.

2. **Propagación a otras habitaciones** (`Grafo.desdeEdificio` + `Dijkstra` + `CalculoAcustico.calcularNivelEnDestino`):
   - Cada habitación es un nodo; cada superficie interior es una arista con peso
     `atenuación`; las fachadas conectan con el nodo `EXTERIOR`.
   - Dijkstra encuentra la **menor atenuación acumulada** hasta cada habitación:
     `nivelDestino = nivelEmitido − atenuaciónAcumulada`.

### Truco para el cálculo a mano

Si en la sala de origen la **superficie total es un número entero `A` m²** y se
emiten `N = A` rayos, cada superficie recibe un número de rayos **proporcional
a su área** (el reparto es proporcional al área). Con `área · coef = número
redondo`, el resultado se calcula con lápiz y papel:

```
nivel = 10 · log10( Σ (rayos_k · 10^((emitido − atenuación_k)/10)) )
```

**Precaución (importante):** los cortes entre superficies deben caer en
múltiplos exactos del paso angular `360/N`, o el redondeo de coma flotante
puede mandar el rayo del borde a la superficie vecina. En estos casos de
prueba se eligió `N = 40` (paso de 9°) y áreas con cortes exactos en 36°,
90°, 144°, 180°, 252° y 270°, por lo que el reparto es exacto y estable.

---

## Escenario 1 — Edificio de dos habitaciones

Un dormitorio (D1) y una sala (S1) separados por una pared interior. La fuente
de 80 dB está en la sala.

```
        D1 (Dormitorio)
     +------------------+
     |                  |
     |  M1 (medianera)  |
     +--------+---------+
              |  S1 (Sala)         ← fuente 80 dB
              |  [F1] fachada al exterior
              +------------------+
```

**Superficies de la sala** (cortes exactos a 90°/180°/270° con N=40, paso de 9°):

| Superficie | Área (m²) | Coef. material | Atenuación = área·coef | Rayos (N=40) |
|---|---|---|---|---|
| M1 — pared interior hacia D1 | 10 | 3.0 | 30 dB | 10 |
| F1 — fachada al exterior | 10 | 1.5 | 15 dB | 10 |
| P1 — piso | 10 | 2.0 | 20 dB | 10 |
| T1 — techo | 10 | 2.5 | 25 dB | 10 |
| **Total** | **40** | | | **40** |

### Cálculo a mano

Cada superficie recibe 10 rayos, todos con su propia atenuación:

| Superficie | Nivel de cada rayo = 80 − atenuación | Aporte (10 rayos) |
|---|---|---|
| M1 | 80 − 30 = **50 dB** | `10 · 10^5` = `1 000 000` |
| F1 | 80 − 15 = **65 dB** | `10 · 10^6.5` = `31 622 777` |
| P1 | 80 − 20 = **60 dB** | `10 · 10^6` = `10 000 000` |
| T1 | 80 − 25 = **55 dB** | `10 · 10^5.5` = `3 162 278` |

Suma energética:

```
L_Sala = 10 · log10( 1 000 000 + 31 622 777 + 10 000 000 + 3 162 278 )
       = 10 · log10( 45 785 054 )
       = 76.61 dB
```

Nivel del dormitorio (la fuente está en S1; el único camino directo es la
medianera de 30 dB):

```
L_D1 = 80 − 30 = 50.00 dB
```

### Resultado esperado vs. verificado

| Magnitud | Esperado (manual) | Verificado (clase) |
|---|---|---|
| Rayos M1 / F1 / P1 / T1 | 10 / 10 / 10 / 10 | ✔ iguales |
| Nivel Sala | 76.61 dB | ✔ |
| Nivel Dormitorio | 50.00 dB | ✔ |

---

## Escenario 2 — Edificio con pasillo

Una oficina (H1) se conecta a otra (H2) a través de un pasillo (P1), con dos
puertas intermedias. La fuente de 80 dB está en H1.

```
        H1 (Oficina 1)      P1 (Pasillo)      H2 (Oficina 2)
     +-----------------+  +-------------+  +-----------------+
     |            PU1  |  |  PU1   PU2  |  |  PU2            |
     |   fuente 80 dB  +--+             +--+                 |
     |  [F1] fachada   |  |             |  |                 |
     +-----------------+  +-------------+  +-----------------+
```

**Superficies de la Oficina 1**:

| Superficie | Área (m²) | Coef. | Atenuación = área·coef | Rayos (N=40) |
|---|---|---|---|---|
| PU1 — puerta al pasillo | 4 | 3.0 | 12 dB | 4 |
| F1 — fachada | 12 | 3.0 | 36 dB | 12 |
| P1 — piso | 12 | 3.0 | 36 dB | 12 |
| T1 — techo | 12 | 3.0 | 36 dB | 12 |
| **Total** | **40** | | | **40** |

### Cálculo a mano

- 4 rayos atraviesan la puerta: `80 − 12 = 68 dB`.
- 36 rayos (fachada + piso + techo): `80 − 36 = 44 dB`.

```
L_H1 = 10 · log10( 4 · 10^(68/10) + 36 · 10^(44/10) )
     = 10 · log10( 4 · 10^6.8 + 36 · 10^4.4 )
     = 10 · log10( 25 238 294 + 904 279 )
     = 10 · log10( 26 142 573 )
     = 74.17 dB
```

Nivel de la Oficina 2: el sonido cruza **las dos puertas** (12 + 12 dB):

```
L_H2 = 80 − 12 − 12 = 56.00 dB
```

### Resultado esperado vs. verificado

| Magnitud | Esperado (manual) | Verificado (clase) |
|---|---|---|
| Rayos PU1 / F1 / P1 / T1 | 4 / 12 / 12 / 12 | ✔ iguales |
| Camino H1 → H2 | Pasa por el pasillo P1 | ✔ |
| Nivel Oficina 1 | 74.17 dB | ✔ |
| Nivel Oficina 2 | 56.00 dB | ✔ |

---

## Escenario 3 — Edificio con fachada a una vía

Una sala con fachada directa a una avenida (tráfico, 85 dB de noche) y un
dormitorio interior detrás de ella.

```
        VIA (85 dB)
           ||
     +-----||--------------------------------+
     |  S1 (Sala)   [FV1] fachada a la vía        |
     |              (20 m², 30 dB)                |
     |   PD1 (pared interior, 30 dB)              |
     +--------+-----------------------------------+
              |  D1 (Dormitorio)
              +-----------------------------------+
```

**Datos:**

| Superficie | Área (m²) | Coef. | Atenuación = área·coef |
|---|---|---|---|
| FV1 — fachada a la vía (Sala) | 20 | 1.5 | 30 dB |
| PD1 — pared interior (Sala ↔ Dormitorio) | 10 | 3.0 | 30 dB |

### Cálculo a mano

La fuente es **exterior**: se usa el grafo desde el nodo `EXTERIOR`. La
fachada conecta `EXTERIOR` con la Sala (30 dB), y la pared interior conecta la
Sala con el Dormitorio (30 dB).

```
L_Sala  = 85 − 30 (fachada)                    = 55.00 dB
L_Dormi = 85 − 30 (fachada) − 30 (pared)       = 25.00 dB
```

**Normativa aplicada** (Normativa.java):

| Habitación | Nivel | Horario | Límite | ¿Habitable? |
|---|---|---|---|---|
| Sala | 55.0 dB | día | 35 dB | ❌ NO (excede en 20 dB) |
| Dormitorio | 25.0 dB | noche | 30 dB | ✅ SÍ |

### Resultado esperado vs. verificado

| Magnitud | Esperado (manual) | Verificado (clase) |
|---|---|---|
| Sala conectada a EXTERIOR | Sí (fachada) | ✔ |
| Dormitorio conectado a EXTERIOR | No (indirecto) | ✔ |
| Nivel Sala | 55.00 dB | ✔ |
| Nivel Dormitorio | 25.00 dB | ✔ |
| Normativa | Sala no cumple (día); Dormitorio cumple (noche) | ✔ |

---

## Cómo ejecutar la verificación

```bash
# Desde la raíz del proyecto:
javac -d out src/acustica/dominio/*.java src/acustica/normativa/*.java \
      src/acustica/simulacion/propagacion/*.java

java -cp out acustica.simulacion.propagacion.VerificacionRayTracing
```

Salida esperada: `19 / 19 verificaciones OK` y «Todas las verificaciones pasaron correctamente.»

---

## Notas

- El modelo de atenuación usado es el del código del equipo:
  `atenuación = área · coeficienteMaterial` (dB), y la suma de aportes es
  energética (incoherente). No representa una ley física de propagación real;
  es el modelo definido en las Tareas 1.x y 4.x.
- Cuando la fuente está **fuera** del edificio (Escenario 3) el código modela
  la propagación con el grafo (fachada → nodo EXTERIOR) y Dijkstra, que es el
  mecanismo previsto en `CalculoAcustico.calcularNivelEnDestino`.
- Deliberadamente se usaron `N = área total` y atenuaciones redondas para que
  el resultado esperado sea verificable a mano.