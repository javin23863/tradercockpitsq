angular.module('app.resultsdatabankactions.save.sq4', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsDatabankAction", 10, {
        title: Ltsq("Save")+":"+Ltsq("Save to SQ X format"),
        extension : {
            name : "sqx",
            description : Ltsq("StrategyQuant files (.SQX)")
        },
        controller: function($scope, SaveButtonService){
            $scope.onClick = function(item){
                SaveButtonService.onItemClick(item);
            }
        },
        id: "databank-action-save-sq4",
        icon: "fa fa-floppy-o",
    });
});
