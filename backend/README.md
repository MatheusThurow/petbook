# Backend Java + SQL Server

Esta pasta contem uma API Java simples para compartilhar dados entre varios dispositivos.

## O que ela faz

- autentica usuarios
- cadastra usuarios
- atualiza perfil
- salva empresa
- lista posts do feed
- cria posts de animais

## Como funciona

- servidor HTTP em Java puro (`com.sun.net.httpserver.HttpServer`)
- acesso ao SQL Server local via `sqlcmd`
- resposta em JSON

## Requisitos

- SQL Server com o banco `PetCompanyDB`
- `sqlcmd.exe` instalado
- JDK 17 ou o `jbr` do Android Studio

## Autenticacao com o banco

A API aceita dois cenarios:

- autenticacao integrada do Windows
- usuario e senha do SQL Server

Para ambiente online, o mais indicado e usar usuario e senha, como em Azure SQL.

## Como rodar

No PowerShell:

```powershell
cd C:\eng2\PetCompanyApp\backend
.\run-local.ps1
```

Se quiser usar login SQL:

```powershell
$env:PETBOOK_DB_USER='seu_usuario_sql'
$env:PETBOOK_DB_PASSWORD='sua_senha_sql'
.\run-local.ps1
```

Servidor local:

- `http://localhost:8080/api/health`

## Endpoints principais

- `POST /api/auth/login`
- `POST /api/users/register`
- `GET /api/users/{id}`
- `PUT /api/users/{id}`
- `POST /api/companies`
- `GET /api/posts?filter=ALL`
- `POST /api/posts`

## Teste rapido

```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/health" | Select-Object -ExpandProperty Content
```

## Teste em dois dispositivos

Para dois celulares verem os mesmos dados, basta:

1. rodar esta API em uma maquina central
2. abrir a porta `8080` no firewall ou usar um tunnel
3. apontar o app Android para o IP publico ou URL dessa API

## Caminho recomendado para ambiente online

- banco: Azure SQL Database
- app server: Windows VM, App Service ou outro host com Java 17
- autenticacao da API com SQL login dedicado

Documentacao oficial:

- [Azure SQL Database quickstart](https://learn.microsoft.com/en-us/azure/azure-sql/database/single-database-create-quickstart)
- [Java on Azure App Service](https://learn.microsoft.com/en-us/azure/app-service/quickstart-java)

## Observacao

Esta API foi preparada para evoluir para deploy em nuvem. O passo seguinte natural e publicar em um host online ou usar um tunnel como ambiente temporario de homologacao.
