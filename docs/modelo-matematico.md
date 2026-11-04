# Modelo matemático — Habitabilidad acústica

Describe lo que calculan `Grafo`, `Dijkstra`, `CalculoAcustico` y `Simulador`,
y las hipótesis que se asumen.

## 1. Magnitudes y unidades

| Símbolo | Significado | Unidad | Clase |
|---|---|---|---|
| L_s | Nivel que emite la fuente s (en su propia habitación) | dBA | `FuenteSonido.nivelSonido` |
| R_k | Índice de reducción sonora de la superficie k (pérdida de transmisión) | dB | `Material.getIndiceReduccionSonora()` |
| S_k | Área de la superficie k | m² | `Superficie.area` |
| V_j | Volumen de la habitación j | m³ | `Habitacion.calcularVolumen()` |
| A_j | Absorción equivalente de la habitación j | m² | `Habitacion.calcularAbsorcion()` |
| L_fondo | Ruido de fondo de la habitación | dBA | `Habitacion.nivelFondo` |
| Lim(a) | Límite de ruido recibido de la actividad *a* (solo las que exigen silencio) | dBA | `Actividad` / `Normativa` |
| E_a | Nivel que genera la actividad *a* (alto impacto) | dBA | `Actividad.emiteDba` |

## 2. Absorción de la habitación receptora (Sabine)

    A_j = 0.16 · V_j / T60        (T60 = 0.5 s por defecto, recinto amoblado;
                                   configurable con Habitacion.setT60())

## 3. Atenuación de una superficie (peso de la arista i → j)

Campo difuso, transmisión directa (forma simplificada de ISO 12354-1):

    L_j = L_i − R_k + 10·log10( S_k / A_j )
    w_{i→j} = max( 0 , R_k − 10·log10( S_k / A_j ) )      [dB]

- El peso **depende del sentido** (usa A de la habitación que recibe), así que el grafo es **dirigido**.
- El área ya aparece dentro del logaritmo: una pared más grande deja pasar *más* energía, no menos.
  Por eso **no** se usa `área · coeficiente`.
- Hacia el nodo `EXTERIOR` se usa w = R_k.

## 4. Camino dominante (Dijkstra)

    Att(origen → destino) = min sobre caminos P de  Σ_{k∈P} w_k
    L_s→j = L_s − Att(origen_s → j)

Dijkstra exige pesos ≥ 0 (por eso el `max(0, ·)`). Hipótesis: solo cuenta el camino de menor
atenuación (se ignora que varios caminos en paralelo suman energía).

## 5. Suma de fuentes y ruido de fondo (suma energética)

    L_j = 10·log10( 10^(L_fondo/10) + Σ_s 10^(L_s→j / 10) )

El ruido de fondo evita niveles negativos sin sentido físico (p. ej. −33 dB).

## 6. Ray tracing (módulo de verificación / reparto por superficie)

La fracción de rayos que cae en la superficie k es S_k / ΣS (campo difuso). Como el reparto
**ya pondera por área**, la pérdida de cada rayo debe ser solo R_k (dB), nunca `S_k · R_k`.
Con N rayos el nivel se **promedia** (no se suma):

    L = 10·log10( (1/N) · Σ_i 10^(L_i/10) )

## 7. Habitabilidad (árbol de decisión por actividad)

Si la habitación tiene una actividad prevista *a* (`Habitacion.getActividadPrevista()`), se
evalúa según su categoría (`Actividad.categoria`):

**Nodo 1 = SÍ — exige silencio** (DORMIR, ESTUDIAR, LEER). Se mide el ruido que **llega** de
otros espacios (Nodo 2, tope 40 dBA):

    HABITABLE            si L_j ≤ Lim_óptimo(a)
    TOLERABLE            si Lim_óptimo < L_j ≤ Lim_máx(a)
    NO HABITABLE         si L_j > Lim_máx(a)

**Nodo 3 = NO — actividad moderada** (LLAMADAS, CONVERSAR): HABITABLE, no se compara contra
ningún límite.

**Nodo 3 = SÍ — alto impacto** (ENTRETENIMIENTO, INSTRUMENTO, EJERCICIO). Se mide si el
aislamiento hacia cada habitación vecina *v* alcanza (Nodo 4):

    aislamiento_requerido(v) = E_a − Lim(v)          E_a = Actividad.emiteDba
    aislamiento_real(v)      = Att(j → v)            (Dijkstra, sección 4)
    HABITABLE            si Att(j → v) ≥ E_a − Lim(v) para toda vecina v
    NO HABITABLE         si falla en alguna

Lim(v) es el límite_máx de la actividad de *v* si exige silencio; si *v* no tiene actividad,
el de `Normativa` para su tipo y horario. Las vecinas con actividad moderada o de alto
impacto, o sin límite definido, no se tienen en cuenta. El resultado reporta la vecina
más afectada (su nivel y su límite).

Si no hay actividad prevista, se usa el límite único de `Normativa` para el tipo de recinto
y el horario:

    HABITABLE            si L_j ≤ Lim(tipo, h)
    NO HABITABLE         si L_j > Lim(tipo, h)
    NO EVALUABLE         si no existe límite para (tipo, h)

## 8. Limitaciones asumidas

- R_k se toma a una frecuencia de referencia (500 Hz); no se modela dependencia con la frecuencia.
- No se modela transmisión estructural (impactos, saltos, vibraciones): solo ruido aéreo.
- Sin decaimiento por distancia dentro de la habitación de origen (campo difuso).
- Valores típicos de R_k (concreto ≈ 50–55 dB, drywall ≈ 30 dB, vidrio doble ≈ 30 dB) son
  órdenes de magnitud de la literatura: citar la fuente exacta en el informe final.
