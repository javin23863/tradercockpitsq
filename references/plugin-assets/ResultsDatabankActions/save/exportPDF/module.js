angular.module('app.resultsdatabankactions.save.pdf', ['sqplugin']).config(function (sqPluginProvider) {

    sqPluginProvider.plugin("ResultsDatabankAction", 30, {
        title: Ltsq("Save")+":"+Ltsq("PDF report"),
        extension: {
            name: "pdf",
            description: Ltsq("PDF report files (.PDF)")
        },
        controller: function ($rootScope, $scope, SaveButtonService) {
            $scope.onClick = function (item) {
                SaveButtonService.onItemClick(item);
            }
        },
        id: "databank-action-save-pdf",
        icon: "fa fa-download",
    });
});
