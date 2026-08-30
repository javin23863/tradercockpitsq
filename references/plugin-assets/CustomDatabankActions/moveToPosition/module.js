angular.module('app.customdatabankactions.moveToPosition', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("CustomDatabankAction", 40, {
        title: Ltsq("Move to position"),
        controller: btnCtrl,
        icon: "fa fa-arrows-h",
    });
    
    function btnCtrl($rootScope, $scope, DatabankService, SQEvents){
        
        $scope.onClick = function(tab){
            if (!tab) return;

            window.parent.callAction('showMoveDatabankDialog', function(newPosition){
                DatabankService.moveDatabankToPosition(tab.title, newPosition, function (data) {
                    SQEvents.notifyListeners(SQEvents.get("CUSTOM_DATABANK_ACTION"), { name : "move", tab : tab });
                });
            });
        }
        
    }
    
});