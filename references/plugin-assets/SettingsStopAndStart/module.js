//product:TASKMANAGER
angular.module('app.settings').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("SettingsTab", 10, {
        title: Ltsq('Stop & Start'),
        help: Ltsq('Allows stopping this custom project when conditions are fulfilled, and start another external custom project after stop.'),
        helpURL: 'stop-and-start',
        task: 'StopAndStart',
        configElemName: 'StopAndStart',
        dataItem: 'tm-stopandstart',
        templateUrl: '../../../plugins/SettingsStopAndStart/settingsStopAndStart.html',
        controller: 'SettingsStopAndStartCtrl',
        getSettingsDescription : function(settingsElement, injector){
            return [];
        }
    });
});