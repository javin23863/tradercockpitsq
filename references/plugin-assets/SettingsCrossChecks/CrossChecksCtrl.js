angular.module('app.settings').controller('CrossChecksCtrl', function ($rootScope, $scope, $element, $timeout, $q, AppService, SQEvents, SQConstants, CrossChecksService, CrossCheckAcceptanceSettingsService, sqPlugin, L) {
    $scope.config = CrossChecksService.config;
    $scope.license = {};

    function init() {
        let projectConfigObj = SQConstants.getProjectConfigObj(AppService.getProject());
        if(!projectConfigObj.tasks || Object.keys(projectConfigObj.tasks).length === 0){   //empty project
            return;
        }

        CrossChecksService.list(function () {
            for (var i = 0; i < $scope.config.crosschecks.length; i++) {
                var crosscheck = $scope.config.crosschecks[i];
                crosscheck.settingsInfo = L.tsq("N/A");
                crosscheck.acceptSettingsInfo = L.tsq("N/A");
                crosscheck.openSettings = openSettings;
                crosscheck.settingsChanged = $scope.settingsChanged;
            }

            CrossChecksService.loadConfig();

            $scope.isRetester = AppService.getTask().type == 'Retest';

            afterSettingsChanged();
            loaded = true;

            $timeout(function () {
                $('.cross-check .nr').each(function (index) {
                    $(this).text((index + 1) + ".");
                });
            }, 0, false);
        })
    }

    function findSettingsPluginByName(name) {
        var plugins = sqPlugin.getPlugins("CrossCheck");
        for (var i = 0; i < plugins.length; i++) {
            var plugin = plugins[i];

            if (plugin.name == name) {
                return plugin;
            }
        }

    }

    function openSettings(crosscheck, acceptanceTab) {
        var plugin = findSettingsPluginByName(crosscheck.name);
        if (!plugin) {
            $rootScope.showError(L.tsq("Cross check - %s settings cannot be displayed. Missing UI plugin.", [$scope.crosscheck.title]));
            return;
        }

        $scope.settings.title = crosscheck.title;
        $scope.settings.desc = crosscheck.desc;
        $scope.settings.name = crosscheck.name;

        $scope.settings.tabs[0].templateUrl = plugin.testSettingsTab.templateUrl;
        $scope.settings.tabs[0].controller = plugin.testSettingsTab.controller;

        if(plugin.filteringTab) {
            $scope.settings.tabs[1].templateUrl = plugin.filteringTab.templateUrl;
        } else {
            $scope.settings.tabs[1].templateUrl = '../../../plugins/SettingsCrossChecks/crossCheckAcceptanceSettings.html'
        }

        var options = {
            direction: true,
            sampleType: true,
            resultType: true
        }

        $timeout(function () {
            if(typeof  $scope.settings.tabs[0].applySettings == 'function'){
                $scope.settings.tabs[0].applySettings(CrossChecksService.getSettingsElem(crosscheck.name, "Settings"));
            }
            if(typeof  $scope.settings.tabs[1].applySettings == 'function'){
                $scope.settings.tabs[1].applySettings(CrossChecksService.getSettingsElem(crosscheck.name, "AcceptanceSettings"), crosscheck.name);
            }

            if (acceptanceTab) {
                $scope.settings.selectTab('acceptance-settings');
            } else {
                $scope.settings.selectTab('settings');
            }

            showPopup("#crossCheckSettingsModal");
        }, 200);

        
    }

    $scope.settingsChanged = function () {
        CrossChecksService.saveConfig();

        afterSettingsChanged();

        settingsChanged = true;
    }

    function afterSettingsChanged() {
        var engineKey = AppService.getCurrentEngineKey();

        loadingInfo = true;
        for (var i = 0; i < $scope.config.crosschecks.length; i++) {
            var crosscheck = $scope.config.crosschecks[i];
            crosscheck.disabled = $scope.config.disabled;

            if(!checkForEngine(engineKey, crosscheck.forEngine) || (AppService.getLicense()?.isSpecialTrial && crosscheck.disabledForSpecialTrial)) {
                crosscheck.disabled = true;
                crosscheck.use = false;
            }

            //print info
            var service = CrossChecksService.get(crosscheck.name);
            if (service && service.getInfo) {
                var settingsElem = CrossChecksService.getSettingsElem(crosscheck.name, "Settings");
                crosscheck.settingsInfo = service.getInfo(settingsElem);
            }

            var acceptSettingsElem = CrossChecksService.getSettingsElem(crosscheck.name, "AcceptanceSettings");
            crosscheck.acceptSettingsInfo = CrossCheckAcceptanceSettingsService.getAcceptanceSettingsInfo(acceptSettingsElem, crosscheck.name);
        }

        try { $scope.$digest(); } catch (er) {}
        $timeout(function(){ loadingInfo = false; }, 0, false);
    }

    //-- settings dialog
    $scope.settings = {
        title: "NA",
        desc: "NA",
        name: null,
        tabs: [{
                title: L.tsq('Settings'),
                dataItem: 'settings',
                templateUrl: null,
                controller: null,
            },
            {
                title: L.tsq('Filtering'),
                dataItem: 'acceptance-settings',
                templateUrl: null,
                controller: 'CrossCheckAcceptanceSettingsCtrl',
            }
        ]
    }

    $scope.onSaveSettings = function () {
        var settingsElem = CrossChecksService.createElement("Settings");
        $scope.settings.tabs[0].loadSettings(settingsElem);

        var acceptSettingsElem = CrossChecksService.createElement("AcceptanceSettings");
        $scope.settings.tabs[1].loadSettings(acceptSettingsElem, $scope.settings.name);

        CrossChecksService.saveSettings($scope.settings.name, settingsElem, acceptSettingsElem);

        afterSettingsChanged();
        settingsChanged = true;

        hidePopup("#crossCheckSettingsModal");

        SQEvents.notifyListeners(SQEvents.get("REFRESH_CC_TIMES"));
    }

    $scope.onLoadCrossChecks = function(){
        var fileExtension = {
            name: 'xml',
            description: L.tsq('XML Files')
        };

        $rootScope.showFilePicker(L.tsq('Select config file to load'), "loadCrossChecks", true, false, null, fileExtension, null, function (paths) {
            if (paths) {
                AppService.loadFile(paths[0], function(data){
                    try {
                        var elCrossChecks = xmlToObject(data).find("CrossChecks")[0];
                        if(!elCrossChecks){
                            $rootScope.showError(L.tsq("Loading failed - Selected file doesn't contain Cross checks settings"));
                            return;
                        }
                        
                        var settingsObj = AppService.getTaskConfig();
                        settingsObj.replaceChild(elCrossChecks, getChildElement(settingsObj, "CrossChecks"));

                        init();

                        $rootScope.showSuccess(L.tsq("Settings loaded"));

                        AppService.updateTaskXML();
                    }
                    catch(err){
                        $rootScope.showError(L.tsq("Loading failed") + " - " + err);
                    }
                });
            }
        });
    }

    $scope.onSaveCrossChecks = function(){
        var fileExtension = {
            name: 'xml',
            description: L.tsq('XML Files')
        };

        var pathName = SQConstants.getSettings()["loadCrossChecks"] ? "loadCrossChecks" : "configsPath";
        var fileName = "CrossChecks";
        
        var elCrossChecks = CrossChecksService.getCrossChecksElem();
        if(!elCrossChecks) {
            return;
        }

        var fileContent = xmlToString(elCrossChecks);

        $rootScope.saveFile(L.tsq("Save config"), fileExtension, pathName, fileName, fileContent);
    }

    $scope.isRetester = AppService.getTask().type == 'Retest';

    //- Event Handlers --------------------------------------------------------------

    $scope.tab.shouldSaveSettings = function () {
        var shouldSaveSettings = settingsChanged;
        settingsChanged = false;
        return shouldSaveSettings;
    }

    function onEvent(event, data) {
        if(event == SQEvents.get('SETTINGS_TAB_RELOAD')){
            if (data == $scope.tab.title || data == 'all') {
                loaded = false;
                init();
            }
        } else if (event == SQEvents.get('SETTINGS_TAB_ACTIVE')) {
            var active = data.title == $scope.tab.title;
            if (active && !loaded) {
                init();
            }

            watchersHandler.onActivated(active);
        } 
        else if(event == SQEvents.get('LANGUAGE_CHANGED') || event == SQEvents.get('MAIN_SETUP_CHANGED') || event == SQEvents.get('AR_MAIN_SETUP_CHANGED')){
            $timeout(afterSettingsChanged, 0, false);
            return;
        } else if (event == SQEvents.get('LICENSE_CHANGED')) {
            $scope.license = data;
            $timeout(afterSettingsChanged, 0, false);
        }
        else return;

        try {
            $scope.$digest();
        } catch (er) {}
    }

    var destroyConfigWatch = $scope.$watch("config", function () {
        if (loaded && !loadingInfo) {
            $scope.settingsChanged();
        }
    }, true);

    $scope.$on("$destroy", function () {
        SQEvents.removeListener(listenerId);
        destroyConfigWatch();
    });

    var settingsChanged = false;
    var loaded = false;
    var loadingInfo = false;
    var listenerId = "CrossChecksCtrl";

    SQEvents.addListener(listenerId, [
        SQEvents.get('SETTINGS_TAB_RELOAD'), 
        SQEvents.get('SETTINGS_TAB_ACTIVE'), 
        SQEvents.get('LANGUAGE_CHANGED'),
        SQEvents.get('MAIN_SETUP_CHANGED'),
        SQEvents.get('AR_MAIN_SETUP_CHANGED'),
        SQEvents.get('LICENSE_CHANGED')
    ], onEvent);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function () {
        watchersHandler.onActivated(false);
    }, 0, false);

});