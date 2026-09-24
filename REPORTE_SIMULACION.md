# Reporte de simulación - Habitabilidad Acústica

Este es el reporte que pedía la issue: nivel por cuarto, límite, si cumple o no, porcentaje de habitables y peor caso. Depende de la 5.1 (el Simulador).

## Qué revisamos
El Simulador (5.1) ya daba nivel, límite y cumplimiento por cada cuarto con `ResultadoHabitabilidad`, y el Main (5.2) mostraba el peor caso. Lo que faltaba era el porcentaje de habitables, así que lo agregamos:

- En `Main.java` el `imprimirResultados` ahora saca `Habitables: X/Y (Z%)` además del peor caso.
- En `Simulador.java` agregamos `calcularPorcentajeHabitable()` para reutilizarlo.

Sin eso la issue quedaba a medias.

## Cómo corre
Escenario del Main: 6 aptos (101-106), 12 cuartos. Fuentes de día: TV 65 en S101, aspiradora 70 en S102, lavadora 58 en S105. Fuentes de noche: ventilador 28 en D101, parlante 55 en D103, conversación 45 en D106.

Ojo: la norma solo tiene `Sala-dia 35` y `Dormitorio-noche 30`. O sea que en día solo se evalúan las 6 salas y en noche solo los 6 dormitorios. Lo demás sale como aviso y no entra al porcentaje.

## Resultados día (solo salas, límite 35)
| Cuarto | Nivel | Límite | Cumple |
|---|---|---|---|
| Sala 101 | 65.0 dB | 35 | NO |
| Sala 102 | 70.0 dB | 35 | NO |
| Sala 103 | -33.4 dB | 35 | SÍ |
| Sala 104 | -36.3 dB | 35 | SÍ |
| Sala 105 | 58.0 dB | 35 | NO |
| Sala 106 | -36.3 dB | 35 | SÍ |

Habitables día: 3/6 (50.0%)
Peor caso día: Sala 102, se pasa por 35.0 dB.

Los negativos son porque están lejos de las fuentes y el modelo resta toda la atenuación, igual el código los suma así.

## Resultados noche (solo dormitorios, límite 30)
| Cuarto | Nivel | Límite | Cumple |
|---|---|---|---|
| Dorm 101 | 28.0 dB | 30 | SÍ |
| Dorm 102 | 7.2 dB | 30 | SÍ |
| Dorm 103 | 55.0 dB | 30 | NO |
| Dorm 104 | 7.2 dB | 30 | SÍ |
| Dorm 105 | 4.0 dB | 30 | SÍ |
| Dorm 106 | 45.0 dB | 30 | NO |

Habitables noche: 4/6 (66.7%)
Peor caso noche: Dorm 103, se pasa por 25.0 dB (el parlante del vecino).

## Conclusión
De día la mitad de las salas no pasa por la TV, aspiradora y lavadora. De noche 2 dormitorios no pasan por el parlante y la conversación. El modelo responde bien a lo que pedía la issue después del arreglo del porcentaje.

Números sacados replicando el modelo (grafo + Dijkstra + suma energética) en PowerShell porque acá no había JDK a la mano; pendientes de confirmar con `java -cp out acustica.Main` cuando se tenga Java.
