angular.module('app.settings').service('ConditionsAndPeriodsService', function($rootScope, AppService, SQConstants, ComplexitiesService, L) {
    
    this.loadSettings = function(){
        loadToArray(instance.config.complexities, angular.copy(ComplexitiesService.settings.complexities));
        instance.config.useDifferentSettings = ComplexitiesService.settings.useDifferentSettings;
        
        oldConfig = angular.copy(instance.config);
    }

    this.checkSettings = function(){
        return true;
    }

    this.saveSettings = function(whatToBuildElem){
        loadToArray(ComplexitiesService.settings.complexities, instance.config.complexities);
        ComplexitiesService.settings.useDifferentSettings = instance.config.useDifferentSettings;
        ComplexitiesService.saveComplexities();
    }

    this.resetSettings = function(useOldSettings){ 
        instance.config.useDifferentSettings = useOldSettings ? oldConfig.useDifferentSettings : true;

        if(useOldSettings){
            loadToArray(instance.config.complexities, oldConfig.complexities);
        }
        else {
            for(var i=0; i<instance.config.complexities.length; i++){
                ComplexitiesService.resetComplexity(instance.config.complexities[i]);
            }
        }
    }

    this.getDescription = function(settingName){
        var complexity = instance.config.complexities[0];
        if(!complexity) return "N/A";

        var description = L.tsq("Conditions to generate: %d-%d", [complexity.minConditions, complexity.maxConditions]);
        description += ", " + L.tsq("Max lookback period: %d", [complexity.maxShift]); 
        description += ", " + L.tsq("Indicator periods: %d-%d", [complexity.minPeriod, complexity.maxPeriod]); 
        return description;
    }

    var instance = this;
    var oldConfig = null;

    this.constants = SQConstants.getConstants();
    this.config = { 
        useDifferentSettings : true,
        complexities : []
    };

});