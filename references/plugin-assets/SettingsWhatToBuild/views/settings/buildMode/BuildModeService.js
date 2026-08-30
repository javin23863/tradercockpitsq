angular.module('app.settings').service('BuildModeService', function($rootScope, AppService, SQConstants, SQEvents, SettingsGeneticOptionsService, L) {
    
    this.loadSettings = function(){
        instance.resetSettings();
        
        var whatToBuildElem = AppService.getCurrentTaskTabSettings("WhatToBuild");
        var buildModeElem = getChildElement(whatToBuildElem, 'BuildMode');

        instance.config.generationType = getAttrStringValue(buildModeElem, "generationType", instance.config.generationType);

        oldConfig = angular.copy(instance.config);

        this.loaded = true;
    }

    this.checkSettings = function(){
        return true;
    }

    this.saveSettings = function(whatToBuildElem){
        var buildModeElem = createChild(whatToBuildElem, 'BuildMode', AppService.xmlDoc);
        buildModeElem.setAttribute("generationType", instance.config.generationType);

        SettingsGeneticOptionsService.config.generationType = angular.copy(instance.config.generationType);
        SettingsGeneticOptionsService.checkIPSettings();
    }

    this.resetSettings = function(useOldSettings) { 
        instance.config.generationType = useOldSettings ? oldConfig.generationType : instance.generationTypes.randomGeneration;
    }

    this.getDescription = function(){
        if(instance.config.generationType == instance.generationTypes.randomGeneration) return L.tsq("Random generation");
        
        var whatToBuildElem = AppService.getCurrentTaskTabSettings("WhatToBuild");        
        var buildModeElem = getChildElement(whatToBuildElem, 'BuildMode');

        var populationSize = getNodeIntValue(buildModeElem, 'PopulationSize', "?");
        var maxGenerations = getNodeIntValue(buildModeElem, 'MaxGenerations', "?");
        var islands = getNodeIntValue(buildModeElem, 'Islands', "?");
        var initGenerationType = getNodeIntValue(buildModeElem, 'InitGenerationType', "?");
        var evoRestartOnFinish = false;

        var EROFObj = getChildElement(buildModeElem, "EvoRestartOnFinish"); 
        if(EROFObj){
            evoRestartOnFinish = getAttrBooleanValue(EROFObj, 'status', false);    
        }

        var description = L.tsq("Genetic evolution");
        description += ", " + L.tsq("%d generations max / %d islands / %d per island", [maxGenerations, islands, populationSize]);
        description += initGenerationType == 2 ? ", " + L.tsq("Initial population used") : "";
        description += evoRestartOnFinish ? ", " + L.tsq("Restart on finish") : "";
        return description;
    }

    var instance = this;
    var oldConfig = null;

    this.loaded = false;
    this.config = {};

    this.constants = SQConstants.getConstants();
    this.generationTypes = SQConstants.getConstants().buildGenerationTypes;

});