angular.module('app.resultsdatabankactions.save.sourceCode.jforex', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsDatabankAction", 40, {
        title: Ltsq("Save")+":"+Ltsq("Source code")+":"+Ltsq("Expert Advisor for JForex (*.java)"),
        extension : {
            name : "java",
            description : Ltsq("JForex Expert Advisor (.java)")
        },
        controller: function($scope, SaveButtonService){
            $scope.onClick = function(item){
                SaveButtonService.onItemClick(item);
            }
        },
        id: "databank-action-sourcecode-jforex"
    });
});
