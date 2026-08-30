angular.module('app.databanks').controller('NewDatabankPopupCtrl', function ($rootScope, $scope, $timeout, L) {
    console.log("NewDatabankPopupCtrl initialized");

    $scope.createDatabank = function () {
        var databankName = $scope.newDatabank.name;

        if (!isFileNameValid(databankName)) {
            $rootScope.showError(L.tsq("Databank name contains forbidden characters."));
            return;
        }

        hidePopup("#newDatabankModal");

        if (databankCallback) databankCallback(databankName);
    }

    function focusSQInput(selector) {
        $timeout(function () {
            $(selector).find('input').trigger('focus');
        });
    }

    $scope.newDatabank = {
        name: ''
    }

    if (window.appConfig.product == 'SQUANT') {
        var databankCallback = null;

        $rootScope.createDatabank = function (callback) {
            databankCallback = callback;

            showPopup("#newDatabankModal");
            focusSQInput("#newDBNameInput");
        }
    }

});