# Trensurb POA - Acompanhamento da Linha 1 em Tempo Real

Aplicativo Android desenvolvido em Kotlin e Jetpack Compose para monitoramento inteligente de viagens na Linha 1 da Trensurb, cobrindo o trajeto entre Porto Alegre e Novo Hamburgo.

---

## Por que este projeto foi criado?

Quem utiliza o transporte sobre trilhos no dia a dia conhece a rotina: colocar fones de ouvido, ler um livro ou simplesmente relaxar durante o trajeto. No entanto, essa comodidade traz um desafio comum: o receio de passar do ponto ou a necessidade constante de olhar pelas janelas e conferir os letreiros para saber qual é a estação atual.

O Trensurb POA nasceu para resolver essa dor com simplicidade e tecnologia. O objetivo é oferecer a cada passageiro um copiloto de viagem confiável, que funciona de forma autônoma no bolso, mantendo as informações essenciais visíveis diretamente na tela de bloqueio do celular e avisando o momento exato de desembarcar por som e vibração.

---

## Como o aplicativo funciona?

O aplicativo se baseia na leitura de dados dos sensores de satélite (GPS, GLONASS e Galileo) do smartphone, combinando modelos matemáticos de suavização com uma base geodésica precisa das 22 estações da Linha 1.

### 1. Detecção automática de onde você está
Ao iniciar a viagem, o aplicativo identifica a sua posição e calcula a distância até as plataformas. Quando o trem para em uma estação, o aplicativo reconhece o estado e indica se você está embarcado ou em deslocamento entre paradas.

### 2. Cálculo de tempo estimado (ETA) e velocidade
Com base na velocidade real do trem e na distância restante pela ferrovia, o sistema calcula uma estimativa confiável de quantos minutos faltam para alcançar cada estação do trajeto.

### 3. Acompanhamento na tela de bloqueio (Segundo Plano)
Você não precisa manter a tela do telefone acesa nem o aplicativo aberto consumindo bateria. Ao acionar o modo de segundo plano, um serviço do Android mantém o rastreamento ativo e fixa uma notificação pública na tela de bloqueio, atualizando a estação atual, a próxima parada e a velocidade a cada trecho percorrido.

### 4. Alerta de desembarque em duas etapas
Ao escolher a sua estação de destino, o aplicativo gerencia dois alertas:
- Alerta de aproximação: disparado quando o trem alcança a estação anterior ou entra no raio de aproximação do destino, sugerindo a preparação para saída.
- Alerta de chegada: disparado no momento em que o trem encosta na plataforma de destino, com vibração diferenciada, aviso sonoro e acendimento automático da tela do aparelho para chamar a sua atenção mesmo com o telefone guardado.

### 5. Diagrama linear do metrô
Um mapa esquemático interativo exibe todas as 22 estações em ordem sequencial, com indicação visual de estações já percorridas, posição atual, próxima parada e destino final, além de suportar modos visuais com alto contraste.

---

## Soluções técnicas de engenharia implementadas

Para garantir precisão e estabilidade durante o uso prático em um trem real em movimento, foram implementadas as seguintes soluções técnicas:

- Calibração georreferenciada no centro das plataformas: as coordenadas de todas as 22 estações foram ajustadas exatamente no eixo central das plataformas ferroviárias sobre os trilhos.
- Filtro antirruído de velocidade (Deadband): leituras de velocidade inferiores a 4 km/h geradas por oscilações de satélites são tratadas como zero absoluto, evitando que o aplicativo mostre pequenas velocidades irreais quando o trem está completamente parado.
- Suavização por Média Móvel Exponencial (EMA): a velocidade informada no painel passa por um amortecimento para evitar picos abruptos causados por túneis ou estruturas metálicas de estações.
- Histerese espacial de entrada e saída: para evitar alternâncias falsas entre "Na Estação" e "Em Trânsito" durante o embarque e desembarque, a entrada na estação ocorre a 350 metros (com baixa velocidade) ou 250 metros, e a saída somente é confirmada após o trem superar 350 a 420 metros em velocidade de deslocamento.
- Serviço de primeiro plano (Foreground Service): utilização de serviço vinculado a notificação persistente com tipo location, garantindo compatibilidade com políticas de energia do Android 10 ao Android 14+.
- WakeLock de tela: acionamento pontual do gerenciador de energia do dispositivo para iluminar o display na tela de bloqueio quando os alertas de chegada são emitidos.

---

## Cobertura da Linha 1 (22 Estações)

O sistema cobre integralmente a extensão da Linha 1 da Trensurb (43,8 km de via):

| Número | Estação | Município | Característica |
|:---|:---|:---|:---|
| 01 | Mercado | Porto Alegre | Terminal Sul e conexão Centro Histórico |
| 02 | Rodoviária | Porto Alegre | Integração com Terminal Rodoviário |
| 03 | São Pedro | Porto Alegre | Bairro São Geraldo / 4º Distrito |
| 04 | Farrapos | Porto Alegre | Integração com corredores de ônibus |
| 05 | Aeroporto | Porto Alegre | Conexão com Aeromóvel e Aeroporto Salgado Filho |
| 06 | Anchieta | Porto Alegre | Limite norte da capital |
| 07 | Niterói | Canoas | Entrada de Canoas |
| 08 | Fátima | Canoas | Bairro Fátima |
| 09 | Canoas / La Salle | Canoas | Região central e universitária |
| 10 | Mathias Velho | Canoas | Polo comercial e residencial |
| 11 | São Luís / Ulbra | Canoas | Acesso ao campus da universidade |
| 12 | Petrobras | Canoas | Região industrial |
| 13 | Esteio | Esteio | Acesso ao Parque de Exposições Assis Brasil |
| 14 | Luiz Pasteur | Esteio / Sapucaia | Divisa municipal |
| 15 | Sapucaia | Sapucaia do Sul | Centro de Sapucaia |
| 16 | Unisinos | São Leopoldo | Campus universitário |
| 17 | São Leopoldo | São Leopoldo | Centro histórico e comercial |
| 18 | Rio dos Sinos | São Leopoldo | Travessia do Rio dos Sinos |
| 19 | Santo Afonso | Novo Hamburgo | Entrada de Novo Hamburgo |
| 20 | Industrial | Novo Hamburgo | Setor moveleiro e industrial |
| 21 | Fenac | Novo Hamburgo | Integração com pavilhões e rodoviária |
| 22 | Novo Hamburgo | Novo Hamburgo | Terminal Norte e centro comercial |

---

## Arquitetura e Tecnologias

- Linguagem: Kotlin
- Interface: Jetpack Compose com Material Design 3
- Arquitetura: MVVM (Model-View-ViewModel) com StateFlow e Coroutines
- Localização: Google Play Services Fused Location Provider Client
- Serviços em Segundo Plano: Foreground Service com NotificationCompat
- Persistência de Preferências: SharedPreferences para configurações de som, vibração e tema
- Testes Automatizados: Robolectric e JUnit 4 para validação de regras de negócio e histerese

---

## Como compilar e executar

### Pré-requisitos
- Android Studio Hedgehog ou mais recente
- JDK 17 ou superior
- Dispositivo Android físico ou emulador com suporte a Google Play Services (API 24 ou superior)

### Passos de compilação
1. Clone este repositório:
   ```bash
   git clone https://github.com/seu-usuario/trensurb-poa.git
   ```
2. Abra a pasta do projeto no Android Studio.
3. Aguarde o sincronismo do Gradle.
4. Para compilar e rodar os testes unitários:
   ```bash
   gradle :app:testDebugUnitTest
   ```
5. Para gerar o pacote APK de depuração:
   ```bash
   gradle :app:assembleDebug
   ```
   O arquivo APK gerado ficará disponível em `app/build/outputs/apk/debug/app-debug.apk`.
