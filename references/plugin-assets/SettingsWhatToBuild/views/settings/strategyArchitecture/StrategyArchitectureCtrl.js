angular.module('app.settings').controller('StrategyArchitectureCtrl', function ($rootScope, $scope, StrategyArchitectureService) {
    
    $scope.settingPlugin.loadSettings = StrategyArchitectureService.loadSettings;
    $scope.settingPlugin.checkSettings = StrategyArchitectureService.checkSettings;
    $scope.settingPlugin.saveSettings = StrategyArchitectureService.saveSettings;
    $scope.settingPlugin.resetSettings = StrategyArchitectureService.resetSettings;
    $scope.settingPlugin.getDescription = StrategyArchitectureService.getDescription;

    $scope.config = StrategyArchitectureService.config;
    $scope.constants = StrategyArchitectureService.constants;
    
});