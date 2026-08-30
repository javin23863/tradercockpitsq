angular.module('app.resultsdatabankactions.save.html', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsDatabankAction", 20, {
        title: Ltsq("Save")+":"+Ltsq("HTML report"),
        extension : {
            name : "html",
            description : Ltsq("HTML report files (.HTML)")
        },
        controller: function($rootScope, $scope, SaveButtonService, L){
            $scope.onClick = function(item){
                SaveButtonService.onItemClick(item);
            }
        },
        id: "databank-action-save-html",
        icon: "fa fa-download",
    });
});
