angular.module('app.settings').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("SettingsTab", 10, {
        title: Ltsq('Apply mass config'),
        help: Ltsq('Apply mass config.'),
        helpURL: 'apply-mass-config',
        task: 'ApplyMassConfig',
        configElemName: 'ApplyMassConfig',
        dataItem: 'tm-apply-mass-config',
        templateUrl: '../../../plugins/SettingsApplyMassConfig/applyMassConfig.html',
        controller: 'ApplyMassConfigCtrl',
        getSettingsDescription : function(settingsElement, injector){
            return "";
        }
    });
});