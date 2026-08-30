//product:BUILDER
angular.module('app.resultsdatabankactions.retest', ['sqplugin']).config(function(sqPluginProvider, SQEventsProvider) {
    
    sqPluginProvider.addPopupWindow("../../../plugins/ResultsDatabankActions/retest/retestDialog.html", 'RetestDialogCtrl', 'SQUANT');

    sqPluginProvider.plugin("ResultsDatabankAction", 50, {
        title: Ltsq("Retest"),
        class: 'btn btn-normal btn-default',
        controller: btnCtrl,
        excludeTasks: 'Retest',
        id: "databank-action-retest"
    });
    
    SQEventsProvider.add("RETEST_DIALOG_DATA", "retest-dialog-data");

    function btnCtrl($rootScope, $scope, AppService, L, DatabankActionsService, SQConstants, SQEvents){
        
        $scope.onClick = function(){
            if(AppService.getProject() == 'Retester'){
                $rootScope.showError(L.tsq("You are already in Retester"));
                return;
            }

            if($rootScope.project.state != projectStates.loading && $rootScope.project.state != projectStates.running){
                if(DatabankActionsService.selectedStrategies){
                    AppService.updateTaskXML().then(function(){
                        window.parent.broadcastEvent(SQEvents.get("RETEST_DIALOG_DATA"), {
                            project: AppService.getProject(),
                            task : AppService.getTask().name,
                            strategies : DatabankActionsService.selectedStrategies
                        });
                        window.parent.showPopup('#retestConfirmDialog');
                    });
                }
                else {
                    $rootScope.showError(L.tsq("You have to select at least one strategy"));
                }
            }
            else {
                $rootScope.showError(L.tsq("Cannot load records to databank when project is in loading/running state"));
            }
        }

        var projectStates = SQConstants.getConstants().runningStatuses;
    }
    
});
