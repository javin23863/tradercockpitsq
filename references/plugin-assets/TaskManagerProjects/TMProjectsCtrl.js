angular.module('app.tmprojects').controller('TMProjectsCtrl', function ($rootScope, $scope, $timeout, $q, AppService, TMProjectsService, ProjectControlPanelService, SQConstants, SQWebSocketService, L, SQEvents) {
    console.log("TMProjects controller initialized");
    $scope.broker = SQConstants.getConstants().broker;
    $scope.isBrazilianEdition = SQConstants.getConstants().isBrazilianEdition;

    $scope.onAddNewProject = function () {
        $scope.errors = null;
        $scope.project.projectName = '';

        showPopup('#newProjectModal');
        $('#newProjectModal').find('.modal-body input').trigger('focus');
    }

    $scope.onOpenProject = function () {
        $scope.project.projectName = null;
        var fileExtension = { name: 'cfx', description: L.tsq('SQX Project Files') };

        $rootScope.showFilePicker(L.tsq('Open existing project'), "tmOpenProject", true, false, null, fileExtension, null, function (paths) {
            if (paths) {

                TMProjectsService.openProject(paths[0], false, function (data) {
                    if (data.resourcesXML) {
                        $rootScope.resolveResources(data.configXML, data.resourcesXML, SQConstants.getConstants().projectResourceActionTypes.loadProject, function (resolveData) {
                            if (resolveData && resolveData.loadAsIs) {
                                TMProjectsService.openProject(paths[0], true, function (result) {
                                    loadProjects(function () {
                                        if (result.projectName) {
                                            setProjectBlinking(result.projectName);
                                        }
                                    });
                                    $rootScope.showSuccess(result.success);
                                });
                            }
                            else {
                                //project is loaded on backend
                                loadProjects(function () {
                                    if (data.projectName) {
                                        setProjectBlinking(data.projectName);
                                    }
                                });
                            }
                        }, paths[0]);
                    }
                    else {
                        loadProjects(function () {
                            if (data.projectName) {
                                setProjectBlinking(data.projectName);
                            }
                        });
                        $rootScope.showSuccess(data.success);
                    }
                });
            }
        });
    }

    $scope.onCreateProject = function (form) {
        if (form) {
            $scope.errors = validate(form);
            if ($scope.errors) {
                return;
            }
        }

        var nameValidationError = getProjectNameValidationError($scope.project.projectName);
        if (nameValidationError) {
            $scope.errors = [nameValidationError];
            return;
        }

        var projectName = $scope.project.projectName.trim();
        TMProjectsService.addProject(projectName, function (data) {
            loadProjects(function () {
                setProjectBlinking(data.projectName);
            });            
        });

        hidePopup('#newProjectModal');
    }

    var blinkingTimeout = null;
    function setProjectBlinking(projectName) {
        $timeout(function () {
            $(".tm-projects-table-item").removeClass("latest-added");
            var items = $(".tm-projects-table-item");
            for (var i = 0; i<items.length; i++) {
                if ($(items[i]).data("name")==projectName) {
                    $(items[i]).addClass("latest-added");
                    items[i].scrollIntoView({ behavior: 'smooth' });
                    // $(".tm-projects-table-wrapper").scrollTo(items[i]);
                    break;
                }
            }
            if (blinkingTimeout) {
                clearTimeout(blinkingTimeout);
            }
            blinkingTimeout = setTimeout(function() {
                $(".tm-projects-table-item").removeClass("latest-added");
                blinkingTimeout = null;
            }, 10000);
        }, 0, false);        
    }

    $scope.onRenameProject = function (form) {
        if (form) {
            $scope.errors = validate(form);
            if ($scope.errors) {
                return;
            }
        }

        var nameValidationError = getProjectNameValidationError($scope.project.projectName);
        if (nameValidationError) {
            $scope.errors = [nameValidationError];
            return;
        }

        var projectName = $scope.project.projectName.trim();
        TMProjectsService.renameProject($scope.project.oldName, projectName, function (data) {
            SQConstants.setProjectConfig($scope.project.oldName, null);

            loadProjects(function () {
                setProjectBlinking(projectName);
            });  
        });

        hidePopup('#renameProjectModal');
    }

    function onWebSocketData(data) {
        if (!data) {
            return;
        }

        if (data.customProjectStats) {
            //save stats to TMProjectsService and update progress icon
            TMProjectsService.lastProjectStats.length = 0;
            var atLeastOneRunning = false;

            for (var i = 0; i < data.customProjectStats.length; i++) {
                var stats = data.customProjectStats[i];
                atLeastOneRunning = atLeastOneRunning || stats.runningStatus == projectStates.running;
                TMProjectsService.lastProjectStats.push(stats);
            }

            displayProgressIcon(atLeastOneRunning);

            //update project panel stats
            for (var i = 0; i < $scope.projects.length; i++) {
                var project = $scope.projects[i];
                if (project.updateStats) project.updateStats();
            }
        }

        try { $scope.$digest(); } catch (err) { }
    }

    function onCustomProjectAction(event, data) {
        projectsLoadedDeferred.promise.then(function () {
            if (data.action == "open") {
                AppService.setProject(data.projectName, function () {
                    AppService.switchToPanel('dashboard');
                });
            }
            else if (data.action == "start") {
                AppService.setProject(data.projectName, function () {
                    AppService.switchToPanel('dashboard');
                    $timeout(function () { ProjectControlPanelService.start(data.projectName); }, 200, false);
                });
            }
            else {
                console.error("Unknown action called", data);
            }
        });
    }

    function displayProgressIcon(display) {
        if (progressDisplayed != display) {
            window.parent.document.getElementById(window.appConfig.product + "-menuprogress").style.display = display ? "block" : "none";
            progressDisplayed = display;
        }
    }

    function loadProjects(callback) {
        TMProjectsService.loadProjects(function (data) {
            $scope.projects.length = 0;

            for (var i = 0; i < data.projects.length; i++) {
                var project = data.projects[i];

                $scope.projects.push({
                    projectName: project.projectName,
                    tasks: project.tasks,
                    databanks: project.databanks,
                    strategies: project.strategies,
                    hasUnresolvedResources: project.hasUnresolvedResources,
                    filePath: project.filePath,
                    loadProjects: loadProjects,
                    renameProject: renameProject,
                    cloneProject: cloneProject,
                    modifySymbols: modifySymbols
                });
            }

            if (callback) {
                callback();
            }

            $timeout(function () { projectsLoadedDeferred.resolve(); }, 0, false);
        });
    }

    function getProjectNameValidationError(projectName) {
        if (!projectName || !projectName.trim()) {
            return L.tsq("Project name cannot be empty.");
        }

        var trimmed = projectName.trim();
        var forbiddenChars = /[\\\/:\*\?"<>\|]/;
        if (forbiddenChars.test(trimmed)) {
            return L.tsq("Project name contains forbidden characters: \\ / : * ? \" < > |");
        }

        // Keep parity with file-name checks used in other dialogs.
        if (trimmed.startsWith(".")) {
            return L.tsq("Project name cannot start with dot.");
        }

        if (/[^\S ]/.test(trimmed)) {
            return L.tsq("Project name cannot contain tabs or new lines.");
        }

        if (/^(nul|prn|con|lpt[0-9]|com[0-9])(\.|$)/i.test(trimmed)) {
            return L.tsq("Project name uses a reserved system name.");
        }

        return null;
    }

    function renameProject(projectName) {
        $scope.errors = null;
        $scope.project.oldName = projectName;
        $scope.project.projectName = projectName;

        showPopup('#renameProjectModal');
        $('#renameProjectModal').find('.modal-body input').trigger('focus');
    }

    function cloneProject(projectName) {
        TMProjectsService.cloneProject(projectName, function () {
            loadProjects();
        });
    }

    //-----------------------------------------------------------------------------------------------------------
    $scope.modifySymbols = [];
    $scope.modifySymbolsList = [];
    $scope.modifySymbolsProject = null;

    $scope.exampleSymbols = [
        {
            "symbolFrom": "EURUSD/H1",
            "symbolTo": "GBPUSD/H1"
        },
        {
            "symbolFrom": "USDJPY/H1",
            "symbolTo": "USDJPY/H4"
        },
        {
            "symbolFrom": "EURUSD/*",
            "symbolTo": "GBPUSD/*"
        },
        {
            "symbolFrom": "*/H1",
            "symbolTo": "*/M30"
        }
    ]

    function modifySymbols(projectName) {
        $scope.modifySymbolsProject = projectName;
        $scope.modifySymbols.length = 0;

        for (var i = 0; i < 10; i++) {
            $scope.modifySymbols.push({
                symbolFrom: null,
                symbolTo: null,
                applyDefaultSettings: false
            })
        }

        TMProjectsService.modifySymbolsList(projectName, function (data) {
            loadToArray($scope.modifySymbolsList, data.symbols);
        });

        showPopup('#modifySymbolsModal');
        $('#modifySymbolsModal').find('.modal-body input').trigger('focus');
    }

    $scope.onModifySymbols = function (form) {
        /*if (form) {
            $scope.errors = validate(form);
            if ($scope.errors) {
                return;
            }
        }*/

        var data = {
            projectName: $scope.modifySymbolsProject,
            symbols: JSON.stringify($scope.modifySymbols),
        }

        TMProjectsService.modifySymbols(data, function (data) {
            hidePopup('#modifySymbolsModal');

            TMProjectsService.getXMLConfig({
                projectName: $scope.modifySymbolsProject
            }, function(data){
                SQConstants.setProjectConfig($scope.modifySymbolsProject, data.config.xml, data.config.tasks);
                $rootScope.showSuccess("Project symbols updated.");
            })
        });
    }

    $scope.loadProjects = function () {
        loadProjects(function () {
            $rootScope.showSuccess("Projects reloaded.");
        });
    }

    //-----------------------------------------------------------------------------------------------------------

    $scope.$on('$destroy', function () {
        SQWebSocketService.unsubscribeGeneral(listenerId);
    });

    $scope.projects = TMProjectsService.projects;
    $scope.project = { projectName: null };

    var projectsLoadedDeferred = $q.defer();
    var projectStates = SQConstants.getConstants().runningStatuses;

    var progressDisplayed = false;

    loadProjects();

    var listenerId = "TMProjectsCtrl-" + window.appConfig.appCode;

    SQWebSocketService.subscribeGeneral(listenerId, onWebSocketData);

    $('.sq-app-taskmanager').addClass('sqn-hide-header');

    $scope.showMenu = function () {
        $($scope.element).find(".dropdown").toggleClass("open");
    };

    $scope.hideMenu = function () {
        $($scope.element).find(".dropdown").removeClass("open");
    }

    SQEvents.addListener(listenerId, [SQEvents.get('CUSTOM_PROJECT_ACTION')], onCustomProjectAction);

});