//product:TASKMANAGER
angular.module('app.settings').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("SettingsTab", 50, {
        title: Ltsq('Wait for user/file'),
        help: Ltsq('Pause the project and wait either for manual restart by user or for appearance of a file in given path.<br>Can be used when project calls external script and waits for some computation.'),
        helpURL: 'wait-for',
        task: 'WaitFor',
        configElemName: 'WaitFor',
        dataItem: 'tm-wait-for',
        templateUrl: '../../../plugins/SettingsWaitFor/settingsWaitFor.html',
        controller: 'SettingsWaitForCtrl',
        getSettingsDescription : function(settingsElement, injector){
            return Ltsq('Wait for user/file');
        }
    });
});