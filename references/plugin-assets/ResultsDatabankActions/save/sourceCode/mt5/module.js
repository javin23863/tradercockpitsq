//product:BUILDER
//product:OPTIMIZER
//product:RETESTER
//product:TASKMANAGER
angular.module('app.resultsdatabankactions.save.sourceCode.mt5', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsDatabankAction", 30, {
        title: Ltsq("Save")+":"+Ltsq("Source code")+":"+Ltsq("Expert Advisor for MetaTrader5 (*.MQ5)"),
        extension : {
            name : "mq5",
            description : Ltsq("MetaTrader 5 Expert Advisor (.MQ5)")
        },
        controller: function($scope, SaveButtonService){
            $scope.onClick = function(item){
                SaveButtonService.onItemClick(item);
            }
        },
        id: "databank-action-sourcecode-mt5"
    });
});
