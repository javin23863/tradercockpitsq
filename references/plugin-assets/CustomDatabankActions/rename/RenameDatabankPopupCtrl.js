angular.module('app.databanks').controller('RenameDatabankPopupCtrl', function ($rootScope, $scope, $timeout, L) {
    console.log("RenameDatabankPopupCtrl initialized");
    
    $scope.onRename = function () {
        var databankName = $scope.newDatabank.name;

        if (!isFileNameValid(databankName)) {
            $rootScope.showError(L.tsq("Databank name contains forbidden characters."));
            return;
        }

        hidePopup("#renameDatabankModal");

        if(databankName==$scope.databankName) {
            return;
        }

        if(databankCallback) databankCallback(databankName);
    }

    $rootScope.showDBRenamePopup = function(databankName, callback){
        $scope.databankName = databankName;
        $scope.newDatabank.name = databankName;

        databankCallback = callback;

        showPopup("#renameDatabankModal");
        focusSQInput("#renameDBNameInput");
    }

    function focusSQInput(selector) {
        $timeout(function () {
            $(selector).find('input').trigger('focus');
        });
    }

    $scope.newDatabank = {
        name : ''
    }

});