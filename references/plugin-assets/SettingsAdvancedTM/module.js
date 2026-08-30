//product:BUILDER
//product:OPTIMIZER
//product:RETESTER
//product:TASKMANAGER
angular.module('app.settings').config(function (sqPluginProvider) {
    sqPluginProvider.plugin("SettingsTab", 35, {
        title: Ltsq('ATM'),
        // help: Ltsq('Advanced Trading Management'),
        help: Ltsqt('Advanced Trading Management') + '<span class="experimental">' + Ltsqt('EXPERIMENTAL') + '</span><br/>' + Ltsqt('allows configurable multiple exits, overrides the standard strategy exits'),
        helpURL: 'https://strategyquant.com/doc/strategyquant/settings-atm/',
        task: 'Build,Optimize,Retest,AutomaticRetest,NeuralNetworkTrainer',
        configElemName: 'ATMs',
        dataItem: 'settings-advancedtm',
        templateUrl: '../../../plugins/SettingsAdvancedTM/advancedTM.html',
        controller: 'AdvancedTMCtrl',
        getSettingsDescription: function (settingsElement, injector) {
            var L = injector.get("L");
            return "";
        }
    });
});