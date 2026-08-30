angular.module('app.customresultspluginactions.delete', ['sqplugin', 'app.resultstabs.resultsplugins']).config(function(sqPluginProvider) {
    sqPluginProvider.plugin("CustomResultsPluginAction", 50, {
        title: Ltsq("Delete"),
        controller: "CustomResultsDeleteCtrl",
        icon: "fa fa-trash-o"
    });
});

angular.module('app.customresultspluginactions.delete').controller('CustomResultsDeleteCtrl', function($scope, $rootScope, ResultsPluginsService, L) {
    $scope.onClick = function(tab) {
        if (!tab || !tab.pluginName) return;
        $rootScope.showConfirm(L.tsq("Remove custom analysis"), L.tsq("Are you sure you want to remove '%s'?", [tab.pluginName]), function(confirmed) {
            if (!confirmed) return;
            ResultsPluginsService.deletePlugin(tab.pluginName, function(result) {
                if (result && result.success) {
                    $rootScope.showSuccess(result.success);
                    $scope.$emit('RELOAD_RESULTS');
                } else {
                    $rootScope.showError((result && result.error) ? result.error : L.tsq('Failed to remove.'));
                }
            });
        }, true);
    };
});
