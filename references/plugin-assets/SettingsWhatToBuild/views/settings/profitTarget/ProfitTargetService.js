angular.module('app.settings').service('ProfitTargetService', function($rootScope, AppService, SQConstants, SQEvents, StopLossService, L) {
    
    this.loadSettings = function(){
        instance.resetSettings();
        
        var whatToBuildElem = AppService.getCurrentTaskTabSettings("WhatToBuild");
        var slptOptionsElem = getChildElement(whatToBuildElem, 'SLPTOptions');
        if(slptOptionsElem){
            instance.config.required = getNodeBooleanValue(slptOptionsElem, "PTRequired", instance.config.required);
            instance.config.sameAsSL = !getNodeBooleanValue(slptOptionsElem, "SeparatedSettings", instance.config.sameAsSL);

            instance.config.fixedPips = getNodeBooleanValue(slptOptionsElem, "PTFixedPips", instance.config.fixedPips);
            instance.config.percent = getNodeBooleanValue(slptOptionsElem, "PTPercent", instance.config.percent);

            if(instance.config.fixedPips){
                instance.config.minPips = getNodeFloatValue(slptOptionsElem, "MinPTInPips", instance.config.minPips);
                instance.config.maxPips = getNodeFloatValue(slptOptionsElem, "MaxPTInPips", instance.config.maxPips);
            }
            else {
                instance.config.minPips = getNodeFloatValue(slptOptionsElem, "MinPTInMoney", instance.config.minPips);
                instance.config.maxPips = getNodeFloatValue(slptOptionsElem, "MaxPTInMoney", instance.config.maxPips);
            }

            instance.config.minPercent = getNodeFloatValue(slptOptionsElem, "MinPTInPercent", instance.config.minPercent);
            instance.config.maxPercent = getNodeFloatValue(slptOptionsElem, "MaxPTInPercent", instance.config.maxPercent);
            instance.config.atrBased = getNodeBooleanValue(slptOptionsElem, "PTATR", instance.config.atrBased);
            instance.config.minATRMultiple = getNodeFloatValue(slptOptionsElem, "MinPTATRMultiple", instance.config.minATRMultiple);
            instance.config.maxATRMultiple = getNodeFloatValue(slptOptionsElem, "MaxPTATRMultiple", instance.config.maxATRMultiple);
            instance.config.minATRPeriod = getNodeFloatValue(slptOptionsElem, "MinPTATRPeriod", instance.config.minATRPeriod);
            instance.config.maxATRPeriod = getNodeFloatValue(slptOptionsElem, "MaxPTATRPeriod", instance.config.maxATRPeriod);
            
            instance.config.limitRRRatio = getNodeBooleanValue(slptOptionsElem, "LimitSLPTRRR", instance.config.limitRRRatio);
            instance.config.rrRatioFrom = getNodeFloatValue(slptOptionsElem, "LimitSLPTRRRFrom", instance.config.sameAsSL);
            instance.config.rrRatioTo = getNodeFloatValue(slptOptionsElem, "LimitSLPTRRRTo", instance.config.sameAsSL);

            instance.config.indicatorBased = getNodeBooleanValue(slptOptionsElem, "PTIndicatorBased", instance.config.indicatorBased);

            if(instance.config.sameAsSL){
                instance.applySLSettings();
            }
            else {
                instance.applyPTSettings();
            }
        }

        instance.initialized = true;
        
        oldConfig = angular.copy(instance.config);
    }

    this.applySLSettings = function(){
        if(!StopLossService.initialized){
            StopLossService.loadSettings();
        }
        var SLSettings = angular.copy(StopLossService.config);
        instance.guiConfig.fixedPips = SLSettings.fixedPips;
        instance.guiConfig.minPips = SLSettings.minPips;
        instance.guiConfig.maxPips = SLSettings.maxPips;
        instance.guiConfig.percent = SLSettings.percent;
        instance.guiConfig.minPercent = SLSettings.minPercent;
        instance.guiConfig.maxPercent = SLSettings.maxPercent;
        instance.guiConfig.atrBased = SLSettings.atrBased;
        instance.guiConfig.minATRMultiple = SLSettings.minATRMultiple;
        instance.guiConfig.maxATRMultiple = SLSettings.maxATRMultiple;
        instance.guiConfig.minATRPeriod = SLSettings.minATRPeriod;
        instance.guiConfig.maxATRPeriod = SLSettings.maxATRPeriod;
    }

    this.applyPTSettings = function(){
        instance.guiConfig.fixedPips = instance.config.fixedPips;
        instance.guiConfig.minPips = instance.config.minPips;
        instance.guiConfig.maxPips = instance.config.maxPips;
        instance.guiConfig.percent = instance.config.percent;
        instance.guiConfig.minPercent = instance.config.minPercent;
        instance.guiConfig.maxPercent = instance.config.maxPercent;
        instance.guiConfig.atrBased = instance.config.atrBased;
        instance.guiConfig.minATRMultiple = instance.config.minATRMultiple;
        instance.guiConfig.maxATRMultiple = instance.config.maxATRMultiple;
        instance.guiConfig.minATRPeriod = instance.config.minATRPeriod;
        instance.guiConfig.maxATRPeriod = instance.config.maxATRPeriod;
    }

    this.checkSettings = function(){
        if(parseFloat(instance.config.rrRatioFrom) > parseFloat(instance.config.rrRatioTo)){
            $rootScope.showError(L.tsq("Risk-Reward ratio error - From must be lower or equal To"));
            return false;
        }
        else return true;
    }

    this.saveSettings = function(whatToBuildElem){
        var slptOptionsElem = createChild(whatToBuildElem, 'SLPTOptions', AppService.xmlDoc);

        if(!instance.config.sameAsSL){
            instance.config.fixedPips = instance.guiConfig.fixedPips;
            instance.config.minPips = instance.guiConfig.minPips;
            instance.config.maxPips = instance.guiConfig.maxPips;
            instance.config.percent = instance.guiConfig.percent;
            instance.config.minPercent = instance.guiConfig.minPercent;
            instance.config.maxPercent = instance.guiConfig.maxPercent;
            instance.config.atrBased = instance.guiConfig.atrBased;
            instance.config.minATRMultiple = instance.guiConfig.minATRMultiple;
            instance.config.maxATRMultiple = instance.guiConfig.maxATRMultiple;
            instance.config.minATRPeriod = instance.guiConfig.minATRPeriod;
            instance.config.maxATRPeriod = instance.guiConfig.maxATRPeriod;
        }        

        var required = getNodeBooleanValue(slptOptionsElem, "PTRequired", false);
        var fixedPips = getNodeBooleanValue(slptOptionsElem, "PTFixedPips", false);
        var atrBased = getNodeBooleanValue(slptOptionsElem, "PTATR", false);
        var indyBased = getNodeBooleanValue(slptOptionsElem, "PTIndicatorBased", false);

        if(instance.config.required != required || 
            instance.config.fixedValue != fixedPips ||
            instance.config.atrBased != atrBased ||
            instance.config.indicatorBased != indyBased 
        ){
            SQEvents.notifyListeners(SQEvents.get("SLPT_SETTINGS_CHANGED"), { 
                setting : 'Profit Target', 
                required : instance.config.required, 
                fixedValue : instance.config.fixedPips,
                atrBased : instance.config.atrBased,
                indicatorBased : instance.config.indicatorBased,
                from : "ProfitTargetService" 
            });
        }
        
        var node = createChild(slptOptionsElem, "PTRequired", AppService.xmlDoc); setNodeValue(node, instance.config.required, AppService.xmlDoc);
        node = createChild(slptOptionsElem, "SeparatedSettings", AppService.xmlDoc); setNodeValue(node, !instance.config.sameAsSL, AppService.xmlDoc);
        node = createChild(slptOptionsElem, "PTFixedPips", AppService.xmlDoc); setNodeValue(node, instance.config.fixedPips, AppService.xmlDoc);
        node = createChild(slptOptionsElem, "MinPTInPips", AppService.xmlDoc); setNodeValue(node, instance.config.minPips, AppService.xmlDoc);
        node = createChild(slptOptionsElem, "MaxPTInPips", AppService.xmlDoc); setNodeValue(node, instance.config.maxPips, AppService.xmlDoc);
        node = createChild(slptOptionsElem, "PTPercent", AppService.xmlDoc); setNodeValue(node, instance.config.percent, AppService.xmlDoc);
        node = createChild(slptOptionsElem, "MinPTInPercent", AppService.xmlDoc); setNodeValue(node, instance.config.minPercent, AppService.xmlDoc);
        node = createChild(slptOptionsElem, "MaxPTInPercent", AppService.xmlDoc); setNodeValue(node, instance.config.maxPercent, AppService.xmlDoc);
        node = createChild(slptOptionsElem, "MinPTInMoney", AppService.xmlDoc); setNodeValue(node, instance.config.minPips, AppService.xmlDoc);
        node = createChild(slptOptionsElem, "MaxPTInMoney", AppService.xmlDoc); setNodeValue(node, instance.config.maxPips, AppService.xmlDoc);
        node = createChild(slptOptionsElem, "PTATR", AppService.xmlDoc); setNodeValue(node, instance.config.atrBased, AppService.xmlDoc);
        node = createChild(slptOptionsElem, "MinPTATRMultiple", AppService.xmlDoc); setNodeValue(node, instance.config.minATRMultiple, AppService.xmlDoc);
        node = createChild(slptOptionsElem, "MaxPTATRMultiple", AppService.xmlDoc); setNodeValue(node, instance.config.maxATRMultiple, AppService.xmlDoc);
        node = createChild(slptOptionsElem, "MinPTATRPeriod", AppService.xmlDoc); setNodeValue(node, instance.config.minATRPeriod, AppService.xmlDoc);
        node = createChild(slptOptionsElem, "MaxPTATRPeriod", AppService.xmlDoc); setNodeValue(node, instance.config.maxATRPeriod, AppService.xmlDoc);
        node = createChild(slptOptionsElem, "LimitSLPTRRR", AppService.xmlDoc); setNodeValue(node, instance.config.limitRRRatio, AppService.xmlDoc);
        node = createChild(slptOptionsElem, "LimitSLPTRRRFrom", AppService.xmlDoc); setNodeValue(node, instance.config.rrRatioFrom, AppService.xmlDoc);
        node = createChild(slptOptionsElem, "LimitSLPTRRRTo", AppService.xmlDoc); setNodeValue(node, instance.config.rrRatioTo, AppService.xmlDoc);
        node = createChild(slptOptionsElem, "PTIndicatorBased", AppService.xmlDoc); setNodeValue(node, instance.config.indicatorBased, AppService.xmlDoc);
    }

    this.resetSettings = function(useOldSettings){ 
        instance.config.required = useOldSettings ? oldConfig.required : true;
        instance.config.sameAsSL = useOldSettings ? oldConfig.sameAsSL : false;
        instance.config.fixedPips = useOldSettings ? oldConfig.fixedPips : true;
        instance.config.minPips = useOldSettings ? oldConfig.minPips : 1;
        instance.config.maxPips = useOldSettings ? oldConfig.maxPips : 10;
        instance.config.percent = useOldSettings ? oldConfig.percent : true;
        instance.config.minPercent = useOldSettings ? oldConfig.minPercent : 1;
        instance.config.maxPercent = useOldSettings ? oldConfig.maxPercent : 10;
        instance.config.atrBased = useOldSettings ? oldConfig.atrBased : true;
        instance.config.minATRMultiple = useOldSettings ? oldConfig.minATRMultiple : 2;
        instance.config.maxATRMultiple = useOldSettings ? oldConfig.maxATRMultiple : 5;
        instance.config.minATRPeriod = useOldSettings ? oldConfig.minATRPeriod : 20;
        instance.config.maxATRPeriod = useOldSettings ? oldConfig.maxATRPeriod : 30;
        instance.config.limitRRRatio = useOldSettings ? oldConfig.limitRRRatio : false;
        instance.config.rrRatioFrom = useOldSettings ? oldConfig.rrRatioFrom : 50;
        instance.config.rrRatioTo = useOldSettings ? oldConfig.rrRatioTo : 80;
        instance.config.indicatorBased = useOldSettings ? oldConfig.indicatorBased : false;

        instance.applyPTSettings();
    }

    this.getDescription = function(settingName){
        var description = instance.config.required ? L.tsq("Required") : L.tsq("Not required");

        if(instance.config.sameAsSL){
            description += ", " + L.tsq("same ranges as SL");
        }
        else {
            if(instance.config.fixedPips){
                description += ", " + L.tsq("Pips based:") + " " + instance.config.minPips + "-" + instance.config.maxPips + " " + L.tsq("pips");
            }
            if(instance.config.percent){
                description += ", " + L.tsq("Percent based:") + " " + instance.config.minPercent + "%-" + instance.config.maxPercent + "%";
            }
            if(instance.config.atrBased){
                description += ", " + L.tsq("ATR based: Coefficient:") + " " + instance.config.minATRMultiple + "-" + instance.config.maxATRMultiple;
            }
        }

        if(instance.config.limitRRRatio){
            description += ", " + L.tsq("Risk-Reward Ratio limit:") + " ";
            if(instance.config.sameAsSL){
                description += instance.config.rrRatioFrom + "-" + instance.config.rrRatioTo + L.tsq("% SL");
            }
            else {
                description += instance.config.rrRatioFrom + "-" + instance.config.rrRatioTo + "%";
            }
        }

        if (instance.config.indicatorBased) {
            description += ", " + L.tsq("Indicator based");
        }

        return description;
    }

    function onEvent(event, data) {
        if (event == SQEvents.get('SLPT_SETTINGS_CHANGED')) {
            if(data.setting == "Profit Target" && data.from != "ProfitTargetService") {
                instance.config.required = data.required;
                instance.saveSettings(AppService.getCurrentTaskTabSettings("WhatToBuild"));
            }
            else if(data.setting == "Stop Loss" && data.from == "StopLossService" && instance.config.sameAsSL){
                instance.config.fixedPips = StopLossService.config.fixedPips;
                instance.config.minPips = StopLossService.config.minPips;
                instance.config.maxPips = StopLossService.config.maxPips;
                instance.config.percent = StopLossService.config.percent;
                instance.config.minPercent = StopLossService.config.minPercent;
                instance.config.maxPercent = StopLossService.config.maxPercent;
                instance.config.atrBased = StopLossService.config.atrBased;
                instance.config.minATRMultiple = StopLossService.config.minATRMultiple;
                instance.config.maxATRMultiple = StopLossService.config.maxATRMultiple;
                instance.config.minATRPeriod = StopLossService.config.minATRPeriod;
                instance.config.maxATRPeriod = StopLossService.config.maxATRPeriod;
                instance.saveSettings(AppService.getCurrentTaskTabSettings("WhatToBuild"));
            }
        } 
    }

    var instance = this;
    var oldConfig = null;

    this.constants = SQConstants.getConstants();
    this.config = {};
    this.guiConfig = {};
    this.initialized = false;

    var listenerId = "ProfitTargetService";
    SQEvents.addListener(listenerId, [SQEvents.get('SLPT_SETTINGS_CHANGED')], onEvent);

});