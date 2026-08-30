angular.module('app.settings').config(function(sqPluginProvider) {
    
    sqPluginProvider.addPopupWindow("../../../plugins/TaskBuild/indicatorsCalibration/indicatorsCalibrationPopup.html", 'IndicatorsCalibrationPopupCtrl', 'SQUANT');

    sqPluginProvider.plugin("SimpleTaskSettings", 0, {
        taskType: 'Build',
        templateUrl: '../../../plugins/TaskBuild/simpleSettings/simpleSettings.html',
        controller: 'SimpleBuildSettingsCtrl',
        getInfoPanels: function(xmlConfig, injector){
            var L = injector.get("L");
            var settingsPlugins = sqPluginProvider.getPlugins('SettingsTab');

            var groups = [
                {
                    title: L.tsq('Build options'),
                    settings: [ 
                        { name : L.tsq("What to build"), value : getTaskInfoPanelSettings(xmlConfig, getItem(settingsPlugins, 'configElemName', "WhatToBuild")) },
                        { name : L.tsq("Building blocks"), value : getTaskInfoPanelSettings(xmlConfig, getItem(settingsPlugins, 'configElemName', "Blocks")) }
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
                        { name : L.tsq("Rankings"), value : getTaskInfoPanelSettings(xmlConfig, getItem(settingsPlugins, 'configElemName', "Rankings")) },
                        { name : L.tsq("CrossChecks"), value : getTaskInfoPanelSettings(xmlConfig, getItem(settingsPlugins, 'configElemName', "CrossChecks")) }
                    ]
                }
            ];

            return groups;
        }
    });
   
});