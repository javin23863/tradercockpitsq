angular.module('app.dataLog', ['sqplugin']).config(function (sqPluginProvider, SQEventsProvider) {
	
	sqPluginProvider.plugin("MainTab", 100, {
        title: Ltsq('Log'),
        help: Ltsq('Here is where you can see the download progress - you can see every symbol / year downloaded by the program.'),
        dataItem: 'log',
        templateUrl: '../../../plugins/DataManagerLog/dataLog.html',
        controller: 'DMDataLogCtrl'
    });
});