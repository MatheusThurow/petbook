# PetBook

Aplicativo Android em Java com proposta de rede social para:

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

## Funcionalidades implementadas

- login único por e-mail e senha
- login com Google
- cadastro de usuário com tipo:
  - pessoa física
  - empresa
- cadastro automático de conta básica ao entrar com Google pela primeira vez
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
- comentários públicos em post perdido
- respostas encadeadas em comentários
- contato direto por chat em posts de adoção
- chat entre usuários
- notificações no app
- push notification via FCM
- cadastro de empresa
- cadastro de animal

## Regras principais do produto

- login não separa mais empresa e pessoa física
- o tipo da conta é definido no cadastro e reconhecido automaticamente após o login
- pessoa física pode criar:
  - perdido
  - adoção
- empresa pode criar:
  - perdido
  - adoção
  - feira
- post de feira aceita vários animais em um único post
- apenas o autor pode ver ações de editar/apagar
- em post perdido:
  - outros usuários veem `Comentar`
  - o autor vê `Comentários`
- em post de adoção/feira:
  - outros usuários veem `Entrar em contato`
  - o autor vê `Comentários`

## Fluxos principais

### 1. Login

- o usuário entra com e-mail e senha
- o app identifica o tipo da conta automaticamente
- se usar Google:
  - autentica no Firebase Auth
  - procura o usuário no Firestore
  - se não existir, cria uma conta básica pessoa física e entra

### 2. Cadastro

- o usuário escolhe:
  - pessoa física
  - empresa
- pessoa física entra direto no feed após cadastro
- empresa pode seguir para completar o cadastro institucional

### 3. Recuperação de senha

- tela `Esqueci minha senha`
- usuário informa e confirma o e-mail
- o app envia e-mail de recuperação pelo Firebase Auth

### 4. Alteração de senha

- disponível no perfil
- exige:
  - senha atual
  - nova senha
  - confirmação da nova senha

### 5. Feed

- filtros:
  - todos
  - perdidos
  - adoção
- barra inferior fixa com:
  - feed
  - conversas
  - adicionar post
  - notificações
  - perfil

### 6. Posts

- perdido:
  - interação pública por comentários
- adoção:
  - contato direto com o dono do post
- feira:
  - exclusivo para empresa
  - mostra vários animais dentro do mesmo post

### 7. Comentários

- usados principalmente em posts de perdido
- exibem todos os comentários públicos
- permitem respostas encadeadas

### 8. Chat

- busca por usuário
- conversa individual
- sincronizado pelo Firebase

### 9. Notificações

- curtidas
- comentários
- mensagens
- interesse em adoção
- atualizações de post

## Firebase usado no projeto

### Firestore

Coleções principais esperadas:

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

## Estrutura do projeto

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
- `app/src/main/java/com/example/petcompanyapp/repositories/FirebaseUserRepository.java`
- `app/src/main/java/com/example/petcompanyapp/repositories/FirebasePostRepository.java`
- `app/src/main/java/com/example/petcompanyapp/repositories/FirebaseChatRepository.java`
- `app/src/main/java/com/example/petcompanyapp/repositories/FirebaseNotificationRepository.java`

## Como rodar

1. Abrir o projeto no Android Studio
2. Sincronizar o Gradle
3. Conferir se o Firebase está configurado
4. Rodar no dispositivo Android

## Observações

- o projeto passou a usar Firebase como base principal das interações
- o SQLite ainda existe como fallback em alguns fluxos legados
- para login com Google funcionar no aparelho:
  - `google-services.json` precisa estar atualizado
  - o `SHA-1` do debug precisa estar cadastrado no Firebase
  - `google_web_client_id` precisa estar correto em `strings.xml`

## Repositório

- [petbook](https://github.com/MatheusThurow/petbook)
