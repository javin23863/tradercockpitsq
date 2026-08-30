//product:TASKMANAGER
angular.module('app.settings').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("SettingsTab", 50, {
        title: Ltsq('Delete file'),
        help: Ltsq('Deletes a specified file on the disc. Can be used in conjunction with Wait for user/file task.'),
        helpURL: 'delete-file',
        task: 'DeleteFile',
        configElemName: 'DeleteFile',
        dataItem: 'tm-delete-file',
        templateUrl: '../../../plugins/SettingsDeleteFile/settingsDeleteFile.html',
        controller: 'SettingsDeleteFileCtrl',
        getSettingsDescription : function(settingsElement, injector){
            return Ltsq('Delete file');
        }
    });
});