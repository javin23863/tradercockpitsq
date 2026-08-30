angular.module('app.settings').service('AdvancedTMService', function ($rootScope, $q, BackendService, AppService, SQConstants, L) {

    this.loadConfig = function(configStr){
        var elAtms;

        if(configStr){
            elAtms = xmlToObject(configStr).find("ATMs")[0];
        }
        else {
            elAtms = AppService.getCurrentTaskTabSettings("ATMs");
        }

        instance.config.enableATM = getAttrBooleanValue(elAtms, "enable", instance.config.enableATM);
        instance.config.atmType = getAttrStringValue(elAtms, "scaleOutType", instance.config.atmType);
        instance.config.sizeDecimals = getAttrIntValue(elAtms, "sizeDecimals", instance.config.sizeDecimals);
        instance.config.minSize = getAttrFloatValue(elAtms, "minSize", instance.config.minSize);

        instance.config.exits.length = 0;

        var atmElems = getChildElements(elAtms, "ATM");
        
        for(var i=0; i<atmElems.length; i++){
            var elATM = atmElems[i];
            var id = getAttrStringValue(elATM, "id", null);

            if(id == "global"){
                var elExits = getChildElement(elATM, "Exits");
                var exitElems = getChildElements(elExits, "Exit");

                for(var a=0; a<exitElems.length; a++){
                    var elExit = exitElems[a];
                    var exitObj = {};

                    var elPositionSizes = getChildElement(elExit, "PositionSizes");
                    loadPositionSizeParams(elPositionSizes, exitObj);

                    var elExitLevels = getChildElement(elExit, "ExitLevels");
                    loadExitLevelParams(elExitLevels, exitObj);

                    var elMoveSL2BE = getChildElement(elExit, "MoveSL2BE");
                    loadMoveSL2BEParams(elMoveSL2BE, exitObj);

                    exitObj.description = instance.getDescription(exitObj);

                    instance.config.exits.push(exitObj);
                }
                break;
            }
        }

        var elGenerateConfig = getChildElement(elAtms, "GenerateConfig");
        var elTypes = getChildElement(elGenerateConfig, "Types");

        var elSLMultiple = getChildElement(elTypes, "SLMultiple");
        instance.config.generate.useSLMultiple = getAttrBooleanValue(elSLMultiple, "use", false);
        instance.config.generate.slMultipleMin = getAttrFloatValue(elSLMultiple, "minMultiplier", 0.5);
        instance.config.generate.slMultipleMax = getAttrFloatValue(elSLMultiple, "maxMultiplier", 2);

        var elPTMultiple = getChildElement(elTypes, "PTMultiple");
        instance.config.generate.usePTMultiple = getAttrBooleanValue(elPTMultiple, "use", false);
        instance.config.generate.ptMultipleMin = getAttrFloatValue(elPTMultiple, "minMultiplier", 0.5);
        instance.config.generate.ptMultipleMax = getAttrFloatValue(elPTMultiple, "maxMultiplier", 2);

        var elFixedProfit = getChildElement(elTypes, "FixedProfit");
        instance.config.generate.useFixedProfit = getAttrBooleanValue(elFixedProfit, "use", false);
        instance.config.generate.useFixedProfitFixedPips = getAttrBooleanValue(elFixedProfit, "useFixedPips", false);
        instance.config.generate.fixedProfitFixedPipsMin = getAttrIntValue(elFixedProfit, "minPips", 30);
        instance.config.generate.fixedProfitFixedPipsMax = getAttrIntValue(elFixedProfit, "maxPips", 80);
        instance.config.generate.useFixedProfitATR = getAttrBooleanValue(elFixedProfit, "useATR", false);
        instance.config.generate.fixedProfitATRMutliplierMin = getAttrFloatValue(elFixedProfit, "minMultiplier", 1.5);
        instance.config.generate.fixedProfitATRMutliplierMax = getAttrFloatValue(elFixedProfit, "maxMultiplier", 3);
        instance.config.generate.fixedProfitATRPeriodMin = getAttrIntValue(elFixedProfit, "minATRPeriod", 20);
        instance.config.generate.fixedProfitATRPeriodMax = getAttrIntValue(elFixedProfit, "maxATRPeriod", 20);

        var elTrailingStop = getChildElement(elTypes, "TrailingStop");
        instance.config.generate.useTS = getAttrBooleanValue(elTrailingStop, "use", false);
        instance.config.generate.useTSFixedPips = getAttrBooleanValue(elTrailingStop, "useFixedPips", false);
        instance.config.generate.tsFixedPipsMin = getAttrIntValue(elTrailingStop, "minPips", 30);
        instance.config.generate.tsFixedPipsMax = getAttrIntValue(elTrailingStop, "maxPips", 80);
        instance.config.generate.useTSATR = getAttrBooleanValue(elTrailingStop, "useATR", false);
        instance.config.generate.tsATRMutliplierMin = getAttrFloatValue(elTrailingStop, "minMultiplier", 1.5);
        instance.config.generate.tsATRMutliplierMax = getAttrFloatValue(elTrailingStop, "maxMultiplier", 3);
        instance.config.generate.tsATRPeriodMin = getAttrIntValue(elTrailingStop, "minATRPeriod", 20);
        instance.config.generate.tsATRPeriodMax = getAttrIntValue(elTrailingStop, "maxATRPeriod", 20);

        var elNone = getChildElement(elTypes, "None");
        instance.config.generate.useNone = getAttrBooleanValue(elNone, "use", false);
        instance.config.generate.useNoneLimit = getAttrBooleanValue(elNone, "useLimit", false);
        instance.config.generate.noneMinBars = getAttrIntValue(elNone, "minBars", 50);
        instance.config.generate.noneMaxBars = getAttrIntValue(elNone, "maxBars", 50);

        var elScenarios = getChildElement(elGenerateConfig, "Scenarios");
        var elTwoExits = getChildElement(elScenarios, "TwoExits");

        instance.config.generate.exits_5050 = getAttrBooleanValue(elTwoExits, "exit5050", false);
        instance.config.generate.exits_3366 = getAttrBooleanValue(elTwoExits, "exit3366", false);
        instance.config.generate.exits_6633 = getAttrBooleanValue(elTwoExits, "exit6633", false);

        var elThreeExits = getChildElement(elScenarios, "ThreeExits");

        instance.config.generate.exits_333333 = getAttrBooleanValue(elThreeExits, "exit333333", false);
        instance.config.generate.exits_502525 = getAttrBooleanValue(elThreeExits, "exit502525", false);
        instance.config.generate.exits_255025 = getAttrBooleanValue(elThreeExits, "exit255025", false);
        instance.config.generate.exits_252550 = getAttrBooleanValue(elThreeExits, "exit252550", false);
    }
    
    function loadPositionSizeParams(elPositionSizes, exitObj){
        let positionSizeElems = getChildElements(elPositionSizes, "PositionSize");

        //load defaults
        exitObj.positionSizeType = instance.atmPositionSizes.percents;
        exitObj.positionPercents = 50;
        exitObj.fixedLots = 0.01;

        for(let i=0; i<positionSizeElems.length; i++){
            let elPositionSize = positionSizeElems[i];
            let psKey = getAttrStringValue(elPositionSize, "key", null);
            if(psKey == null) continue;

            if(getAttrBooleanValue(elPositionSize, "use", false)){
                exitObj.positionSizeType = psKey;
            }

            switch(psKey){
                case instance.atmPositionSizes.percents:
                    exitObj.positionPercents = getNodeIntValue(getChildByAttribute(elPositionSize, "Param", "key", "Percents"), null, exitObj.positionPercents);
                    break;
                case instance.atmPositionSizes.fixedSize:
                    exitObj.fixedLots = getNodeFloatValue(getChildByAttribute(elPositionSize, "Param", "key", "Lots"), null, exitObj.fixedLots);
                    break;
                case instance.atmPositionSizes.allRemaining:
                    break;
                default:
                    console.error("Unknown position size key: '" + psKey + "'");
            }
        }
    }

    function savePositionSizes(elPositionSizes, exitObj, xmlDoc){
        for(let attr in instance.atmPositionSizes){
            var type = instance.atmPositionSizes[attr];
            var elPositionSize = createChild(elPositionSizes, "PositionSize", xmlDoc, true);
            elPositionSize.setAttribute("key", type);
            elPositionSize.setAttribute("use", type == exitObj.positionSizeType ? "true" : "false");

            switch(type){
                case instance.atmPositionSizes.percents:
                    addNode("Param", exitObj.positionPercents, elPositionSize, xmlDoc, true).setAttribute("key", "Percents");
                    break;
                case instance.atmPositionSizes.fixedSize:
                    addNode("Param", exitObj.fixedLots, elPositionSize, xmlDoc, true).setAttribute("key", "Lots");
                    break;
            }
        }
    }

    function loadExitLevelParams(elExitLevels, exitObj){
        let exitLevelElems = getChildElements(elExitLevels, "ExitLevel");

        //load defaults
        exitObj.exitLevel = instance.atmExitLevels.slBased;
        exitObj.slMultiplicator = 1;
        exitObj.ptMultiplicator = 1;
        exitObj.fixedProfitType = instance.atmTypes.typeFixedPips;
        exitObj.fixedProfitPips = 50;
        exitObj.fixedProfitATRMultiplicator = 1.2;
        exitObj.fixedProfitATRPeriod = 20;
        exitObj.trailingStopType = instance.atmTypes.typeFixedPips;
        exitObj.trailingStopPips = 50;
        exitObj.trailingStopATRMultiplicator = 1.2;
        exitObj.trailingStopATRPeriod = 20;
        exitObj.limitExitAfterBars = false;
        exitObj.maximumBars = 50;

        for(let i=0; i<exitLevelElems.length; i++){
            let elExitLevel = exitLevelElems[i];
            let elKey = getAttrStringValue(elExitLevel, "key", null);
            if(elKey == null) continue;

            if(getAttrBooleanValue(elExitLevel, "use", false)){
                exitObj.exitLevel = elKey;
            }

            switch(elKey){
                case instance.atmExitLevels.slBased:
                    exitObj.slMultiplicator = getNodeFloatValue(getChildByAttribute(elExitLevel, "Param", "key", "Multiplicator"), null, exitObj.slMultiplicator);
                    break;
                case instance.atmExitLevels.ptBased:
                    exitObj.ptMultiplicator = getNodeFloatValue(getChildByAttribute(elExitLevel, "Param", "key", "Multiplicator"), null, exitObj.ptMultiplicator);
                    break;
                case instance.atmExitLevels.fixedProfit:
                    exitObj.fixedProfitType = getNodeIntValue(getChildByAttribute(elExitLevel, "Param", "key", "Type"), null, exitObj.fixedProfitType);
                    exitObj.fixedProfitPips = getNodeIntValue(getChildByAttribute(elExitLevel, "Param", "key", "FixedPips"), null, exitObj.fixedProfitPips);
                    exitObj.fixedProfitATRMultiplicator = getNodeFloatValue(getChildByAttribute(elExitLevel, "Param", "key", "AtrMultiplicator"), null, exitObj.fixedProfitATRMultiplicator);
                    exitObj.fixedProfitATRPeriod = getNodeIntValue(getChildByAttribute(elExitLevel, "Param", "key", "AtrPeriod"), null, exitObj.fixedProfitATRPeriod);
                    break;
                case instance.atmExitLevels.trailingStop:
                    exitObj.trailingStopType = getNodeIntValue(getChildByAttribute(elExitLevel, "Param", "key", "Type"), null, exitObj.trailingStopType);
                    exitObj.trailingStopPips = getNodeFloatValue(getChildByAttribute(elExitLevel, "Param", "key", "FixedPips"), null, exitObj.trailingStopPips);
                    exitObj.trailingStopATRMultiplicator = getNodeFloatValue(getChildByAttribute(elExitLevel, "Param", "key", "AtrMultiplicator"), null, exitObj.trailingStopATRMultiplicator);
                    exitObj.trailingStopATRPeriod = getNodeIntValue(getChildByAttribute(elExitLevel, "Param", "key", "AtrPeriod"), null, exitObj.trailingStopATRPeriod);
                    break;
                case instance.atmExitLevels.none:
                    exitObj.limitExitAfterBars = getNodeBooleanValue(getChildByAttribute(elExitLevel, "Param", "key", "LimitExitAfterBars"), null, exitObj.limitExitAfterBars);
                    exitObj.maximumBars = getNodeIntValue(getChildByAttribute(elExitLevel, "Param", "key", "MaxBars"), null, exitObj.maximumBars);
                    break;
                default:
                    console.error("Unknown exit level key: '" + elKey + "'");
            }
        }
    }

    function saveExitLevels(elExitLevels, exitObj, xmlDoc){
        for(let attr in instance.atmExitLevels){
            var type = instance.atmExitLevels[attr];
            var elExitLevel = createChild(elExitLevels, "ExitLevel", xmlDoc, true);
            elExitLevel.setAttribute("key", type);
            elExitLevel.setAttribute("use", type == exitObj.exitLevel ? "true" : "false");

            switch(type){
                case instance.atmExitLevels.slBased:
                    addNode("Param", exitObj.slMultiplicator, elExitLevel, xmlDoc, true).setAttribute("key", "Multiplicator");
                    break;
                case instance.atmExitLevels.ptBased:
                    addNode("Param", exitObj.ptMultiplicator, elExitLevel, xmlDoc, true).setAttribute("key", "Multiplicator");
                    break;
                case instance.atmExitLevels.fixedProfit:
                    addNode("Param", exitObj.fixedProfitType, elExitLevel, xmlDoc, true).setAttribute("key", "Type");
                    addNode("Param", exitObj.fixedProfitPips, elExitLevel, xmlDoc, true).setAttribute("key", "FixedPips");
                    addNode("Param", exitObj.fixedProfitATRMultiplicator, elExitLevel, xmlDoc, true).setAttribute("key", "AtrMultiplicator");
                    addNode("Param", exitObj.fixedProfitATRPeriod, elExitLevel, xmlDoc, true).setAttribute("key", "AtrPeriod");
                    break;
                case instance.atmExitLevels.trailingStop:
                    addNode("Param", exitObj.trailingStopType, elExitLevel, xmlDoc, true).setAttribute("key", "Type");
                    addNode("Param", exitObj.trailingStopPips, elExitLevel, xmlDoc, true).setAttribute("key", "FixedPips");
                    addNode("Param", exitObj.trailingStopATRMultiplicator, elExitLevel, xmlDoc, true).setAttribute("key", "AtrMultiplicator");
                    addNode("Param", exitObj.trailingStopATRPeriod, elExitLevel, xmlDoc, true).setAttribute("key", "AtrPeriod");
                    break;
                case instance.atmExitLevels.none:
                    addNode("Param", exitObj.limitExitAfterBars, elExitLevel, xmlDoc, true).setAttribute("key", "LimitExitAfterBars");
                    addNode("Param", exitObj.maximumBars, elExitLevel, xmlDoc, true).setAttribute("key", "MaxBars");
                    break;
            }
        }
    }

    function loadMoveSL2BEParams(elMoveSL2BE, exitObj){
        //load defaults
        exitObj.sl2beType = instance.atmMoveSL2BETypes.fixedPips;
        exitObj.moveSL2BE = false;
        exitObj.moveSL2BEAddPips = false;
        exitObj.addPipsValue = 10;

        exitObj.sl2beType = getNodeIntValue(getChildByAttribute(elMoveSL2BE, "Param", "key", "Type"), null, exitObj.sl2beType);
        exitObj.moveSL2BE = getNodeBooleanValue(getChildByAttribute(elMoveSL2BE, "Param", "key", "MoveSL2BE"), null, exitObj.moveSL2BE);
        exitObj.moveSL2BEAddPips = getNodeBooleanValue(getChildByAttribute(elMoveSL2BE, "Param", "key", "MoveSL2BEAddPips"), null, exitObj.moveSL2BEAddPips);
        exitObj.addPipsValue = getNodeIntValue(getChildByAttribute(elMoveSL2BE, "Param", "key", "AddPipsValue"), null, exitObj.addPipsValue);
    }

    function saveMoveSL2BE(elMoveSL2BE, exitObj, xmlDoc){
        addNode("Param", exitObj.moveSL2BE, elMoveSL2BE, xmlDoc, true).setAttribute("key", "MoveSL2BE");
        addNode("Param", exitObj.moveSL2BEAddPips, elMoveSL2BE, xmlDoc, true).setAttribute("key", "MoveSL2BEAddPips");
        addNode("Param", exitObj.sl2beType, elMoveSL2BE, xmlDoc, true).setAttribute("key", "Type");
        addNode("Param", exitObj.addPipsValue, elMoveSL2BE, xmlDoc, true).setAttribute("key", "AddPipsValue");
    }

    this.getDescription = function(exitObj){
        var description = "Close ";

        switch(exitObj.positionSizeType){
            case instance.atmPositionSizes.percents:
                description += L.tsq("%d% of position").format([exitObj.positionPercents]) + " ";
                break;
            case instance.atmPositionSizes.fixedSize:
                description += L.tsq("%f lots").format([exitObj.fixedLots]) + " ";
                break;
            case instance.atmPositionSizes.allRemaining:
                description += L.tsq("remaining position") + " ";
                break;
            default:
                description += L.tsq("{unknown position size}") + " ";
        }

        switch(exitObj.exitLevel){
            case instance.atmExitLevels.slBased:
                description += L.tsq("at %f x Original Stop Loss size").format([exitObj.slMultiplicator]);
                break;
            case instance.atmExitLevels.ptBased:
                description += L.tsq("at %f x Original Profit Target size").format([exitObj.ptMultiplicator]);
                break;
            case instance.atmExitLevels.fixedProfit:
                if(exitObj.fixedProfitType == instance.atmTypes.typeFixedPips){
                    description += L.tsq("at %d pips above Order Open price").format([exitObj.fixedProfitPips]);
                }
                else {
                    description += L.tsq("at fixed profit %f * ATR(%d)").format([exitObj.fixedProfitATRMultiplicator, exitObj.fixedProfitATRPeriod]);
                }
                break;
            case instance.atmExitLevels.trailingStop:
                if(exitObj.trailingStopType == instance.atmTypes.typeFixedPips){
                    description += L.tsq("at trailing stop %d pips").format([exitObj.trailingStopPips]);
                }
                else {
                    description += L.tsq("at trailing stop %f * ATR(%d)").format([exitObj.trailingStopATRMultiplicator, exitObj.trailingStopATRPeriod]);
                }
                break;
            case instance.atmExitLevels.none:
                description += L.tsq("by Exit rule %s").format([exitObj.limitExitAfterBars ? L.tsq("- limit to max. %d bars" ).format([exitObj.maximumBars]) : ""]);
                break;
            default:
                description += L.tsq("at {unknown level}");
        }

        if(exitObj.moveSL2BE){
            description += "<br> + " + L.tsq("Move SL 2 BE");

            if(exitObj.moveSL2BEAddPips){
                switch(exitObj.sl2beType){
                    case instance.atmMoveSL2BETypes.fixedPips:
                        description += " + " + exitObj.addPipsValue + " " + L.tsq("pips");
                        break;
                    default:
                        description += " + " + L.tsq("{unknown size}");
                }
                
            }
        }

        return description;
    }

    this.saveConfig = function(){
        var xmlDoc = AppService.xmlDoc;

        var settingsElem = AppService.getTaskConfig();
        if(!settingsElem) return;

        var elATMs = getChildElement(settingsElem, 'ATMs', true);
        if (elATMs) {
            removeAllChildren(elATMs);
        }
        else {
            elATMs = createChild(settingsElem, 'ATMs', xmlDoc, true);
        }

        elATMs.setAttribute("enable", instance.config.enableATM ? "true" : "false");
        elATMs.setAttribute("scaleOutType", instance.config.atmType);
        elATMs.setAttribute("sizeDecimals", instance.config.sizeDecimals);
        elATMs.setAttribute("minSize", instance.config.minSize);

        var elATM = createChild(elATMs, "ATM", xmlDoc);
        elATM.setAttribute("id", "global");

        var elExits = createChild(elATM, "Exits", xmlDoc);
        
        for(var i=0; i<instance.config.exits.length; i++){
            var exitObj = instance.config.exits[i];

            var elExit = createChild(elExits, "Exit", xmlDoc, true);

            var elPositionSizes = createChild(elExit, "PositionSizes", xmlDoc);
            savePositionSizes(elPositionSizes, exitObj, xmlDoc);

            var elExitLevels = createChild(elExit, "ExitLevels", xmlDoc);
            saveExitLevels(elExitLevels, exitObj, xmlDoc);

            var elMoveSL2BE = createChild(elExit, "MoveSL2BE", xmlDoc);
            saveMoveSL2BE(elMoveSL2BE, exitObj, xmlDoc);
        }
        
        var elGenerateConfig = createChild(elATMs, "GenerateConfig", xmlDoc);
        var elTypes = createChild(elGenerateConfig, "Types", xmlDoc);

        var elSLMultiple = createChild(elTypes, "SLMultiple", xmlDoc);
        elSLMultiple.setAttribute("use", instance.config.generate.useSLMultiple);
        elSLMultiple.setAttribute("minMultiplier", instance.config.generate.slMultipleMin);
        elSLMultiple.setAttribute("maxMultiplier", instance.config.generate.slMultipleMax);

        var elPTMultiple = createChild(elTypes, "PTMultiple", xmlDoc);
        elPTMultiple.setAttribute("use", instance.config.generate.usePTMultiple);
        elPTMultiple.setAttribute("minMultiplier", instance.config.generate.ptMultipleMin);
        elPTMultiple.setAttribute("maxMultiplier", instance.config.generate.ptMultipleMax);
        
        var elFixedProfit = createChild(elTypes, "FixedProfit", xmlDoc);
        elFixedProfit.setAttribute("use", instance.config.generate.useFixedProfit);
        elFixedProfit.setAttribute("useFixedPips", instance.config.generate.useFixedProfitFixedPips);
        elFixedProfit.setAttribute("minPips", instance.config.generate.fixedProfitFixedPipsMin);
        elFixedProfit.setAttribute("maxPips", instance.config.generate.fixedProfitFixedPipsMax);
        elFixedProfit.setAttribute("useATR", instance.config.generate.useFixedProfitATR);
        elFixedProfit.setAttribute("minMultiplier", instance.config.generate.fixedProfitATRMutliplierMin);
        elFixedProfit.setAttribute("maxMultiplier", instance.config.generate.fixedProfitATRMutliplierMax);
        elFixedProfit.setAttribute("minATRPeriod", instance.config.generate.fixedProfitATRPeriodMin);
        elFixedProfit.setAttribute("maxATRPeriod", instance.config.generate.fixedProfitATRPeriodMax);

        var elTrailingStop = createChild(elTypes, "TrailingStop", xmlDoc);
        elTrailingStop.setAttribute("use", instance.config.generate.useTS);
        elTrailingStop.setAttribute("useFixedPips", instance.config.generate.useTSFixedPips);
        elTrailingStop.setAttribute("minPips", instance.config.generate.tsFixedPipsMin);
        elTrailingStop.setAttribute("maxPips", instance.config.generate.tsFixedPipsMax);
        elTrailingStop.setAttribute("useATR", instance.config.generate.useTSATR);
        elTrailingStop.setAttribute("minMultiplier", instance.config.generate.tsATRMutliplierMin);
        elTrailingStop.setAttribute("maxMultiplier", instance.config.generate.tsATRMutliplierMax);
        elTrailingStop.setAttribute("minATRPeriod", instance.config.generate.tsATRPeriodMin);
        elTrailingStop.setAttribute("maxATRPeriod", instance.config.generate.tsATRPeriodMax);
        
        var elNone = createChild(elTypes, "None", xmlDoc);
        elNone.setAttribute("use", instance.config.generate.useNone);
        elNone.setAttribute("useLimit", instance.config.generate.useNoneLimit);
        elNone.setAttribute("minBars", instance.config.generate.noneMinBars);
        elNone.setAttribute("maxBars", instance.config.generate.noneMaxBars);

        var elScenarios = createChild(elGenerateConfig, "Scenarios", xmlDoc);
        var elTwoExits = createChild(elScenarios, "TwoExits", xmlDoc);

        elTwoExits.setAttribute("exit5050", instance.config.generate.exits_5050);
        elTwoExits.setAttribute("exit3366", instance.config.generate.exits_3366);
        elTwoExits.setAttribute("exit6633", instance.config.generate.exits_6633);
        
        var elThreeExits = createChild(elScenarios, "ThreeExits", xmlDoc);

        elThreeExits.setAttribute("exit333333", instance.config.generate.exits_333333);
        elThreeExits.setAttribute("exit502525", instance.config.generate.exits_502525);
        elThreeExits.setAttribute("exit255025", instance.config.generate.exits_255025);
        elThreeExits.setAttribute("exit252550", instance.config.generate.exits_252550);
    }

    var instance = this;

    this.atmTypes = SQConstants.getConstants().atmTypes;
    this.atmPositionSizes = SQConstants.getConstants().atmPositionSizes;
    this.atmExitLevels = SQConstants.getConstants().atmExitLevels;
    this.atmMoveSL2BETypes = SQConstants.getConstants().atmMoveSL2BETypes;
    this.atmMoveSL2BEOptions = [
        { name : "Fixed Pips", value : SQConstants.getConstants().atmMoveSL2BETypes.fixedPips }
    ];

    this.config = {
        enableATM: false,
        atmType: this.atmTypes.fromStrategy,
        sizeDecimals: 1,
        minSize: 0.1,
        exits: [],
        generate: {
            useSLMultiple: false,
            slMultipleMin: 0.5,
            slMultipleMax: 2,
            usePTMultiple: false,
            ptMultipleMin: 0.5,
            ptMultipleMax: 2,
            useFixedProfit: false,
            useFixedProfitFixedPips: false,
            fixedProfitFixedPipsMin: 30,
            fixedProfitFixedPipsMax: 80,
            useFixedProfitATR: false,
            fixedProfitATRMutliplierMin: 1.5,
            fixedProfitATRMutliplierMax: 3,
            fixedProfitATRPeriodMin: 20,
            fixedProfitATRPeriodMax: 20,
            useTS: false,
            useTSFixedPips: false,
            tsFixedPipsMin: 30,
            tsFixedPipsMax: 80,
            useTSATR: false,
            tsATRMutliplierMin: 1.5,
            tsATRMutliplierMax: 3,
            tsATRPeriodMin: 20,
            tsATRPeriodMax: 20,
            useNone: false,
            useNoneLimit: false,
            noneMinBars: 50,
            noneMaxBars: 50,
            exits_5050: true,
            exits_3366: true,
            exits_6633: true,
            exits_333333: true,
            exits_502525: true,
            exits_255025: true,
            exits_252550: true
        }
    }

});