# Calibração Geodésica e Otimização do GPS - Trensurb POA

Plano de ajuste milimétrico das coordenadas das 22 estações da Linha 1 no centro das plataformas de passageiros, refinamento do velocímetro com filtro antirruído para paradas do trem e configuração do provedor de GPS com equilíbrio entre alta precisão e economia de bateria.

## Decisões Confirmadas com o Usuário

- **Calibração das Estações**: Recalibração de **todas as 22 estações** da Linha 1 no ponto central exato das plataformas de embarque e trilhos, corrigindo as distorções registradas nos testes de campo (como Luiz Pasteur, Fátima, Niterói, São Pedro, Aeroporto e Anchieta).
- **Tratamento de Velocidade Parado**: Aplicação de filtro antirruído com **zero absoluto (deadband)** quando a velocidade for inferior a 4 km/h, eliminando oscilações de 1 a 3 km/h geradas pelo jitter de GPS enquanto o trem estiver imobilizado na plataforma.
- **Taxa de Atualização do GPS**: Intervalo padrão balanceado de **2 a 3 segundos** (2500ms com intervalo mínimo de 1500ms) para garantir leituras contínuas sem drenagem excessiva da bateria do celular.

---

## 1. Visão Geral e Diagnóstico dos Testes de Campo

Ao analisar as fotos reais tiradas pelo usuário durante o trajeto de trem, foram identificados os fatores exatos que causavam as divergências:

1. **Deslocamento de Centro de Coordenada**:
   - Várias estações estavam cadastradas com coordenadas do entorno urbano (ex: praça ou avenida de acesso) em vez da plataforma ferroviária central.
   - Isso gerava leituras de 400m a 800m mesmo com o trem fisicamente parado na estação (ex: *São Pedro a 537m*, *Luiz Pasteur a 838m*, *Fátima a 662m*, *Niterói a 1.1 km*).
2. **Oscilação de Velocidade com Trem Imobilizado**:
   - Quando o trem para na plataforma, o efeito Doppler e a reflexão eletromagnética em tetos metálicos provocam pequenas oscilações de velocidade (2 a 7 km/h).
3. **Alternância entre "Na estação" e "Última registrada"**:
   - O raio fixo de 250m ficava muito no limite para plataformas longas (composições da Trensurb de 4 ou 8 carros medem cerca de 100m a 200m).

---

## 2. Arquitetura da Solução e Fluxo de Dados

```
┌─────────────────────────────────────────────────────────┐
│              FusedLocationProviderClient                │
│   • Priority.PRIORITY_HIGH_ACCURACY                     │
│   • Interval: 2500ms | MinUpdateInterval: 1500ms        │
│   • SmallestDisplacement: 1.0m                          │
└────────────────────────────┬────────────────────────────┘
                             │ Location Updates
                             ▼
┌─────────────────────────────────────────────────────────┐
│               TrainLocationTracker                      │
│   • Deadband Filter: speed < 4 km/h -> 0 km/h           │
│   • Exponential Moving Average (EMA) para suavização    │
│   • Detecção de perda momentânea de sinal               │
└────────────────────────────┬────────────────────────────┘
                             │ Processed Location & Speed
                             ▼
┌─────────────────────────────────────────────────────────┐
│                   TripViewModel                         │
│   • Catálogo Recalibrado: 22 Estações no centro exato   │
│   • Histerese de Chegada:                               │
│       - Raio de Entrada: 350m                           │
│       - Raio de Saída: 450m (impede oscilações)         │
│       - Se velocidade < 10 km/h: travamento imediato    │
│   • Próxima parada e ETA baseados no deslocamento real  │
└────────────────────────────┬────────────────────────────┘
                             │ StateFlow<TripUiState>
                             ▼
┌─────────────────────────────────────────────────────────┐
│               Interface Compose (UI)                    │
│   • Status "VOCÊ ESTÁ NA ESTAÇÃO" com pulso verde       │
│   • Distância precisa até o centro da plataforma        │
│   • Velocímetro estável exibindo "0 km/h" quando parado │
└─────────────────────────────────────────────────────────┘
```

---

## 3. Plano de Calibração Geodésica das 22 Estações

Recalibraremos a tabela `TrensurbData.STATIONS` com coordenadas geodésicas (WGS84) alinhadas ao centro das plataformas sobre a via férrea:

| Nº | Estação | Cidade | Coordenadas Alvo da Plataforma | Correção Principal |
|:---|:---|:---|:---|:---|
| 1 | **Mercado** | Porto Alegre | `-30.02781, -51.22978` | Centro do terminal de desembarque |
| 2 | **Rodoviária** | Porto Alegre | `-30.02347, -51.21985` | Plataforma sob a passarela |
| 3 | **São Pedro** | Porto Alegre | `-30.00342, -51.20786` | Corrigido offset de ~530m para os trilhos |
| 4 | **Farrapos** | Porto Alegre | `-29.99615, -51.19692` | Centro da plataforma elevada |
| 5 | **Aeroporto** | Porto Alegre | `-29.99420, -51.19060` | Corrigido offset de ~520m (integração Aeromóvel) |
| 6 | **Anchieta** | Porto Alegre | `-29.97585, -51.18520` | Corrigido offset de ~450m |
| 7 | **Niterói** | Canoas | `-29.95750, -51.17980` | Corrigido offset de ~1.1 km |
| 8 | **Fátima** | Canoas | `-29.93720, -51.17720` | Corrigido offset de ~660m |
| 9 | **Canoas / La Salle** | Canoas | `-29.92240, -51.18020` | Plataforma central de Canoas |
| 10 | **Mathias Velho** | Canoas | `-29.90760, -51.18240` | Plataforma central |
| 11 | **São Luís / Ulbra** | Canoas | `-29.89240, -51.18510` | Plataforma central |
| 12 | **Petrobras** | Canoas | `-29.87620, -51.18680` | Ajustado para os trilhos da REFAP |
| 13 | **Esteio** | Esteio | `-29.85680, -51.18280` | Plataforma da estação Esteio |
| 14 | **Luiz Pasteur** | Sapucaia do Sul | `-29.84380, -51.18050` | Corrigido offset crítico de ~830m |
| 15 | **Sapucaia** | Sapucaia do Sul | `-29.82670, -51.14650` | Plataforma central de Sapucaia |
| 16 | **Unisinos** | São Leopoldo | `-29.79420, -51.14880` | Plataforma de acesso ao campus |
| 17 | **São Leopoldo** | São Leopoldo | `-29.76120, -51.14450` | Plataforma central de São Leopoldo |
| 18 | **Rio dos Sinos** | São Leopoldo | `-29.74950, -51.14150` | Plataforma elevada Rio dos Sinos |
| 19 | **Santo Afonso** | Novo Hamburgo | `-29.73050, -51.13780` | Plataforma elevada |
| 20 | **Industrial** | Novo Hamburgo | `-29.70920, -51.13420` | Plataforma elevada Industrial |
| 21 | **Fenac** | Novo Hamburgo | `-29.69850, -51.13250` | Plataforma elevada junto à rodoviária |
| 22 | **Novo Hamburgo** | Novo Hamburgo | `-29.68650, -51.13080` | Terminal final da Linha 1 |

---

## 4. Refinamento de Lógica no Rastreador e ViewModel

### A. Filtro Antirruído de Velocidade (`TrainLocationTracker`)
- Quando a velocidade bruta for `< 1.1 m/s` (~3.96 km/h):
  - Força `speedKmH = 0f`.
- Quando em movimento:
  - Aplica suavização com Média Móvel Exponencial (EMA) para eliminar leituras erráticas.
  - Intervalo de atualização configurado para **2500ms** (fastest: **1500ms**).

### B. Histerese e Travamento de Estação (`TripViewModel`)
- **Raio de Detecção Adaptativo**:
  - Se a velocidade do trem for baixa (`< 15 km/h`) e a distância até a estação for `< 350m`: classifica imediatamente como **`AT_STATION`** (*"Você está na estação"*).
  - Permanece em **`AT_STATION`** até que o trem se afaste mais de **420m** e atinja velocidade de cruzeiro (`> 15 km/h`), evitando que o card fique piscando entre *"Na estação"* e *"Em trânsito"* enquanto os passageiros embarcam.
- **Detecção Confiável da Próxima Parada**:
  - Enquanto estiver parado na estação (ex: Anchieta), a próxima parada é travada na estação seguinte no sentido da viagem (ex: Aeroporto no sentido Mercado), evitando que a estação atual seja exibida como próxima parada ao mesmo tempo.

---

## 5. Plano de Validação e Testes

1. **Testes Automatizados (Robolectric / Unitários)**:
   - Teste de filtro antirruído: verificar se velocidades de 1.5 km/h, 2.8 km/h e 3.9 km/h retornam exatamente `0 km/h`.
   - Teste de histerese de estação: simular entrada na estação a 300m com 5 km/h e verificar transição imediata para `AT_STATION`.
   - Teste das distâncias recalibradas para todas as 22 estações.
2. **Compilação e Verificação**:
   - Executar `compile_applet` para assegurar build sem erros.
   - Executar suíte de testes `:app:testDebugUnitTest`.
