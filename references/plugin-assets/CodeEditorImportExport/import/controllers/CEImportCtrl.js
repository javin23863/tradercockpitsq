angular.module('app').controller('CEImportCtrl', function ($rootScope, $scope, ImportExportService, L, SQWebSocketService, SQEvents, CodeEditorService) {
    console.debug("CEImportCtrl initialized");

    $scope.onPluginClicked = function (item) {
        var extension = {
            name: 'sxp',
            description: L.tsq('SQ Extension Files')
        };

        $rootScope.showFilePicker(L.tsq("Select files to import"), ImportExportService.pathKey, true, true, null, ImportExportService.fileExtension, null, function (paths) {
            if (paths) {
                ImportExportService.import(paths, function(result) {
                    $scope.imported.files = result.data.newFiles;
                    $scope.imported.notImportedFiles = result.data.notImportedFiles;

                    SQEvents.notifyListeners(SQEvents.get('EDITOR_RELOAD_NAVIGATOR'), null);

                    showPopup("#CEImportedFilesPopup");
                });
            }
        });
    }

    function onWebSocketData(data) {
        if (data.overwriteConfirmation) {
            $scope.filePath = (data.overwriteConfirmation.filePath.length > 30) ? ("..." + data.overwriteConfirmation.filePath.slice(-20)) : data.overwriteConfirmation.filePath;

            try {
                $scope.$digest();
            } catch (err) {}

            showPopup("#CEOverwriteConfirmPopup");
        }
    }

    $scope.onRecompileAll = function(){
        CodeEditorService.compileAll();
        $rootScope.showSuccess(L.tsq("Compilation started"));
    }

    $scope.$on("$destroy", function () {
        SQEvents.removeListener(listenerId);
    });

    $scope.onOverwrite = ImportExportService.onOverwrite;
    $scope.onOverwriteAlways = ImportExportService.onOverwriteAlways;
    $scope.onSkip = ImportExportService.onSkip;
    $scope.onSkipAlways = ImportExportService.onSkipAlways;
    $scope.onCancel = ImportExportService.onCancel;

    $scope.imported = {
        files : []
    }

    var listenerId = "CEImportCtrl";
    SQWebSocketService.subscribeGeneral(listenerId, onWebSocketData);

});