angular.module('app.tmprojects').controller('TMProjectControlCtrl', function ($scope, $rootScope, $timeout, L, AppService, SQConstants, TMProjectsService, UniqueIdGenerator) {
    console.log("TMProjectControl controller initialized");

    $('.sq-app-taskmanager').addClass('sqn-hide-header');

    $scope.rowId = cssId("tm-projects-"+$scope.instance.projectName);

    $scope.$on('$destroy', function () {
        UniqueIdGenerator.removeId($scope.rowId);
    });

    function updateStats() {
        var thisProjectStats = getItem(TMProjectsService.lastProjectStats, 'projectName', $scope.instance.projectName);
        if (thisProjectStats) {
            $scope.databanks = thisProjectStats.databanks;
            $scope.strategies = thisProjectStats.strategies;
            $scope.tasks = thisProjectStats.tasks;

            if ($scope.controlPanelInstance && $scope.controlPanelInstance.updateStatus) {
                $scope.controlPanelInstance.updateStatus(thisProjectStats.runningStatus);
            }
        }
    }

    $scope.resolveResources = function () {
        TMProjectsService.checkResources({projectName: $scope.instance.projectName}, function(data) {
            if(data.hasUnresolvedResources) {
                let config = JSON.parse(data.config);

                console.log("aaaaaaaaaa", config.xml)
                $rootScope.resolveResources(config.xml, data.resourcesXML, SQConstants.getConstants().projectResourceActionTypes.resolveProjectResources, function (resolveData) {
                    $scope.instance.loadProjects();
                }, $scope.instance.filePath);
            } else {
                $scope.instance.loadProjects();
            }
        })
    }

    $scope.switchToPanel = function (panel) {
        AppService.setProject($scope.instance.projectName, function () {
            AppService.switchToPanel(panel);
        });
    }

    $scope.onRemove = function () {
        var projectName = $scope.instance.projectName;

        $rootScope.showConfirm(L.tsq("Remove project"), L.tsq("Are you sure you want to remove project '%s'?", [projectName]), function (confirmed) {
            if (!confirmed) {
                return;
            }

            TMProjectsService.removeProject(projectName, function (data) {
                SQConstants.setProjectConfigObj(projectName, null);

                $scope.instance.loadProjects();
            });
        });
    }

    $scope.onSave = function () {
        var projectName = $scope.instance.projectName;
        var fileExtension = { name: 'cfx', description: L.tsq('SQX Project Files') };

        $rootScope.saveFile(L.tsq('Save project'), fileExtension, "tmSaveProject", projectName, "", function (path) {
            TMProjectsService.saveProject(projectName, path, function (data) {
                $rootScope.showSuccess(data.success);
                window.parent.hidePopup("#saveFileDialog");
            });
        });
    }

    $scope.onRename = function () {
        $scope.instance.renameProject($scope.instance.projectName);
    }

    $scope.onClone = function () {
        $scope.instance.cloneProject($scope.instance.projectName);
    }

    $scope.onModifySymbols = function () {
        $scope.instance.modifySymbols($scope.instance.projectName);
    }

    $scope.instance.updateStats = updateStats;

    $scope.databanks = $scope.instance.databanks;
    $scope.strategies = $scope.instance.strategies;
    $scope.tasks = $scope.instance.tasks;

    $scope.controlPanelInstance = {};

    $timeout(function () {
        updateStats();
        try { $scope.$digest(); } catch (err) { }
    }, 0, false);

    $scope.showContextMenu = function (e) {
        $("#" + $scope.rowId).contextMenu({
            x: e.pageX,
            y: e.pageY,
        });
    }
    $.contextMenu('destroy', "#" + $scope.rowId);
    $timeout(function () {
        let element = window.parent.document.getElementById('task-manager-frame').contentWindow.document.getElementById($scope.rowId);
        $.contextMenu({
            selector: "#" + $scope.rowId,
            appendTo: element || document.body,
            callback: function (itemKey, opt, e) {
                $scope[itemKey]();
            },
            items: {
                "onSave": {
                    name: L.tsq("Save custom project config"),
                    icon: "fa-floppy-o",
                },
                "onRename": {
                    name: L.tsq("Rename custom project"),
                    icon: "fa-i-cursor",
                },
                "onClone": {
                    name: L.tsq("Clone custom project"),
                    icon: "fa-clone",
                },
                "onRemove": {
                    name: L.tsq("Delete custom project"),
                    icon: "fa-trash-o",
                },
                "onModifySymbols": {
                    name: L.tsq("Modify symbols"),
                    icon: "fa-retweet",
                },
            }
        });
    }, 100, false);


});