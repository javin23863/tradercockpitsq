angular.module('app.settings').service('TradingDirectionsService', function($rootScope, AppService, SQConstants, L) {
    
    this.loadSettings = function(){
        instance.resetSettings();
        
        var whatToBuildElem = AppService.getCurrentTaskTabSettings("WhatToBuild");
        var marketSidesElem = getChildElement(whatToBuildElem, 'MarketSides', true);
        if(marketSidesElem){
            instance.config.type = getAttrStringValue(marketSidesElem, 'type', instance.constants.buildStrategyDirections.both);
            instance.config.entrySymmetry = getNodeBooleanValue(marketSidesElem, 'EntrySymmetry', true);
            instance.config.exitSymmetry = getNodeBooleanValue(marketSidesElem, 'ExitSymmetry', true);
        }
        
        oldConfig = angular.copy(instance.config);
    }

    this.checkSettings = function(){
        return true;
    }

    this.saveSettings = function(whatToBuildElem){
        var marketSidesElem = createChild(whatToBuildElem, 'MarketSides', AppService.xmlDoc);
        marketSidesElem.setAttribute("type", instance.config.type);

        if(instance.config.type == instance.constants.buildStrategyDirections.both){
            addNode('EntrySymmetry', instance.config.entrySymmetry, marketSidesElem, AppService.xmlDoc);
            addNode('ExitSymmetry', instance.config.exitSymmetry, marketSidesElem, AppService.xmlDoc);
        }
        else {
            addNode('EntrySymmetry', false, marketSidesElem, AppService.xmlDoc);
            addNode('ExitSymmetry', false, marketSidesElem, AppService.xmlDoc);
        }
    }

    this.resetSettings = function(useOldSettings){ 
        instance.config.type = useOldSettings ? oldConfig.type : instance.constants.buildStrategyDirections.both; 
        instance.config.entrySymmetry = useOldSettings ? oldConfig.entrySymmetry : true;
        instance.config.exitSymmetry = useOldSettings ? oldConfig.exitSymmetry : true;
    }

    this.getDescription = function(){
        var directions = instance.constants.buildStrategyDirections;
        switch(instance.config.type){
            case directions.both: 
                var description = L.tsq("Both (Long & Short)");
                if(!instance.config.entrySymmetry && !instance.config.exitSymmetry){
                    description += ", " + L.tsq("No symmetry");
                }
                else {
                    description += instance.config.entrySymmetry ? ", " + L.tsq("Entry symmetry") : "";
                    description += instance.config.exitSymmetry ? ", " + L.tsq("Exit symmetry") : "";
                }
                return description;
            case directions.long: return L.tsq("Long only");
            case directions.short: return L.tsq("Short only");
            default: return L.tsq("N/A");
        }
    }

    var instance = this;
    var oldConfig = null;

    this.constants = SQConstants.getConstants();
    this.config = {};

});