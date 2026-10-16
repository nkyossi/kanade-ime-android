# Builds and runs the core tests (core-tests/) on Windows with Visual Studio 2022,
# using the CMake and Ninja that come with Visual Studio.
#
#   powershell -ExecutionPolicy Bypass -File tools\host-test.ps1 [-Config Release|Debug] [-Filter <ctest regex>]
#
# The golden tests run the original Python engine and take about five minutes;
# -Filter core_tests runs the unit tests only.
#
# The build tree goes to %LOCALAPPDATA%\HiraganaIME-android-build\host so that
# it is not synced by OneDrive.

param(
    [ValidateSet('Release', 'Debug')] [string] $Config = 'Release',
    [string] $Filter = ''
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$vswhere = Join-Path ${env:ProgramFiles(x86)} 'Microsoft Visual Studio\Installer\vswhere.exe'
$vs = & $vswhere -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath
if (-not $vs) { throw 'Visual Studio with the C++ tools was not found.' }
$vcvars = Join-Path $vs 'VC\Auxiliary\Build\vcvarsall.bat'
$cmake = Join-Path $vs 'Common7\IDE\CommonExtensions\Microsoft\CMake\CMake\bin\cmake.exe'
$ctest = Join-Path $vs 'Common7\IDE\CommonExtensions\Microsoft\CMake\CMake\bin\ctest.exe'
$ninja = Join-Path $vs 'Common7\IDE\CommonExtensions\Microsoft\CMake\Ninja\ninja.exe'

$src = Join-Path $root 'core-tests'
$build = Join-Path $env:LOCALAPPDATA 'HiraganaIME-android-build\host'
$filterArg = if ($Filter) { "-R `"$Filter`"" } else { '' }
$cmd = "call `"$vcvars`" x64 >nul && " +
       "`"$cmake`" -S `"$src`" -B `"$build`" -G Ninja -DCMAKE_MAKE_PROGRAM=`"$ninja`" -DCMAKE_BUILD_TYPE=$Config && " +
       "`"$cmake`" --build `"$build`" && " +
       "cd /d `"$build`" && `"$ctest`" --output-on-failure $filterArg"
cmd /c $cmd
if ($LASTEXITCODE -ne 0) { throw 'host tests failed' }
