angular.module('app.resultsdatabankactions.save.sq3', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsDatabankAction", 100, {
        title: Ltsq("Save")+":"+Ltsq("Save stats in SQ3 format"),
        extension : {
            name : "str",
            description : Ltsq("Strategy result files (.str)")
        },
        controller: function($scope, SaveButtonService){
            $scope.onClick = function(item){
                SaveButtonService.onItemClick(item);
            }
        },
        id: "databank-action-save-sq3",
        icon: "fa fa-floppy-o",
    });
});
