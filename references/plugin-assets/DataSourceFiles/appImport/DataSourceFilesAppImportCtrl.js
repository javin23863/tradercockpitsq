angular.module('app.datasource.files').controller('DataSourceFilesAppImportCtrl', function ($rootScope, $scope, $q, SQEvents, SQConstants, DataSourceFilesService, L) {
   
    $scope.action.onSelect = function () {
        console.log('config', $scope.config);
        console.log("DataSourceFilesAppImportCtrl")
        showPopup('#addAppImportModal');
    }
   
    $scope.onSelectFolder = function () {
        var filePicker = $rootScope.showFilePicker(L.tsq('Select data folder'), "sourceImport", false, false);
        filePicker.onSelect = function (paths) {
            if (paths) {
                $scope.config.path = paths[0];

                try {
                    $scope.$digest();
                } catch (er) { }
            }
        }
    }

    $scope.onSave = function (form) {
        if (form) {
            $scope.errors = validate(form);
            if ($scope.errors) {
                console.error($scope.errors);
                return;
            }
        }

        if (typeof $scope.path==''){
            $rootScope.showError("No path is selected."); 
            return;
        }

        DataSourceFilesService.appImport($scope.config,function (result) {
            if (result.success){
               $rootScope.setProgressInfo(L.tsq('Application data import'), L.tsq("Importing data..."), 0, function() {
                    DataSourceFilesService.cancelAppImport($scope.config);
                }, true);
            }
        });


        hidePopup('#addAppImportModal');
    }

    $scope.config = {
        path: "",
        license: ""
    };
});