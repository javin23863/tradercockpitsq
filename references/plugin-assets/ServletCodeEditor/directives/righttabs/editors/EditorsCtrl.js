angular.module('app').controller('EditorsCtrl', function ($scope, $rootScope, $timeout, SQEvents, CodeEditorService, L) {
    console.log("EditorsCtrl controller initialized");
    $scope.tabs = CodeEditorService.openedTabs;
    $scope.activeTab = null;
    $scope.openFile = function (fileToOpen) {
        SQEvents.notifyListeners(SQEvents.get('EDITOR_FILE_SELECTED'), {
            file: fileToOpen
        });
        /* SQEvents.notifyListeners(SQEvents.get('EDITOR_ACTIVE_CHANGED'), {
            file: fileToOpen
        }); */
    }

    $scope.getRelativePath = function (fullPath) {
        const regex1 = /(.*\\user\\extend\\)(.*)/gm;
        const regex2 = /(.*\\extend\\)(.*)/gm;
        const subst1 = `User\\$2`;
        const subst2 = `Builtin\\$2`;
        return fullPath.replace(/\//gm, "\\").replace(regex1, subst1).replace(regex2, subst2);
    }

    var listenerId = "EditorsCtrl";
    SQEvents.addListener(listenerId, [
        SQEvents.get('EDITOR_ACTIVE_CHANGED'),
    ], onEvent);

    function onEvent(event, data) {
        if (event == SQEvents.get('EDITOR_ACTIVE_CHANGED')) {
            $scope.activeTab = data.file;
            for (var i = 0; i < $scope.tabs.length; i++) {
                if ($scope.tabs[i].file == $scope.activeTab) {
                    if ($('.editors-opened > div').get(i)) {
                        $('.editors-opened > div').get(i).scrollIntoView();
                    }                    
                }
            }
        }
    }
});