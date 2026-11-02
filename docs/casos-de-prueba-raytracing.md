# Casos de prueba — Ray tracing y propagación (modelo v2)

Diseño de escenarios de prueba para `RayTracing`, `CalculoAcustico`, `Grafo` y
`Dijkstra`, con su **resultado esperado calculado a mano** y la verificación
automática (`VerificacionRayTracing.java`).

> **Nota de versión:** este documento reemplaza el modelo anterior basado en
> `atenuación = área · coeficiente` y en **sumar** los rayos. El modelo v2,
> descrito en `docs/modelo-matematico.md`, usa el índice de reducción sonora
> `R_k` y **promedia** los rayos. Por eso los valores esperados cambiaron.

| Escenario | Qué comprueba |
|---|---|
| 1. Edificio de dos habitaciones | Reparto de rayos por área, promedio por N y propagación directa a la habitación contigua. |
| 2. Edificio con pasillo | Propagación **a través de un nodo intermedio**: la atenuación se acumula restando las dos puertas. |
| 3. Edificio con fachada a una vía | Ruido **exterior** que entra por la fachada (Dijkstra desde `EXTERIOR`) y comparación con la normativa. |

---

## Modelo de cálculo (resumen)

### 1. Ray tracing en la habitación de origen

- Se emiten `N` rayos uniformes en 360°; el paso angular es `360/N`.
- Cada superficie recibe una fracción de los rayos proporcional a su área:
  `fracción_k = S_k / ΣS`.
- Cada rayo pierde **solo** `R_k` (índice de reducción sonora del material),
  nunca `S_k · R_k`.
- El nivel se **promedia** (no se suma):

```
L = 10 · log10( (1/N) · Σ_i 10^(L_i / 10) )
```

Si el reparto es exacto, esto equivale a:

```
L = 10 · log10( Σ_k (S_k / ΣS) · 10^((L_s - R_k) / 10) )
```

El promedio es importante: si se sumaran los `N` rayos, el resultado
dependería de `N` (duplicar `N` subiría ~3 dB). El test incluye una
comprobación explícita de que `N=40` y `N=80` dan lo mismo.

### 2. Propagación a otras habitaciones (Grafo + Dijkstra)

- Cada habitación es un nodo; cada superficie interior es una arista dirigida
  con peso:

```
w_{i→j} = max( 0 , R_k − 10 · log10( S_k / A_j ) )      [dB]
A_j = 0.16 · V_j / T60                                  (Sabine)
```

- `A_j` es la absorción equivalente de la habitación **receptora** y
  `T60 = 0.5 s` por defecto.
- Hacia el nodo `EXTERIOR` se usa `w = R_k` (no hay recinto receptor).
- Dijkstra encuentra la **menor atenuación acumulada**:

```
L_destino = L_fuente − Σ w
```

### 3. Suma de fuentes y ruido de fondo

```
L_total = 10 · log10( 10^(L_fondo/10) + Σ_s 10^(L_s→j/10) )
```

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

**Superficies de la sala** (4 superficies de 10 m², total 40 m², N=40):

| Superficie | Área (m²) | `R_k` (dB) | Nivel por rayo = 80 − R_k | Rayos |
|---|---|---|---|---|
| M1 — pared interior hacia D1 | 10 | 30 | **50 dB** | 10 |
| F1 — fachada al exterior | 10 | 15 | **65 dB** | 10 |
| P1 — piso | 10 | 20 | **60 dB** | 10 |
| T1 — techo | 10 | 25 | **55 dB** | 10 |
| **Total** | **40** | | | **40** |

### Cálculo a mano

Cada superficie recibe 10 rayos. El nivel se promedia sobre los 40 rayos:

```
L_S1 = 10 · log10( (10·10^5 + 10·10^6.5 + 10·10^6 + 10·10^5.5) / 40 )
     = 10 · log10( (1 000 000 + 31 622 777 + 10 000 000 + 3 162 278) / 40 )
     = 10 · log10( 45 785 055 / 40 )
     = 10 · log10( 1 144 626.4 )
     = 60.59 dB
```

Nivel del dormitorio (la fuente está en S1; el único camino directo es la
medianera):

```
A_D1 = 0.16 · (3·4·3) / 0.5 = 11.52 m²
w    = 30 − 10·log10(10 / 11.52) = 30.61 dB
L_D1 = 80 − 30.61 = 49.39 dB
```

### Resultado esperado vs. verificado

| Magnitud | Esperado (manual) | Verificado (clase) |
|---|---|---|
| Rayos M1 / F1 / P1 / T1 | 10 / 10 / 10 / 10 | ✔ |
| Nivel por rayo M1 / F1 / P1 / T1 | 50 / 65 / 60 / 55 dB | ✔ |
| Nivel Sala (promedio) | 60.59 dB | ✔ |
| Nivel Dormitorio | 49.39 dB | ✔ |
| Independencia de N (40 vs 80) | mismo nivel | ✔ |
| Habitación sin superficies | 80.00 dB (no 96.02) | ✔ |

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

**Superficies de la Oficina 1** (total 40 m², N=40):

| Superficie | Área (m²) | `R_k` (dB) | Nivel por rayo = 80 − R_k | Rayos |
|---|---|---|---|---|
| PU1 — puerta al pasillo | 4 | 12 | **68 dB** | 4 |
| F1 — fachada | 12 | 36 | **44 dB** | 12 |
| P1 — piso | 12 | 36 | **44 dB** | 12 |
| T1 — techo | 12 | 36 | **44 dB** | 12 |
| **Total** | **40** | | | **40** |

### Cálculo a mano

```
L_H1 = 10 · log10( (4·10^6.8 + 36·10^4.4) / 40 )
     = 10 · log10( (25 238 294 + 904 279) / 40 )
     = 10 · log10( 26 142 573 / 40 )
     = 10 · log10( 653 564.3 )
     = 58.15 dB
```

Nivel de la Oficina 2: el sonido cruza **las dos puertas**. Cada puerta tiene
`R_k = 12 dB`, área 4 m² y un recinto receptor con `A = 11.52 m²`:

```
w_puerta = 12 − 10·log10(4 / 11.52) = 16.59 dB
L_H2     = 80 − 16.59 − 16.59 = 46.81 dB
```

### Resultado esperado vs. verificado

| Magnitud | Esperado (manual) | Verificado (clase) |
|---|---|---|
| Rayos PU1 / F1 / P1 / T1 | 4 / 12 / 12 / 12 | ✔ |
| Nivel por rayo PU1 / F1 / P1 / T1 | 68 / 44 / 44 / 44 dB | ✔ |
| Camino H1 → H2 | Pasa por el pasillo P1 | ✔ |
| Nivel Oficina 1 | 58.15 dB | ✔ |
| Nivel Oficina 2 | 46.81 dB | ✔ |

---

## Escenario 3 — Edificio con fachada a una vía

Una sala con fachada directa a una avenida (tráfico, 85 dB de noche) y un
dormitorio interior detrás de ella.

```
        VIA (85 dB)
           ||
     +-----||--------------------------------+
     |  S1 (Sala)   [FV1] fachada a la vía        |
     |              (20 m², R_k = 30 dB)          |
     |   PD1 (pared interior, R_k = 30 dB)        |
     +--------+-----------------------------------+
              |  D1 (Dormitorio)
              +-----------------------------------+
```

**Datos:**

| Superficie | Área (m²) | `R_k` (dB) | Receptor | `A` (m²) |
|---|---|---|---|---|
| FV1 — fachada a la vía (Sala) | 20 | 30 | Sala (5·4·3 = 60 m³) | 19.20 |
| PD1 — pared interior (Sala ↔ Dormitorio) | 10 | 30 | Dormitorio (3·4·3 = 36 m³) | 11.52 |

### Cálculo a mano

La fuente es **exterior**: se usa el grafo desde el nodo `EXTERIOR`.

```
w_FV1 = 30 − 10·log10(20 / 19.20) = 29.82 dB
w_PD1 = 30 − 10·log10(10 / 11.52) = 30.61 dB

L_Sala  = 85 − 29.82                    = 55.18 dB
L_Dormi = 85 − 29.82 − 30.61            = 24.56 dB
```

**Normativa aplicada** (`Normativa.java`):

| Habitación | Nivel | Horario | Límite | ¿Habitable? |
|---|---|---|---|---|
| Sala | 55.18 dB | día | 35 dB | ❌ NO (excede en 20.18 dB) |
| Dormitorio | 24.56 dB | noche | 30 dB | ✅ SÍ |

### Resultado esperado vs. verificado

| Magnitud | Esperado (manual) | Verificado (clase) |
|---|---|---|
| Sala conectada a EXTERIOR | Sí (fachada) | ✔ |
| Dormitorio conectado a EXTERIOR | No (indirecto) | ✔ |
| Nivel Sala | 55.18 dB | ✔ |
| Nivel Dormitorio | 24.56 dB | ✔ |
| Normativa | Sala no cumple (día); Dormitorio cumple (noche) | ✔ |

---

## Cómo ejecutar la verificación

```bash
# Desde la raíz del proyecto:
javac -d out $(find src -name "*.java")

java -cp out acustica.simulacion.propagacion.VerificacionRayTracing
java -cp out acustica.simulacion.propagacion.VerificacionGrafo
java -cp out acustica.normativa.VerificacionNormativa
```

Salida esperada:

```
VerificacionRayTracing: 21 / 21 verificaciones OK
VerificacionGrafo:      16 / 16 verificaciones OK
VerificacionNormativa:  15 / 15 verificaciones OK
```

---

## Notas

- El modelo es una simplificación didáctica, no una ley física de propagación
  real. Ver las limitaciones en `docs/modelo-matematico.md`.
- La atenuación usa `R_k` de banda única (referencia 500 Hz); no se modela la
  dependencia con la frecuencia.
- Solo se considera el camino de menor atenuación (Dijkstra); no se suman
  caminos paralelos ni reflexiones.
- Los valores de `R_k` son típicos de la literatura (concreto ≈ 50–55 dB,
  drywall ≈ 30 dB, vidrio doble ≈ 30 dB); para el informe final conviene citar
  la fuente exacta.
- Los casos se eligieron con áreas y `R_k` redondos para que el cálculo a mano
  sea reproducible.
