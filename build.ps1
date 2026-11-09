$ErrorActionPreference='Stop'
if(-not $env:JAVA_HOME){$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.13.21-hotspot'}
if(-not $env:ANDROID_HOME){$env:ANDROID_HOME=Join-Path $env:LOCALAPPDATA 'Android\Sdk'}
Push-Location $PSScriptRoot
try { & .\gradlew.bat --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug; if($LASTEXITCODE -ne 0){throw 'Build or checks failed'} } finally {Pop-Location}
