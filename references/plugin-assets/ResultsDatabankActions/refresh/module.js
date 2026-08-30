angular.module('app.resultsdatabankactions.refresh', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsDatabankAction", 190, {
        title: "",
        class: 'btn btn-normal btn-default',
        enabledHelp: Ltsq('Refresh databank content'),
        controller: btnCtrl,
        id: "databank-action-refresh"
    });
    
    function btnCtrl($rootScope, $scope, L){
        
        $scope.onClick = function(tab){
            tab.loadStrategies(false, function(){
                tab.getGrid().bodyRedraw(true);
                tab.refreshView();
                $rootScope.showSuccess(L.tsq("Databank reloaded"));
            }, true);
        }

    }
    
});
