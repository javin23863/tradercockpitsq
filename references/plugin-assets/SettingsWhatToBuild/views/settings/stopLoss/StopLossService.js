angular.module('app.settings').service('StopLossService', function ($rootScope, AppService, SQConstants, SQEvents, L) {

    this.loadSettings = function () {
        instance.resetSettings();

        var whatToBuildElem = AppService.getCurrentTaskTabSettings("WhatToBuild");
        var slptOptionsElem = getChildElement(whatToBuildElem, 'SLPTOptions');
        if (slptOptionsElem) {
            instance.config.required = getNodeBooleanValue(slptOptionsElem, "SLRequired", instance.config.required);
            instance.config.fixedPips = getNodeBooleanValue(slptOptionsElem, "SLFixedPips", instance.config.fixedPips);
            instance.config.percent = getNodeBooleanValue(slptOptionsElem, "SLPercent", instance.config.percent);

            if (instance.config.fixedPips) {
                instance.config.minPips = getNodeFloatValue(slptOptionsElem, "MinSLInPips", instance.config.minPips);
                instance.config.maxPips = getNodeFloatValue(slptOptionsElem, "MaxSLInPips", instance.config.maxPips);
            }
            else {
                instance.config.minPips = getNodeFloatValue(slptOptionsElem, "MinSLInMoney", instance.config.minPips);
                instance.config.maxPips = getNodeFloatValue(slptOptionsElem, "MaxSLInMoney", instance.config.maxPips);
            }

            instance.config.minPercent = getNodeFloatValue(slptOptionsElem, "MinSLInPercent", instance.config.minPercent);
            instance.config.maxPercent = getNodeFloatValue(slptOptionsElem, "MaxSLInPercent", instance.config.maxPercent);
            instance.config.atrBased = getNodeBooleanValue(slptOptionsElem, "SLATR", instance.config.atrBased);
            instance.config.minATRMultiple = getNodeFloatValue(slptOptionsElem, "MinSLATRMultiple", instance.config.minATRMultiple);
            instance.config.maxATRMultiple = getNodeFloatValue(slptOptionsElem, "MaxSLATRMultiple", instance.config.maxATRMultiple);
            instance.config.minATRPeriod = getNodeFloatValue(slptOptionsElem, "MinSLATRPeriod", instance.config.minATRPeriod);
            instance.config.maxATRPeriod = getNodeFloatValue(slptOptionsElem, "MaxSLATRPeriod", instance.config.maxATRPeriod);
            
            instance.config.indicatorBased = getNodeBooleanValue(slptOptionsElem, "SLIndicatorBased", instance.config.indicatorBased);
        }

        instance.initialized = true;

        oldConfig = angular.copy(instance.config);
    }

    this.checkSettings = function () {
        return true;
    }

    this.saveSettings = function (whatToBuildElem) {
        var slptOptionsElem = createChild(whatToBuildElem, 'SLPTOptions', AppService.xmlDoc);

        var required = getNodeBooleanValue(slptOptionsElem, "SLRequired", false);
        var fixedPips = getNodeBooleanValue(slptOptionsElem, "SLFixedPips", false);
        var percent = getNodeBooleanValue(slptOptionsElem, "SLPercent", false);
        var atrBased = getNodeBooleanValue(slptOptionsElem, "SLATR", false);
        var indyBased = getNodeBooleanValue(slptOptionsElem, "SLIndicatorBased", false);

        if(instance.config.required != required || 
            instance.config.fixedValue != fixedPips ||
            instance.config.atrBased != atrBased ||
            instance.config.indicatorBased != indyBased 
        ){
            SQEvents.notifyListeners(SQEvents.get("SLPT_SETTINGS_CHANGED"), { 
                setting : 'Stop Loss', 
                required : instance.config.required, 
                fixedValue : instance.config.fixedPips,
                atrBased : instance.config.atrBased,
                indicatorBased : instance.config.indicatorBased,
                from : "StopLossService" 
            });
        }

        var node = createChild(slptOptionsElem, "SLRequired", AppService.xmlDoc); setNodeValue(node, instance.config.required, AppService.xmlDoc);
        node = createChild(slptOptionsElem, "SLFixedPips", AppService.xmlDoc); setNodeValue(node, instance.config.fixedPips, AppService.xmlDoc);
        node = createChild(slptOptionsElem, "MinSLInPips", AppService.xmlDoc); setNodeValue(node, instance.config.minPips, AppService.xmlDoc);
        node = createChild(slptOptionsElem, "MaxSLInPips", AppService.xmlDoc); setNodeValue(node, instance.config.maxPips, AppService.xmlDoc);
        node = createChild(slptOptionsElem, "SLPercent", AppService.xmlDoc); setNodeValue(node, instance.config.percent, AppService.xmlDoc);
        node = createChild(slptOptionsElem, "MinSLInPercent", AppService.xmlDoc); setNodeValue(node, instance.config.minPercent, AppService.xmlDoc);
        node = createChild(slptOptionsElem, "MaxSLInPercent", AppService.xmlDoc); setNodeValue(node, instance.config.maxPercent, AppService.xmlDoc);
        node = createChild(slptOptionsElem, "MinSLInMoney", AppService.xmlDoc); setNodeValue(node, instance.config.minPips, AppService.xmlDoc);
        node = createChild(slptOptionsElem, "MaxSLInMoney", AppService.xmlDoc); setNodeValue(node, instance.config.maxPips, AppService.xmlDoc);
        node = createChild(slptOptionsElem, "SLATR", AppService.xmlDoc); setNodeValue(node, instance.config.atrBased, AppService.xmlDoc);
        node = createChild(slptOptionsElem, "MinSLATRMultiple", AppService.xmlDoc); setNodeValue(node, instance.config.minATRMultiple, AppService.xmlDoc);
        node = createChild(slptOptionsElem, "MaxSLATRMultiple", AppService.xmlDoc); setNodeValue(node, instance.config.maxATRMultiple, AppService.xmlDoc);
        node = createChild(slptOptionsElem, "MinSLATRPeriod", AppService.xmlDoc); setNodeValue(node, instance.config.minATRPeriod, AppService.xmlDoc);
        node = createChild(slptOptionsElem, "MaxSLATRPeriod", AppService.xmlDoc); setNodeValue(node, instance.config.maxATRPeriod, AppService.xmlDoc);
        node = createChild(slptOptionsElem, "SLIndicatorBased", AppService.xmlDoc); setNodeValue(node, instance.config.indicatorBased, AppService.xmlDoc);
    }

    this.resetSettings = function (useOldSettings) {
        instance.config.required = useOldSettings ? oldConfig.required : true;
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
        instance.config.indicatorBased = useOldSettings ? oldConfig.indicatorBased : false;
    }

    this.getDescription = function (settingName) {
        var description = instance.config.required ? L.tsq("Required") : L.tsq("Not required");
        if (instance.config.fixedPips) {
            description += ", " + L.tsq("Pips based: %d-%d pips", [instance.config.minPips, instance.config.maxPips]);
        }
        if(instance.config.percent){
            description += ", " + L.tsq("Percent based: %f%-%f%", [instance.config.minPercent, instance.config.maxPercent]);
        }
        if (instance.config.atrBased) {
            description += ", " + L.tsq("ATR based: Coefficient: %f-%f", [instance.config.minATRMultiple, instance.config.maxATRMultiple]);
        }
        if (instance.config.indicatorBased) {
            description += ", " + L.tsq("Indicator based");
        }

        return description;
    }

    function onEvent(event, data) {
        if (event == SQEvents.get('SLPT_SETTINGS_CHANGED') && data.setting == "Stop Loss" && data.from != "StopLossService") {
            instance.config.required = data.required;
            instance.saveSettings(AppService.getCurrentTaskTabSettings("WhatToBuild"));
        }
    }

    var instance = this;
    var oldConfig = null;

    this.constants = SQConstants.getConstants();
    this.config = {};
    this.initialized = false;

    var listenerId = "StopLossService";
    SQEvents.addListener(listenerId, [SQEvents.get('SLPT_SETTINGS_CHANGED')], onEvent);

});