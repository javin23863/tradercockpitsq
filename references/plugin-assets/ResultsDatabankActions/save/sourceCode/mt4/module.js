//product:BUILDER
//product:OPTIMIZER
//product:RETESTER
//product:TASKMANAGER
angular.module('app.resultsdatabankactions.save.sourceCode.mt4', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsDatabankAction", 20, {
        title: Ltsq("Save")+":"+Ltsq("Source code")+":"+Ltsq("Expert Advisor for MetaTrader4 (*.MQ4)"),
        extension : {
            name : "mq4",
            description : Ltsq("MetaTrader 4 Expert Advisor (.MQ4)")
        },
        controller: function($scope, SaveButtonService){
            $scope.onClick = function(item){
                SaveButtonService.onItemClick(item);
            }
        },
        id: "databank-action-sourcecode-mt4"
    });
});
