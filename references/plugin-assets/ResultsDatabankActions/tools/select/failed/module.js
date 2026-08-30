angular.module('app.resultsdatabankactions.tools.select.failed', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsDatabankAction", 101, {
        title: Ltsq("Tools")+":"+Ltsq("Select")+":"+Ltsq("Failed"),
        action: 'selectFailed',
        product: "RETESTER,TASKMANAGER",
        controller: function($scope, DatabankSelectService){

            $scope.onClick = function(item, tab){
                DatabankSelectService.onSelect(tab.getGrid(), false);
            }
            
        }
    });
});
