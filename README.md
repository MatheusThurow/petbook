# PetBook

Aplicativo Android em Java para publicacao e visualizacao de posts sobre animais perdidos e para adocao.

Pacote Android atual:

- `com.petbook.app`

O projeto foi desenvolvido no Android Studio com foco em:

- interface mobile em XML
- navegacao simples entre telas
- persistencia local para demonstracao
- base pronta para evolucao com API/backend

## Funcionalidades atuais

- login local
- login com Google preparado no front
- cadastro de usuario com tipo:
  - pessoa fisica
  - empresa
- cadastro de empresa
- cadastro de animal
- criacao de posts de:
  - animal perdido
  - animal para adocao
- upload de imagem do animal
- localizacao do post
- feed com filtros:
  - todos
  - perdidos
  - adocao
- perfil do usuario
- recuperacao de senha local
- logout e navegacao com voltar
- chat separado entre usuarios
- busca de usuarios para iniciar conversa
- sincronizacao de chat preparada com Firebase Firestore

## Estrutura do projeto

- `app/src/main/java/com/example/petcompanyapp/activities`
  Telas do aplicativo.
- `app/src/main/java/com/example/petcompanyapp/repositories`
  Regras de acesso a dados locais, API e Firebase.
- `app/src/main/java/com/example/petcompanyapp/database`
  Banco SQLite local do app.
- `app/src/main/java/com/example/petcompanyapp/utils`
  Validacoes, mascaras, sessao e utilitarios gerais.
- `app/src/main/res/layout`
  Layouts XML das telas.
- `backend`
  Base de uma API Java para proximo sprint.
- `database/sqlserver`
  Scripts SQL Server do projeto.

## Banco de dados

Hoje o app esta configurado para rodar localmente no telefone com persistencia em SQLite.

Para chat em dois aparelhos, o projeto tambem esta preparado para usar Firebase Firestore.

Arquivos principais:

- `app/src/main/java/com/example/petcompanyapp/database/AppDatabaseHelper.java`
- `app/src/main/java/com/example/petcompanyapp/repositories/UserRepository.java`
- `app/src/main/java/com/example/petcompanyapp/repositories/AnimalPostRepository.java`
- `app/src/main/java/com/example/petcompanyapp/repositories/CompanyRepository.java`
- `app/src/main/java/com/example/petcompanyapp/repositories/AnimalRepository.java`

O projeto tambem possui scripts SQL Server para evolucao da arquitetura:

- `database/sqlserver/01_create_database.sql`
- `database/sqlserver/02_create_tables.sql`
- `database/sqlserver/03_seed_data.sql`
- `database/sqlserver/04_views.sql`
- `database/sqlserver/05_upgrade_local_instance.sql`

## Backend

Existe uma base de backend Java em `backend/`, preparada para integracao futura com banco compartilhado.

No estado atual da apresentacao, o app esta em modo local.

Arquivo de controle:

- `app/src/main/res/values/bools.xml`

Valor atual:

- `use_remote_api = false`

Chat Firebase:

- `use_firebase_chat = true`

Arquivos principais do chat:

- `app/src/main/java/com/example/petcompanyapp/activities/ConversationListActivity.java`
- `app/src/main/java/com/example/petcompanyapp/activities/ChatActivity.java`
- `app/src/main/java/com/example/petcompanyapp/repositories/FirebaseChatRepository.java`
- `app/src/main/java/com/example/petcompanyapp/repositories/FirebaseUserDirectoryRepository.java`
- `app/src/main/java/com/example/petcompanyapp/utils/FirebaseChatConfig.java`
- `app/google-services.json`

Colecoes esperadas no Firestore:

- `users`
- `chat_conversations`
- `messages` (subcolecao por conversa)

## Como executar

1. Abra a pasta do projeto no Android Studio.
2. Aguarde a sincronizacao do Gradle.
3. Rode o app em um emulador ou telefone Android.

## Como testar o chat em dois aparelhos

1. Deixe os dois aparelhos conectados a internet.
2. Instale o app nos dois aparelhos.
3. Entre com usuarios diferentes.
4. Abra `Conversas`.
5. Pesquise o outro usuario.
6. Inicie a conversa e envie mensagens.
7. Verifique no Firebase se as colecoes foram criadas.

## Observacoes importantes

- O fluxo principal da apresentacao esta configurado para funcionar localmente no aparelho.
- O login com Google esta preparado visualmente, mas depende de configuracao de credenciais para uso real.
- O mapa usa integracao no app e pode exigir internet no dispositivo para carregar corretamente.
- O chat com Firebase depende de `google-services.json` configurado para o pacote `com.petbook.app`.
- O Firestore deve estar criado e com rules publicadas para testes entre aparelhos.
- Arquivos locais do Android Studio, como configuracoes da pasta `.idea`, nao devem ser usados como referencia funcional do projeto.

## Repositorio

GitHub:

- [https://github.com/MatheusThurow/petbook](https://github.com/MatheusThurow/petbook)
