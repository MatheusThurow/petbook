# SQL Server

Scripts preparados para o banco `PetCompanyDB`:

1. `01_create_database.sql`
2. `02_create_tables.sql`
3. `03_seed_data.sql`
4. `04_views.sql`

## Ordem de execucao

Execute os scripts nessa ordem no SQL Server Management Studio.

## Estrutura principal

- `app.UserTypes`: tipos de usuario do sistema
- `app.Users`: usuarios pessoa fisica e empresa
- `app.Companies`: dados institucionais da empresa
- `app.Animals`: animais cadastrados por pessoa fisica ou empresa
- `app.FeedPosts`: conteudo exibido no feed

## Observacao

Os scripts foram organizados para facilitar evolucao futura para API ou integracao com o app Android.
