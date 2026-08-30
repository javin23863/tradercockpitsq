angular.module('app.resultsdatabankactions.rename', ['sqplugin']).config(function(sqPluginProvider) {
    
    //registering plugin for Databank Actions
    sqPluginProvider.plugin("ResultsDatabankAction", 55, {
        title: "Rename",
        class: 'btn btn-normal btn-default',
        controller: btnCtrl,
        id: "databank-action-rename"
    });

    //creating popup window
    sqPluginProvider.addPopupWindow("../../../plugins/DatabankRename/ui/databankRenamePopup.html", 'DatabankRenamePopupCtrl', 'SQUANT');
    
    function btnCtrl($rootScope, $scope, DatabankService, DatabankActionsService, L, SQConstants, AppService){
        
        //user clicked on the button
        $scope.onClick = function(){

            //checking if project is running
            if($rootScope.project.state != projectStates.loading && $rootScope.project.state != projectStates.running) {

                //checking selected strategies
                if(!DatabankActionsService.selectedStrategies) {
                    $rootScope.showError(L.tsq("You have to select at least one strategy to rename."));
                    return;
                }

                //opening popup window
                window.parent.broadcastEvent("showDatabankRenamePopup", {});
                
            } else { //project is running, cannot rename strategies
                $rootScope.showError(L.tsq("Cannot rename strategies when project is in loading/running state"));
            }
        }

        var projectStates = SQConstants.getConstants().runningStatuses;

    }
});
