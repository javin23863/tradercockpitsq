//product:BUILDER
//product:OPTIMIZER
//product:RETESTER
//product:TASKMANAGER
angular.module('app.resultsdatabankactions.save.sourceCode.mc', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsDatabankAction", 10, {
        title: Ltsq("Save")+":"+Ltsq("Source code")+":"+Ltsq("EasyLanguage for Tradestation/MultiCharts"),
        extension : [
            { name : "el", description : Ltsq("EasyLanguage for Tradestation (*.el)") },
            { name : "pla", description : Ltsq("EasyLanguage for MultiCharts (*.pla)") }
        ],
        controller: function($scope, SaveButtonService){
            $scope.onClick = function(item){
                SaveButtonService.onItemClick(item);
            }
        },
        id: "databank-action-sourcecode-mc"
    });
});
