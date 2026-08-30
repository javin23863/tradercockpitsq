angular.module('app.resultsdatabankactions.tools.edit.parameters').controller('EditParametersCtrl', function ($rootScope, $scope, DatabankActionsService, AppService) {

    $scope.onClick = function() {
        var data = DatabankActionsService.getSelectedStrategy();       
        if(!data.strategy) {
            $rootScope.showError("No Strategy selected.");
        }
        else {
            window.parent.callAction("prepareStrategyParametersPopup", AppService.getProject(), $rootScope.activeDatabankTab.title, DatabankActionsService.strategyName);
            window.parent.showPopup("#EditStrategyParametersModal");
        }
    }

});