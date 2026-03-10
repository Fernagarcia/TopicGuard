# 🛡️ TopicGuard

TopicGuard es un bot de Discord diseñado para mantener comunidades organizadas, evitando la duplicación de consultas y automatizando la gestión de foros y canales.

Utiliza técnicas de análisis de texto para detectar publicaciones similares y redirigir a los usuarios hacia discusiones ya existentes, reduciendo el ruido y mejorando el acceso a la información dentro del servidor.

El proyecto nació como una solución práctica para un servidor educativo de la UTN, con el objetivo de facilitar el intercambio de conocimiento y mantener la estructura de las conversaciones.

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
- Sistema de permisos por roles: los administradores pueden delegar moderación del bot a roles específicos

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

## 🔐 Sistema de permisos

TopicGuard implementa un sistema de permisos en capas:

| Nivel | Quién | Puede hacer |
|---|---|---|
| Administrador Discord | Tiene permiso `ADMINISTRATOR` | Todo, sin restricciones |
| Rol permitido | Tiene un rol en `allowedRoleIds` | Comandos de moderación: `cooldown`, `logchannel`, `defaulttag`, `stats` |
| Usuario común | Cualquier otro miembro | Solo crear publicaciones |

El comando `/config allowedrole` es exclusivo de administradores para evitar escalada de privilegios.

---

## ⚙️ Configuración

El bot se configura mediante comandos slash. Los comandos de configuración requieren permisos de administrador o rol permitido. La gestión de roles es exclusiva de administradores.

| Comando | Permiso requerido | Descripción |
|---|---|---|
| `/config cooldown <segundos>` | Admin o rol permitido | Tiempo mínimo entre publicaciones por usuario |
| `/config logchannel <canal>` | Admin o rol permitido | Canal privado donde se registran los logs |
| `/config defaulttag <tag>` | Admin o rol permitido | Tag que se aplica automáticamente al crear una publicación |
| `/config allowedrole add <rol>` | Solo administrador | Agrega un rol con permisos de moderación del bot |
| `/config allowedrole remove <rol>` | Solo administrador | Remueve un rol con permisos de moderación |
| `/config allowedrole list` | Solo administrador | Lista los roles con permisos configurados |
| `/stats` | Admin o rol permitido | Métricas del bot |

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

