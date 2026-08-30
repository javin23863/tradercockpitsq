//product:TASKMANAGER
angular.module('app.settings').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("SettingsTab", 50, {
        title: Ltsq('Filtering'),
        help: Ltsq('Filter strategies in databank using conditions below and determine what to do with the ones that match teh filter - delete, copy or move to another databank.'),
        helpURL: 'filtering',
        task: 'Filtering',
        configElemName: 'Filtering',
        dataItem: 'tm-filtering',
        templateUrl: '../../../plugins/SettingsFiltering/settingsFiltering.html',
        controller: 'SettingsFilteringCtrl',
        getSettingsDescription : function(settingsElement, injector){
            var L = injector.get("L");
            var elSettings = findParentElement(settingsElement, "Settings");
            var elDatabanks = getChildElement(elSettings, "Databanks");
            var databankElems = getChildElements(elDatabanks, "Databank");

            if(databankElems.length) return "N/A";

            var sourceDb = "???";
            var targetDb = "???";

            for(var i=0; i<databankElems.length; i++){
                var databankElem = databankElems[i];
                var databankType = getAttrStringValue(databankElem, "name");
                var databankName = getAttrStringValue(databankElem, "value");

                if(databankType == "Source"){
                    sourceDb = databankName;
                }
                else if(databankType == "Target"){
                    targetDb = databankName;
                }
            }
            
            var actionType = getNodeIntValue(settingsElement, "ActionType");
            var settingsText = "";
            
            switch(actionType) {
                case 0: //delete
                    settingsText = L.tsq("Delete from %s", [sourceDb]);
                    break;
                case 1: //copy from source to target
                    settingsText = L.tsq("Copy from %s to %s", [sourceDb, targetDb]);
                    break;
                case 2: //move from source to target
                    settingsText = L.tsq("Move from %s to %s", [sourceDb, targetDb]);
                    break;
                default: return L.tsq("N/A");
            }	

            var elConditions = getChildElement(settingsElement, "Conditions");
            var conditionElems = getChildElements(elConditions, "Condition");
            var conditionsCount = 0;

            for(var i=0; i<conditionElems.length; i++){
                if(getAttrBooleanValue(conditionElems[i], "use")) conditionsCount++;
            }

            settingsText += " " + L.tsq("with %d conditions", [conditionsCount]);

			return settingsText;
        }
    });
});