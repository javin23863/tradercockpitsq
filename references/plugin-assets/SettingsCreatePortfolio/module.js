//product:TASKMANAGER
angular.module('app.settings').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("SettingsTab", 50, {
        title: Ltsq('Create portfolio'),
        help: Ltsq('Create portfolio from all strategies in source databank, save it to target databank.'),
        helpURL: 'create-portfolio',
        task: 'CreatePortfolio',
        configElemName: 'CreatePortfolio',
        dataItem: 'tm-create-portfolio',
        templateUrl: '../../../plugins/SettingsCreatePortfolio/settingsCreatePortfolio.html',
        controller: 'SettingsCreatePortfolioCtrl',
        getSettingsDescription : function(settingsElement, injector){
            return Ltsq('Create portfolio from all strategies in source databank, save it to target databank.');
        }
    });
});