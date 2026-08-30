angular.module('app.resultsdatabankactions.mergewf', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsDatabankAction", 200, {
        title: Ltsq("Portfolio")+":"+Ltsq("Merge WF results"),
        class: 'btn btn-normal btn-default',
        controller: btnCtrl,
        product: "BUILDER,RETESTER,OPTIMIZER,TASKMANAGER",
        id: "databank-action-mergewf"
    });

    function btnCtrl($rootScope, $scope, DatabankActionsService, L, SQConstants, DatabankService, AppService){
        
        $scope.onClick = function(){
            if($rootScope.project.state != projectStates.loading && $rootScope.project.state != projectStates.running) {
                if(!DatabankActionsService.selectedStrategies) {
                    $rootScope.showError(L.tsq("You have to select at least one strategy."));
                    return;
                }

                DatabankService.mergeWFStrategies(DatabankActionsService.selectedStrategies).then(function(result) {
                    if(!result.success){
                        $rootScope.showError(L.tsq("Error while merging WF strategies. %s", [ result.error ]));
                    }
                });
            }
            else {
                $rootScope.showError(L.tsq("Cannot merge portfolios when project is in loading/running state"));
            }
        }

        var projectStates = SQConstants.getConstants().runningStatuses;

    }
    
});
