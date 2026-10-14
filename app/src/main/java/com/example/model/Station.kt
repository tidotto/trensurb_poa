package com.example.model

import android.location.Location

enum class TrainDirection(val label: String) {
    TO_NOVO_HAMBURGO("Sentido Novo Hamburgo"),
    TO_MERCADO("Sentido Mercado (Porto Alegre)"),
    UNKNOWN("Detectando sentido…")
}

enum class StationStatus {
    AT_STATION,
    IN_TRANSIT
}

enum class DiagramThemeMode(val label: String) {
    LIGHT("Claro"),
    DARK("Escuro"),
    HIGH_CONTRAST("Alto Contraste")
}

data class Station(
    val id: Int,
    val name: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val connections: List<String> = emptyList(),
    val isTerminal: Boolean = false,
    val zoneDescription: String = ""
) {
    fun distanceTo(lat: Double, lon: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(latitude, longitude, lat, lon, results)
        return results[0]
    }
}

object TrensurbData {
    val STATIONS: List<Station> = listOf(
        Station(
            id = 1,
            name = "Mercado",
            city = "Porto Alegre",
            latitude = -30.0278,
            longitude = -51.2287,
            connections = listOf("Centro Histórico", "Catamarã Guaíba", "Linhas de Ônibus Urbanos"),
            isTerminal = true,
            zoneDescription = "Terminal Central de Porto Alegre"
        ),
        Station(
            id = 2,
            name = "Rodoviária",
            city = "Porto Alegre",
            latitude = -30.0234,
            longitude = -51.2185,
            connections = listOf("Terminal Rodoviário Interestadual de Porto Alegre"),
            zoneDescription = "Integração Intermunicipal e Interestadual"
        ),
        Station(
            id = 3,
            name = "São Pedro",
            city = "Porto Alegre",
            latitude = -30.0076,
            longitude = -51.2078,
            connections = listOf("4º Distrito", "Av. São Pedro"),
            zoneDescription = "Polo de Inovação e Gastronomia"
        ),
        Station(
            id = 4,
            name = "Farrapos",
            city = "Porto Alegre",
            latitude = -29.9967,
            longitude = -51.1979,
            connections = listOf("Terminal de Integração Farrapos", "Av. Farrapos"),
            zoneDescription = "Terminal de Integração da Zona Norte"
        ),
        Station(
            id = 5,
            name = "Aeroporto",
            city = "Porto Alegre",
            latitude = -29.9926,
            longitude = -51.1812,
            connections = listOf("Conexão Aeromóvel ao Aeroporto Salgado Filho"),
            zoneDescription = "Acesso Direto ao Terminal Aéreo"
        ),
        Station(
            id = 6,
            name = "Anchieta",
            city = "Porto Alegre",
            latitude = -29.9806,
            longitude = -51.1804,
            connections = listOf("Arena do Grêmio", "Bairro Humaitá / Anchieta"),
            zoneDescription = "Última estação de Porto Alegre"
        ),
        Station(
            id = 7,
            name = "Niterói",
            city = "Canoas",
            latitude = -29.9482,
            longitude = -51.1807,
            connections = listOf("UniRitter Campus Canoas", "Bairro Niterói"),
            zoneDescription = "Entrada Sul de Canoas"
        ),
        Station(
            id = 8,
            name = "Fátima",
            city = "Canoas",
            latitude = -29.9329,
            longitude = -51.1806,
            connections = listOf("Santuário de Fátima", "Bairro Fátima"),
            zoneDescription = "Região Residencial de Canoas"
        ),
        Station(
            id = 9,
            name = "Canoas / La Salle",
            city = "Canoas",
            latitude = -29.9195,
            longitude = -51.1803,
            connections = listOf("Universidade La Salle", "Centro de Canoas", "Canoas Shopping"),
            zoneDescription = "Centro Comercial e Universitário"
        ),
        Station(
            id = 10,
            name = "Mathias Velho",
            city = "Canoas",
            latitude = -29.9048,
            longitude = -51.1807,
            connections = listOf("Terminal de Integração Mathias Velho", "Av. Guilherme Schell"),
            zoneDescription = "Maior Terminal Urbano de Canoas"
        ),
        Station(
            id = 11,
            name = "São Luís / Ulbra",
            city = "Canoas",
            latitude = -29.8893,
            longitude = -51.1805,
            connections = listOf("Universidade ULBRA", "Hospital Universitário"),
            zoneDescription = "Campus Universitário ULBRA"
        ),
        Station(
            id = 12,
            name = "Petrobras",
            city = "Canoas",
            latitude = -29.8732,
            longitude = -51.1809,
            connections = listOf("Refinaria Alberto Pasqualini (REFAP)"),
            zoneDescription = "Polo Industrial Petroquímico"
        ),
        Station(
            id = 13,
            name = "Esteio",
            city = "Esteio",
            latitude = -29.8542,
            longitude = -51.1813,
            connections = listOf("Parque de Exposições Assis Brasil (Expointer)", "Centro de Esteio"),
            zoneDescription = "Palco da Maior Feira Agropecuária da América Latina"
        ),
        Station(
            id = 14,
            name = "Luiz Pasteur",
            city = "Sapucaia do Sul",
            latitude = -29.8398,
            longitude = -51.1683,
            connections = listOf("Hospital Municipal Getúlio Vargas"),
            zoneDescription = "Divisa Esteio / Sapucaia do Sul"
        ),
        Station(
            id = 15,
            name = "Sapucaia",
            city = "Sapucaia do Sul",
            latitude = -29.8272,
            longitude = -51.1472,
            connections = listOf("Centro de Sapucaia do Sul", "Terminal de Ônibus"),
            zoneDescription = "Centro Comercial de Sapucaia"
        ),
        Station(
            id = 16,
            name = "Unisinos",
            city = "São Leopoldo",
            latitude = -29.7915,
            longitude = -51.1479,
            connections = listOf("Universidade do Vale do Rio dos Sinos (Unisinos)", "Tecnosinos"),
            zoneDescription = "Polo Universitário e Tecnológico do Vale dos Sinos"
        ),
        Station(
            id = 17,
            name = "São Leopoldo",
            city = "São Leopoldo",
            latitude = -29.7615,
            longitude = -51.1470,
            connections = listOf("Centro Histórico de São Leopoldo", "Rua Independência"),
            zoneDescription = "Centro Urbano de São Leopoldo"
        ),
        Station(
            id = 18,
            name = "Rio dos Sinos",
            city = "São Leopoldo",
            latitude = -29.7483,
            longitude = -51.1396,
            connections = listOf("Zona Norte de São Leopoldo", "Margens do Rio dos Sinos"),
            zoneDescription = "Transição para Novo Hamburgo"
        ),
        Station(
            id = 19,
            name = "Santo Afonso",
            city = "Novo Hamburgo",
            latitude = -29.7289,
            longitude = -51.1319,
            connections = listOf("Bairro Santo Afonso", "Primeira parada em Novo Hamburgo"),
            zoneDescription = "Entrada Sul de Novo Hamburgo"
        ),
        Station(
            id = 20,
            name = "Industrial",
            city = "Novo Hamburgo",
            latitude = -29.7126,
            longitude = -51.1327,
            connections = listOf("Distrito Industrial", "Bairro Industrial"),
            zoneDescription = "Polo Coureiro-Calçadista e Industrial"
        ),
        Station(
            id = 21,
            name = "Fenac",
            city = "Novo Hamburgo",
            latitude = -29.6974,
            longitude = -51.1332,
            connections = listOf("Pavilhões da FENAC", "Rodoviária de Novo Hamburgo"),
            zoneDescription = "Eventos, Feiras e Terminal Rodoviário"
        ),
        Station(
            id = 22,
            name = "Novo Hamburgo",
            city = "Novo Hamburgo",
            latitude = -29.6841,
            longitude = -51.1306,
            connections = listOf("Centro de Novo Hamburgo", "Shopping Bourbon", "Praça do Imigrante"),
            isTerminal = true,
            zoneDescription = "Terminal Norte da Linha 1"
        )
    )
}
