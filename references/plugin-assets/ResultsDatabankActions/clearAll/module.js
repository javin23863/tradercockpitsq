angular.module('app.resultsdatabankactions.clearall', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsDatabankAction", 40, {
        title: Ltsq("Clear all"),
        class: 'btn btn-normal btn-default btn-delete',
        controller: btnCtrl,
        id: "databank-action-clear"
    });
    
    function btnCtrl($rootScope, $scope, SQEvents, SQConstants, DatabankService) {
        
        $scope.onClick = function(){
            if($rootScope.project.name == 'Builder' || ($rootScope.project.state != projectStates.loading && $rootScope.project.state != projectStates.running)){
                $rootScope.showConfirm("Removing reports", 
                                    "Are you sure you want to clear all the reports from this databank?", 
                                    onRemove, false, null, null, true);
            }
            else {
                $rootScope.showError("Cannot clear records from databank when project is in loading/running state");
            }
        }

        function onRemove(confirmed){
            if(confirmed){
                $rootScope.setProgressInfo("Removing strategies", "");
                
                DatabankService.removeAllReports().then(function(result){
                    if(result.success){
                        if(result.success != "Canceled") $rootScope.showSuccess("Databank cleared");

                        DatabankService.selectStrategy(null);
                    } 
                    else {
                        $rootScope.showError("Error while removing reports. "+result.error);
                    }
                });
            }
        }

        var projectStates = SQConstants.getConstants().runningStatuses;

    } 
});
