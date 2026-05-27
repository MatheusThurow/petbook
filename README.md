# PetBook

Aplicativo Android em `Java` com proposta de rede social para:

- animais perdidos
- animais para adocao
- feiras de adocao

Pacote Android atual:

- `com.petbook.app`

## Stack

- `Java`
- `Android Studio`
- `XML`
- `Firebase Firestore`
- `Firebase Auth`
- `Firebase Cloud Messaging`
- `SQLite` como fallback legado em alguns fluxos

## Estado atual do projeto

O app hoje funciona em modo `Firebase-first`.

Isso significa que os fluxos principais usam Firebase como base:

- autenticacao
- login com Google
- cadastro
- feed
- posts
- comentarios
- chat
- notificacoes
- perfil
- empresa
- feira de adocao

A API local continua no projeto, mas nao e o fluxo principal por padrao.

Configuracao atual:

- `app/src/main/res/values/bools.xml`
  - `use_remote_api = false`
  - `use_firebase_chat = true`

## Funcionalidades atuais

- login unico por e-mail e senha
- login com Google
- cadastro para:
  - pessoa fisica
  - empresa
- conclusao de cadastro para novos usuarios vindos do Google
- recuperacao de senha por e-mail
- alteracao de senha dentro do perfil
- feed com filtros
- posts de:
  - animal perdido
  - animal para adocao
  - feira de adocao
- comentarios publicos em posts de perdido
- respostas encadeadas em comentarios
- contato direto por chat em posts de adocao e feira
- notificacoes no app
- exclusao individual de notificacoes
- limpar todas as notificacoes
- dark mode
- navegacao inferior fixa
- swipe horizontal entre abas principais

## Regras principais do produto

- o login nao separa empresa e pessoa fisica
- o tipo da conta e escolhido no cadastro
- pessoa fisica pode criar:
  - post de animal perdido
  - post de adocao
- empresa pode criar:
  - post de animal perdido
  - post de adocao
  - post de feira
- o post de feira aceita varios animais em um unico post
- apenas o autor pode editar ou apagar o proprio post
- em post de perdido:
  - outros usuarios veem `Comentar`
  - o autor ve `Comentarios`
- em post de adocao ou feira:
  - outros usuarios veem `Entrar em contato`
  - o autor ve `Comentarios`

## Fluxo atual

### 1. Login

- o usuario entra com e-mail e senha
- o app identifica automaticamente o tipo da conta
- no login com Google:
  - autentica no Firebase Auth
  - procura o usuario no Firestore
  - se a conta ja existir, entra normalmente
  - se a conta nao existir, redireciona para concluir cadastro

### 2. Conclusao de cadastro via Google

Quando o usuario entra com Google e ainda nao tem conta no sistema:

- vai para a tela de cadastro em modo de conclusao
- nome e e-mail vindos do Google entram preenchidos
- o e-mail fica bloqueado
- o usuario escolhe:
  - pessoa fisica
  - empresa
- informa os dados obrigatorios do tipo escolhido
- cria senha
- confirma a senha
- o cadastro so e efetivado no final dessa etapa

### 3. Cadastro manual

- o usuario escolhe o tipo da conta
- pessoa fisica pode seguir direto para o feed
- empresa pode seguir para completar os dados institucionais

### 4. Recuperacao e alteracao de senha

- `Esqueci minha senha` envia e-mail de redefinicao pelo Firebase Auth
- `Alterar senha` fica disponivel no perfil
- o fluxo de alteracao exige:
  - senha atual
  - nova senha
  - confirmacao da nova senha

### 5. Feed

- filtros disponiveis:
  - todos
  - perdidos
  - adocao
- navegacao inferior com:
  - feed
  - conversas
  - adicionar post
  - notificacoes
  - perfil

### 6. Posts

- perdido:
  - funciona como post publico de rede social
  - recebe comentarios publicos
- adocao:
  - prioriza contato direto com o autor
- feira:
  - exclusiva para empresa
  - reune varios animais em um unico post

## Firebase usado no projeto

### Firestore

Colecoes principais:

- `users`
- `posts`
- `notifications`
- `chat_conversations`

Subcolecoes usadas no fluxo:

- `messages`
- `comments`
- `interests`

### Authentication

Provedores esperados:

- `E-mail/senha`
- `Google`

### Cloud Messaging

- usado para push notification

## Como rodar em qualquer PC

### Requisitos

- Android Studio atualizado
- Android SDK instalado
- JDK configurado pelo proprio Android Studio
- acesso ao Firebase do projeto

### O que o projeto ja inclui

Para facilitar abrir em qualquer maquina, o repositorio agora inclui:

- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.jar`
- `gradle/wrapper/gradle-wrapper.properties`

Ou seja, nao depende mais de Gradle instalado manualmente na outra maquina.

### Passo a passo

1. Clone o repositorio.
2. Abra `C:\eng2\PetCompanyApp` no Android Studio.
3. Aguarde o Sync do Gradle.
4. Confirme que o arquivo `app/google-services.json` esta presente.
5. Rode o app em um dispositivo ou emulador Android.

### Observacoes importantes

- `local.properties` nao deve ser versionado.
  - cada PC gera o proprio automaticamente no primeiro sync
- o projeto esta configurado para funcionar sem depender da sua API local
  - `use_remote_api = false`
- o Firebase e a base principal do fluxo atual

## API local

O projeto ainda tem suporte legado para API HTTP, mas ela e opcional.

Valor atual em:

- `app/src/main/res/values/strings.xml`
  - `api_base_url = http://127.0.0.1:8080`

Isso foi deixado em `127.0.0.1` para evitar depender do IP de uma maquina especifica.

Se voce quiser usar a API local em outro PC:

1. suba o backend na mesma maquina
2. mantenha `127.0.0.1:8080`
3. altere `use_remote_api` para `true`

Se o objetivo for apenas rodar o app atual, nao precisa mexer nisso.

## Login com Google

Para o login com Google funcionar no aparelho:

- `google-services.json` precisa estar atualizado
- o `SHA-1` do debug precisa estar cadastrado no Firebase
- o `google_web_client_id` precisa estar correto em:
  - `app/src/main/res/values/strings.xml`

## Estrutura principal

- `app/src/main/java/com/example/petcompanyapp/activities`
- `app/src/main/java/com/example/petcompanyapp/adapters`
- `app/src/main/java/com/example/petcompanyapp/models`
- `app/src/main/java/com/example/petcompanyapp/repositories`
- `app/src/main/java/com/example/petcompanyapp/utils`
- `app/src/main/res/layout`
- `app/src/main/res/drawable`

## Arquivos centrais

- `app/google-services.json`
- `app/build.gradle`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/example/petcompanyapp/activities/LoginActivity.java`
- `app/src/main/java/com/example/petcompanyapp/activities/UserRegisterActivity.java`
- `app/src/main/java/com/example/petcompanyapp/activities/FeedActivity.java`
- `app/src/main/java/com/example/petcompanyapp/repositories/FirebaseUserRepository.java`
- `app/src/main/java/com/example/petcompanyapp/repositories/FirebasePostRepository.java`
- `app/src/main/java/com/example/petcompanyapp/repositories/FirebaseChatRepository.java`
- `app/src/main/java/com/example/petcompanyapp/repositories/FirebaseNotificationRepository.java`

## Comandos uteis

No Windows:

```bat
gradlew.bat tasks
gradlew.bat assembleDebug
gradlew.bat signingReport
```

No macOS ou Linux:

```bash
./gradlew tasks
./gradlew assembleDebug
./gradlew signingReport
```

## Repositorio

- [petbook](https://github.com/MatheusThurow/petbook)
