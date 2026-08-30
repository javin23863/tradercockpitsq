angular.module('app.customresultspluginactions.rename').controller('RenameResultsPluginPopupCtrl', function($rootScope, $scope, $timeout, L) {
    var renameCallback = null;

    function isPluginNameValid(name) {
        if (!name || !name.trim()) return false;
        var trimmed = name.trim();
        if (!/^[a-zA-Z0-9_\s-]+$/.test(trimmed)) return false;
        if (trimmed.indexOf('..') !== -1 || trimmed.indexOf('/') !== -1 || trimmed.indexOf('\\') !== -1) return false;
        return true;
    }

    $scope.onRename = function() {
        var newName = ($scope.newName && $scope.newName.trim()) ? $scope.newName.trim() : '';

        if (!isPluginNameValid(newName)) {
            $rootScope.showError(L.tsq("Name may only contain letters, numbers, spaces, hyphens and underscores."));
            return;
        }

        hidePopup("#renameResultsPluginModal");

        if (newName === $scope.pluginName) {
            if (renameCallback) renameCallback(null);
            return;
        }

        if (renameCallback) renameCallback(newName);
    };

    $rootScope.showResultsPluginRenamePopup = function(pluginName, callback) {
        $scope.pluginName = pluginName;
        $scope.newName = pluginName;
        renameCallback = callback;
        showPopup("#renameResultsPluginModal");
        $timeout(function() {
            var el = document.querySelector("#renameResultsPluginNameInput input");
            if (el) el.focus();
        }, 100);
    };
});
