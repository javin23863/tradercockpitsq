//product:TASKMANAGER
angular.module('app.settings').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("SettingsTab", 50, {
        title: Ltsq('Call external script'),
        help: Ltsq('Calls specified external script or program. It can be either Windows patch or PowerShell file or an executable.<br>Can be used to call an external program to perform some computation on generated data.'),
        helpURL: 'call-external-script',
        task: 'CallExternalScript',
        configElemName: 'CallExternalScript',
        dataItem: 'tm-call-external-script',
        templateUrl: '../../../plugins/SettingsCallExternalScript/settingsCallExternalScript.html',
        controller: 'SettingsCallExternalScriptCtrl',
        getSettingsDescription : function(settingsElement, injector){
            return Ltsq('Call external script');
        }
    });
});