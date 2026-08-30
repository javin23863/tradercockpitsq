angular.module('app.settings').controller('ConditionsAndPeriodsCtrl', function ($rootScope, $scope, ConditionsAndPeriodsService) {

    $scope.settingPlugin.loadSettings = ConditionsAndPeriodsService.loadSettings;
    $scope.settingPlugin.checkSettings = ConditionsAndPeriodsService.checkSettings;
    $scope.settingPlugin.saveSettings = ConditionsAndPeriodsService.saveSettings;
    $scope.settingPlugin.resetSettings = ConditionsAndPeriodsService.resetSettings;
    $scope.settingPlugin.getDescription = ConditionsAndPeriodsService.getDescription;

    $scope.settingsChanged = function(){
        
    }

    $scope.config = ConditionsAndPeriodsService.config;

});