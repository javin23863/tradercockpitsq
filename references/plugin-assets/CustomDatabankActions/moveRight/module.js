angular.module('app.customdatabankactions.moveRight', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("CustomDatabankAction", 30, {
        title: Ltsq("Move right"),
        controller: btnCtrl,
        icon: "fa fa-chevron-circle-right",
    });
    
    function btnCtrl($rootScope, $scope, DatabankService, SQEvents){
        
        $scope.onClick = function(tab){
            if (!tab) return;
    
            DatabankService.moveDatabank(tab.title, false, function (data) {
                SQEvents.notifyListeners(SQEvents.get("CUSTOM_DATABANK_ACTION"), { name : "move", tab : tab });
            });
        }
        
    }
    
});