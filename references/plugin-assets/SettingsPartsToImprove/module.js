//product:BUILDER
//product:TASKMANAGER
angular.module('app.settings').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("SettingsTab", 6, {
        title: Ltsq('Parts to improve'),
        help: Ltsq('Configure which parts of the strategy should be improved. You can further configure if you want to replace the whole part, or add new blocks to it.'),
        helpURL: 'parts-to-improve',
        task: 'Build',
        configElemName: 'PartsToImprove',
        dataItem: 'settings-partstoimprove',
        templateUrl: '../../../plugins/SettingsPartsToImprove/views/partsToImprove.html',
        controller: 'PartsToImproveCtrl'
    });
});