# PetBook

Aplicativo Android em `Java` com proposta de rede social para:

- animais perdidos
- animais para adoção
- feiras de adoção

Pacote Android atual:

- `com.petbook.app`

## Stack

- `Java`
- `Android Studio`
- `XML`
- `Firebase Firestore`
- `Firebase Auth`
- `Firebase Cloud Messaging`
- `SQLite` como fallback/local legado

## Funcionalidades atuais

- login único por e-mail e senha
- login com Google
- cadastro de conta para:
  - pessoa física
  - empresa
- recuperação de senha por e-mail
- alteração de senha dentro do perfil
- perfil com edição de dados
- dark mode com preferência salva
- feed com filtros
- posts de:
  - animal perdido
  - adoção
  - feira de adoção
- edição e exclusão de post apenas pelo autor
- curtidas
- comentários públicos em posts de perdido
- respostas encadeadas em comentários
- contato direto por chat em posts de adoção e feira
- chat entre usuários
- notificações no app
- push notification com FCM
- cadastro de empresa
- cadastro de animal
- navegação inferior fixa
- navegação horizontal por gesto entre telas principais

## Regras principais do produto

- o login não separa mais empresa e pessoa física
- o tipo da conta é escolhido no cadastro e reconhecido automaticamente após o login
- pessoa física pode criar:
  - post de animal perdido
  - post de adoção
- empresa pode criar:
  - post de animal perdido
  - post de adoção
  - post de feira
- post de feira aceita vários animais em um único post
- apenas o autor pode editar ou apagar o próprio post
- em post de perdido:
  - outros usuários veem `Comentar`
  - o autor vê `Comentários`
- em post de adoção ou feira:
  - outros usuários veem `Entrar em contato`
  - o autor vê `Comentários`

## Fluxo atual do app

### 1. Login

- o usuário entra com e-mail e senha
- o app identifica automaticamente o tipo da conta
- no login com Google:
  - autentica no Firebase Auth
  - procura o usuário no Firestore
  - se não existir, cria uma conta básica de pessoa física

### 2. Cadastro

- o usuário escolhe o tipo da conta:
  - pessoa física
  - empresa
- pessoa física vai direto para o feed após o cadastro
- empresa pode seguir para completar os dados institucionais

### 3. Recuperação e alteração de senha

- `Esqueci minha senha` envia e-mail de redefinição pelo Firebase Auth
- `Alterar senha` fica disponível dentro do perfil
- o fluxo de alteração exige:
  - senha atual
  - nova senha
  - confirmação da nova senha

### 4. Feed

- filtros disponíveis:
  - todos
  - perdidos
  - adoção
- navegação inferior com:
  - feed
  - conversas
  - adicionar post
  - notificações
  - perfil
- troca de telas também por gesto lateral

### 5. Posts

- perdido:
  - funciona como post público de rede social
  - recebe comentários públicos
- adoção:
  - prioriza contato direto com o autor
- feira:
  - exclusiva para empresa
  - reúne vários animais em um único post

### 6. Comentários

- usados principalmente em posts de perdido
- mostram comentários públicos de todos os usuários
- suportam respostas encadeadas

### 7. Chat

- busca por usuário
- conversa individual
- sincronização via Firebase
- área de conversas recentes na tela de conversas

### 8. Notificações

- curtidas
- comentários
- mensagens
- interesse em adoção
- atualização de post

## Firebase usado no projeto

### Firestore

Coleções principais:

- `users`
- `posts`
- `notifications`
- `chat_conversations`

Subcoleções:

- `messages`
- `comments`
- `interests`

### Authentication

Provedores esperados:

- `E-mail/senha`
- `Google`

### Cloud Messaging

- usado para push notification

## Observações importantes

- o Firebase é a base principal das interações do app
- o SQLite ainda existe como fallback em fluxos legados
- o app não cria mais postagens de exemplo no banco local
- o feed também faz limpeza das postagens de exemplo antigas no Firebase quando necessário
- a foto de perfil não está ativa no fluxo atual

## Como rodar

1. Abrir o projeto no Android Studio
2. Sincronizar o Gradle
3. Conferir a configuração do Firebase
4. Rodar em um dispositivo Android

## Login com Google

Para o login com Google funcionar no aparelho:

- `google-services.json` precisa estar atualizado
- o `SHA-1` do debug precisa estar cadastrado no Firebase
- o `google_web_client_id` precisa estar correto em `strings.xml`

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
- `app/src/main/java/com/example/petcompanyapp/activities/FeedActivity.java`
- `app/src/main/java/com/example/petcompanyapp/repositories/FirebaseUserRepository.java`
- `app/src/main/java/com/example/petcompanyapp/repositories/FirebasePostRepository.java`
- `app/src/main/java/com/example/petcompanyapp/repositories/FirebaseChatRepository.java`
- `app/src/main/java/com/example/petcompanyapp/repositories/FirebaseNotificationRepository.java`

## Repositório

- [petbook](https://github.com/MatheusThurow/petbook)
