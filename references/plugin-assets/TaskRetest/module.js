angular.module('app.settings').config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("SimpleTaskSettings", 2, {
        taskType: 'Retest',
        templateUrl: '../../../plugins/TaskRetest/simpleSettings/simpleSettings.html',
        controller: 'SimpleRetestSettingsCtrl',
        getInfoPanels: function(xmlConfig, injector){
            var L = injector.get("L");
            var settingsPlugins = sqPluginProvider.getPlugins('SettingsTab');

            var groups = [
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
                        { name : L.tsq("Rankings"), value : getTaskInfoPanelSettings(xmlConfig, getItem(settingsPlugins, 'configElemName', "Rankings")) },
                        { name : L.tsq("CrossChecks"), value : getTaskInfoPanelSettings(xmlConfig, getItem(settingsPlugins, 'configElemName', "CrossChecks")) }
                    ]
                }
            ];
            
            return groups;
        }
    });
});