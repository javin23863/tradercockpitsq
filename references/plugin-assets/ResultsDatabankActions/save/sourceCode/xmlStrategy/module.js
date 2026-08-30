//product:BUILDER
//product:OPTIMIZER
//product:RETESTER
//product:TASKMANAGER
angular.module('app.resultsdatabankactions.save.sourceCode.xmlStrategy', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsDatabankAction", 50, {
        title: Ltsq("Save")+":"+Ltsq("Source code")+":"+Ltsq("XML Strategy (*.XML)"),
        extension : {
            name : "xml",
            description : Ltsq("Strategy XML file (.XML)")
        },
        controller: function($scope, SaveButtonService){
            $scope.onClick = function(item){
                SaveButtonService.onItemClick(item);
            }
        },
        id: "databank-action-sourcecode-xml"
    });
});
