# Trensurb POA - Guia de Estações em Tempo Real por GPS

Aplicativo Android moderno e acessível em Jetpack Compose que acompanha a viagem do passageiro na Linha 1 da Trensurb (Mercado ↔ Novo Hamburgo), identificando via GPS a estação atual, a próxima estação e emitindo alertas sonoros e táteis de desembarque.

### User Review & Critical Decisions

> [!IMPORTANT]
> **Decisões Confirmadas com o Usuário:**
> - **Fluxo de Viagem**: Seleção de estação de destino com cálculo de estações restantes e alerta automático sonoro/vibração ao se aproximar.
> - **Estilo Visual**: Painel digital estilo metrô contemporâneo com HUD de bordo e diagrama linear interativo de todas as 22 estações.
> - **Fonte de Localização**: Rastreamento 100% via GPS real do dispositivo (usando `FusedLocationProviderClient` / `LocationManager` com filtro de proximidade geodésica).

- **Tratamento de Percurso / Direção do Trem**: A direção da viagem (sentido Mercado ou sentido Novo Hamburgo) é inferida dinamicamente pela sequência de coordenadas e velocidade do GPS ou confirmada ao escolher a estação de destino.
- **Alertas de Desembarque**: Disparo de vibração e sinal sonoro sutil quando a distância até a estação de destino for inferior a 450 metros ou na desaceleração para a plataforma.

---

### 1. Overview & Core Concept

- **O Que Faz**: Detecta com precisão a posição do trem ao longo dos 43,8 km da Linha 1 da Trensurb. Exibe em destaque a estação em que o trem está (ou da qual acabou de sair), a próxima estação, a distância restante, o tempo estimado e o progresso em um mapa linear estilo painel metroviário.
- **Público-Alvo**: Usuários do Trensurb da Região Metropolitana de Porto Alegre (Porto Alegre, Canoas, Esteio, Sapucaia do Sul, São Leopoldo e Novo Hamburgo) que viajam em trens cheios, com fones de ouvido ou sem aviso sonoro claro na cabine.
- **Valor Principal**: Paz de espírito durante o trajeto, garantindo que o passageiro nunca mais perca a sua estação de descida.

---

### 2. User Experience & Visual Design

#### Fluxo Principal do Usuário
1. **Abertura & Permissão**: Boas-vindas rápidas com solicitação clara da permissão de localização (`ACCESS_FINE_LOCATION`).
2. **Seleção de Destino (Opcional ou Guiada)**:
   - O passageiro pode simplesmente abrir o app e ver onde está;
   - Ou tocar em "Definir Destino" e escolher a estação final para ativar o alarme de desembarque.
3. **Tela Principal de Viagem (HUD Metrô)**:
   - **Cartão Superior de Destaque**: Estação atual com badge de status ("Na estação" ou "A caminho de..."), sentido da linha (ex: "Sentido Novo Hamburgo") e velocidade em km/h.
   - **Próxima Estação**: Card com nome grande, tempo estimado, conexões (ex: Integração Aeromóvel no Aeroporto, Rodoviária, Universidades Unisinos/Ulbra).
   - **Diagrama Linear da Linha**: Visualizador horizontal e vertical scrollável com as 22 estações, marcando estações já percorridas, estação atual pulsante e destino destacado.
   - **Alerta de Chegada**: Modal / banner com vibração e sinal sonoro "Atenção: Próxima parada é o seu destino!".

#### Identidade Visual & Tema
- **Vibe**: Metroviário premium, limpo, de alto contraste para leitura rápida em movimento sob luz solar ou dentro do vagão.
- **Paleta de Cores**:
  - *Primária*: Azul Trensurb clássico (`#005CA9` / `#1E88E5`) com verde de sinalização ferroviária (`#00A859`).
  - *Acentos*: Âmbar alerta (`#FFA000`) para a próxima parada e Magenta/Laranja para o destino selecionado.
  - *Superfícies*: Cinza escuro / grafite profundo no modo escuro e superfícies limpas M3 no modo claro, evitando reflexos no vidro do trem.
- **Tipografia**: Hierarquia clara com números e nomes de estações em peso Bold/SemiBold para leitura instantânea a distância.

---

### 3. Key Product Decisions & Trade-Offs

- **Algoritmo de Detecção de Estação**:
  - *Abordagem*: Cálculo da distância geodésica (Haversine) contra a base de dados exata das 22 estações oficiais da Trensurb.
  - *Zona de Estação*: Raio de 250m = "Na estação". Raio maior com vetor de deslocamento = "Em trânsito para a próxima".
  - *Por quê*: Garante estabilidade contra pequenos desvios do sinal GPS dentro da estrutura metálica do trem, sem disparar falsos positivos.
- **Gerenciamento de Energia e GPS**:
  - *Abordagem*: Intervalo balanceado de 3 a 5 segundos de leitura quando em viagem ativa, e suspensão inteligente quando o app não estiver em uso.
  - *Por quê*: Preserva a bateria do smartphone durante deslocamentos longos (a viagem completa leva cerca de 53 minutos).
- **Feedback Háptico e Sonoro**:
  - *Abordagem*: Uso do `Vibrator` / `VibrationEffect` do sistema e `ToneGenerator` ou `MediaPlayer` nativo para aviso auditivo e tátil sem dependências externas pesadas.

---

### 4. Technical Architecture & Data Strategy

#### Diagrama de Arquitetura do Sistema

```
┌─────────────────────────────────────────────────────────────┐
│                      Trensurb POA App                       │
├─────────────────────────────────────────────────────────────┤
│  UI Layer (Jetpack Compose M3)                              │
│  ┌───────────────────────┐  ┌────────────────────────────┐  │
│  │ MainHUDStationCard    │  │ NextStationBanner          │  │
│  │ (Atual + Sentido)     │  │ (Distância + Conexões)     │  │
│  └───────────────────────┘  └────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ TrainLineDiagramView (Diagrama Linear das 22 Estações)│  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────┐  ┌────────────────────────────┐  │
│  │ DestinationSelector   │  │ ArrivalAlertOverlay        │  │
│  │ (BottomSheet/Dialog)  │  │ (Vibração + Som)           │  │
│  └───────────────────────┘  └────────────────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│  State Management & ViewModel                               │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ TrainTripViewModel                                    │  │
│  │ - State: currentStation, nextStation, destination,    │  │
│  │   direction, speedKmH, distanceToNextMeters, alertFired│  │
│  └───────────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│  Services & Data Layer                                      │
│  ┌───────────────────────┐  ┌────────────────────────────┐  │
│  │ TrensurbStationsRepo  │  │ LocationTrackerService     │  │
│  │ (22 Estações, Coords, │  │ (FusedLocationProvider /   │  │
│  │  Cidades, Conexões)   │  │  LocationManager Listener) │  │
│  └───────────────────────┘  └────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ NotificationAlertHelper (VibrationEffect + Tone)      │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

#### Catálogo Oficial das 22 Estações da Linha 1 (Trensurb)
1. **Mercado** (Porto Alegre) - Centro Histórico / Barco Catamarã
2. **Rodoviária** (Porto Alegre) - Terminal Rodoviário Interestadual
3. **São Pedro** (Porto Alegre) - 4° Distrito
4. **Farrapos** (Porto Alegre) - Terminal de ônibus zona norte
5. **Aeroporto** (Porto Alegre) - Conexão Aeromóvel ao Aeroporto Salgado Filho
6. **Anchieta** (Porto Alegre) - Arena do Grêmio
7. **Niterói** (Canoas) - UniRitter Canoas
8. **Fátima** (Canoas) - Bairro Fátima
9. **Canoas** (Canoas) - Centro / Unilasalle
10. **Mathias Velho** (Canoas) - Terminal de ônibus Mathias Velho
11. **São Luís** (Canoas) - Universidade ULBRA
12. **Petrobras** (Canoas) - Refinaria Alberto Pasqualini (REFAP)
13. **Esteio** (Esteio) - Parque de Exposições Assis Brasil (Expointer)
14. **Luiz Pasteur** (Sapucaia do Sul) - Divisa Esteio/Sapucaia
15. **Sapucaia** (Sapucaia do Sul) - Centro de Sapucaia do Sul
16. **Unisinos** (São Leopoldo) - Universidade do Vale do Rio dos Sinos
17. **São Leopoldo** (São Leopoldo) - Centro de São Leopoldo
18. **Rio dos Sinos** (São Leopoldo) - Zona Norte de São Leopoldo
19. **Santo Afonso** (Novo Hamburgo) - Bairro Santo Afonso
20. **Industrial** (Novo Hamburgo) - Distrito Industrial
21. **Fenac** (Novo Hamburgo) - Rodoviária de Novo Hamburgo e Pavilhões Fenac
22. **Novo Hamburgo** (Novo Hamburgo) - Terminal Central de Novo Hamburgo
