angular.module('app.settings').controller('StopLossCtrl', function ($rootScope, $scope, StopLossService, SQEvents) {

    $scope.settingPlugin.loadSettings = StopLossService.loadSettings;
    $scope.settingPlugin.checkSettings = StopLossService.checkSettings;
    $scope.settingPlugin.saveSettings = StopLossService.saveSettings;
    $scope.settingPlugin.resetSettings = StopLossService.resetSettings;
    $scope.settingPlugin.getDescription = StopLossService.getDescription;

    $scope.config = StopLossService.config;

});