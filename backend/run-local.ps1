$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$sourceRoot = Join-Path $PSScriptRoot "src\main\java"
$buildRoot = Join-Path $PSScriptRoot "build\classes"

if (-not $env:JAVA_HOME) {
    $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
}

$javac = Join-Path $env:JAVA_HOME "bin\javac.exe"
$java = Join-Path $env:JAVA_HOME "bin\java.exe"

if (-not (Test-Path $javac)) {
    throw "javac.exe nao encontrado em JAVA_HOME=$($env:JAVA_HOME)"
}

New-Item -ItemType Directory -Path $buildRoot -Force | Out-Null

$javaFiles = Get-ChildItem -Path $sourceRoot -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
& $javac -encoding UTF-8 -d $buildRoot $javaFiles

if (-not $env:PETBOOK_DB_USER) {
    $env:PETBOOK_DB_USER = "petbook_api"
}

if (-not $env:PETBOOK_DB_PASSWORD) {
    $env:PETBOOK_DB_PASSWORD = "Petbook@123"
}

& $java `
    -Dapi.port=8080 `
    -Ddb.server="tcp:127.0.0.1,1433" `
    -Ddb.name="PetCompanyDB" `
    -Ddb.trustCertificate=true `
    -Ddb.user="$env:PETBOOK_DB_USER" `
    -Ddb.password="$env:PETBOOK_DB_PASSWORD" `
    -cp $buildRoot `
    com.example.petcompanyapp.backend.ApiServer
