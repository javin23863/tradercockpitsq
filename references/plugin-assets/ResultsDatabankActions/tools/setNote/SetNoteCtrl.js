angular.module('app.resultsdatabankactions.tools.setNote').controller('SetNoteCtrl', function ($rootScope, $scope, DatabankActionsService, AppService, L) {

    $scope.onClick = function(option, tab) {
        window.parent.callAction("setNoteInit", AppService.getProject(), $rootScope.activeDatabankTab.title, DatabankActionsService.selectedStrategies, tab);
        window.parent.showPopup("#SetNoteModal");
    }
});