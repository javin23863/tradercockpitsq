angular.module('app.settings').controller('SimpleSettingsCACtrl', function ($rootScope, $scope, $timeout, AppService, SQConstants, SQEvents, SettingsCustomAnalysisService, L) {
    console.log("SimpleSettingsCA controller initialized");

    $scope.getMethodLabel = function(value) {
        var method = getItem($scope.config.availableMethods, "value", value);
        return method ? method.name : '';
    }

    $scope.settingsChanged = function() {
        SettingsCustomAnalysisService.saveSettings();
        AppService.updateTaskXML();
    }
    
    $scope.config = SettingsCustomAnalysisService.config;
});