//product:TASKMANAGER
angular.module('app.settings').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("SettingsTab", 50, {
        title: Ltsq('Go To Task'),
        help: Ltsq('Skips to a defined task when the conditions below are true.'),
        helpURL: 'go-to-task',
        task: 'GoToTask',
        configElemName: 'GoToTask',
        dataItem: 'tm-gototask',
        templateUrl: '../../../plugins/SettingsGoToTask/settingsGoToTask.html',
        controller: 'SettingsGoToTaskCtrl',
        getSettingsDescription : function(settingsElement, injector){
            var L = injector.get("L");
            var elConditions = getChildElement(settingsElement, "Conditions");
            var conditionElems = getChildElements(elConditions, "Condition");

            var taskName = getAttrStringValue(settingsElement, "task", "???");
            var conditionsCount = 0;

            for(var i=0; i<conditionElems.length; i++){
                if(getAttrBooleanValue(conditionElems[i], "use")) conditionsCount++;
            }

            return L.tsq("Go to %s with %d conditions", [taskName, conditionsCount]);
        }
    });
});