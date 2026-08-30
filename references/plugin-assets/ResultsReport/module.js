angular.module('app.resultstabs.report', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsReport", 1, {
        title: Ltsq('Report'),
        dataItem: 'report',
        controller: 'ReportCtrl',
        templateUrl: '../../../plugins/ResultsReport/report.html',
    });
});