# Meshrabiya

Meshrabiya es una red mallada (mesh) para Android que funciona sobre WiFi. Permite a las aplicaciones comunicarse de forma transparente a través de múltiples saltos entre grupos WiFi Direct y/o Local Only Hotspot.

Cada dispositivo obtiene una dirección IP virtual (normalmente 169.254.x.y). La aplicación puede usar SocketFactory y DatagramSocket para enviar y recibir datos entre nodos como si estuvieran conectados directamente. Funciona con bibliotecas de red de nivel superior como OkHttp.

Se diseña para entornos donde varios dispositivos Android necesitan comunicarse sin un punto de acceso WiFi central, por ejemplo en escuelas, clínicas de salud sin infraestructura WiFi, caminatas al aire libre, etc. WiFi permite conexiones de alta velocidad y el enrutamiento por múltiples saltos aumenta la cantidad de dispositivos conectados.

Meshrabiya es de código abierto y no depende de servicios propietarios: funciona en Android Open Source Project y no requiere Google Play Services ni APIs cercanas.

## Cómo funciona

1. Un nodo crea un hotspot WiFi (WiFi Direct o Local Only Hotspot) y genera un "connect link" con SSID, contraseña, dirección de enlace y puerto de servicio.
2. Otro nodo recibe el connect link (por QR u otro canal), se conecta al hotspot y envía un paquete UDP al nodo inicial para presentarse.
3. Los nodos intercambian mensajes de origen con su IP virtual y connect link. Estos anuncios se propagan hasta un límite de saltos y permiten calcular la ruta siguiente hacia cada destino.
4. Cada nodo puede operar un hotspot entrante y una conexión saliente como estación simultáneamente, según soporte de hardware y versión Android.

### Modos de hotspot

- WiFi Direct Group: la mayoría de dispositivos Android pueden crear un grupo y permanecer conectados a otra red. En este modo se usan direcciones IPv6 de enlace local para evitar conflictos y permitir el cálculo de dirección MAC.
- Local Only Hotspot: disponible en Android 8+, con concurrencia AP/Station en Android 11+ (hardware requerido). Proporciona un rango de subred aleatorio para evitar conflictos.

## Uso

### Agregar la dependencia

En settings.gradle incluir el repositorio que provea el paquete y en build.gradle:

```
implementation "com.example.Meshrabiya:lib-meshrabiya:0.1-snapshot"
```

### Crear el nodo virtual

```kotlin
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "meshr_settings")

val myNode = AndroidVirtualNode(
    appContext = applicationContext,
    dataStore = applicationContext.dataStore,
)
```

### Iniciar hotspot y obtener connect link

```kotlin
myNode.setWifiHotspotEnabled(
  enabled = true,
  preferredBand = ConnectBand.BAND_5GHZ,
)

val connectLink = myNode.state.filter { it.connectUri != None }.first()
```

### Conectar desde otro nodo

```kotlin
val connectLink = ... // obtenido por QR u otro canal
val connectConfig = MeshrabiyaConnectLink.parseUri(connectLink).hotspotConfig
if (connectConfig != None) {
  myNode.connectAsStation(connectConfig)
}
```

### Intercambio de datos TCP

Servidor:
```kotlin
val serverVirtualAddr: InetAddress = myNode.address
val serverSocket = ServerSocket(port)
```

Cliente:
```kotlin
val socketFactory = myNode.socketFactory
val clientSocket = socketFactory.createSocket(serverVirtualAddr, port)
```

### Intercambio de datos UDP

```kotlin
val datagramSocket = myNode.createBoundDatagramSocket(port)
```

El socket se puede usar con send/receive de forma normal, y el broadcast se envía a 255.255.255.255 en la red virtual.

## Problemas conocidos

En pruebas instrumentadas, cambie la configuración de depuración a "java only" para evitar problemas con el depurador de Android Studio.
