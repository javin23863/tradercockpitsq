angular.module('app.resultstabs.sourcecode', ['sqplugin']).config(function(sqPluginProvider) {

    sqPluginProvider.plugin("ResultsTab", 100, {
        title: Ltsq('Source Code'),
        dataItem: 'sourceCode',
        templateUrl: '../../../plugins/ResultsSourceCode/sourceCode.html',
        controller: 'SourceCodeCtrl'
    });
});