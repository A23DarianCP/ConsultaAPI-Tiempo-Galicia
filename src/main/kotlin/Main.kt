import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class WeatherResponse(val daily: DailyData? = null)

@Serializable
data class DailyData(
    val time: List<String> = emptyList(),
    val precipitation_sum: List<Double> = emptyList(),
    val temperature_2m_max: List<Double> = emptyList(),
    val temperature_2m_min: List<Double> = emptyList()
)

fun main() {
    val locations = if (PedirCoordenadas()) listOf(RecojerCoordenadas()) else LocalizacionesPredefinidas()

    val weatherData = locations.mapNotNull { (name, lat, lon) ->
        fetchWeatherData(lat, lon)?.let { name to it }
    }

    analyzeWeatherData(weatherData)
}

fun PedirCoordenadas(): Boolean {
    print("¿Quieres introducir coordenadas manualmente? (s/n): ")
    return readLine()?.lowercase() == "s"
}

fun RecojerCoordenadas(): Triple<String, Double, Double> {
    print("Introduce la latitud: ")
    val lat = readLine()?.toDoubleOrNull()
    print("Introduce la longitud: ")
    val lon = readLine()?.toDoubleOrNull()

    return if (lat != null && lon != null) Triple("Ubicación Personalizada", lat, lon)
    else {
        println(" Coordenadas inválidas. Se usarán ciudades predefinidas.")
        LocalizacionesPredefinidas().first()
    }
}
dad
fun LocalizacionesPredefinidas() = listOf(
    Triple("Santiago de Compostela", 42.8805, -8.5463),
    Triple("A Coruña", 43.3623, -8.4115),
    Triple("Vigo", 42.2406, -8.7207),
    Triple("Lugo", 43.0125, -7.5583),
    Triple("Ourense", 42.3409, -7.8641)
)
// Marcar estas casillas para que el código funcione correctamente: En la sección "Daily Weather Variables", marcar las siguientes opciones:
// Precipitation Sum (mm) → precipitation_sum
// 2m Temperature Max (°C) → temperature_2m_max
// 2m Temperature Min (°C) → temperature_2m_min
fun fetchWeatherData(latitude: Double, longitude: Double): WeatherResponse? {
    val url = "https://archive-api.open-meteo.com/v1/archive?latitude=$latitude&longitude=$longitude" +
            "&start_date=2023-01-01&end_date=2023-12-31&daily=precipitation_sum,temperature_2m_max,temperature_2m_min&timezone=auto"

    return try {
        val response = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder().uri(URI.create(url)).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        ).body()

        if (response.contains("\"error\":true")) {
            println(" Error en la respuesta de la API para ($latitude, $longitude): $response")
            null
        } else Json { ignoreUnknownKeys = true }.decodeFromString(response)
    } catch (e: Exception) {
        println(" Error al obtener datos para ($latitude, $longitude): ${e.message}")
        null
    }
}

fun analyzeWeatherData(weatherData: List<Pair<String, WeatherResponse>>) {
    if (weatherData.isEmpty()) {
        println(" No hay datos disponibles para analizar.")
        return
    }

    val results = weatherData.map { (name, data) ->
        val totalRain = data.daily?.precipitation_sum?.sum() ?: 0.0
        val avgMaxTemp = data.daily?.temperature_2m_max?.takeIf { it.isNotEmpty() }?.average() ?: 0.0
        val avgMinTemp = data.daily?.temperature_2m_min?.takeIf { it.isNotEmpty() }?.average() ?: 0.0
        val avgTemp = (avgMaxTemp + avgMinTemp) / 2
        val hottestDay = data.daily?.temperature_2m_max?.maxOrNull()
        val coldestDay = data.daily?.temperature_2m_min?.minOrNull()

        println(
            """
             $name
            - Lluvia total: $totalRain mm
            - Temp. media: $avgTemp°C
            - Día más caluroso: ${hottestDay ?: "N/A"}°C
            - Día más frío: ${coldestDay ?: "N/A"}°C
            """.trimIndent()
        )

        Triple(name, totalRain, avgTemp)
    }

    val mostRainy = results.maxByOrNull { it.second }
    val hottest = results.maxByOrNull { it.third }

    println("\n **Resumen Final**:")
    println(" Lugar más lluvioso: ${mostRainy?.first ?: "N/A"} con ${mostRainy?.second ?: "N/A"} mm de lluvia.")
    println(" Lugar más caluroso: ${hottest?.first ?: "N/A"} con temperatura media de ${hottest?.third ?: "N/A"}°C.")
}
