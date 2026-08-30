//product:TASKMANAGER
angular.module('app.settings').config(function(sqPluginProvider, SQEventsProvider) {
    
	sqPluginProvider.plugin("SettingsTab", 10, {
        title: Ltsq('Custom Data'),
        help: Ltsq('Configure trading engine, symbols and timeframes for the main backtest. You can configure also default spread, slippage, commissions.'),
        helpURL: 'automatic-retest-data',
        task: 'AutomaticRetest',
        configElemName: 'CustomData',
        dataItem: 'settings-custom-data',
        templateUrl: '../../../plugins/SettingsAutoRetestData/views/autoRetestData.html',
        controller: 'AutoRetestDataCtrl',
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