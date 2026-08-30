angular.module('app.resultstabs.sourcecode').controller('SourceCodeCtrl', function ($rootScope, $scope, $timeout, $q, AppService, SQConstants, SourceCodeService, SQResultsData, $element, SkinService, SQEvents, L) {

    function init() {
        initEditor();

        var broker = SQConstants.getConstants().broker;
        if (broker) {
            $scope.broker.encryption = broker.encryption;
            $scope.broker.name = broker.name;
        }

        if ($scope.broker.encryption) {
            $scope.broker.supportsMT4 = broker.platforms.indexOf("MT4") >= 0;
            $scope.broker.supportsMT5 = broker.platforms.indexOf("MT5") >= 0;

            $scope.broker.configPopupTitle = L.tsq("Choose path to your %s MetaTrader installation", [$scope.broker.name]);

            $scope.broker.mt4Path = SQConstants.getSettings()["mt4InstallPath"];
            $scope.broker.mt5Path = SQConstants.getSettings()["mt5InstallPath"];
            $scope.broker.mt4DataPath = SQConstants.getSettings()["mt4DataPath"];
            $scope.broker.mt5DataPath = SQConstants.getSettings()["mt5DataPath"];
        }

        var data = InitializationData().results.sourceCode;
        SourceCodeService.init(data);
    }

    function initEditor() {
        require.config({
            paths: {
                'vs': 'libs/vs'
            }
        });

        require(['vs/editor/editor.main'], function () {
            $timeout(function () {
                var config = {
                    value: L.tsq('No strategy selected.'),
                    readOnly: true,
                    language: 'html',
                    fontSize: 12,
					minimap: {
						enabled: false
					}
                };
                var targetElement = document.getElementById('sc-editor');

                editor = monaco.editor.create(targetElement, config);

                var skin = SkinService.selectedSkin.name;
                if (skin == 'Dark skin') {
                    monaco.editor.setTheme('vs-dark')
                } else {
                    monaco.editor.setTheme('vs')
                }

                $timeout(function () {
                    editor.layout();
                    loaded = true;
                    editorDeferred.resolve();
                }, 100);
            }, 0, false);
        });
    }

    $scope.onSave = function () {
        if (!strategyName && !SourceCodeService.isAlgoWizardApp()) {
            $rootScope.showError(L.tsq('No strategy selected.'));
            return;
        }

        displayCustomIndicatorsPopup(function () {
            var fileContent = editor.getValue();

            if (SQResultsData.isAlgoWizardCloud()) {
                if (("" + $scope.extension.constructor).indexOf("Array") >= 0) {
                    createFileDownload(fileContent, "AlgoWizardStrategy." + $scope.extension[0].name, "text/plain");
                }
                else {
                    createFileDownload(fileContent, "AlgoWizardStrategy." + $scope.extension.name, "text/plain");
                }
                return;
            }

            $("body").append($('#sourceCodeSaveFilePicker'));

            $rootScope.saveFile(L.tsq("Save source code"), $scope.extension, "saveSourceCode", fileName ? fileName : strategyName, fileContent);
        });
    }

    $scope.onSaveEA = function (mt4) {
        if ((mt4 && (!$scope.broker.mt4Path || !$scope.broker.mt4DataPath)) || (!mt4 && (!$scope.broker.mt5Path || !$scope.broker.mt5DataPath))) {
            $scope.configToShow = mt4 ? "MT4" : "MT5";
            saveCallback = function () { SourceCodeService.saveEA(mt4); };
            showPopup('#terminalConfigPopup');
            return;
        }

        SourceCodeService.saveEA(mt4);
    }

    $scope.onConfigure = function () {
        $scope.configToShow = "all";
        saveCallback = null;
        showPopup('#terminalConfigPopup');
    }

    $scope.onSaveConfig = function () {
        var settings = SQConstants.getSettings();
        settings["mt4InstallPath"] = $scope.broker.mt4Path;
        settings["mt5InstallPath"] = $scope.broker.mt5Path;
        settings["mt4DataPath"] = $scope.broker.mt4DataPath;
        settings["mt5DataPath"] = $scope.broker.mt5DataPath;

        SourceCodeService.saveMTPaths($scope.broker, function () {
            $rootScope.showSuccess(L.tsq("Settings saved"));
            hidePopup("#terminalConfigPopup");

            if (saveCallback) {
                saveCallback();
                saveCallback = null;
            }
        });
    }

    $scope.onSelectInstallFolder = function (mt4) {
        $rootScope.showFilePicker(L.tsq('Select installation folder'), mt4 ? "mt4ExportInstallFolder" : "mt5ExportInstallFolder", false, false, null, null, null, function (paths) {
            if (paths) {
                $scope.broker[mt4 ? "mt4Path" : "mt5Path"] = paths[0];
                try { $scope.$digest(); } catch (err) { }

                SourceCodeService.getDataPath(paths[0], mt4, function (dataPath) {
                    $scope.broker[mt4 ? "mt4DataPath" : "mt5DataPath"] = dataPath;
                    try { $scope.$digest(); } catch (err) { }
                });
            }
        });
    }

    $scope.onSelectDataFolder = function (mt4) {
        $rootScope.showFilePicker(L.tsq('Select data folder'), mt4 ? "mt4ExportDataFolder" : "mt5ExportDataFolder", false, false, null, null, null, function (paths) {
            if (paths) {
                $scope.broker[mt4 ? "mt4DataPath" : "mt5DataPath"] = paths[0];

                try { $scope.$digest(); } catch (err) { }
            }
        });
    }

    $scope.onRefresh = function () {
        refreshContent(false, false, true);
    }

    $scope.onCopyToClipboard = function () {
        var textToCopy = editor.getValue();

        displayCustomIndicatorsPopup();

        //hackaround solution, but it's the only way
        var textarea = document.createElement('textarea');
        textarea.style.position = "relative";
        textarea.style.left = "-9999px";
        textarea.value = textToCopy;

        document.body.appendChild(textarea);

        textarea.select();

        try {
            var successful = window.document.execCommand('Copy');
            if (!successful) throw successful;
        } catch (err) {
            $rootScope.showError(L.tsq('Copy to clipboard failed.'));
        }

        textarea.remove();

        $(".source-code-tab .source-code-copy").hide();
        $(".source-code-tab .source-code-copied").fadeIn(500);
        setTimeout(function () {
            $(".source-code-tab .source-code-copied").hide();
            $(".source-code-tab .source-code-copy").fadeIn(500);
        }, 3000, false);
    }

    $scope.onDontDisplayAgainCIPopup = function () {
        AppService.saveSetting("SourceCodeCIPopupDisabled", true, function () { });

        hidePopup('#customIndicatorsPopup');
    }

    $scope.onCustomIndysPopupClosed = function () {
        if (CIPopupCallback) CIPopupCallback();
    }

    $scope.onLearnMore = function(){
        AppService.openBrowser($scope.learnMoreURL);
    }

    function displayCustomIndicatorsPopup(callback) {
        if (CIPopupDisplayed || SQConstants.getSettings().SourceCodeCIPopupDisabled) {
            if (callback) callback();
            return;
        }

        showPopup('#customIndicatorsPopup');

        CIPopupDisplayed = true;
        CIPopupCallback = callback;
    }

    function getEditorMode(type) {
        if (type && type.toLowerCase().includes('xml')) {
            return "xml";
        }

        return "cpp";
    }

    function getSelectedParams() {
        var code = 0;

        if ($scope.config.parametrizeType == 0) {
            code += 10000000;

        } else if ($scope.config.parametrizeType == 1) {
            if ($scope.config.periodParams) code += 1;
            if ($scope.config.shiftParams) code += 10;
            if ($scope.config.exitParamsUsed) code += 100;
            if ($scope.config.constantsParams) code += 1000;
            if ($scope.config.otherParams) code += 10000;
            if ($scope.config.exitParamsUnused) code += 100000;
            if ($scope.config.symmetricVariables) code += 1000000;
            if ($scope.config.entryParams) code += 10000000;
            if ($scope.config.entryLogic) code += 100000000;
        }

        return code;
    }

    //- Event Handlers --------------------------------------------------------------

    var destroyConfigWatch = $scope.$watch('config', function (newValue, oldValue) {
        if (loaded) {
            if (oldValue != newValue) {
                if (lastSelectedGenerator != $scope.config.type) {
                    loaded = false;
                    SourceCodeService.loadParamsConfig();
                    loaded = true;
                }

                refreshContent();

                lastSelectedGenerator = $scope.config.type;
                SourceCodeService.setParamsConfig();
            }
        }
    }, true);

    function refreshContent(strategyChanged, handlerContent, force) {
        watchersHandler.onActivated($scope.tab.active);

        if (!$scope.tab.active) return;

        if (!loaded && !initCalled) {
            initCalled = true;
            init();
        }

        var dataInfo = SQResultsData.getDataInfo();
        strategyName = dataInfo.strategyName;
        fileName = dataInfo.fileName;

        if (!dataInfo.strategyName && !SourceCodeService.isAlgoWizardApp()) {
            if (editor) {
                editor.setValue(L.tsq('No strategy selected.'));
                $scope.warning = null;
                $scope.learnMoreURL = null;
            }
            return;
        }

        if (strategyChanged) {
            updateSettings(handlerContent);
        } else {
            selectedParams = getSelectedParams();
        }

        SQResultsData.getData(
            handlerId, 
            [
                $scope.config.type,
                selectedParams,
                $scope.config.mmType,
                SourceCodeService.isAlgoWizardApp() ? SourceCodeService.strategyXML : null
            ], 
            force
        ).then(function (result) {
            latestResult = result;
            
            if(loaded){
                displayCode();
            }
            else {
                if(!initCallbackDefined){
                    editorDeferred.promise.then(displayCode);
                    initCallbackDefined = true;
                }
            }
        });
    }

    function displayCode(){
        var generator = SourceCodeService.getGenerator();
        if (generator) {
            $scope.generatorDescription = generator.desc;
            //$("#source-code-desc").html(generator.desc);

            if (generator.extension) {
                if (("" + generator.extension.constructor).indexOf("Array") >= 0) {
                    $scope.extension = [];

                    for (var i = 0; i < generator.extension.length; i++) {
                        var genExt = generator.extension[i];
                        $scope.extension.push({ name: genExt.types, description: genExt.name });
                    }
                }
                else {
                    $scope.extension = {
                        name: generator.extension.types,
                        description: generator.extension.name
                    }
                }

            } else {
                $scope.extension = {};
            }
        }

        if (!editor) {
            return;
        }

        var mode = getEditorMode($scope.config.type);
        monaco.editor.setModelLanguage(editor.getModel(), mode);
        editor.layout();
        editor.setValue(latestResult.data.code || L.tsq('Source code not available.'));

        $scope.warning = latestResult.data.warning;
        $scope.learnMoreURL = latestResult.data.learnMoreURL;
        try { $scope.$digest(); } catch (err) { }
    }

    function updateSettings(handlerContent) {
        if (!handlerContent || (!handlerContent.projectName && !SourceCodeService.isAlgoWizardApp())) {
            $scope.config.mmType = $scope.mmTypes[0].value;
            return;
        }

        var attributes = handlerContent.data.attributes;
        $scope.config.type = attributes.codeType;
        $scope.config.mmType = attributes.mmType;
        selectedParams = attributes.selectedParams;

        try {
            $scope.$digest();
        } catch (err) { }
    }

    function fetchData(params) {
        var deferred = $q.defer();

        SourceCodeService.print(params, function (result) {
            deferred.resolve(result);
        });

        return deferred.promise;
    }

    $scope.getMMLabel = function () {
        var mmType = getItem($scope.mmTypes, 'value', $scope.config.mmType);
        return mmType ? L.tsq(mmType.name) : '';
    }

    $scope.$on('$destroy', function () {
        destroyConfigWatch();

        if (window.parent && window.parent.removeListener) {
            window.parent.removeListener("SourceCodeEditorCtrlSkin");
        }

        SQResultsData.removeHandler(handlerId);
    });


    //- Initialization --------------------------------------------------------------

    var CIPopupDisplayed = false;
    var CIPopupCallback = null;

    var initCalled = false;
    var loaded = false;
    var latestResult = null;
    var initCallbackDefined = false;

    var strategyName = null; //the name of the selected strategy
    var fileName = null;
    var editor = null;
    var selectedParams = null;

    $scope.warning = null;
    $scope.learnMoreURL = null;

    $scope.generators = SourceCodeService.generators;
    $scope.config = SourceCodeService.config;

    $scope.broker = {
        encryption: false,
        supportedPlatforms: []
    }

    $scope.mmTypes = SourceCodeService.mmTypes;

    $scope.filePicker = {};

    $scope.generatorDescription = "";
    $scope.configToShow = "MT4";

    $scope.isAWCloud = SQResultsData.isAlgoWizardCloud();
    $scope.customIndicatorsInfo = "<i>Please note that you need to install some custom indicators in order to use this strategy in your trading platform. <a href='https://algowizard.io/custom-indicators-installation' target='_blank'>Learn more</a></i>";

    $scope.selectedInfoPopupTab = "metatrader";
    $scope.SQDir = SQConstants.getSettings().appPath;

    var editorDeferred = $q.defer();
    var saveCallback = null;

    var handlerId = $scope.tab.title; //handler id must be set to tab title
    var handler = SQResultsData.addHandler(handlerId, fetchData, refreshContent, ['codeType', 'selectedParams', 'mmType', 'strategyXML']);

    var lastSelectedGenerator = $scope.config.type;

    handler.setStrategyXML = function (strategyXML, settingsXML) {
        SourceCodeService.strategyXML = strategyXML;
        SourceCodeService.settingsXML = settingsXML;
    }

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function () {
        watchersHandler.onActivated(false);
        SQResultsData.resultsTabLoaded("SourceCodeCtrl");
    }, 0, false);

    function onSkinChanged() {
        var skin = SkinService.selectedSkin.name;
        if (typeof monaco !== 'undefined') {
            if (skin == 'Dark skin') { //dark
                monaco.editor.setTheme('vs-dark')
            } else { //light
                monaco.editor.setTheme('vs')
            }
        }
    }

    $(window).on("resize", function () {
        if (editor) {
            editor.layout();
        }
    });

    if (window.parent && window.parent.addListener) {
        window.parent.addListener("SourceCodeEditorCtrlSkin", [SQEvents.get("SKIN_CHANGED")], onSkinChanged);
    }

    if (isAlgoWizard()) {
        SQEvents.addListener("SourceCodeEditorCtrlSkin", [
            SQEvents.get("SKIN_CHANGED")
        ], onSkinChanged);
    }

});