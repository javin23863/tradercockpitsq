angular.module('app.settings').config(function(sqPluginProvider) {
	
    sqPluginProvider.plugin("SettingsTab", 800, {
        title: Ltsq('Automatic Portfolio Builder'),
        help: Ltsq('Automatic Portfolio Computer, a tool for automatic creation of optimal portfolio with given predefined constraints.'),
        helpURL: 'automatic-portfolio-builder',
        task: 'AutomaticPortfolioBuilder',
        configElemName: 'AutomaticPortfolioBuilder',
        dataItem: 'tm-automatic-portfolio-builder',
        templateUrl: '../../../plugins/SettingsAutomaticPortfolioBuilder/automaticPortfolioBuilder.html',
        controller: 'AutomaticPortfolioBuilderCtrl',
        getSettingsDescription : function(settingsElement, injector){
            return "";
        }
    });
});