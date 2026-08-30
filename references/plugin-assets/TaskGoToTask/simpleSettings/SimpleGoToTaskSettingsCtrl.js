angular.module('app.settings').controller('SimpleGoToTaskSettingsCtrl', function ($rootScope, $scope, sqPlugin, $timeout, AppService, SQEvents, SettingsGoToTaskService, L) {
    console.log("SimpleGoToTaskSettings controller initialized");

    $scope.onTaskChange = function() {
        SettingsGoToTaskService.saveSettings();
    }

    $scope.showAdvancedSettings = function (event) {
        AppService.switchToPanel('settings');
        event.stopPropagation();
        $timeout(tabsShowHiddenTabs, 0, false);
    }

    $scope.getTaskLabel = function(){
        var task = getItem($scope.config.tasks, "taskName", $scope.config.task);
        return task ? task.taskTitle : '';
    }

    $scope.config = SettingsGoToTaskService.config;
    $scope.conditionsInstance = {};

});