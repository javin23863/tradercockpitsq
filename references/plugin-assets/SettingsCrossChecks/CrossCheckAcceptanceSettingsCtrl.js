angular.module('app').controller('CrossCheckAcceptanceSettingsCtrl', function ($scope, $rootScope, $timeout, SQConstants, CrossCheckAcceptanceSettingsService, L, $q) {

    var deferred = $q.defer();
    var loadedCG = false;
    
    $scope.config = {
        condGridInstance: {},
        thresholdPct: 50,
        robRows : 3,
        robCols : 3,
        robComb : 7,

        profitOptPct: 30,
        avgProfit: 0,
        uniformDistrChanges: 5,
        stdevAvgProfit: 1,

        evalProfitOptCheck: true,
        evalAvgProfitCheck:  true,
        evalUniformDistrCheck:  true,
        evalTopProfitCheck:  true,
        
        condCheck: 'all',
        minConditions: 2,
        minMarkets: 1,

        pctToPass: 80,
        resultsCount: 5,
        stabilityRange: 30
    }

    $scope.wfRows = [2,3,4,5];
    $scope.wfColumns = [2,3,4,5];
    $scope.wfCombinations = [];
    for(let i = 0; i <= 25; i++) { 
        $scope.wfCombinations.push(i);
    }

    $scope.crosscheckName = null;

    $scope.tab.loadSettings = function (acceptSettingsElem, crosscheckName) {
        CrossCheckAcceptanceSettingsService.loadSettings(acceptSettingsElem, $scope.config, crosscheckName);
    }

    $scope.tab.applySettings = function (acceptSettingsElem, crosscheckName) {
        if(crosscheckName === "SequentialOptimization"){
            CrossCheckAcceptanceSettingsService.applySettings(acceptSettingsElem, $scope.config, crosscheckName);
        }
        else {
            deferred.promise.then(function(){
                CrossCheckAcceptanceSettingsService.applySettings(acceptSettingsElem, $scope.config, crosscheckName);
            });
        }

        $scope.crosscheckName = crosscheckName;
    }

    $scope.tab.getInfo = function() {
        return "N/A";
    }

    $scope.copyGlobalConditions = function() {
        
    }

    $scope.onLoadDefaultWFConditions = function() {
        var options = [
            {title: L.tsq("Replace"), key: "replace"},
            {title: L.tsq("Add"), key: "add"}
        ]

        if($scope.config.condGridInstance.size()>0) {
            $rootScope.showOptionsDialog(L.tsq("Set recommended WF conditions"), L.tsq("There already are some conditions in the table. Do you want to Replace them or Add recommended WF conditions to the table?"),  function(option) {
                loadDefaultWFConditions(option.key);
            }, options);
        } else {
            loadDefaultWFConditions("add");
        }        
    }

    function loadDefaultWFConditions(action) {
        var conditionsObj = $scope.config.condGridInstance.getConditionsObj();
        var conditions = xmlToString(conditionsObj);

        CrossCheckAcceptanceSettingsService.getDefaultWFConditions({conditions: conditions, action: action} , function(data) {
            $scope.config.thresholdPct = 80;
            $scope.config.robRows = 3;
            $scope.config.robCols = 3;
            $scope.config.robComb = 7;

            var conditionsObj = xmlToObject(data.conditions).find("Conditions")[0];
            $scope.config.condGridInstance.setConditionsObj(conditionsObj, true);
            
            $scope.$applyAsync();
        });
    }

    $scope.onLoadDefaultOptProfileSettings = function() {
        $rootScope.showConfirm(L.tsq("Default settings"), L.tsq("Do you really want to set default settings?<br>It will overwrite your current settings."),  function(confirmed) {
                    if(!confirmed) {
                        return;
                    }

                    CrossCheckAcceptanceSettingsService.getDefaultSPPConditions(function(data) {
                        $scope.config.profitOptPct = 30;
                        $scope.config.avgProfit = 0;
                        $scope.config.uniformDistrChanges = 5;
                        $scope.config.stdevAvgProfit = 1;

                        $scope.config.evalProfitOptCheck = true;
                        $scope.config.evalAvgProfitCheck =  true;
                        $scope.config.evalUniformDistrCheck = true;
                        $scope.config.evalTopProfitCheck = true;

                        var conditionsObj = xmlToObject(data.conditions).find("Conditions")[0];
                        $scope.config.condGridInstance.setConditionsObj(conditionsObj, true);
                        
                        $scope.$applyAsync();
                    });
            },true
        );
    }
    
    $scope.onLoadDefaultSequentialOptimizationSettings = function() {
        $rootScope.showConfirm(L.tsq("Default settings"), L.tsq("Do you really want to set default settings?<br>It will overwrite your current settings."),  function(confirmed) {
                if(!confirmed) {
                    return;
                }

                $scope.config.pctToPass = 80;
                $scope.config.resultsCount = 5;
                $scope.config.stabilityRange = 30;

                $scope.$applyAsync();
            },
            true
        );
    }

    $scope.afterCGInit = function(data){
        var columnTypes = SQConstants.getConstants().databankColumnTypes; 
        var columns = SQConstants.getColumnsByType([columnTypes.general, columnTypes.wfSpecial], ["Fitness"], true);  

        $timeout(function() {
            $scope.config.condGridInstance.setDefaultSampleType(SQConstants.getConstants().sampleTypes.in);
            $scope.config.condGridInstance.setData(columns, SQConstants.getColumns().recentColumns);

             loadedCG = true;
             deferred.resolve();
        });
    }
    
    $scope.onCGRefresh = function(grid){
        if(!loadedCG){
            return;
        }
    }
});