angular.module('app.settings').controller('SimpleOptimizeSettingsCtrl', function ($rootScope, $scope, sqPlugin, $timeout, $q, AppService, SQConstants, SQEvents, DatabankService, OptimizationService, ParametersGridService, L) {

    function loadSettings() {
        initialized = false;

        loadDatabanks(function(){
            OptimizationService.loadSettings(function () {
                oldSource = $scope.config.source;

                $timeout(function () { 
                    updateTradingOptionsValues();
                    initialized = true; 
                }, 0, false);
            });
        });
    }

    function loadDatabanks(callback){
        var databankTypes = $scope.constants.gedatabanks;

        DatabankService.loadVisibleDatabanks($scope.databanks, function(){
            for(var i=$scope.databanks.length-1; i>=0; i--){
                var databank = $scope.databanks[i];

                if (databank.name == databankTypes.results && $scope.isOptimizer) {
                    $scope.databanks.splice(i, 1);
                    break;
                }
            }
            if(callback) callback();
        });
    }

    //updates trading options current values in parameters grids
    function updateTradingOptionsValues(params){
        var settingsObj = AppService.getTaskConfig();
        var optionsObj = getChildElement(settingsObj, 'Options'); 
        var BuildTradingOptionsElem = getChildElement(optionsObj, 'BuildTradingOptions'); 
        var elParams = getChildElement(BuildTradingOptionsElem, "Params");
        var paramElems = getChildElements(elParams, "Param");

        var somethingChanged = false;

        for(var i=0; i<paramElems.length; i++){
            var elParam = paramElems[i];
            var paramKey = getAttrStringValue(elParam, 'className', 'NA') + "." + getAttrStringValue(elParam, 'key', 'NA');

            for(var v=0; v<ParametersGridService.variables.length; v++){
                var variable = ParametersGridService.variables[v];
                
                if(variable.id === paramKey){
                    var value = getNodeValue(elParam);

                    if(variable.type === 'time'){
                        value = parseInt(value / 60);
                        value = parseInt(value / 60) * 100 + (value % 60);
                    }

                    variable.value = value;
                    somethingChanged = true;
                    break;
                }
            }
        }

        if(somethingChanged || (params && params.forceSaving)){
            ParametersGridService.save();
        }
    }          

    $scope.printDistribution = function() {
        return L.tsq("Automatic - %d% up / %d% down distribution with %d steps", [$scope.config.distributionUp, $scope.config.distributionDown, $scope.config.maxSteps]);
    }

    $scope.isDashboard = function(){
        return AppService.getActivePanel() == "dashboard"
    }

    $scope.onOptimizationTypeSelect = function (selectedType) {
        $scope.config.optimization = selectedType;
        SQEvents.notifyListeners(SQEvents.get("OPTIMIZATION_TYPE_CHANGED"), selectedType);
    }

    $scope.optimizationSimpleTypeChanged = function (type) {
        SQEvents.notifyListeners(SQEvents.get("OPTIMIZATION_SIMPLE_TYPE_CHANGED"), type);
    }

    $scope.onParametersChange = function () {
        OptimizationService.saveConfig();
        AppService.updateTaskXML();
    }

    $scope.onSelectFile = function () {
        startButtonEnableDeffered = $q.defer();

        SQEvents.notifyListeners(SQEvents.get("PROJECT_DISABLE_START"), startButtonEnableDeffered.promise);

        OptimizationService.onSelectFile(function () {
            $scope.config.mode = $scope.parametersModes.manual;
        }, true);
    }

    function onEvent(event, data) {
        if (event == SQEvents.get('OPTIMIZATION_FILE_CHANGED')) {
            $scope.config.filePath = data.filePath;
        }
        else if (event == SQEvents.get('PARAMETERS_GRID_RELOAD')) {
            if (startButtonEnableDeffered != null) {
                startButtonEnableDeffered.resolve();
                startButtonEnableDeffered = null;
            }
        }
        else if(event == SQEvents.get("RELOAD_DATABANKS")){    //databank created
            loadDatabanks();
        }
        else if(event == SQEvents.get("CUSTOM_DATABANK_ACTION") && data.name == "rename"){
            if($scope.config.sourceDatabank == data.oldName){
                $scope.config.sourceDatabank = data.newName;

                OptimizationService.saveConfig();
                ParametersGridService.save(true);
            }
        }
        else if (event == SQEvents.get('TRADING_OPTIONS_CHANGED')) {
            updateTradingOptionsValues(data);
            return;
        }
        else return;

        try { $scope.$digest(); } catch (err) { }
    }

    $scope.showAdvancedSettings = function (event) {
        AppService.switchToPanel('settings');
        event.stopPropagation();
        $timeout(tabsShowHiddenTabs, 0, false);
    }

    $scope.onSourceChange = function(){
        if($scope.config.source == $scope.constants.optimization.sources.file){
            $scope.config.mode = $scope.parametersModes.manual;
        }
        else {
            $scope.config.mode = $scope.parametersModes.automatic;
        }

        OptimizationService.saveConfig();
        AppService.updateTaskXML();
    }

    $scope.$on("$destroy", function () {
        
    });

    var startButtonEnableDeffered = null;

    $scope.constants = SQConstants.getConstants();
    $scope.optimizationTypes = $scope.constants.optimization.optimizations;
    $scope.parametersModes = OptimizationService.modes;
    $scope.isOptimizer = OptimizationService.isOptimizer;
    $scope.config = OptimizationService.config;
    $scope.projectStates = SQConstants.getConstants().runningStatuses;
    $scope.databanks = [];

    $scope.printParamLabel = OptimizationService.printParamLabel;

    $scope.parametersGrid = {};

    $timeout(loadSettings, 0, false);

    var listenerId = "SimpleOptimizeSettingsCtrl";
    SQEvents.addListener(listenerId, [
        SQEvents.get("OPTIMIZATION_FILE_CHANGED"),
        SQEvents.get("RELOAD_DATABANKS"),
        SQEvents.get("PARAMETERS_GRID_RELOAD"),
        SQEvents.get("CUSTOM_DATABANK_ACTION"),
        SQEvents.get('TRADING_OPTIONS_CHANGED')
    ], onEvent);

});