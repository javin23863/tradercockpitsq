angular.module('app.customdatabankactions.rename', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.addPopupWindow("../../../plugins/CustomDatabankActions/rename/renameDatabankPopup.html", 'RenameDatabankPopupCtrl', 'SQUANT');

    sqPluginProvider.plugin("CustomDatabankAction", 10, {
        title: Ltsq("Rename"),
        controller: btnCtrl,
        icon: "fa fa-i-cursor",
    });
    
    function btnCtrl($rootScope, $scope, DatabankService, SQEvents, SQConstants, L){
        
        $scope.onClick = function(tab){
            if (!tab) return;

            var databankName = tab.title;

            if ($rootScope.project.state == projectStates.loading || $rootScope.project.state == projectStates.running || $rootScope.project.state == projectStates.paused) {
                $rootScope.showError(L.tsq("Cannot rename databank '%s' when project is running ", [databankName]));
                return;
            }
    
            window.parent.callAction("showDBRenamePopup", databankName, function(newName){
                DatabankService.renameDatabank(tab.title, newName, function (data) {
                    SQEvents.notifyListeners(SQEvents.get("CUSTOM_DATABANK_ACTION"), { name : "rename", oldName : tab.title, newName : newName });
                    SQEvents.notifyListeners(SQEvents.get("SETTINGS_TAB_RELOAD"), 'all');
                    $rootScope.showSuccess(L.tsq("Databank renamed"));
                });
            });
        }
        
        var projectStates = SQConstants.getConstants().runningStatuses;
    }
    
});