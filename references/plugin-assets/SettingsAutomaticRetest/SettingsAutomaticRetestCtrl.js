angular.module('app.settings').controller('SettingsAutomaticRetestCtrl', function ($rootScope, $scope, $timeout, $element, TaskAutoRetestService, AppService, SQConstants, SQEvents) {
    console.log("SettingsAutomaticRetest controller initialized");

    $scope.onSave = TaskAutoRetestService.saveSettings;
    $scope.projectStates = TaskAutoRetestService.constants.runningStatuses;
    $scope.config = TaskAutoRetestService.config;

    TaskAutoRetestService.loadSettings();

});