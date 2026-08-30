angular.module('app.resultsdatabankactions.tools.runCa').controller('RunCaCtrl', function ($rootScope, $scope, DatabankActionsService, AppService, L) {

    $scope.onClick = function() {
        window.parent.callAction("customAnalyzeInit", AppService.getProject(), $rootScope.activeDatabankTab.title, DatabankActionsService.selectedStrategies);
        window.parent.showPopup("#RunCaModal");
    }
});