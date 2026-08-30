angular.module('app.settings').service('WhatToBuildService', function($rootScope, $q, $injector, sqPlugin, BackendService, AppService, SQConstants, SQEvents, DatabankService) {
    
    this.init = function(){
        if(initialized) return;

        BackendService.sendRequest("buildType/listFiles", null, function(result){
            loadToArray(instance.strategyFiles, result.strategyFiles);
            loadToArray(instance.strategyTemplateFiles, result.strategyTemplateFiles);
            
            initialized = true;
        });
    }

    this.loadConfig = function(){
        var settingsElem = AppService.getTaskConfig();
        var whatToBuildElem = getChildElement(settingsElem, 'WhatToBuild', true);

        if(whatToBuildElem){
            var strategyTypeElem = getChildElement(whatToBuildElem, 'StrategyType', true);
            if(strategyTypeElem){
                instance.config.strategyType = getAttrStringValue(strategyTypeElem, 'type', instance.strategyTypes.simple);
                instance.config.additionalCharts = getAttrIntValue(strategyTypeElem, 'additionalCharts', 1);
                instance.config.templateFile = getAttrStringValue(strategyTypeElem, 'templateFile', '');
                instance.config.improveType = getAttrStringValue(strategyTypeElem, 'improveType', instance.improveTypes.strategy);
                instance.config.strategyFile = getAttrStringValue(strategyTypeElem, 'strategyFile', '');
                instance.config.improveDatabank = getAttrStringValue(strategyTypeElem, 'improveDatabank', instance.isBuilder ? constants.gedatabanks.strategiesToImprove : constants.gedatabanks.results);
            }
        }

        DatabankService.loadVisibleDatabanks(instance.availableDatabanks);
        
        instance.config.outputDatabank = DatabankService.getTaskOutputDatabank();

        instance.updateTabs();
        instance.checkStrategyType();
    }

    this.loadMarketSides = function(targetObj, settingsElem){
        var whatToBuildElem = getChildElement(settingsElem, 'WhatToBuild', true);
        if(whatToBuildElem){
            var marketSidesElem = getChildElement(whatToBuildElem, 'MarketSides', true);
            if(marketSidesElem){
                targetObj.type = getAttrStringValue(marketSidesElem, 'type', instance.directions.both);
                targetObj.entrySymmetry = getNodeBooleanValue(marketSidesElem, 'EntrySymmetry', true);
                targetObj.exitSymmetry = getNodeBooleanValue(marketSidesElem, 'ExitSymmetry', true);
            }
        }
    }

    this.loadTemplate = function (fileName, reload, callback) {
        BackendService.sendRequest('buildType/getTemplateConfig', { fileName: fileName, reload: reload }, callback, 'POST');
    }

    /**
     * Shows/hides settings tabs according to current config 
     */
    this.updateTabs = function(){
        var show = instance.config.strategyType == instance.strategyTypes.improve;

        SQEvents.notifyListeners(SQEvents.get('SETTINGS_TAB_SHOW'), { 
            tab : 'settings-partstoimprove', 
            show : show
        });

        if(show){
            var settingsElem = AppService.getTaskConfig();
            var partsToImproveElem = getChildElement(settingsElem, 'PartsToImprove', true);

            if(!partsToImproveElem){
                var PartsToImproveService = $injector.get("PartsToImproveService");
                if(PartsToImproveService) PartsToImproveService.saveConfig();
            }
        }
    }

    this.checkStrategyType = function(){
        var defaultDatabank = SQConstants.getConstants().gedatabanks.strategiesToImprove;
        var showDatabank = false;

        var settingsElem = AppService.getTaskConfig();
        if(!settingsElem) return;
        
        var buildTypeElem = getChildElement(settingsElem, 'WhatToBuild', true);
        if(buildTypeElem) {
            var strategyTypeElem = getChildElement(buildTypeElem, 'StrategyType', true);
            if(strategyTypeElem){
                var strategyType = getAttrStringValue(strategyTypeElem, 'type', instance.strategyTypes.simple);
                var improveType = getAttrStringValue(strategyTypeElem, 'improveType', instance.improveTypes.strategy);

                if(strategyType == instance.strategyTypes.improve && improveType == instance.improveTypes.databank){
                    showDatabank = true;
                }
            }
        }
        
        SQEvents.notifyListeners(SQEvents.get("DATABANK_DISPLAY"), {
            name : defaultDatabank, 
            show : instance.isBuilder && showDatabank,
            displayFirst : true,
            improveAllFromDatabank: showDatabank //used for displaying help text on the Results Ranking tab
        });
    }

    this.saveConfig = function(){
        var xmlDoc = AppService.xmlDoc;
        
        var settingsElem = AppService.getTaskConfig();
        if(!settingsElem) return;
        
        var whatToBuildElem = createChild(settingsElem, 'WhatToBuild', xmlDoc);

        var strategyTypeElem = createChild(whatToBuildElem, 'StrategyType', xmlDoc);
        strategyTypeElem.setAttribute("type", instance.config.strategyType);
        strategyTypeElem.setAttribute("additionalCharts", instance.config.additionalCharts);
        strategyTypeElem.setAttribute("templateFile", instance.config.templateFile || '');
        strategyTypeElem.setAttribute("improveType", instance.config.improveType);
        strategyTypeElem.setAttribute("strategyFile", instance.config.strategyFile || '');
        strategyTypeElem.setAttribute("improveDatabank", instance.config.improveDatabank || '');
        
        instance.checkStrategyType();
    }

    this.checkTemplateResources = function(filePath){
        BackendService.sendRequest("project/checkTemplateResources", { filePath : filePath }, function(result){
            if(result.resourcesXML){
                window.parent.callAction("onCheckTemplateResources", result.configXML, result.resourcesXML, filePath);
            }
        });
    }

    var instance = this;
    var initialized = false;

    var constants = SQConstants.getConstants();
    this.strategyTypes = constants.buildStrategyTypes;
    this.improveTypes = constants.buildImproveTypes;
    this.directions = constants.buildStrategyDirections;
    this.architectures = constants.strategyArchitectures;

    this.strategyFiles = [];
    this.strategyTemplateFiles = [];

    this.isBuilder = window.appConfig.product == 'BUILDER';

    this.settingsPlugins = sqPlugin.getPlugins("WhatToBuildSettings");
    this.activeSettings = { plugin : null };

    this.config = {
        strategyType : this.strategyTypes.simple,
        improveType : this.improveTypes.strategy,
        improveDatabank : this.isBuilder ? constants.gedatabanks.strategiesToImprove : constants.gedatabanks.results,
        outputDatabank : constants.gedatabanks.results,
        additionalCharts : 1
    };

    this.availableDatabanks = [];

    SQEvents.addListener("WhatToBuildService", [SQEvents.get("DATABANKS_LOADED")], instance.checkStrategyType);

});