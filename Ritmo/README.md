# Ritmo

Player de música para Android que toca **somente as músicas baixadas no aparelho**.
Sem internet, sem conta, sem anúncios. Inspirado no Spotify e no YouTube Music.

## O que o app faz

- **Biblioteca** lida direto do aparelho: músicas, álbuns e artistas, com capas
- **Playlists completas**: criar, renomear, excluir, adicionar várias músicas de uma vez,
  **reordenar arrastando**, remover músicas
- **Fila editável**: tocar a seguir, adicionar à fila, arrastar para reordenar, remover
- **Aleatório de verdade** (a fila embaralha e volta à ordem original ao desligar),
  repetir tudo / repetir uma, **timer de sono**
- **Músicas curtidas**, tocadas recentemente, mais tocadas, adicionadas recentemente
- **Busca** por música, artista, álbum e playlist (ignora acentos)
- Controles na **notificação e na tela de bloqueio**, pausa ao tirar o fone
- **Continua de onde parou** ao reabrir o app
- Tema escuro, claro ou automático; tela "Tocando agora" com cores tiradas da capa

## Como gerar o APK

### Opção A: sem instalar nada (GitHub, grátis)

1. Crie uma conta em github.com e um repositório novo (pode ser privado).
2. Envie **todo o conteúdo desta pasta** para o repositório (botão *Add file > Upload files*,
   arrastando os arquivos descompactados; inclua a pasta `.github`).
3. Abra a aba **Actions**. O fluxo *Gerar APK* roda sozinho (leva uns 5 a 10 minutos).
   Se não rodar, escolha *Gerar APK > Run workflow*.
4. Quando terminar, abra a execução e baixe o arquivo **Ritmo-APK** no fim da página.
   Descompacte: dentro está o `app-debug.apk`.
5. Passe o APK para o celular e instale (o Android pede para permitir instalar de fontes desconhecidas).

### Opção B: Android Studio

1. Abra o Android Studio e use *File > Open* na pasta do projeto. Aguarde a sincronização do Gradle.
2. Menu *Build > Build Bundle(s) / APK(s) > Build APK(s)*.
3. O arquivo fica em `app/build/outputs/apk/debug/app-debug.apk`.

## Notas

- Requer Android 8.0 (API 26) ou mais novo.
- Na primeira abertura o app pede acesso aos arquivos de áudio e, no Android 13+, permissão de notificação.
- Filas muito grandes (mais de 1000 músicas) são limitadas a 1000 por vez, por restrição do Android
  na comunicação entre o app e o serviço de reprodução.
- Este projeto foi escrito sem acesso a um ambiente Android para compilar. Se o primeiro build
  acusar algum erro, ele costuma ser pequeno (um import ou um nome de função); é só corrigir e repetir.
