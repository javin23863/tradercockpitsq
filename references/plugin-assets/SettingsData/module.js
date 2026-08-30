//product:BUILDER
//product:OPTIMIZER
//product:RETESTER
//product:TASKMANAGER
angular.module('app.settings').config(function(sqPluginProvider, SQEventsProvider) {
    
    SQEventsProvider.add('MAIN_TF_CHANGED', 'main-tf-changed');
    SQEventsProvider.add('MAIN_SETUP_CHANGED', 'main-setup-changed');
    SQEventsProvider.add('MAIN_SYMBOL_CHANGED', 'main-symbol-changed');

	sqPluginProvider.plugin("SettingsTab", 10, {
        title: Ltsq('Data'),
        help: Ltsq('Configure trading engine, symbols and timeframes for the main backtest. You can configure also Out of sample (unseen) periods, and default spread, slippage, commissions.'),
        helpURL: 'data',
        task: 'Build,Optimize,Retest,NeuralNetworkTrainer',
        configElemName: 'Data',
        dataItem: 'settings-data',
        templateUrl: '../../../plugins/SettingsData/views/data.html',
        controller: 'DataCtrl',
        getSettingsDescription : function(settingsElement, injector){
            var L = injector.get("L");
            var elSetups = getChildElement(settingsElement, "Setups");
            var elSetup = getChildElement(elSetups, "Setup");
            var elChart = getChildElement(elSetup, "Chart");

            if(!elSetup || !elChart) return L.tsq("No data defined");

            var symbol = getAttrValue(elChart, "symbol");

            if(!symbol) return L.tsq("No data defined");

            return symbol + " / " + getAttrValue(elChart, "timeframe") + "<br>" + getAttrValue(elSetup, "dateFrom") + " - " + getAttrValue(elSetup, "dateTo");
        }
    });
});