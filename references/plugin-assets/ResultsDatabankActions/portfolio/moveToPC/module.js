angular.module('app.resultsdatabankactions.movetopc', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsDatabankAction", 300, {
        title: Ltsq("Portfolio")+":"+Ltsq("Move to Portfolio Composer"),
        class: 'btn btn-normal btn-default',
        controller: btnCtrl,
        product: "BUILDER,RETESTER,OPTIMIZER,TASKMANAGER",
        id: "databank-action-move-to-pc"
    });

    function btnCtrl($rootScope, $scope, DatabankActionsService, L, SQConstants, DatabankService, AppService){
        
        $scope.onClick = function(){
            if($rootScope.project.state != projectStates.loading && $rootScope.project.state != projectStates.running) {
                let strategies = DatabankActionsService.selectedStrategies;
                
                if(!strategies) {
                    $rootScope.showError(L.tsq("You have to select at least one strategy to move."));
                    return;
                }

                let count = strategies.split(",").length;

                $rootScope.showConfirm("Move to Portfolio Composer",  "Are you sure you want to move selected strategies to Portfolio Composer?",  function() {
                    DatabankService.moveToPortfolioComposer(strategies).then(function(result) {
                        if(!result.success){
                            $rootScope.showError(L.tsq("Error while moving strategies to Portfolio Composer. %s", [ result.error ]));
                        }
                    });
                });


            }
            else {
                $rootScope.showError(L.tsq("Cannot move strategies when project is in loading/running state"));
            }
        }

        var projectStates = SQConstants.getConstants().runningStatuses;

    }
    
});
