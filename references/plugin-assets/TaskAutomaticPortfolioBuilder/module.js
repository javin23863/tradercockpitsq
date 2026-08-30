angular.module('app.settings').config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("SimpleTaskSettings", 1, {
        taskType: 'AutomaticPortfolioBuilder',
        templateUrl: '../../../plugins/TaskAutomaticPortfolioBuilder/simpleSettings/simpleSettings.html',
        controller: 'SimpleAutoPortfolioBuilderCtrl',
        getInfoPanels: function(xmlConfig, injector){
            var L = injector.get("L");
            var settingsPlugins = sqPluginProvider.getPlugins('SettingsTab');

            var groups = [
                {
                    title: L.tsq('Portfolio options'),
                    settings: [
                    ]
                }
            ];
            
            return groups;
        }
    });
});