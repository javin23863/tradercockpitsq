//product:TASKMANAGER
angular.module('app.settings').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("SettingsTab", 50, {
        title: Ltsq('Update data'),
        help: Ltsq('Updates data - downloads the most recent data for all the symbols used in the project.'),
        helpURL: 'update-data',
        task: 'UpdateData',
        configElemName: 'UpdateData',
        dataItem: 'tm-update-data',
        templateUrl: '../../../plugins/SettingsUpdateData/settingsUpdateData.html',
        controller: 'SettingsUpdateDataCtrl',
        getSettingsDescription : function(settingsElement, injector){
            return Ltsq('Update data');
        }
    });
});