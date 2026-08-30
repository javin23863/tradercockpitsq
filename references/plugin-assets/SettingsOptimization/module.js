//product:OPTIMIZER
//product:TASKMANAGER
angular.module('app.settings').config(function(sqPluginProvider, SQEventsProvider) {
	
    SQEventsProvider.add("OPTIMIZATION_SRC_CHANGED", 'optimization-src-changed');       //called when optimization source change occurs, args contain boolean databank
    SQEventsProvider.add("OPTIMIZATION_FILE_CHANGED", 'optimization-file-changed');     //called after selecting strategy file
    SQEventsProvider.add("OPTIMIZATION_TYPE_CHANGED", 'optimization-type-changed');     //called after selecting strategy file
    SQEventsProvider.add("OPTIMIZATION_SIMPLE_TYPE_CHANGED", 'optimization-simple-type-changed');     //called after selecting strategy file

    sqPluginProvider.plugin("SettingsTab", 5, {
        title: Ltsq('Optimization'),
        help: Ltsq('Choose strategy to optimize or alternatively optimize all strategies in source databank. You can also configure optimization type - simple or Walk-Forward.'),
        helpURL: 'optimization',
        task: 'Optimize',
        configElemName: 'Optimization',
        dataItem: 'optimizer-settings-blocks',
        templateUrl: '../../../plugins/SettingsOptimization/views/optimization.html',
        controller: 'OptimizationCtrl',
        getSettingsDescription : function(settingsElement, injector){
            var L = injector.get("L");
            switch(getAttrIntValue(settingsElement, "type")){
                case 1: return L.tsq("Simple");
                case 2: return L.tsq("Walk - Forward");
                case 3: return L.tsq("Walk - Forward matrix");
                default: return L.tsq("N/A");
            }
        }
    });
});