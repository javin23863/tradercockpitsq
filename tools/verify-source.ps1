[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$sourceRoot = Join-Path $repositoryRoot 'sources'

$expectedCounts = [ordered]@{
    'engine-core' = 844
    'platform-runtime' = 223
    'launcher-app' = 7
    'indicators-building-blocks' = 929
    'data-lib' = 178
    'grid-lib' = 57
    'jobs-lib' = 8
    'plugin-api' = 17
    'web-gui-lib' = 25
    'wizard-business' = 22
    'plugins' = 404
}

foreach ($rootName in $expectedCounts.Keys) {
    $rootPath = Join-Path $sourceRoot $rootName
    if (-not (Test-Path -LiteralPath $rootPath -PathType Container)) {
        throw "Missing source root: $rootName"
    }

    $actualCount = @(Get-ChildItem -LiteralPath $rootPath -Recurse -File -Filter '*.java').Count
    if ($actualCount -ne $expectedCounts[$rootName]) {
        throw "Unexpected Java count for ${rootName}: expected $($expectedCounts[$rootName]), found $actualCount"
    }

    Write-Output ("{0}: {1} Java files" -f $rootName, $actualCount)
}

$requiredFiles = @(
    'sources/engine-core/com/strategyquant/tradinglib/engine/BacktestEngine.java'
    'sources/engine-core/com/strategyquant/tradinglib/backtestrunner/BacktestRunner.java'
    'sources/engine-core/com/strategyquant/tradinglib/gp/GPEngine.java'
    'sources/engine-core/com/strategyquant/tradinglib/optimization/OptimizationProfile.java'
    'sources/engine-core/com/strategyquant/tradinglib/montecarlo/retest/MonteCarloRetestList.java'
    'sources/engine-core/com/strategyquant/tradinglib/robustnesstests/RobustnessTestMethod.java'
    'sources/indicators-building-blocks/SQ/ExitMethods/StopLoss.java'
    'sources/indicators-building-blocks/SQ/ExitMethods/TrailingStop.java'
    'sources/indicators-building-blocks/SQ/Blocks/Order/Modify/SetStopLoss.java'
    'sources/indicators-building-blocks/SQ/TradingOptions/UseInitialSLPT.java'
    'sources/plugins/TaskBuild/com/strategyquant/plugin/Task/impl/Build/BuildTask.java'
    'sources/plugins/TaskRetest/com/strategyquant/plugin/Task/impl/Retest/RetestTask.java'
    'sources/plugins/TaskOptimize/com/strategyquant/plugin/Task/impl/Optimize/OptimizeTask.java'
    'sources/plugins/CrossCheckMonteCarloManipulation/com/strategyquant/plugin/CrossCheck/impl/MonteCarloManipulation/MonteCarloManipulationPlugin.java'
    'sources/plugins/CrossCheckMonteCarloRetest/com/strategyquant/plugin/CrossCheck/impl/MonteCarloRetest/MonteCarloRetestPlugin.java'
)

foreach ($relativePath in $requiredFiles) {
    $absolutePath = Join-Path $repositoryRoot $relativePath
    if (-not (Test-Path -LiteralPath $absolutePath -PathType Leaf)) {
        throw "Missing representative source file: $relativePath"
    }
}

$placeholderPatterns = @(
    'throw new RuntimeException\("Method not decompiled'
    '/* ERROR */'
    'UnsupportedOperationException\("Method not decompiled'
)
$sourceFiles = Get-ChildItem -LiteralPath $sourceRoot -Recurse -File -Filter '*.java'
foreach ($pattern in $placeholderPatterns) {
    $matches = @($sourceFiles | Select-String -Pattern $pattern)
    if ($matches.Count -gt 0) {
        throw "Decompiler placeholder found: $($matches[0].Path):$($matches[0].LineNumber)"
    }
}

$totalCount = ($expectedCounts.Values | Measure-Object -Sum).Sum
Write-Output "Verified $totalCount Java source files and all required engine/module anchors."
