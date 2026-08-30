angular.module('app.customdatabankactions.delete', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("CustomDatabankAction", 50, {
        title: Ltsq("Delete"),
        controller: btnCtrl,
        icon: "fa fa-trash-o",
    });
    
    function btnCtrl($rootScope, $scope, DatabankService, SQEvents, L){
        
        $scope.onClick = function(tab){
            if (!tab) return;
    
            var databankName = tab.title;
    
            $rootScope.showConfirm(L.tsq("Remove databank"), L.tsq("Are you sure you want to remove databank '%s'?", [databankName]), function (confirmed) {
                if (!confirmed) {
                    return;
                }
    
                var taskNames = DatabankService.checkDatabankUsed(databankName);
                if (taskNames.length) {
                    $rootScope.showError(L.tsq("Cannot remove databank '%s' as it is currently used by tasks: ", [databankName]) + toSingleString(taskNames, ', '));
                    return;
                }
    
                DatabankService.removeDatabank(databankName, function (data) {
                    SQEvents.notifyListeners(SQEvents.get("CUSTOM_DATABANK_ACTION"), { name : "delete", tab : tab });
                    $rootScope.showSuccess(data.success);
                });
            }, true);
        }
    }
    
});