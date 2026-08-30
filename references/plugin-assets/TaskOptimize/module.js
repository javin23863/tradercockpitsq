angular.module('app.settings').config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("SimpleTaskSettings", 1, {
        taskType: 'Optimize',
        templateUrl: '../../../plugins/TaskOptimize/simpleSettings/simpleSettings.html',
        controller: 'SimpleOptimizeSettingsCtrl',
        getInfoPanels: function(xmlConfig, injector){
            var L = injector.get("L");
            var settingsPlugins = sqPluginProvider.getPlugins('SettingsTab');

            var groups = [
                {
                    title: L.tsq('Optimization options'),
                    settings: [ 
                        { name : L.tsq("Optimization"), value : getTaskInfoPanelSettings(xmlConfig, getItem(settingsPlugins, 'configElemName', "Optimization")) }
                    ]
                },
                {
                    title: L.tsq('Backtest options'),
                    settings: [
                        { name : L.tsq("Data"), value : getTaskInfoPanelSettings(xmlConfig, getItem(settingsPlugins, 'configElemName', "Data")) },
                        { name : L.tsq("Trading options"), value : getTaskInfoPanelSettings(xmlConfig, getItem(settingsPlugins, 'configElemName', "Options")) },
                        { name : L.tsq("Money Management"), value : getTaskInfoPanelSettings(xmlConfig, getItem(settingsPlugins, 'configElemName', "RiskMoneyManagement"), injector) }
                    ]
                },
                {
                    title: L.tsq('Ranking & Filtering options'),
                    settings: [
                        { name : L.tsq("Rankings"), value : getTaskInfoPanelSettings(xmlConfig, getItem(settingsPlugins, 'configElemName', "Rankings")) }
                    ]
                }
            ];

            return groups;
        }
    });
});