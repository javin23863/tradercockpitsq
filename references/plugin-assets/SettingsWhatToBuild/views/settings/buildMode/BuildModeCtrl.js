angular.module('app.settings').controller('BuildModeCtrl', function ($rootScope, $scope, SQConstants, BuildModeService) {
    
    var constants = SQConstants.getConstants();

    $scope.config = BuildModeService.config;
    $scope.generationTypes = BuildModeService.generationTypes;

    $scope.settingPlugin.loadSettings = BuildModeService.loadSettings;
    $scope.settingPlugin.checkSettings = BuildModeService.checkSettings;
    $scope.settingPlugin.saveSettings = BuildModeService.saveSettings;
    $scope.settingPlugin.resetSettings = BuildModeService.resetSettings;
    $scope.settingPlugin.getDescription = BuildModeService.getDescription;

    $scope.initGenerationTypeChanged = function (type) {
        $scope.config.initGenerationType = type;
    }

});