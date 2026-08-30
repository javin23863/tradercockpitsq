angular.module('app.settings').controller('TradingDirectionsCtrl', function ($rootScope, $scope, TradingDirectionsService) {
    
    $scope.settingPlugin.loadSettings = TradingDirectionsService.loadSettings;
    $scope.settingPlugin.checkSettings = TradingDirectionsService.checkSettings;
    $scope.settingPlugin.saveSettings = TradingDirectionsService.saveSettings;
    $scope.settingPlugin.resetSettings = TradingDirectionsService.resetSettings;
    $scope.settingPlugin.getDescription = TradingDirectionsService.getDescription;

    $scope.config = TradingDirectionsService.config;
    $scope.constants = TradingDirectionsService.constants;

});