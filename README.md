# PetCompanyApp

Aplicativo Android em Java com 5 telas principais:

- Login
- Cadastro de Usuario
- Cadastro de Empresa
- Cadastro de Animal
- Feed

## Estrutura

- `app/src/main/java/com/example/petcompanyapp/activities`: telas do app
- `app/src/main/java/com/example/petcompanyapp/utils`: validacoes e mascaras
- `app/src/main/java/com/example/petcompanyapp/models`: modelos simples
- `app/src/main/res/layout`: layouts XML

## Fluxo

1. `LoginActivity`
2. `UserRegisterActivity`
3. `CompanyRegisterActivity`
4. `FeedActivity`
5. `AnimalRegisterActivity`

## Banco de dados

Os scripts SQL Server estao em `database/sqlserver`:

- `01_create_database.sql`
- `02_create_tables.sql`
- `03_seed_data.sql`
- `04_views.sql`

## Observacao

O `sqlcmd` nao esta no `PATH` deste ambiente, entao os scripts foram preparados para execucao via SSMS ou por configuracao local do cliente SQL Server.
