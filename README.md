# PS3 ISO Decryptor

Uma GUI elegante e profissional em Java Swing para descriptografar arquivos de jogos ISO do PlayStation 3 utilizando chaves DKEY. Baseado na eficiente ferramenta [ps3dec](https://github.com/al3xtjames/ps3dec) escrita em Rust.

![PS3 ISO Decryptor App](assets/AppIcon.png)

## Funcionalidades
- **Interface Amigável**: Baseada em UI Swing customizada no estilo "dark-mode" moderno.
- **Identificação Automática do Game ID**: Sistema embutido para descobrir facilmente o ID do Jogo no nome da ISO para buscar online.
- **Integração Descomplicada do ps3dec**: Interface visual por cima da linha de comando complexa.
- **Barra de Progresso Dinâmica**: Simulador e monitoramento de tempo decorrido.
- **Lançador Nativo (macOS)**: É possível empacotar num `.app` nativo sem necessidade de rodar via linha de comando.

## Pré-requisitos
- JDK 11 ou superior
- Instalação [Maven](https://maven.apache.org/) (opcional, mas recomendado)
- A ferramenta CLI original `ps3decrs` copilada para sua plataforma nativa.

## Como Compilar e Rodar

1. **Baixar o código**
   ```bash
   git clone https://github.com/seu-usuario/ps3dec-ui.git
   cd ps3dec-ui
   ```

2. **Compilar (via Maven)**
   ```bash
   mvn clean package
   ```
   Isso irá gerar o executável autossuficiente (fat jar) no diretório `target/ps3dec-ui-2.0.0.jar`.

3. **Executar**
   ```bash
   # Certifique-se de que o ps3decrs está na mesma pasta, ou no PATH do sistema.
   java -jar target/ps3dec-ui-2.0.0.jar
   ```

## Empacotamento macOS (Criando o .app)

Os usuários de Mac podem gerar um pacote de aplicativo `.app` natural que já embute tanto o arquivo JAR compilado quanto o binário nativo `ps3decrs`.
1. Execute o script de apoio raiz:
   ```bash
   bash build_mac_app.sh
   ```
2. O aplicativo nativo montado estará disponível na pasta `dist/PS3Decryptor.app`. Você pode arrastá-lo para a pasta de Aplicativos.

## Créditos
- Core (Binário de Descriptação): [al3xtjames/ps3dec](https://github.com/al3xtjames/ps3dec)
- GUI Frontend: Dalvan Schmitz
