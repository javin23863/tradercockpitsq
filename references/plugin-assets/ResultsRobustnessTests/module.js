angular.module('app.resultstabs.robustnesstests', ['sqplugin']).config(function(sqPluginProvider, SQEventsProvider) {

    sqPluginProvider.plugin("ResultsTab", 60, {
        title: Ltsq('Monte Carlo tests'),
        dataItem: 'monteCarloTests',
        templateUrl: '../../../plugins/ResultsRobustnessTests/robustnessTests.html',
        controller: 'RobustnessTestsResultsCtrl',
        hideAfterInit: true
    });

    SQEventsProvider.add('RT_SETUP_SELECTED', 'rt-setup-selected');
});