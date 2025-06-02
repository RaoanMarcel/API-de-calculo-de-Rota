
package com.example

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.*
import kotlin.random.Random

@Serializable
data class Coordenada(val userId: String, val latitude: Double, val longitude: Double)

object CoordenadasCache {
    private val coordenadas = mutableListOf<Coordenada>()

    fun adicionar(coordenada: Coordenada) {
        coordenadas.add(coordenada)
    }

    fun listar(): List<Coordenada> = coordenadas.toList()

    fun limpar() {
        coordenadas.clear()
    }
}

fun calcularDistancia(a: Coordenada, b: Coordenada): Double {
    val R = 6371e3
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)
    val deltaLat = Math.toRadians(b.latitude - a.latitude)
    val deltaLon = Math.toRadians(b.longitude - a.longitude)

    val h = sin(deltaLat / 2).pow(2.0) +
            cos(lat1) * cos(lat2) *
            sin(deltaLon / 2).pow(2.0)
    val c = 2 * atan2(sqrt(h), sqrt(1 - h))

    return R * c
}

fun gerarPopulacao(pontos: List<Coordenada>, tamanho: Int): List<List<Coordenada>> {
    return List(tamanho) { pontos.shuffled() }
}

fun avaliarCaminho(caminho: List<Coordenada>): Double {
    return caminho.zipWithNext().sumOf { calcularDistancia(it.first, it.second) }
}

fun cruzar(a: List<Coordenada>, b: List<Coordenada>): List<Coordenada> {
    val corte = Random.nextInt(a.size)
    val inicio = a.take(corte)
    val resto = b.filterNot { it in inicio }
    return inicio + resto
}

fun mutar(caminho: List<Coordenada>): List<Coordenada> {
    val copia = caminho.toMutableList()
    val i = Random.nextInt(copia.size)
    val j = Random.nextInt(copia.size)
    copia[i] = copia[j].also { copia[j] = copia[i] }
    return copia
}

fun algoritmoGenetico(pontos: List<Coordenada>, geracoes: Int = 300, populacaoSize: Int = 100): List<Coordenada> {
    var populacao = gerarPopulacao(pontos, populacaoSize)
    repeat(geracoes) {
        populacao = populacao.sortedBy { avaliarCaminho(it) }.take(populacaoSize / 2)
        val filhos = mutableListOf<List<Coordenada>>()
        while (filhos.size < populacaoSize / 2) {
            val a = populacao.random()
            val b = populacao.random()
            filhos.add(mutar(cruzar(a, b)))
        }
        populacao += filhos
    }
    return populacao.minByOrNull { avaliarCaminho(it) } ?: pontos
}

suspend fun calcularRotaGoogle(pontos: List<Coordenada>, apiKey: String): String {
    val origin = pontos.first()
    val destination = pontos.last()
    val waypoints = pontos.drop(1).dropLast(1)
        .joinToString("|") { "\${it.latitude},\${it.longitude}" }

    val urlStr = "https://maps.googleapis.com/maps/api/directions/json?" +
            "origin=\${origin.latitude},\${origin.longitude}" +
            "&destination=\${destination.latitude},\${destination.longitude}" +
            "&waypoints=optimize:true|\${URLEncoder.encode(waypoints, "UTF-8")}" +
            "&key=\$apiKey"

    val url = URL(urlStr)
    val conn = withContext(Dispatchers.IO) { url.openConnection() as HttpURLConnection }
    conn.requestMethod = "GET"

    return conn.inputStream.bufferedReader().use { it.readText() }
}

fun main() {
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            io.ktor.serialization.json()
        }
        install(CORS) {
            anyHost()
        }

        routing {
            post("/coordenadas") {
                val coordenada = call.receive<Coordenada>()
                CoordenadasCache.adicionar(coordenada)
                call.respond(HttpStatusCode.Created, "Coordenada recebida.")
            }

            get("/coordenadas") {
                call.respond(CoordenadasCache.listar())
            }

            post("/rota-genetica") {
                val pontos = CoordenadasCache.listar()
                val rota = algoritmoGenetico(pontos)
                CoordenadasCache.limpar()
                call.respond(mapOf("rotaCalculada" to rota))
            }

            post("/rota-google") {
                val pontos = CoordenadasCache.listar()
                val apiKey = System.getenv("GOOGLE_MAPS_API_KEY") ?: "SUA_API_KEY_AQUI"
                val resposta = calcularRotaGoogle(pontos, apiKey)
                CoordenadasCache.limpar()
                call.respondText(resposta, ContentType.Application.Json)
            }
        }
    }.start(wait = true)
}
