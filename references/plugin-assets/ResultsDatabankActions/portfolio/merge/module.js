angular.module('app.resultsdatabankactions.mergestrategies', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsDatabankAction", 100, {
        title: Ltsq("Portfolio")+":"+Ltsq("Merge strategies"),
        class: 'btn btn-normal btn-default',
        controller: btnCtrl,
        product: "BUILDER,RETESTER,OPTIMIZER,TASKMANAGER,PORTFOLIOMASTER",
        id: "databank-action-mergestrategies"
    });

    sqPluginProvider.addPopupWindow("../../../plugins/ResultsDatabankActions/portfolio/merge/mergeStrategiesPopup.html", 'MergeStrategiesPopupCtrl', 'SQUANT');
    
    function btnCtrl($rootScope, $scope, DatabankService, DatabankActionsService, L, SQConstants, AppService){
        
        $scope.onClick = function(){
            if($rootScope.project.state != projectStates.loading && $rootScope.project.state != projectStates.running) {
                if(!DatabankActionsService.selectedStrategies) {
                    $rootScope.showError(L.tsq("You have to select at least two strategies to merge."));
                    return;
                }

                var strategies = DatabankActionsService.selectedStrategies.split(',');
                if(strategies[0] != 'all' && strategies.length<2) {
                    $rootScope.showError(L.tsq("You have to select at least two strategies to merge."));
                    return;
                }

                var databanks = [];
                DatabankService.loadVisibleDatabanks(databanks, function(){
                    window.parent.broadcastEvent("showMergeStrategiesPopup", { databanks : databanks });
                }, true);
            }
            else {
                $rootScope.showError(L.tsq("Cannot merge strategies when project is in loading/running state"));
            }
        }

        var projectStates = SQConstants.getConstants().runningStatuses;

    }
    
});
