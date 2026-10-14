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
            latitude = -30.026242091097117,
            longitude = -51.228178651357844,
            connections = listOf("Centro Histórico", "Catamarã Guaíba", "Linhas de Ônibus Urbanos"),
            isTerminal = true,
            zoneDescription = "Terminal Central de Porto Alegre"
        ),
        Station(
            id = 2,
            name = "Rodoviária",
            city = "Porto Alegre",
            latitude = -30.02184971767209,
            longitude = -51.21917009168575,
            connections = listOf("Terminal Rodoviário Interestadual de Porto Alegre"),
            zoneDescription = "Integração Intermunicipal e Interestadual"
        ),
        Station(
            id = 3,
            name = "São Pedro",
            city = "Porto Alegre",
            latitude = -30.00630152567084,
            longitude = -51.209375764620866,
            connections = listOf("4º Distrito", "Av. São Pedro"),
            zoneDescription = "Polo de Inovação e Gastronomia"
        ),
        Station(
            id = 4,
            name = "Farrapos",
            city = "Porto Alegre",
            latitude = -29.997304257992695,
            longitude = -51.197488242186886,
            connections = listOf("Terminal de Integração Farrapos", "Av. Farrapos"),
            zoneDescription = "Terminal de Integração da Zona Norte"
        ),
        Station(
            id = 5,
            name = "Aeroporto",
            city = "Porto Alegre",
            latitude = -29.987623741108635,
            longitude = -51.18284933306111,
            connections = listOf("Conexão Aeromóvel ao Aeroporto Salgado Filho"),
            zoneDescription = "Acesso Direto ao Terminal Aéreo"
        ),
        Station(
            id = 6,
            name = "Anchieta",
            city = "Porto Alegre",
            latitude = -29.977225536671963,
            longitude = -51.17904363379267,
            connections = listOf("Arena do Grêmio", "Bairro Humaitá / Anchieta"),
            zoneDescription = "Última estação de Porto Alegre"
        ),
        Station(
            id = 7,
            name = "Niterói",
            city = "Canoas",
            latitude = -29.955523496795724,
            longitude = -51.17655908364142,
            connections = listOf("UniRitter Campus Canoas", "Bairro Niterói"),
            zoneDescription = "Entrada Sul de Canoas"
        ),
        Station(
            id = 8,
            name = "Fátima",
            city = "Canoas",
            latitude = -29.93888828747931,
            longitude = -51.17736382827113,
            connections = listOf("Santuário de Fátima", "Bairro Fátima"),
            zoneDescription = "Região Residencial de Canoas"
        ),
        Station(
            id = 9,
            name = "Canoas / La Salle",
            city = "Canoas",
            latitude = -29.918808169260537,
            longitude = -51.18214548567415,
            connections = listOf("Universidade La Salle", "Centro de Canoas", "Canoas Shopping"),
            zoneDescription = "Centro Comercial e Universitário"
        ),
        Station(
            id = 10,
            name = "Mathias Velho",
            city = "Canoas",
            latitude = -29.90380257863693,
            longitude = -51.17883570066543,
            connections = listOf("Terminal de Integração Mathias Velho", "Av. Guilherme Schell"),
            zoneDescription = "Maior Terminal Urbano de Canoas"
        ),
        Station(
            id = 11,
            name = "São Luís / Ulbra",
            city = "Canoas",
            latitude = -29.887588330615024,
            longitude = -51.17953723679188,
            connections = listOf("Universidade ULBRA", "Hospital Universitário"),
            zoneDescription = "Campus Universitário ULBRA"
        ),
        Station(
            id = 12,
            name = "Petrobras",
            city = "Canoas",
            latitude = -29.87670709732483,
            longitude = -51.18087804506136,
            connections = listOf("Refinaria Alberto Pasqualini (REFAP)"),
            zoneDescription = "Polo Industrial Petroquímico"
        ),
        Station(
            id = 13,
            name = "Esteio",
            city = "Esteio",
            latitude = -29.852099208041817,
            longitude = -51.179355848572115,
            connections = listOf("Parque de Exposições Assis Brasil (Expointer)", "Centro de Esteio"),
            zoneDescription = "Palco da Maior Feira Agropecuária da América Latina"
        ),
        Station(
            id = 14,
            name = "Luiz Pasteur",
            city = "Sapucaia do Sul",
            latitude = -29.8328774936885,
            longitude = -51.16565150200816,
            connections = listOf("Hospital Municipal Getúlio Vargas"),
            zoneDescription = "Divisa Esteio / Sapucaia do Sul"
        ),
        Station(
            id = 15,
            name = "Sapucaia",
            city = "Sapucaia do Sul",
            latitude = -29.823083118320454,
            longitude = -51.14897182904473,
            connections = listOf("Centro de Sapucaia do Sul", "Terminal de Ônibus"),
            zoneDescription = "Centro Comercial de Sapucaia"
        ),
        Station(
            id = 16,
            name = "Unisinos",
            city = "São Leopoldo",
            latitude = -29.786845558569496,
            longitude = -51.14054897305165,
            connections = listOf("Universidade do Vale do Rio dos Sinos (Unisinos)", "Tecnosinos"),
            zoneDescription = "Polo Universitário e Tecnológico do Vale dos Sinos"
        ),
        Station(
            id = 17,
            name = "São Leopoldo",
            city = "São Leopoldo",
            latitude = -29.769234797708346,
            longitude = -51.141260901692625,
            connections = listOf("Centro Histórico de São Leopoldo", "Rua Independência"),
            zoneDescription = "Centro Urbano de São Leopoldo"
        ),
        Station(
            id = 18,
            name = "Rio dos Sinos",
            city = "São Leopoldo",
            latitude = -29.748969463758844,
            longitude = -51.145110098173745,
            connections = listOf("Zona Norte de São Leopoldo", "Margens do Rio dos Sinos"),
            zoneDescription = "Transição para Novo Hamburgo"
        ),
        Station(
            id = 19,
            name = "Santo Afonso",
            city = "Novo Hamburgo",
            latitude = -29.72983810526672,
            longitude = -51.14034623597428,
            connections = listOf("Bairro Santo Afonso", "Primeira parada em Novo Hamburgo"),
            zoneDescription = "Entrada Sul de Novo Hamburgo"
        ),
        Station(
            id = 20,
            name = "Industrial",
            city = "Novo Hamburgo",
            latitude = -29.71576459807928,
            longitude = -51.13487016042341,
            connections = listOf("Distrito Industrial", "Bairro Industrial"),
            zoneDescription = "Polo Coureiro-Calçadista e Industrial"
        ),
        Station(
            id = 21,
            name = "Fenac",
            city = "Novo Hamburgo",
            latitude = -29.700949936078025,
            longitude = -51.1349760107716,
            connections = listOf("Pavilhões da FENAC", "Rodoviária de Novo Hamburgo"),
            zoneDescription = "Eventos, Feiras e Terminal Rodoviário"
        ),
        Station(
            id = 22,
            name = "Novo Hamburgo",
            city = "Novo Hamburgo",
            latitude = -29.686570489409593,
            longitude = -51.132920377377495,
            connections = listOf("Centro de Novo Hamburgo", "Shopping Bourbon", "Praça do Imigrante"),
            isTerminal = true,
            zoneDescription = "Terminal Norte da Linha 1"
        )
    )
}
