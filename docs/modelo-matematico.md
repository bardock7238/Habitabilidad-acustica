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

Además de las fuentes (`FuenteSonido`), la actividad de **otras** habitaciones también llega al
destino: cada habitación *i ≠ j* con actividad de `emiteDba` = E_a aporta `E_a − Att(i → j)`
(si E_a > 0). La actividad de la propia habitación *j* no cuenta: el límite mide lo que
transmiten los demás espacios.

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

### Límites de los espacios comunes

`Parqueadero`, `Salon comunal`, `Cuarto de maquinas` y `Pasillo` no tienen límite OMS. Cuando no
tienen actividad se evalúan con el sector B (zonas residenciales) de `datos/sectores.csv`
(Res. 0627/2006): 65 dBA de día y 55 dBA de noche. Esos valores son de ruido ambiental por
zona; usarlos dentro del edificio es una aproximación. `Estudio` usa 35 dBA de día y 30 dBA de
noche, igual que `Sala`.

## 8. Limitaciones asumidas

- R_k se toma a una frecuencia de referencia (500 Hz); no se modela dependencia con la frecuencia.
- No se modela transmisión estructural (impactos, saltos, vibraciones): solo ruido aéreo.
- Sin decaimiento por distancia dentro de la habitación de origen (campo difuso).
- Valores típicos de R_k (concreto ≈ 50–55 dB, drywall ≈ 30 dB, vidrio doble ≈ 30 dB) son
  órdenes de magnitud de la literatura: citar la fuente exacta en el informe final.
- Las actividades no dependen del horario: su ruido se aplica igual de día y de noche.
- Los límites de los espacios comunes (sector B) son una aproximación, ver sección 7.

## 9. Estructuras de árboles utilizadas

El sistema utiliza árboles balanceados para organizar los resultados de habitabilidad acústica y las posibles soluciones de mejora. Estas estructuras permiten realizar consultas ordenadas sin recorrer necesariamente todos los elementos almacenados.

### 9.1. Árbol AVL: resultados de habitabilidad

El árbol AVL se utiliza para organizar los resultados de habitabilidad de las habitaciones según su exceso de ruido.

La clave de ordenamiento considera el exceso de ruido respecto al límite permitido y un identificador de desempate para distinguir habitaciones con valores iguales.

El exceso se calcula como:

\[
Exceso = NivelEvaluado - LimitePermitido
\]

El nivel evaluado depende del tipo de actividad. En las actividades que exigen silencio se considera el ruido recibido, mientras que en las actividades de alto impacto puede representar el nivel del vecino más afectado.

Se utiliza un AVL porque mantiene una condición estricta de equilibrio: la diferencia de alturas entre sus subárboles no puede superar una unidad. Esto permite realizar búsquedas, inserciones y eliminaciones en tiempo O(log n).

El AVL facilita identificar las habitaciones más críticas y consultar aquellas que superan determinados umbrales.

### 9.2. Árbol rojo-negro: soluciones de mejora

El árbol rojo-negro se utiliza para almacenar las soluciones candidatas de aislamiento acústico asociadas a una habitación no habitable.

Las soluciones se ordenan utilizando como clave principal su costo total a largo plazo. En caso de empate, se prioriza la solución con mayor reducción acústica y, finalmente, su identificador.

Se utiliza un árbol rojo-negro porque permite mantener los elementos balanceados con operaciones de inserción, búsqueda y eliminación en O(log n), y requiere un rebalanceo menos estricto que el AVL.

Esta característica resulta conveniente para los planes de mejora, donde se agregan nuevas alternativas y se descartan soluciones que no cumplen los requisitos.

La solución de menor costo no necesariamente es la más adecuada. También debe verificarse que consiga la reducción acústica requerida.

### 9.3. Árbol B+: consultas por piso y nivel de ruido

El árbol B+ permite organizar los resultados de una simulación mediante una clave compuesta:

\[
Clave = (piso,\ nivelRecibido,\ habitacionId)
\]

El atributo `nivelRecibido` corresponde al ruido presente en el espacio y no necesariamente coincide con `nivelFinal`, especialmente en actividades de alto impacto.

El identificador de habitación evita perder resultados cuando dos habitaciones tienen el mismo piso y nivel de ruido.

El árbol B+ permite consultar las habitaciones de un piso, encontrar las que superan un nivel determinado y obtener resultados dentro de un rango de niveles.

Sus hojas enlazadas facilitan los recorridos ordenados y las consultas por rango.

Esta estructura corresponde a la funcionalidad del issue #45 y debe verificarse después de integrar su implementación definitiva.

## 10. Soluciones de aislamiento y costo a largo plazo

Cuando una habitación se clasifica como `NO_HABITABLE`, el sistema puede generar un plan de mejora con alternativas destinadas a reducir el ruido.

### 10.1. Estrategia de soluciones por nivel de dificultad

El proyecto plantea una estrategia progresiva de intervención acústica, en la que se priorizan las soluciones de menor dificultad antes de recurrir a modificaciones constructivas de mayor complejidad.

**Nivel 1. Reubicación de actividades entre pisos**

Consiste en trasladar actividades de alto impacto a pisos inferiores o alejados de los espacios sensibles al ruido, mientras que las actividades que requieren silencio se ubican en zonas con menor exposición acústica.

**Nivel 2. Reorganización de espacios dentro del mismo piso**

Busca aumentar la separación entre las fuentes de ruido y los espacios sensibles. Puede incluir espacios intermedios, como pasillos o bodegas, que modifiquen los caminos de propagación acústica.

El modelo de grafos y Dijkstra permite evaluar los cambios de atenuación cuando se modifica la distribución y las conexiones del edificio.

**Nivel 3. Intervención sobre la fuente sonora**

Comprende medidas como encapsulamiento de equipos, instalación de bases antivibratorias, reducción de emisiones o restricciones de horario para actividades ruidosas.

**Nivel 4. Mejoramiento del aislamiento acústico**

Incluye intervenciones constructivas como reforzamiento de muros, instalación de ventanas de doble vidrio, puertas acústicas y sellado de aberturas.

Estas soluciones pueden requerir mayor inversión y modificaciones físicas del edificio.

### Criterios de selección

Las alternativas se plantean en orden creciente de dificultad estimada. Sin embargo, su selección también debe considerar la reducción acústica necesaria, el costo total a largo plazo y el cumplimiento de los límites normativos.

Esta clasificación representa la estrategia de intervención propuesta para el proyecto. Su implementación efectiva debe distinguirse de las soluciones que el simulador ya puede generar y verificar.


### 10.2. Reducción acústica requerida

Para una habitación que supera el límite permitido:

\[
Exceso = NivelEvaluado - LimitePermitido
\]

Una solución debe proporcionar una reducción suficiente para cubrir dicho exceso.

En actividades de alto impacto, el aislamiento requerido hacia una habitación vecina se calcula mediante:

\[
AislamientoRequerido(v) = E_a - Lim(v)
\]

Donde:

- \(E_a\) es el nivel emitido por la actividad.
- \(Lim(v)\) es el límite aplicable a la habitación vecina.

Este cálculo reemplaza el criterio anterior que utilizaba un valor fijo de 70 dB, porque el límite depende de las condiciones de la habitación receptora.

### 10.3. Degradación y mantenimiento

Una solución puede perder efectividad acústica con el paso del tiempo.

El margen inicial de reducción se define como:

\[
Margen = ReduccionDb - ExcesoDb
\]

Cuando existe degradación anual positiva y la reducción cubre el exceso, el modelo estima los años de efectividad sin mantenimiento como:

\[
AniosSinMantenimiento =
\max\left(1,\frac{Margen}{DegradacionAnual}\right)
\]

Si la solución no se degrada, no requiere mantenimiento por pérdida de efectividad dentro de este modelo.

### 10.4. Costo total

El costo a largo plazo incluye la inversión inicial y los mantenimientos necesarios durante un horizonte de evaluación.

\[
CostoTotal = CostoInicial + N_{mantenimientos}\cdot CostoMantenimiento
\]

El horizonte predeterminado es de 10 años.

El sistema ordena las alternativas según este costo total, pero también considera su capacidad de reducción y si han sido verificadas.

Los costos y tasas de degradación utilizados por el generador son valores de ejemplo en unidades relativas. No deben interpretarse como cotizaciones reales ni como predicciones exactas de desempeño.

## 11. Justificación de los cambios frente al árbol de decisión original

Durante el desarrollo del proyecto se realizaron ajustes al árbol de decisión inicial para representar con mayor precisión las condiciones de habitabilidad acústica de los espacios.

### 11.1. Aislamiento según el límite de la habitación vecina

En el planteamiento inicial se consideraba un valor fijo de 70 dB para determinar el aislamiento requerido en actividades de alto impacto.

Sin embargo, este criterio no diferenciaba las necesidades acústicas de los espacios receptores.

Por esta razón, el modelo actualizado calcula el aislamiento requerido para cada habitación vecina mediante:

\[
AislamientoRequerido(v) = E_a - Lim(v)
\]

Donde:

- \(E_a\): nivel de ruido emitido por la actividad de alto impacto.
- \(Lim(v)\): límite acústico aplicable a la habitación vecina.

El aislamiento real se obtiene a partir de la atenuación calculada mediante Dijkstra.

Una actividad de alto impacto se considera habitable cuando el aislamiento real es suficiente para cumplir los límites de todas las habitaciones vecinas evaluables.

Este cambio permite que la evaluación dependa de las características de los espacios receptores, en lugar de utilizar un único valor de referencia.

### 11.2. Incorporación del estado TOLERABLE

El árbol de decisión original contemplaba principalmente los estados `HABITABLE` y `NO_HABITABLE`.

En el modelo actualizado se incorporó el estado `TOLERABLE` para las actividades que exigen silencio, como dormir, estudiar y leer.

La clasificación distingue tres situaciones:

- **HABITABLE:** el ruido recibido no supera el límite óptimo.
- **TOLERABLE:** el ruido supera el límite óptimo, pero permanece dentro del máximo permitido.
- **NO_HABITABLE:** el ruido supera el límite máximo permitido.

Esta clasificación permite diferenciar las condiciones acústicas ideales de aquellas que todavía se encuentran dentro de un margen aceptable.

El estado `TOLERABLE` no se aplica indiscriminadamente a todas las actividades, sino a aquellas para las que el modelo establece límites óptimos y máximos.

### 11.3. Relación con la estrategia de soluciones

Los cambios en la evaluación permiten identificar con mayor precisión qué espacios necesitan intervención y cuál es la reducción acústica requerida.

Como estrategia para la etapa final del proyecto, se propone priorizar las soluciones según su dificultad de implementación:

1. Reubicación de actividades entre pisos.
2. Reorganización de espacios dentro del mismo piso.
3. Intervención sobre la fuente sonora.
4. Mejoramiento del aislamiento constructivo.

Este orden corresponde a la estrategia metodológica propuesta y no implica que el algoritmo actual ya seleccione automáticamente las soluciones siguiendo esa prioridad.

La implementación de esta priorización se contempla para una etapa posterior del proyecto.