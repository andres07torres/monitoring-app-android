# 📱 InfraMonitor - Aplicación Móvil Android

![Estado](https://img.shields.io/badge/Estado-Desarrollo-success?style=for-the-badge&logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack-Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Material3](https://img.shields.io/badge/Material-3-757575?style=for-the-badge&logo=materialdesign&logoColor=white)

**InfraMonitor** es la interfaz móvil diseñada para interactuar con el [Agente Orquestador API](https://github.com/andres07torres/monitoring-agent-api). Permite a los administradores de sistemas monitorear servidores Ubuntu y gestionar contenedores Docker directamente desde su smartphone, de forma segura y eficiente.

## 🚀 Características Principales

- **Dashboard en Tiempo Real**: Visualización intuitiva del uso de CPU (núcleos y frecuencia), RAM y almacenamiento del host remoto.
- **Gestión de Contenedores**:
  - Listado completo de contenedores (activos e inactivos).
  - Acciones rápidas: Iniciar, detener, reiniciar y eliminar.
  - Visualización de **Logs en vivo** para depuración rápida.
- **Diagnóstico Remoto**: Terminal integrada para ejecutar comandos del sistema (ping, uptime, etc.) y recibir la salida en tiempo real.
- **Alertas y Healthchecks**: Monitoreo en segundo plano de servicios críticos y URLs con notificaciones push ante caídas.
- **Conexión Segura**: Autenticación mediante `X-API-Key` y configuración dinámica de la URL del servidor.

## 🛠️ Stack Tecnológico

- **Lenguaje:** ![Kotlin](https://img.shields.io/badge/-Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white) 1.9+
- **UI Framework:** ![Compose](https://img.shields.io/badge/-Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)
- **Arquitectura:** MVVM (Model-View-ViewModel) con StateFlow.
- **Networking:** ![Retrofit](https://img.shields.io/badge/-Retrofit-black?style=flat-square) + OkHttp para comunicación REST.
- **Tareas en Segundo Plano:** ![WorkManager](https://img.shields.io/badge/-WorkManager-4285F4?style=flat-square) para alertas persistentes.
- **Inyección de Dependencias:** ViewModel y Factory patterns.

## 📂 Estructura del Proyecto

```text
app/src/main/java/com/example/inframonitor/
├── data/               # Modelos de datos y clientes de red (Retrofit)
├── ui/                 # Componentes de UI
│   ├── screens/        # Pantallas completas (Dashboard, Containers, etc.)
│   ├── theme/          # Definición de colores, tipografía y formas (Material 3)
│   └── components/     # Widgets reutilizables
├── viewmodel/          # Lógica de estado de la UI
└── worker/             # Tareas programadas con WorkManager
```

## ⚙️ Configuración Inicial

Para conectar la aplicación con tu servidor:

1. Instala el **Agente Orquestador API** en tu servidor Ubuntu.
2. Al abrir la app por primera vez, ingresa la **URL del servidor** (ej: `http://tu-ip:8000`) y la **API Key** generada por el agente.
3. Una vez validada, la configuración se guardará localmente para accesos futuros.

---
Desarrollado con ❤️ para llevar el control de tu infraestructura en el bolsillo.
