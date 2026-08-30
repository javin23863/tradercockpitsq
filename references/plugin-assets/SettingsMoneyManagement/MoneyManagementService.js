angular.module('app.settings').service('MoneyManagementService', function ($rootScope, $q, BackendService, AppService, SQConstants, L) {

    this.init = function(){
        var rmElem = AppService.getCurrentTaskTabSettings("RiskMoneyManagement");
        instance.config.useCustomSettings = getAttrBooleanValue(rmElem, "customSettings", false);
    }

    this.getMMSettings = function (type) {
        var mmElem = getSettingsElem("MoneyManagement");

        instance.config.initialCapital = getNodeFloatValue(mmElem, 'InitialCapital', instance.config.initialCapital);

        var methods = parseSettingsMethods(mmElem);
        var targetMethod = methods.length > 0 ? methods[0] : null;

        for (var i = 0; i < methods.length; i++) {
            var method = methods[i];

            if ((type && method.method == type) || (!type && method.use)) {
                targetMethod = method;
                break;
            }
        }

        var method = targetMethod ? targetMethod.method : null;
        var params = targetMethod ? targetMethod.params : null;

        return {
            method: method,
            initialCapital: instance.config.initialCapital,
            params: params
        };
    }

    this.getRMSettings = function (type) {
        var rmElem = getSettingsElem("RiskManagement");

        var methods = parseSettingsMethods(rmElem);
        var targetMethod = methods.length > 0 ? methods[0] : null;

        for (var i = 0; i < methods.length; i++) {
            var method = methods[i];

            if ((type && method.method == type) || (!type && method.use)) {
                targetMethod = method;
                break;
            }
        }

        return targetMethod;
    }

    function getSettingsElem(name) {
        var rmElem = AppService.getCurrentTaskTabSettings("RiskMoneyManagement");
        return getChildElement(rmElem, name);
    }

    this.getDefaultMMConfig = function (className) {
        for (var i = 0; i < instance.mmMethods.length; i++) {
            var mmMethod = instance.mmMethods[i];
            if (mmMethod.class == className) {
                return mmMethod.config;
            }
        }
    }

    this.getDefaultRMConfig = function (className) {
        for (var i = 0; i < instance.rmMethods.length; i++) {
            var rmMethod = instance.rmMethods[i];
            if (rmMethod.class == className) {
                return rmMethod.config;
            }
        }
    }

    this.getDescription = function (elMoneyManagement) {
        return getMMDescription(elMoneyManagement, instance.mmMethods, L);
    }

    this.saveConfig = function (dontSaveMethods) {
        var xmlDoc = AppService.xmlDoc;

        var settingsObj = AppService.getTaskConfig();
        if (!settingsObj) return;

        var rmmObj = getChildElement(settingsObj, 'RiskMoneyManagement');
        if (!rmmObj) {
            var rmmElem = xmlDoc.createElement('RiskMoneyManagement');
            rmmObj = settingsObj.appendChild(rmmElem);
        }

        rmmObj.setAttribute("customSettings", instance.config.useCustomSettings);   //applies only for AutomaticRetest task

        var mmObj = createMMSettings(xmlDoc, rmmObj, dontSaveMethods);
        rmmObj.appendChild(mmObj);

        var rmObj = createRMSettings(xmlDoc, rmmObj, dontSaveMethods);
        rmmObj.appendChild(rmObj);
    }

    function createMMSettings(xmlDoc, rmmObj, dontSaveMethods) {
        var mmObj = getChildElements(rmmObj, 'MoneyManagement')[0];
        if (!mmObj) {
            var mmElem = xmlDoc.createElement('MoneyManagement');
            mmObj = rmmObj.appendChild(mmElem);
        }

        //save initial capital
        var capitalObj = getChildElements(mmObj, 'InitialCapital')[0];

        if (!capitalObj) {
            var capitalElem = xmlDoc.createElement('InitialCapital');
            capitalObj = mmObj.appendChild(capitalElem);
        }

        removeAllChildren(capitalObj);
        capitalObj.appendChild(xmlDoc.createTextNode(instance.config.initialCapital));

        if (dontSaveMethods) return mmObj;

        var type = instance.config.mmPropGridInstance.getSelectedType();
        var methodObjs = getChildElements(mmObj, 'Method');
        var targetMethodObj;

        for (var i = 0; i < methodObjs.length; i++) {
            var methodObj = methodObjs[i];
            var methodType = getAttrStringValue(methodObj, 'type');

            if (methodType == type) {
                targetMethodObj = methodObj;
            }
            methodObj.setAttribute("use", 'false');
        }

        if (!targetMethodObj) {
            var methodElem = xmlDoc.createElement('Method');
            targetMethodObj = mmObj.appendChild(methodElem);
            targetMethodObj.setAttribute("type", type);
        }

        targetMethodObj.setAttribute("use", 'true');
        instance.config.mmPropGridInstance.setPropertyValues(xmlDoc, targetMethodObj);

        return mmObj;
    }

    function createRMSettings(xmlDoc, rmmObj, dontSaveMethods) {
        var rmObj = getChildElements(rmmObj, 'RiskManagement')[0];
        if (!rmObj) {
            var rmElem = xmlDoc.createElement('RiskManagement');
            rmObj = rmmObj.appendChild(rmElem);
        }

        if (dontSaveMethods) return rmObj;

        var type = instance.config.rmPropGridInstance.getSelectedType();
        var methodObjs = getChildElements(rmObj, 'Method');
        var targetMethodObj;

        for (var i = 0; i < methodObjs.length; i++) {
            var methodObj = methodObjs[i];
            var methodType = getAttrStringValue(methodObj, 'type');

            if (methodType == type) {
                targetMethodObj = methodObj;
                break;
            }
            methodObj.setAttribute("use", 'false');
        }

        if (!targetMethodObj) {
            var methodElem = xmlDoc.createElement('Method');
            targetMethodObj = rmObj.appendChild(methodElem);
            targetMethodObj.setAttribute("type", type);
        }

        targetMethodObj.setAttribute("use", 'true');
        instance.config.rmPropGridInstance.setPropertyValues(xmlDoc, targetMethodObj);

        return rmObj;
    }

    var instance = this;

    this.mmMethods = SQConstants.getConstants().mmMethods;
    this.rmMethods = SQConstants.getConstants().rmMethods;

    this.config = {
        initialCapital: 10000,
        mmPropGridInstance: {},
        rmPropGridInstance: {},
        useCustomSettings: false
    }

});