angular.module('app.settings').controller('SimpleAutoRetestSettingsCtrl', function ($scope, AppService, TaskAutoRetestService) {

    $scope.onSave = TaskAutoRetestService.saveSettings;
    $scope.projectStates = TaskAutoRetestService.constants.runningStatuses;
    $scope.config = TaskAutoRetestService.config;

    $scope.isDashboard = function(){
        return AppService.getActivePanel() == "dashboard"
    }

    TaskAutoRetestService.loadSettings();

});