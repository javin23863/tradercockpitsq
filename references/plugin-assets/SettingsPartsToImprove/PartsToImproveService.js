angular.module('app.settings').service('PartsToImproveService', function ($rootScope, $q, BackendService, AppService, WhatToBuildService, SQEvents, SQConstants, L) {

    this.init = function () {
        loadConfig();
    }

    function loadConfig() {
        var xmlObj = AppService.xmlConfig;
        var xmlDoc = AppService.xmlDoc;

        var settingsElem = AppService.getTaskConfig();
        var partsToImproveElem = getChildElement(settingsElem, 'PartsToImprove', true);

        if (partsToImproveElem) {
            var entryRulesElem = getChildElement(partsToImproveElem, 'EntryRules', true);
            if (entryRulesElem) {
                loadRulesConfig(entryRulesElem, instance.config.entryRules);
            }

            var orderTypesElem = getChildElement(partsToImproveElem, 'OrderTypes', true);
            if (orderTypesElem) {
                loadRulesConfig(orderTypesElem, instance.config.orderTypes);
            }

            var exitRulesElem = getChildElement(partsToImproveElem, 'ExitRules', true);
            if (exitRulesElem) {
                loadRulesConfig(exitRulesElem, instance.config.exitRules);
            }
        }

        if($rootScope.license.isUltimateVersion){
            instance.config.improveATM = getAttrBooleanValue(partsToImproveElem, "improveATM", false);
        }
        else {
            instance.config.improveATM = false;
        }
    }

    function loadRulesConfig(parentElem, targetObj) {
        targetObj.symmetry = getAttrBooleanValue(parentElem, 'symmetry', true);

        loadImprovementConfig(parentElem, 'LongImprovement', targetObj.long);

        if (targetObj.symmetry) {
            targetObj.short = angular.copy(targetObj.long);
        }
        else {
            loadImprovementConfig(parentElem, 'ShortImprovement', targetObj.short);
        }
    }

    function loadImprovementConfig(parentElem, improvementElemName, targetObj) {
        var improvementElem = getChildElement(parentElem, improvementElemName, true);
        if (improvementElem) {
            targetObj.use = getAttrBooleanValue(improvementElem, 'use', true);
            targetObj.action = getAttrStringValue(improvementElem, 'action', instance.actionTypes.addOrReplace);
        }
    }

    this.getConditionText = function (condition) {
        if (condition.use) {
            if (condition.action) {
                switch (condition.action) {
                    case instance.actionTypes.addOrReplace: return L.tsq('Keep existing or generate randomly');
                    case instance.actionTypes.add: return L.tsq('Keep existing + generate randomly');
                    default: return L.tsq('Generate randomly');
                }
            }
            else {
                return L.tsq('Generate randomly');
            }
        }
        else {
            return L.tsq('Keep existing');
        }
    }

    this.updateMarketSides = function () {
        if (window.appConfig.product == 'OPTIMIZER' || window.appConfig.task.type == 'Optimize') {
            instance.config.entryRules.symmetry = false;
            instance.config.exitRules.symmetry = false;
            return;
        }

        var marketSides = {};
        var settingsElem = AppService.getTaskConfig();

        WhatToBuildService.loadMarketSides(marketSides, settingsElem);
        instance.config.entryRules.symmetry = marketSides.entrySymmetry;
        instance.config.exitRules.symmetry = marketSides.exitSymmetry;
        instance.config.direction = marketSides.type;
        
        if (instance.config.entryRules.symmetry) {
            instance.config.entryRules.short = angular.copy(instance.config.entryRules.long);
        }
        if (instance.config.exitRules.symmetry) {
            instance.config.exitRules.short = angular.copy(instance.config.exitRules.long);
        }
    }

    this.saveConfig = function () {
        var xmlDoc = AppService.xmlDoc;
        
        var settingsElem = AppService.getTaskConfig();
        if(!settingsElem) return;

        var partsToImproveElem = getChildElement(settingsElem, 'PartsToImprove', true);
        if (partsToImproveElem) {
            removeAllChildren(partsToImproveElem);
        }
        else {
            partsToImproveElem = createChild(settingsElem, 'PartsToImprove', xmlDoc, true);
        }

        partsToImproveElem.setAttribute("improveATM", instance.config.improveATM);

        //entry rules settings
        saveRulesConfig(partsToImproveElem, 'EntryRules', instance.config.entryRules, xmlDoc);

        //order types settings
        var orderTypesElem = createChild(partsToImproveElem, 'OrderTypes', xmlDoc, true);

        var orderTypesLongImprovementElem = createChild(orderTypesElem, 'LongImprovement', xmlDoc, true);
        orderTypesLongImprovementElem.setAttribute("use", instance.config.orderTypes.long.use);

        var orderTypesShortImprovementElem = createChild(orderTypesElem, 'ShortImprovement', xmlDoc, true);
        orderTypesShortImprovementElem.setAttribute("use", instance.config.orderTypes.short.use);

        //exit rules settings
        saveRulesConfig(partsToImproveElem, 'ExitRules', instance.config.exitRules, xmlDoc);
    }

    function saveRulesConfig(parentElem, elementName, jsonObj, xmlDoc) {
        var rulesElem = createChild(parentElem, elementName, xmlDoc, true);
        rulesElem.setAttribute("symmetry", jsonObj.symmetry);

        saveImprovementConfig(rulesElem, 'LongImprovement', jsonObj.long, xmlDoc);
        saveImprovementConfig(rulesElem, 'ShortImprovement', jsonObj.short, xmlDoc);
    }

    function saveImprovementConfig(parentElem, elementName, jsonObj, xmlDoc) {
        var improvementElem = createChild(parentElem, elementName, xmlDoc, true);
        improvementElem.setAttribute("use", jsonObj.use);
        improvementElem.setAttribute("action", jsonObj.action);
    }

    var instance = this;

    this.actionTypes = SQConstants.getConstants().improveActionTypes;
    this.actions = [
        { name: L.tsq('Add or replace'), value: instance.actionTypes.addOrReplace },
        { name: L.tsq('Replace'), value: instance.actionTypes.replace },
        { name: L.tsq('Add'), value: instance.actionTypes.add },
    ];

    this.directions = SQConstants.getConstants().buildStrategyDirections;

    this.config = {
        direction: this.directions.both,
        entryRules: {
            symmetry: true,
            long: {
                use: true,
                action: this.actionTypes.addOrReplace,
            },
            short: {
                use: true,
                action: this.actionTypes.addOrReplace
            }
        },
        orderTypes: {
            long: {
                use: true
            },
            short: {
                use: true
            }
        },
        exitRules: {
            symmetry: true,
            long: {
                use: true,
                action: this.actionTypes.addOrReplace,
            },
            short: {
                use: true,
                action: this.actionTypes.addOrReplace
            }
        },
        improveATM: false
    };

});