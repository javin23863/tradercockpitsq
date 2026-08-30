//product:BUILDER
//product:OPTIMIZER
//product:RETESTER
//product:TASKMANAGER
angular.module('app.resultsdatabankactions.save.sourceCode.pseudoCode', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsDatabankAction", 60, {
        title: Ltsq("Save")+":"+Ltsq("Source code")+":"+Ltsq("Pseudo code (*.TXT)"),
        extension : {
            name : "txt",
            description : Ltsq("Pseudo code text file (.TXT)")
        },
        controller: function($scope, SaveButtonService){
            $scope.onClick = function(item){
                SaveButtonService.onItemClick(item);
            }
        },
        id: "databank-action-sourcecode-pseudo"
    });
});
