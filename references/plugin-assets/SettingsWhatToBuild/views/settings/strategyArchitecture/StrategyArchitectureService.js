angular.module('app.settings').service('StrategyArchitectureService', function($rootScope, AppService, SQConstants, L) {
    
    this.loadSettings = function(){
        instance.resetSettings();
        
        var whatToBuildElem = AppService.getCurrentTaskTabSettings("WhatToBuild");
        var strategyTypeElem = getChildElement(whatToBuildElem, 'StrategyType');

        instance.config.type = getAttrStringValue(strategyTypeElem, 'architecture', instance.config.type);
        instance.config.minTrue = getAttrIntValue(strategyTypeElem, 'minTrue', instance.config.minTrue);
        instance.config.maxTrue = getAttrIntValue(strategyTypeElem, 'maxTrue', instance.config.maxTrue);
        
        oldConfig = angular.copy(instance.config);
    }

    this.checkSettings = function(){
        if(instance.config.type == instance.constants.strategyArchitectures.sq4Fuzzy && instance.config.minTrue > instance.config.maxTrue){
            $rootScope.showError(L.tsq("Invalid Fuzzy Logic settings. Min value must be lower or equal to max value"));
            return false;
        }
        return true;
    }

    this.saveSettings = function(whatToBuildElem){
        var strategyTypeElem = getChildElement(whatToBuildElem, 'StrategyType');
        strategyTypeElem.setAttribute("architecture", instance.config.type || instance.constants.strategyArchitectures.sq4);

        if(instance.config.type == instance.constants.strategyArchitectures.sq4Fuzzy){
            strategyTypeElem.setAttribute("minTrue", instance.config.minTrue);
            strategyTypeElem.setAttribute("maxTrue", instance.config.maxTrue);
        }
        else {
            strategyTypeElem.removeAttribute("minTrue");
            strategyTypeElem.removeAttribute("maxTrue");
        }
    }

    this.resetSettings = function(useOldSettings){ 
        instance.config.type = useOldSettings ? oldConfig.type : instance.constants.strategyArchitectures.sq4;
        instance.config.minTrue = useOldSettings ? oldConfig.minTrue : 70;
        instance.config.maxTrue = useOldSettings ? oldConfig.maxTrue : 70;
    }

    this.getDescription = function(){
        switch(instance.config.type){
            case instance.constants.strategyArchitectures.sq4: return L.tsq("SQX Signals");
            case instance.constants.strategyArchitectures.sq4Fuzzy: return L.tsq("SQX Signals with Fuzzy Logic, True conditions: %d-%d %", [instance.config.minTrue, instance.config.maxTrue]);
            case instance.constants.strategyArchitectures.sq3: return L.tsq("Old SQ3 architecture");
            default: return L.tsq("N/A");
        }
    }

    var instance = this;
    var oldConfig = null;

    this.constants = SQConstants.getConstants();
    this.config = {};

});