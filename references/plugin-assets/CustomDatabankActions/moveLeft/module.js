angular.module('app.customdatabankactions.moveLeft', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("CustomDatabankAction", 20, {
        title: Ltsq("Move left"),
        controller: btnCtrl,
        icon: "fa fa-chevron-circle-left",
    });
    
    function btnCtrl($rootScope, $scope, DatabankService, SQEvents){
        
        $scope.onClick = function(tab){
            if (!tab) return;
    
            DatabankService.moveDatabank(tab.title, true, function (data) {
                SQEvents.notifyListeners(SQEvents.get("CUSTOM_DATABANK_ACTION"), { name : "move", tab : tab });
            });
        }
        
    }
    
});