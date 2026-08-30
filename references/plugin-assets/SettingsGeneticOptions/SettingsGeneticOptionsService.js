angular.module('app.settings').service('SettingsGeneticOptionsService', function($q, $timeout, AppService, SQConstants, SQEvents, L) {

    this.loadSettings = function(){
        instance.loaded = false;
        var whatToBuildElem = AppService.getCurrentTaskTabSettings("WhatToBuild");        
        var buildModeElem = getChildElement(whatToBuildElem, 'BuildMode');

        instance.config.generationType = getAttrStringValue(buildModeElem, "generationType", instance.config.generationType);

        //Genetic options
        instance.config.populationSize = getNodeIntValue(buildModeElem, 'PopulationSize', instance.config.populationSize);
        instance.config.maxGenerations = getNodeIntValue(buildModeElem, 'MaxGenerations', instance.config.maxGenerations);
        instance.config.crossoverProbability = getNodeIntValue(buildModeElem, 'CrossoverProbability', instance.config.crossoverProbability);
        instance.config.mutationProbability = getNodeIntValue(buildModeElem, 'MutationProbability', instance.config.mutationProbability);
        instance.config.showAdvancedSettings = getNodeBooleanValue(buildModeElem, 'ShowAdvancedGeneticSettings', instance.config.showAdvancedSettings); 
        instance.config.islands = getNodeIntValue(buildModeElem, 'Islands', instance.config.islands);
        instance.config.migrationModulo = getNodeIntValue(buildModeElem, 'MigrationModulo', instance.config.migrationModulo);
        instance.config.migrationRate = getNodeIntValue(buildModeElem, 'MigrationRate', instance.config.migrationRate);
        instance.config.showLastGenerationDatabank = getNodeBooleanValue(buildModeElem, 'ShowLastGenerationDatabank', instance.config.showLastGenerationDatabank); 

        //Initial population generation
        instance.config.initGenerationType = getNodeIntValue(buildModeElem, 'InitGenerationType', instance.config.initGenerationType);
        instance.config.decimationCoef = getNodeIntValue(buildModeElem, 'DecimationCoef', instance.config.decimationCoef);
        
        //Evolution management
        var EROFObj = getChildElement(buildModeElem, "EvoRestartOnFinish"); 
        if(EROFObj){
            instance.config.evoRestartOnFinish = getAttrBooleanValue(EROFObj, 'status', instance.config.evoRestartOnFinish);    
        }

        var EROSObj = getChildElement(buildModeElem, "EvoRestartOnStagnation"); 
        if(EROSObj){
            instance.config.evoRestartOnStagnation = getAttrBooleanValue(EROSObj, 'status', instance.config.evoRestartOnStagnation);    
            instance.config.evoFitnessRestartType = getAttrIntValue(EROSObj, 'fitnessType', instance.config.evoFitnessRestartType);
            if(instance.config.evoFitnessRestartType==0) instance.config.evoFitnessRestartType = instance.sampleTypes.in;
            instance.config.evoStagnationGenerations = getAttrIntValue(EROSObj, 'generations', instance.config.evoStagnationGenerations);
        }

        var EROSObj = getChildElement(buildModeElem, "EvoInSamplePeriod"); 
        if(EROSObj){
            instance.config.evoTrainingValidationRatio = getAttrIntValue(EROSObj, 'ratio', instance.config.evoTrainingValidationRatio);
        }

        var conditionsObj = getChildElement(buildModeElem, "Conditions");    
        if(instance.controllerReady){
            instance.config.condGridInstance.setConditionsObj(conditionsObj);       //set conditions obj immediately
        }
        else {
            instance.conditionsToSet = conditionsObj;                               //set conditions obj later after proper init
        }
        
        //Fresh blood
        instance.config.freshBloodReplaceSimilar = getNodeBooleanValue(buildModeElem, 'FreshBloodReplaceSimilar', instance.config.freshBloodReplaceSimilar);
        instance.config.freshBloodReplaceWeakest = getNodeBooleanValue(buildModeElem, 'FreshBloodReplaceWeakest', instance.config.freshBloodReplaceWeakest);
        instance.config.freshBloodWeakestPct = getNodeIntValue(buildModeElem, 'FreshBloodWeakestPct', instance.config.freshBloodWeakestPct);
        instance.config.freshBloodWeakestGenerations = getNodeIntValue(buildModeElem, 'FreshBloodWeakestGenerations', instance.config.freshBloodWeakestGenerations);

        instance.loaded = true;
    }

    this.saveSettings = function(){
        var whatToBuildElem = AppService.getCurrentTaskTabSettings("WhatToBuild");
        if(!whatToBuildElem) return;

        var buildModeElem = createChild(whatToBuildElem, 'BuildMode', AppService.xmlDoc);
        removeAllChildren(buildModeElem);
        
        buildModeElem.setAttribute("generationType", instance.config.generationType);

        //Genetic options
        addNode('PopulationSize', instance.config.populationSize, buildModeElem, AppService.xmlDoc);
        addNode('MaxGenerations', instance.config.maxGenerations, buildModeElem, AppService.xmlDoc);
        addNode('CrossoverProbability', instance.config.crossoverProbability, buildModeElem, AppService.xmlDoc);
        addNode('MutationProbability', instance.config.mutationProbability, buildModeElem, AppService.xmlDoc);
        addNode('ShowAdvancedGeneticSettings', instance.config.showAdvancedSettings, buildModeElem, AppService.xmlDoc);
        addNode('Islands', instance.config.islands, buildModeElem, AppService.xmlDoc);
        addNode('MigrationModulo', instance.config.migrationModulo, buildModeElem, AppService.xmlDoc);
        addNode('MigrationRate', instance.config.migrationRate, buildModeElem, AppService.xmlDoc);
        addNode('ShowLastGenerationDatabank', instance.config.showLastGenerationDatabank, buildModeElem, AppService.xmlDoc);

        //Initial population generation
        addNode('InitGenerationType', instance.config.initGenerationType, buildModeElem, AppService.xmlDoc);
        addNode('DecimationCoef', instance.config.decimationCoef, buildModeElem, AppService.xmlDoc);
        
        var conditionsObj;

        if(instance.controllerReady){
            conditionsObj = instance.config.condGridInstance.getConditionsObj();
        }
        else {
            conditionsObj = instance.conditionsToSet;
        }

        buildModeElem.appendChild(conditionsObj);
        
        //Evolution management
        var EROFObj = createChild(buildModeElem, 'EvoRestartOnFinish', AppService.xmlDoc, true);
        EROFObj.setAttribute("status", instance.config.evoRestartOnFinish);
        
        var EROSObj = createChild(buildModeElem, 'EvoRestartOnStagnation', AppService.xmlDoc, true);
        EROSObj.setAttribute("status", instance.config.evoRestartOnStagnation);
        EROSObj.setAttribute("fitnessType", instance.config.evoFitnessRestartType);
        EROSObj.setAttribute("generations", instance.config.evoStagnationGenerations);

        var EROSObj = createChild(buildModeElem, 'EvoInSamplePeriod', AppService.xmlDoc, true);
        EROSObj.setAttribute("ratio", instance.config.evoTrainingValidationRatio);

        //Fresh blood
        addNode('FreshBloodReplaceSimilar', instance.config.freshBloodReplaceSimilar, buildModeElem, AppService.xmlDoc);
        addNode('FreshBloodReplaceWeakest', instance.config.freshBloodReplaceWeakest, buildModeElem, AppService.xmlDoc);
        addNode('FreshBloodWeakestPct', instance.config.freshBloodWeakestPct, buildModeElem, AppService.xmlDoc);
        addNode('FreshBloodWeakestGenerations', instance.config.freshBloodWeakestGenerations, buildModeElem, AppService.xmlDoc);

        instance.checkIPSettings();
    }
    
    this.checkIPSettings = function(){
        var currentTask = AppService.getTask()

        var taskElems = getTasks(AppService.getProjectConfig());

        var IPDatabankName = instance.constants.gedatabanks.initialPopulation;
        var LGDatabankName = instance.constants.gedatabanks.lastGeneration;

        var showGeneticOptions = false;
        var showLastGenerationDatabank = false;
        var showInitialPopulationDatabank = false;

        for(var i=0; i<taskElems.length; i++){
            var taskElem = taskElems[i];
            var taskXMLFile = getAttrStringValue(taskElem, "taskXMLFile", null);
            if(!taskXMLFile) {
                console.error("Task " + xmlToString(taskElem) + " doesn't have taskXMLFile filled");
                continue;
            }

            var settingsObj = null;
            if(taskXMLFile === AppService.getTask().taskXMLFile){
                settingsObj = AppService.getTaskConfig();       //current changed task config is not saved in SQConstants yet
            }
            else {
                settingsObj = SQConstants.getTaskConfig(AppService.getProject(), taskXMLFile, true);
            }
            
            var whatToBuildElem = getChildElement(settingsObj, "WhatToBuild");
            var buildModeElem = getChildElement(whatToBuildElem, "BuildMode");
            if(!buildModeElem) continue;
            
            var generationType = getAttrStringValue(buildModeElem, "generationType", instance.generationTypes.randomGeneration);
            var initGenerationType = getNodeIntValue(buildModeElem, 'InitGenerationType', 1);
            
            if(generationType == instance.generationTypes.geneticEvolution){
                if(currentTask.name == getAttrValue(taskElem, 'name')) {            
                    showGeneticOptions = true;
                }
                
                if(initGenerationType == 2){
                    showInitialPopulationDatabank = true;
                }

                if(getNodeBooleanValue(buildModeElem, 'ShowLastGenerationDatabank', instance.config.showLastGenerationDatabank)){
                    showLastGenerationDatabank = true;
                }
            }
        }
        
        $timeout(function(){
            SQEvents.notifyListeners(SQEvents.get("DATABANK_DISPLAY"), { name : IPDatabankName, show : showInitialPopulationDatabank });
            SQEvents.notifyListeners(SQEvents.get("DATABANK_DISPLAY"), { name : LGDatabankName, show : showLastGenerationDatabank });
            SQEvents.notifyListeners(SQEvents.get('RELOAD_DATABANKS'), {});
        }, 500, false);

        SQEvents.notifyListeners(SQEvents.get('SETTINGS_TAB_SHOW'), { 
            tab : 'genetic-options', 
            show : AppService.getTask().type=="Build" && showGeneticOptions
        });
    }

    function onEvent(event, data) {
        if(event == SQEvents.get("DATABANKS_LOADED")) {
            if(!AppService.isTaskManager()) instance.checkIPSettings();
        } 
        else if(event == SQEvents.get("SETTINGS_TAB_RELOAD") && data == "all") {        //controller may not be initialized
            instance.loadSettings();
            instance.checkIPSettings();
        } 
    }

    var instance = this;

    this.constants = SQConstants.getConstants();
    this.generationTypes = SQConstants.getConstants().buildGenerationTypes;
    this.controllerReady = false;
    this.conditionsToSet = null;

    this.sampleTypes = SQConstants.getConstants().sampleTypes;

    this.evoFitnessRestartTypes = [{
        'name': L.tsq('In sample (whole)'),
        'value': instance.sampleTypes.in
    }, {
        'name': L.tsq('In sample (Training)'),
        'value': instance.sampleTypes.ist
    }, {
        'name': L.tsq('In sample (Validation)'),
        'value': instance.sampleTypes.isv
    }];    

    this.config = {
        condGridInstance : {},
        populationSize : 5,
        maxGenerations : 10,
        crossoverProbability : 50,
        mutationProbability : 20,
        showAdvancedSettings : false,
        islands : 10,
        migrationModulo : 50,
        migrationRate : 20,
        initGenerationType : 1,
        decimationCoef : 1,
        evoRestartOnFinish : true,
        evoRestartOnStagnation : true,
        evoFitnessRestartType : instance.sampleTypes.in,
        evoStagnationGenerations : 30,
        showLastGenerationDatabank : false,
        freshBloodReplaceSimilar : true,
        freshBloodReplaceWeakest : true,
        freshBloodWeakestPct : 10,
        freshBloodWeakestGenerations : 2,
        evoTrainingValidationRatio: 50
    };

    this.loaded = false;

    SQEvents.addListener('SettingsGeneticOptionsService', [SQEvents.get("DATABANKS_LOADED"), SQEvents.get('SETTINGS_TAB_RELOAD')], onEvent);

});