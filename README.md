# 🛡️ TopicGuard

TopicGuard es un bot de Discord desarrollado inicialmente para un servidor educativo de la UTN, con el objetivo de mantener los canales organizados y evitar la repetición de consultas.

El proyecto nace como una solución práctica a un problema común en comunidades académicas: la creación de múltiples hilos sobre el mismo tema y el desorden en los canales.

---

## 🎯 ¿Qué hace?

- Crea automáticamente un hilo cuando un estudiante envía una consulta.
- Detecta si ya existe un hilo similar.
- Permite reutilizar el hilo existente para evitar duplicados.
- Reabre hilos archivados cuando corresponde.
- Mantiene los canales limpios eliminando el mensaje original.

---

## 🧠 ¿Cómo funciona?

1. El usuario envía su consulta con el formato configurado.
2. El bot valida el mensaje.
3. Busca hilos similares usando un algoritmo de similitud.
4. Si encuentra uno parecido, pide confirmación para usarlo.
5. Si no existe, crea un nuevo hilo automáticamente.

---

## 🛠 Tecnologías

- Java
- JDA (Discord API)
- Algoritmo de distancia de Levenshtein

---

## 🎓 Origen del proyecto

TopicGuard comenzó como una herramienta interna para mejorar la organización de un Discord educativo de la UTN.  
El objetivo principal es facilitar el intercambio de conocimiento y mantener el orden en comunidades técnicas.

---

## 🚀 Futuras mejoras

- Persistencia en base de datos
- Configuración dinámica desde Discord
- Métricas de uso
- Mejoras en el algoritmo de similitud

---

Proyecto en desarrollo continuo.
