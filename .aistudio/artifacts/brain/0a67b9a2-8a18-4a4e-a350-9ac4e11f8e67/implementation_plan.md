# Plano de Engenharia: Confiabilidade de Alarmes e Usabilidade no Bolso

Plano de aprimoramento da arquitetura do aplicativo Trensurb POA, focado em transformar o app no copiloto definitivo para o passageiro diário do trem, assegurando que o alarme desperte o passageiro com precisão absoluta mesmo com o aparelho guardado no bolso ou com fones de ouvido, e garantindo resiliência operacional caso o GPS perca satélites em coberturas de concreto e túneis através de projeção temporal (Dead Reckoning).

---

### Decisões Confirmadas com o Usuário

- **Prioridade Estratégica**: Usabilidade e confiabilidade máxima dos alarmes no bolso.
- **Resiliência de Sinal**: Projeção estimada por tempo (Dead Reckoning ferroviário) até o restabelecimento do sinal de satélite.
- **Configuração de Energia**: Manter comportamento padrão nativo do Android sem telas adicionais de aviso de fabricante.

---

## 1. Visão Geral e Conceito

O objetivo é eliminar qualquer chance de o passageiro perder a estação de desembarque, mesmo em cenários adversos típicos de transporte público: passageiro dormindo, fones de ouvido com cancelamento de ruído, celular dentro da mochila ou perda momentânea de sinal de satélite em áreas cobertas de estações centrais.

```
┌─────────────────────────────────────────────────────────────┐
│                   PIPELINE DE CONFIABILIDADE                │
├─────────────────┬───────────────────┬───────────────────────┤
│  Entrada GPS    │   Filtro & Perda  │  Alarme de Desembarque│
│  - Satélites OK │   - Histerese OK  │  - Pulso Contínuo     │
│  - Perda Fix ───┼─► - Dead Reckoning│  - Audio USAGE_ALARM  │
│                     (Projeção tempo)│  - WakeLock Display   │
│                                     │  - Desarme Tátil      │
└─────────────────┴───────────────────┴───────────────────────┘
```

---

## 2. Experiência do Usuário e Design de Interação

### 2.1 Alarme de Desembarque Contínuo com Desarme Seguro
- **Problema resolvido**: O alarme anterior tocava apenas uma vez. Se o passageiro estivesse em sono profundo, um único bip de 700ms poderia passar despercebido.
- **Nova experiência**:
  - Ao entrar na plataforma de destino, o alarme entra em **modo de atenção contínua**: pulso vibratório rítmico e aviso sonoro intermitente a cada 3 segundos.
  - A tela de bloqueio acende com notificação persistente de alta prioridade contendo o botão de ação rápida: **"Desembarquei / Desligar Alarme"**.
  - O alarme cessa imediatamente ao tocar no botão de desarme ou ao afastar-se da estação.

### 2.2 Projeção Temporal Inteligente (Dead Reckoning Ferroviário)
- **Problema resolvido**: Em coberturas metálicas espessas (ex.: Mercado, São Pedro ou dias de tempestade severa), o sinal de GPS pode degradar por 1 a 2 minutos.
- **Nova experiência**:
  - Se o GPS não emitir leitura válida por mais de 15 segundos em movimento, o sistema ativa suavemente o estado **"Estimando por tempo decorrido"**.
  - Com base na velocidade média cadastrada no trecho (~45 km/h, cerca de 2,5 minutos entre estações), o relógio avança a previsão sem deixar o usuário sem retorno.
  - Quando o sinal de satélite é restabelecido, a posição é reconciliada instantaneamente sem saltos bruscos.

### 2.3 Atalhos de Destino Rápido (Casa & Trabalho)
- No card de destino, inclusão de 2 chips de acesso rápido customizáveis pelo passageiro:
  - Exemplo: "Trabalho (Mercado)" e "Casa (Unisinos)".
  - Um único toque define o destino e arma o alerta para a viagem do dia, sem necessidade de percorrer o seletor com 22 estações.

---

## 3. Decisões Técnicas e Trade-offs

### Decisão 1: Alarme com Categoria USAGE_ALARM e Canal Crítico
- **Abordagem**: Utilizar `AudioAttributes.USAGE_ALARM` no canal de notificação e no gerador sonoro do `AlertHelper`.
- **Por que**: Notificações convencionais (`USAGE_NOTIFICATION`) podem ser abafadas por modos de silêncio ou fones de ouvido bluetooth. O canal de alarme garante prioridade de entrega no bolso.
- **Alternativa descartada**: Disparar alarme de mídia contínuo (MediaPlayer com arquivo .mp3 pesado), descartado para manter o APK leve e evitar consumo desnecessário de memória.

### Decisão 2: Dead Reckoning Baseado em Tabela de Tempos Inter-Estações
- **Abordagem**: Modelar a matriz de tempo nominal de travessia entre paradas adjacentes (média de 140 a 180 segundos por trecho).
- **Por que**: A ferrovia é um ambiente linear unidirecional perfeito para projeção temporal determinística. Se o trem partiu de Rodoviária e perdeu sinal há 120 segundos, ele está necessariamente desacelerando para São Pedro.

---

## 4. Arquitetura e Componentes Técnicos

```
┌─────────────────────────────────────────────────────────────┐
│                 ARQUITETURA DE DADOS & FLUXO                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  [FusedLocationProviderClient]                              │
│              │                                              │
│              ▼                                              │
│  [TrainLocationTracker] ──(Perda > 15s)──► [DeadReckoner]   │
│              │                                  │           │
│              ▼                                  │           │
│        [TripViewModel] ◄────────────────────────┘           │
│         │          │                                        │
│         │          ▼                                        │
│         │     [TrainTrackingService] (Lockscreen Notif)     │
│         ▼                                                   │
│   [AlertHelper] ──► WakeLock + USAGE_ALARM + Loop Háptico   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Componentes Envolvidos:
1. **`com.example.util.AlertHelper`**:
   - Loop de vibração contínuo com timeout de segurança (30 segundos máximos).
   - Ação de desarme (`dismissAlert()`) via Intent de Notificação e UI.
   - Atributos sonoros configurados para canal de alarme com toque nítido.
2. **`com.example.location.TrainLocationTracker`**:
   - Detecção de perda de sinal de GPS após limiar configurado.
   - Projeção de posição por dead reckoning linear baseado no último sentido confirmado.
3. **`com.example.viewmodel.TripViewModel`**:
   - Armazenamento em preferências locais de até 2 destinos rápidos (Casa e Trabalho).
   - Gerenciamento do estado de desarme do alarme ativo.
4. **`com.example.ui.components.DestinationCard`**:
   - Inclusão dos botões de acesso rápido aos destinos frequentes.
   - Botão para "Testar Alarme no Bolso" com contagem regressiva de 5 segundos.
