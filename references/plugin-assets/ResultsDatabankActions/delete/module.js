angular.module('app.resultsdatabankactions.remove', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsDatabankAction", 30, {
        title: Ltsq("Delete"),
        class: 'btn btn-normal btn-default btn-delete',
        controller: btnCtrl,
        id: "databank-action-delete"
    });
    
    function btnCtrl($rootScope, $scope, DatabankService, DatabankActionsService, SQEvents, SQConstants, L){
        
        var databankTab = null;

        $scope.onClick = function(tab){
            databankTab = tab;

            if($rootScope.project.name == 'Builder' || ($rootScope.project.state != projectStates.loading && $rootScope.project.state != projectStates.running)){
                if(DatabankActionsService.selectedStrategies){
                    var strategies = DatabankActionsService.selectedStrategies.split(',');
                    var count = strategies[0] != 'all' ? strategies.length : 'all';

                    $rootScope.showConfirm(L.tsq("Removing reports"), 
                                        L.tsq("Are you sure you want to remove selected reports (%d)?", [count]), 
                                        onRemove, false, null, null, true);
                }
                else {
                    $rootScope.showError(L.tsq("You have to select at least one strategy to delete"));
                }
            }
            else {
                $rootScope.showError(L.tsq("Cannot delete records from databank when project is in loading/running state"));
            }
        }
        
        function onRemove(confirmed){
            if(confirmed){
                $rootScope.setProgressInfo(L.tsq("Removing strategies"), "");

                databankTab.deleteStrategies(DatabankActionsService.selectedStrategies, true);
            }
        }

        var projectStates = SQConstants.getConstants().runningStatuses;

    }
    
});
