angular.module('app.resultstabs.resultsplugins').service('ResultsPluginsService', function(BackendService) {

    // Map short format keys (as on Source Code tab) to backend type names for sourcecode/print
    var SOURCE_CODE_FORMAT_TO_TYPE = {
        'xml': 'Strategy XML',
        'pseudo': 'Pseudo Code(*.TXT)',
        'mq4': 'MetaTrader 4 (*.mq4)',
        'mq5': 'MetaTrader 5 (*.mq5)',
        'el': 'EasyLanguage for Tradestation (*.el)',
        'pla': 'EasyLanguage for MultiCharts (*.pla)',
        'java': 'JForex (*.java)'
    };

    this.listPlugins = function(callback) {
        BackendService.sendRequest('resultsPlugins/list', null, callback);
    };

    this.createPlugin = function(pluginName, callback) {
        BackendService.sendRequest('resultsPlugins/create', { pluginName: pluginName }, callback, 'POST');
    };

    this.renamePlugin = function(pluginName, newName, callback) {
        BackendService.sendRequest('resultsPlugins/rename', { pluginName: pluginName, newName: newName }, callback, 'POST');
    };

    this.deletePlugin = function(pluginName, callback) {
        BackendService.sendRequest('resultsPlugins/delete', { pluginName: pluginName }, callback, 'POST');
    };

    this.getStats = function(params, callback) {
        BackendService.sendRequest('resultsPlugins/stats', params, callback);
    }

    this.getOrders = function(params, callback) {
        BackendService.sendRequest('resultsPlugins/orders', params, callback);
    }

    this.getLastSettingsXml = function(params, callback) {
        BackendService.sendRequest('resultsPlugins/settings', params, callback);
    }

    /**
     * Request source code for the current strategy in the given format.
     * Params: projectName, databankName, strategyName (auto-merged by PluginIframeCtrl),
     * and either format (e.g. 'xml', 'mq4', 'mq5', 'el', 'pseudo') or type (full name e.g. 'Strategy XML').
     * Calls backend sourcecode/print; callback receives code.
     */
    this.getSourceCode = function(params, callback) {
        var type = params.type;
        if (!type && params.format) {
            type = SOURCE_CODE_FORMAT_TO_TYPE[params.format] || params.format;
        }
        if (!type) {
            type = SOURCE_CODE_FORMAT_TO_TYPE['pseudo'];
        }
        var data = {
            project: params.projectName,
            databank: params.databankName,
            strategy: params.strategyName || params.fileName,
            type: type,
            useVariables: params.useVariables !== false,
            symmetricVariables: params.symmetricVariables !== false,
            mmType: params.mmType || 'fromStrategy',
            periodParams: params.periodParams !== false,
            constantsParams: params.constantsParams === true,
            shiftParams: params.shiftParams === true,
            otherParams: params.otherParams === true,
            entryParams: params.entryParams === true,
            entryLogic: params.entryLogic === true,
            exitParamsUsed: params.exitParamsUsed !== false,
            exitParamsUnused: params.exitParamsUnused === true,
            booleanParams: params.booleanParams === true,
            recommendedParams: params.recommendedParams !== false
        };
        BackendService.sendRequest('sourcecode/print', data, callback, 'POST');
    }
});

