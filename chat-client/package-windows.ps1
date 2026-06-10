param(
    [ValidateSet("app-image", "exe", "msi")]
    [string]$Type = "app-image",

    [string]$AppName = "ChatClient",

    [string]$AppVersion = "1.0.0"
)

$ErrorActionPreference = "Stop"

$ClientDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Resolve-Path (Join-Path $ClientDir "..")
$InputDir = Join-Path $ClientDir "target\jpackage-input-windows"
$DistDir = Join-Path $ClientDir "target\dist-windows"
$ClientJar = Join-Path $ClientDir "target\chat-client-1.0.0-SNAPSHOT.jar"
$CommonJar = Join-Path $ProjectRoot "chat-common\target\chat-common-1.0.0-SNAPSHOT.jar"
$DependencyDir = Join-Path $ClientDir "target\dependency"

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw "Maven was not found on PATH."
}

if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
    throw "jpackage was not found on PATH. Install a JDK that includes jpackage, such as JDK 21+."
}

Remove-Item -Recurse -Force $InputDir, $DistDir -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $InputDir, $DistDir | Out-Null

Push-Location $ProjectRoot
try {
    mvn -pl chat-client -am package dependency:copy-dependencies -DincludeScope=runtime -DskipTests
}
finally {
    Pop-Location
}

Copy-Item $ClientJar $InputDir
Copy-Item (Join-Path $DependencyDir "*.jar") $InputDir
Copy-Item $CommonJar $InputDir -Force

$JpackageArgs = @(
    "--type", $Type,
    "--dest", $DistDir,
    "--input", $InputDir,
    "--name", $AppName,
    "--main-jar", "chat-client-1.0.0-SNAPSHOT.jar",
    "--main-class", "com.chatapp.client.ChatClientLauncher",
    "--app-version", $AppVersion,
    "--vendor", "ChatApp"
)

if ($Type -eq "exe" -or $Type -eq "msi") {
    $JpackageArgs += @(
        "--win-menu",
        "--win-shortcut",
        "--win-dir-chooser"
    )
}

jpackage @JpackageArgs

Write-Host ""
Write-Host "Windows package created in: $DistDir"
if ($Type -eq "app-image") {
    Write-Host "Executable: $(Join-Path $DistDir "$AppName\$AppName.exe")"
}
