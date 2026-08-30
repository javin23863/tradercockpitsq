angular.module('app.resultsdatabankactions.splitstrategies', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsDatabankAction", 200, {
        title: Ltsq("Portfolio")+":"+Ltsq("Split strategies"),
        class: 'btn btn-normal btn-default',
        controller: btnCtrl,
        product: "BUILDER,RETESTER,OPTIMIZER,TASKMANAGER,PORTFOLIOMASTER",
        id: "databank-action-splitstrategies"
    });

    function btnCtrl($rootScope, $scope, DatabankActionsService, L, SQConstants, DatabankService, AppService){
        
        $scope.onClick = function(){
            if($rootScope.project.state != projectStates.loading && $rootScope.project.state != projectStates.running) {
                if(!DatabankActionsService.selectedStrategies) {
                    $rootScope.showError(L.tsq("You have to select at least one portfolio to split."));
                    return;
                }

                DatabankService.splitStrategies(DatabankActionsService.selectedStrategies).then(function(result) {
                    if(!result.success){
                        $rootScope.showError(L.tsq("Error while splitting strategies. %s", [ result.error ]));
                    }
                });
            }
            else {
                $rootScope.showError(L.tsq("Cannot split portfolios when project is in loading/running state"));
            }
        }

        var projectStates = SQConstants.getConstants().runningStatuses;

    }
    
});
