angular.module('app.customresultspluginactions.rename', ['sqplugin', 'app.resultstabs.resultsplugins']).config(function(sqPluginProvider) {
    sqPluginProvider.addPopupWindow("../../../plugins/CustomResultsPluginActions/rename/renameResultsPluginPopup.html", 'RenameResultsPluginPopupCtrl', 'RESULTS');
    sqPluginProvider.plugin("CustomResultsPluginAction", 10, {
        title: Ltsq("Rename"),
        controller: "CustomResultsRenameCtrl",
        icon: "fa fa-i-cursor"
    });
});

angular.module('app.customresultspluginactions.rename').controller('CustomResultsRenameCtrl', function($scope, $rootScope, ResultsPluginsService, L) {
    $scope.onClick = function(tab) {
        if (!tab || !tab.pluginName) return;
        $rootScope.showResultsPluginRenamePopup(tab.pluginName, function(newName) {
            if (!newName) return;
            ResultsPluginsService.renamePlugin(tab.pluginName, newName, function(result) {
                if (result && result.success) {
                    $rootScope.showSuccess(result.success);
                    $scope.$emit('RELOAD_RESULTS');
                } else {
                    $rootScope.showError((result && result.error) ? result.error : L.tsq('Failed to rename.'));
                }
            });
        });
    };
});
