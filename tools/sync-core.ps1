# Compares app/src/main/cpp/core and core-tests with Hiragana IME for Windows.
#
#   powershell -ExecutionPolicy Bypass -File tools\sync-core.ps1 [-WinDir ..\hiragana-ime-win] [-Apply]
#
# Without -Apply it only lists the files that differ. With -Apply it copies the
# Windows files over the Android ones; check the changes of CORE_ORIGIN.md
# ("Android 版で変えたところ") afterwards, as they are overwritten too.

param(
    [string] $WinDir = '',
    [switch] $Apply
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
if (-not $WinDir) { $WinDir = Join-Path (Split-Path -Parent $root) 'hiragana-ime-win' }

$pairs = @(
    @{ From = Join-Path $WinDir 'src\core'; To = Join-Path $root 'app\src\main\cpp\core' },
    @{ From = Join-Path $WinDir 'tests'; To = Join-Path $root 'core-tests' }
)
foreach ($p in $pairs) {
    Get-ChildItem $p.To -File -Recurse | Where-Object { $_.Extension -in '.cpp', '.h', '.inc', '.py' } | ForEach-Object {
        $rel = $_.FullName.Substring($p.To.Length + 1)
        $src = Join-Path $p.From $rel
        if (-not (Test-Path $src)) {
            Write-Host "android only: $rel"
        } elseif ((Get-FileHash $src).Hash -ne (Get-FileHash $_.FullName).Hash) {
            Write-Host "differs:      $rel"
            if ($Apply) { Copy-Item $src $_.FullName }
        }
    }
}
