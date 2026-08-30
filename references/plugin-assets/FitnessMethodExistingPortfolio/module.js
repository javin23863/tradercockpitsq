angular.module('app.fitnessExistingPortfolio', ['sqplugin']).config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("FitnessMethod", 30, {
        title: Ltsq('Existing portfolio'),
        key: 'ExistingPortfolio', 
        templateUrl: '../../../plugins/FitnessMethodExistingPortfolio/fitnessMethodExistingPortfolio.html',
        controller: 'FitnessMethodExistingPortfolioCtrl'
    });

});