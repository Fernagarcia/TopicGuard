# 🛡️ TopicGuard

TopicGuard es un bot de Discord desarrollado para mantener comunidades organizadas, evitando la duplicación de consultas y automatizando la gestión de foros y canales.

El proyecto nació como una solución práctica para un servidor educativo de la UTN, pero está diseñado para ser usado en cualquier comunidad técnica.

---

## 📦 Estructura del proyecto

TopicGuard está dividido en módulos Maven independientes:
```
TopicGuard/
├── common/       # Motores de similitud, utilidades compartidas
├── forum-bot/    # Gestión y organización de foros (Parte 1)
└── reply-bot/    # Respuestas automáticas con NLP (Parte 2 - en desarrollo)
```

---

## 🎯 ¿Qué hace?

### Forum Bot (estable)
- Detecta publicaciones duplicadas o similares en canales de foro
- Cierra automáticamente posts exactamente duplicados
- Solicita confirmación al autor si el tema es similar
- Crea hilos organizados desde canales de texto con formato de comando
- Registra logs de creación y cierre de foros en un canal privado
- Aplica cooldown configurable entre publicaciones por usuario
- Indexa posts existentes para búsqueda eficiente de candidatos

### Reply Bot (en desarrollo)
- Respuestas automáticas basadas en intención del usuario
- Base de conocimiento construida desde foros resueltos

---

## 🧠 ¿Cómo funciona la detección de similitud?

TopicGuard usa un motor híbrido que combina tres algoritmos con pesos balanceados:

| Motor | Peso | Propósito |
|---|---|---|
| Levenshtein | 20% | Detecta typos y variaciones tipográficas |
| Jaccard | 50% | Compara conjuntos de palabras sin importar el orden |
| Containment | 30% | Detecta si un título está contenido en otro |

El pipeline de procesamiento de texto incluye normalización de acentos, eliminación de stopwords y stemming en español antes de la comparación.

### Flujo de decisión en foros
```
Nueva publicación
       ↓
ThreadIndexService → candidatos por tokens
       ↓
HybridSimilarityEngine → score
       ↓
EXACT  → avisa + cierra en 60s
SIMILAR → votación del autor (30 min) → cierra o se mantiene
NONE   → se indexa + se loguea
```

---

## ⚙️ Configuración

El bot se configura mediante comandos slash con permisos de administrador:

| Comando | Descripción |
|---|---|
| `/config cooldown <segundos>` | Tiempo mínimo entre publicaciones por usuario |
| `/config logchannel <canal>` | Canal privado donde se registran los logs |
| `/stats` | Métricas del bot |

La configuración se persiste en `data/server_settings.json` y se restaura automáticamente al reiniciar.

---

## 🛠 Tecnologías

- Java 17
- JDA 5 (Discord API)
- Maven multi-módulo
- Jackson (persistencia JSON)
- Algoritmos: Levenshtein, Jaccard, Containment, TF-IDF

---

## 🚀 Instalación

### Requisitos
- Java 17+
- Maven 3.8+
- Token de bot de Discord

### Pasos

1. Cloná el repositorio:
```bash
git clone https://github.com/tu-usuario/TopicGuard.git
cd TopicGuard
```

2. Compilá el proyecto:
```bash
mvn clean install
```

3. Configurá el token:
```bash
# Windows
set DISCORD_TOKEN=tu_token_aqui

# Linux / Mac
export DISCORD_TOKEN=tu_token_aqui
```

4. Ejecutá el bot:
```bash
java -jar forum-bot/target/forum-bot-1.0-SNAPSHOT.jar
```

---

## 📊 Métricas disponibles

TopicGuard registra internamente:
- Mensajes procesados
- Hilos creados
- Redirecciones exactas
- Confirmaciones solicitadas / aceptadas / rechazadas

---

## 🗺️ Roadmap

- [x] Detección de duplicados con motor híbrido
- [x] Gestión de foros con votación
- [x] Logs con embeds en canal privado
- [x] Configuración persistente por servidor
- [x] Estructura multi-módulo Maven
- [ ] Soporte de tags de foro como factor de similitud (v2)
- [ ] Comparación de contenido con TF-IDF (v2)
- [ ] Reply Bot: respuestas automáticas con NLP (Parte 2)
- [ ] Migración a SQLite para múltiples servidores

---

## 🎓 Origen del proyecto

TopicGuard comenzó como una herramienta interna para mejorar la organización de un Discord educativo de la UTN. El objetivo principal es facilitar el intercambio de conocimiento y mantener el orden en comunidades técnicas.
