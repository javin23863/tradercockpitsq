angular.module('app.settings').config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("SimpleTaskSettings", 4, {
        taskType: 'CreatePortfolio',
        templateUrl: '../../../plugins/TaskCreatePortfolio/simpleSettings/simpleSettings.html',
        controller: 'SimpleCreatePortfolioSettingsCtrl',
        getInfoPanels: function(xmlConfig, injector){
            return [];
        }
    });
    
});