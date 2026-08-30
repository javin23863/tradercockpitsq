angular.module('app').controller('loadBtnCtrl', function ($rootScope, $scope, SQConstants, L, DatabankService, LoadService) {

    $scope.onClick = function () {
        if ($rootScope.project.state != projectStates.loading && $rootScope.project.state != projectStates.running) {
            $rootScope.showFilePicker(L.tsq('Select files to load'), "databankSelectFile-sq4", true, true, null, fileExtension, null, function (paths) {

                DatabankService.loadFiles(paths).then(function (result) {
                    if (result.error) {
                        $rootScope.showError(L.tsq("Error while loading reports. ") + result.error);

                        window.parent.hidePopup('#loadModal');
                    }
                });
            });
        }
        else {
            $rootScope.showError(L.tsq("Cannot load records to databank when project is in loading/running state"));
        }
    }

    $(document).on('keydown', function (e) {
        if (window.appConfig.product == window.top.activeApp) {
            if (e.ctrlKey && e.key == "o") {
                e.preventDefault();
                $scope.onClick();
                return false;
            }
        }
    });

    var fileExtension = {
        name: 'sq4,str,stp,owf,sqw,sqx',
        description: 'SQ X Strategy Files'
    }

    var projectStates = SQConstants.getConstants().runningStatuses;

    LoadService.subscribeToProgressChannel();

});