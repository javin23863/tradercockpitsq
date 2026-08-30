angular.module('app.resultsdatabankactions.tools.compareStrategies').controller('CompareStrategiesCtrl', function ($rootScope, $scope, DatabankActionsService, AppService, L) {

    $scope.onClick = function() {
        var data = DatabankActionsService.getSelectedStrategy();       
        if(!data.strategy) {
            $rootScope.showError("No Strategies selected.");

            return;
        }
        else {
            var strategies = DatabankActionsService.selectedStrategies.split(',');
            if(strategies.length!=2) {
                $rootScope.showError(L.tsq("Please select exactly two strategies to compare"));

                return;
            }

            window.parent.callAction("compareStrategiesPopup", AppService.getProject(), $rootScope.activeDatabankTab.title, DatabankActionsService.selectedStrategies);
            window.parent.showPopup("#CompareStrategiesModal");
        }
    }
});