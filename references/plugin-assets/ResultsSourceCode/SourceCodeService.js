angular.module('app.resultstabs.sourcecode').service('SourceCodeService', function ($rootScope, BackendService, SQConstants, SQResultsData, L) {

    this.init = function (data) {
        //load generator types -------------------------------------
        loadToArray(instance.generators, data.engineTypes);

        if (data.engineTypes.length > 0 && !instance.config.type) {
            instance.config.type = data.engineTypes[0].name;
        }

        var broker = SQConstants.getConstants().broker;
        if (broker) {
            for (var i = instance.generators.length - 1; i >= 0; i--) {
                var generator = instance.generators[i];

                if (generator.name.indexOf("Pseudo") >= 0) {
                    instance.config.type = generator.name;
                }
                else if (broker.encryption) {
                    instance.generators.splice(i, 1);
                }
            }
        }

        instance.loadParamsConfig();

        //load MM types -------------------------------------
        loadToArray(instance.mmTypes, data.mmTypes);

        if (data.mmTypes.length > 0) {
            instance.config.mmType = data.mmTypes[0].value;
        }
    }

    this.loadParamsConfig = function () {
        try {
            var generator = getItem(instance.generators, 'name', instance.config.type);
            var config = JSON.parse(SQConstants.getSettings()["SourceCodeParams" + generator.key]);

            instance.config.periodParams = config.periodParams;
            instance.config.constantsParams = config.constantsParams;
            instance.config.shiftParams = config.shiftParams;
            instance.config.otherParams = config.otherParams;
            instance.config.entryParams = config.entryParams;
            instance.config.entryLogic = config.entryLogic;
            instance.config.exitParamsUsed = config.exitParamsUsed;
            instance.config.exitParamsUnused = config.exitParamsUnused;
            instance.config.booleanParams = config.booleanParams;
            instance.config.symmetricVariables = config.symmetricVariables;
            instance.config.recommendedParams = config.recommendedParams;

            if (typeof config.parametrizeType != 'undefined') {
                instance.config.parametrizeType = config.parametrizeType;
            } else {
                if (instance.config.recommendedParams) {
                    instance.config.parametrizeType = 0;
                } else if (!instance.config.periodParams && !instance.config.constantsParams && !instance.config.shiftParams && !instance.config.otherParams &&
                    !instance.config.entryParams && !instance.config.entryLogic &&!instance.config.exitParamsUsed && !instance.config.exitParamsUnused && !instance.config.booleanParams) {
                    instance.config.parametrizeType = 2;
                } else {
                    instance.config.parametrizeType = 1;
                }
            }
        } catch (err) { }
    }

    this.setParamsConfig = function () {
        try {
            var generator = getItem(instance.generators, 'name', instance.config.type);
            SQConstants.getSettings()["SourceCodeParams" + generator.key] = JSON.stringify(instance.config);
        } catch (err) { }
    }

    this.print = function (params, callOnSuccess) {
        var data = {
            project: params.projectName,
            databank: params.databankName,
            strategy: params.strategyName,
            fileName: params.fileName,
            type: instance.config.type,
            useVariables: instance.config.useVariables,

            symmetricVariables: instance.config.parametrizeType == 2 ? false : instance.config.symmetricVariables,
            periodParams: instance.config.parametrizeType == 2 ? false : instance.config.periodParams,
            shiftParams: instance.config.parametrizeType == 2 ? false : instance.config.shiftParams,
            exitParamsUsed: instance.config.parametrizeType == 2 ? false : instance.config.exitParamsUsed,
            constantsParams: instance.config.parametrizeType == 2 ? false : instance.config.constantsParams,
            otherParams: instance.config.parametrizeType == 2 ? false : instance.config.otherParams,
            entryParams: instance.config.parametrizeType == 2 ? false : instance.config.entryParams,
            entryLogic: instance.config.parametrizeType == 2 ? false : instance.config.entryLogic,
            exitParamsUnused: instance.config.parametrizeType == 2 ? false : instance.config.exitParamsUnused,
            booleanParams: instance.config.parametrizeType == 2 ? false : instance.config.booleanParams,
            recommendedParams: instance.config.parametrizeType == 2 ? false : instance.config.parametrizeType == 0,

            mmType: instance.config.mmType,
            strategyXML: instance.isAlgoWizardApp() ? instance.strategyXML : null         //used by AlgoWizard to print source code of strategy that has not been backtested yet
        }

        if (params.isAlgoWizardCloud || isAlgoWizard) {
            data.backtestID = params.backtestID;
            data.isAlgoWizardCloud = params.isAlgoWizardCloud;
            data.nodeID = params.nodeID;
        }

        BackendService.sendRequest('sourcecode/print', data, callOnSuccess, 'POST');
    }

    this.getDataPath = function (installPath, isMT4, callback) {
        BackendService.sendRequest('sourcecode/getDataPath', { installPath: installPath, isMT4: isMT4 }, function (result) {
            callback(result.dataPath);
        }, 'POST');
    }

    this.saveMTPaths = function (brokerObj, callback) {
        var args = {
            mt4InstallPath: brokerObj.mt4Path,
            mt5InstallPath: brokerObj.mt5Path,
            mt4DataPath: brokerObj.mt4DataPath,
            mt5DataPath: brokerObj.mt5DataPath
        };

        BackendService.sendRequest('sourcecode/saveMTPaths', args, callback, 'POST');
    }

    this.saveEA = function (mt4) {
        var dataInfo = SQResultsData.getDataInfo();
        var args = {
            project: dataInfo.projectName,
            databank: dataInfo.databankName,
            strategy: dataInfo.fileName? dataInfo.fileName : dataInfo.strategyName,
            isMT4: mt4,
            useVariables: instance.config.useVariables,

            symmetricVariables: instance.config.parametrizeType == 2 ? false : instance.config.symmetricVariables,
            periodParams: instance.config.parametrizeType == 2 ? false : instance.config.periodParams,
            shiftParams: instance.config.parametrizeType == 2 ? false : instance.config.shiftParams,
            exitParamsUsed: instance.config.parametrizeType == 2 ? false : instance.config.exitParamsUsed,
            constantsParams: instance.config.parametrizeType == 2 ? false : instance.config.constantsParams,
            otherParams: instance.config.parametrizeType == 2 ? false : instance.config.otherParams,
            entryParams: instance.config.parametrizeType == 2 ? false : instance.config.entryParams,
            entryLogic: instance.config.parametrizeType == 2 ? false : instance.config.entryLogic,
            exitParamsUnused: instance.config.parametrizeType == 2 ? false : instance.config.exitParamsUnused,
            booleanParams: instance.config.parametrizeType == 2 ? false : instance.config.booleanParams,
            recommendedParams: instance.config.parametrizeType == 2 ? false : instance.config.parametrizeType == 0,

            mmType: instance.config.mmType,
            strategyXML: instance.isAlgoWizardApp() ? instance.strategyXML : null
        };

        if (dataInfo.isAlgoWizardCloud || dataInfo.isAlgoWizard) args.backtestID = dataInfo.backtestID;

        BackendService.sendRequest('sourcecode/saveEA', args, function () {
            $rootScope.showSuccess(L.tsq("EA saved"));
        }, 'POST');
    }

    this.saveWFCalendar = function (filePath) {
        var dataInfo = SQResultsData.getDataInfo();
        var args = {
            project: dataInfo.projectName,
            databank: dataInfo.databankName,
            strategy: dataInfo.fileName? dataInfo.fileName : dataInfo.strategyName,
            useVariables: instance.config.useVariables,

            symmetricVariables: instance.config.parametrizeType == 2 ? false : instance.config.symmetricVariables,
            periodParams: instance.config.parametrizeType == 2 ? false : instance.config.periodParams,
            shiftParams: instance.config.parametrizeType == 2 ? false : instance.config.shiftParams,
            exitParamsUsed: instance.config.parametrizeType == 2 ? false : instance.config.exitParamsUsed,
            constantsParams: instance.config.parametrizeType == 2 ? false : instance.config.constantsParams,
            otherParams: instance.config.parametrizeType == 2 ? false : instance.config.otherParams,
            entryParams: instance.config.parametrizeType == 2 ? false : instance.config.entryParams,
            entryLogic: instance.config.parametrizeType == 2 ? false : instance.config.entryLogic,
            exitParamsUnused: instance.config.parametrizeType == 2 ? false : instance.config.exitParamsUnused,
            booleanParams: instance.config.parametrizeType == 2 ? false : instance.config.booleanParams,
            recommendedParams: instance.config.parametrizeType == 2 ? false : instance.config.parametrizeType == 0,

            mmType: instance.config.mmType,
            strategyXML: instance.isAlgoWizardApp() ? instance.strategyXML : null,
            filePath: filePath
        };

        if (dataInfo.isAlgoWizardCloud || dataInfo.isAlgoWizard) args.backtestID = dataInfo.backtestID;

        BackendService.sendRequest('sourcecode/saveWFCalendar', args, function () {
            $rootScope.showSuccess(L.tsq("WF calendar saved"));
        }, 'POST');
    }

    this.getGenerator = function () {
        for (var i = 0; i < instance.generators.length; i++) {
            if (instance.generators[i].name == instance.config.type) {
                return instance.generators[i];
            }
        }
    }

    this.isAlgoWizardApp = function () {
        return isAlgoWizard() || window.parent.activeApp == 'AlgoWizard';
    }

    var instance = this;

    this.generators = [];
    this.mmTypes = [];

    this.config = {
        type: isAlgoWizardCloud() ? 'Pseudo Code(*.TXT)' : (SQConstants.getSettings().sourceCodeType || 'Strategy XML'),
        useVariables: SQConstants.getSettings().sourceCodeVariables == "true",

        periodParams: true,
        constantsParams: false,
        shiftParams: false,
        otherParams: false,
        entryParams: false,
        entryLogic: false,
        exitParamsUsed: true,
        exitParamsUnused: false,
        booleanParams: false,
        symmetricVariables: true,
        recommendedParams: true,

        mmType: null,
        parametrizeType: 0,
    }

    this.strategyXML = null;
    this.settingsXML = null;

});