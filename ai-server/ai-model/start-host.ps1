$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Ngrok = Join-Path $Root ".tools\ngrok\ngrok.exe"
$LangServePort = if ($env:PORT) { [int]$env:PORT } else { 8000 }
$LangServeUrl = "http://127.0.0.1:$LangServePort"
$PublicUrl = if ($env:NGROK_PUBLIC_URL) { $env:NGROK_PUBLIC_URL } else { "https://dividable-surcharge-stump.ngrok-free.dev" }
$OllamaUrl = "http://127.0.0.1:11434/api/tags"

function Test-HttpEndpoint {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Uri,
        [int] $TimeoutSec = 3
    )

    try {
        Invoke-WebRequest -Uri $Uri -UseBasicParsing -TimeoutSec $TimeoutSec | Out-Null
        return $true
    }
    catch {
        return $false
    }
}

if (-not (Test-Path $Ngrok)) {
    throw "ngrok was not found at $Ngrok"
}

if (Test-HttpEndpoint -Uri $OllamaUrl) {
    Write-Host "Ollama is already running on http://127.0.0.1:11434"
}
else {
    $Ollama = Get-Command "ollama" -ErrorAction SilentlyContinue
    if (-not $Ollama) {
        throw "Ollama was not found in PATH. Start Ollama manually or add ollama.exe to PATH."
    }

    Write-Host "Starting Ollama on http://127.0.0.1:11434"
    Start-Process `
        -FilePath $Ollama.Source `
        -ArgumentList @("serve") `
        -WindowStyle Hidden | Out-Null
    Start-Sleep -Seconds 5
}

if (Test-HttpEndpoint -Uri "$LangServeUrl/docs") {
    Write-Host "LangServe is already running on $LangServeUrl"
}
else {
    Write-Host "Starting LangServe on $LangServeUrl"
    Start-Process `
        -FilePath "python" `
        -ArgumentList @("-m", "uvicorn", "app.server:app", "--host", "0.0.0.0", "--port", "$LangServePort") `
        -WorkingDirectory $Root `
        -WindowStyle Hidden | Out-Null
    Start-Sleep -Seconds 5
}

try {
    $tunnels = Invoke-RestMethod -Uri "http://127.0.0.1:4040/api/tunnels" -TimeoutSec 3
    $existing = $tunnels.tunnels | Where-Object { $_.public_url -eq $PublicUrl }
    if ($existing) {
        Write-Host "ngrok is already running at $PublicUrl"
        exit 0
    }
}
catch {
}

Write-Host "Starting ngrok at $PublicUrl"
Start-Process `
    -FilePath $Ngrok `
    -ArgumentList @("http", "$LangServePort", "--url=$PublicUrl") `
    -WorkingDirectory $Root `
    -WindowStyle Hidden | Out-Null

Start-Sleep -Seconds 5
Invoke-RestMethod -Uri "http://127.0.0.1:4040/api/tunnels" |
    Select-Object -ExpandProperty tunnels |
    Select-Object public_url, proto
