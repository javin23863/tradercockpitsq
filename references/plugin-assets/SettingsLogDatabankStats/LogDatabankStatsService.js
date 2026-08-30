angular.module('app.settings').service('LogDatabankStatsService', function($q, BackendService, AppService, L, DatabankService, SQEvents, ColumnSettingsService, ConditionsGridService) {

    function loadDatabanks() {
        DatabankService.loadVisibleDatabanks(instance.config.availableDatabanks);
    }

    this.saveSettings = function() {
        var xmlDoc = AppService.xmlDoc;
        var logDatabankStatsObj = AppService.getCurrentTaskTabSettings("LogDatabankStats");

        removeAllChildren(logDatabankStatsObj);
        addNode('Databank', instance.config.databank, logDatabankStatsObj, xmlDoc);

        for (var i = 0; i < instance.config.statsTypes.length; i++) {
            var statsType = instance.config.statsTypes[i];

            var logStatsObj = createChild(logDatabankStatsObj, 'LogStats', xmlDoc, true);
            logStatsObj.setAttribute("type", statsType.type);
            
            var conditionsObj = statsType.condGridInstance.getConditionsObj();
            logStatsObj.appendChild(conditionsObj);

            logStatsObj.appendChild(statsType.columnObj);
            
            logDatabankStatsObj.appendChild(logStatsObj);
        }

        instance.config.simpleSettingsText = printSimpleSettings();
    }
    
    this.loadSettings = function() {
        var xmlDoc = AppService.xmlDoc;

        var logDatabankStatsObj = AppService.getCurrentTaskTabSettings("LogDatabankStats");
        if(!logDatabankStatsObj) return;

        this.config.statsTypes.length = 0;

        this.config.databank = getNodeValue(logDatabankStatsObj, "Databank", 'Results');
        
        var logStatsObjs = getChildElements(logDatabankStatsObj, 'LogStats');
        for(var i=0; i<logStatsObjs.length; i++) {
            var logStatsObj = logStatsObjs[i];

            var data = {
                type: getAttrStringValue(logStatsObj, "type", 'sum'),
                columnObj: getChildElement(logStatsObj, 'Column'),
                condGridInstance: {},
                conditionsObj:  getChildElement(logStatsObj, "Conditions")
            }

            this.config.statsTypes.push(data);
        }

        instance.config.simpleSettingsText = printSimpleSettings();
    }

    this.edit = function(item) {
        if(!instance.onEdit) return;

        instance.onEdit(item);
    }

    this.remove = function(item) {
        if(!instance.settingsChanged) return;
        
        var index = this.config.statsTypes.indexOf(item);
        if (index !== -1) {
            this.config.statsTypes.splice(index, 1);

            instance.settingsChanged();
        }
    }

    function printSimpleSettings() {
        var text = 'NA';
        
        try {

            text = L.tsq("Databank: %s", [instance.config.databank])+"<br><br>";

            for (var i = 0; i < instance.config.statsTypes.length; i++) {
                var statsType = instance.config.statsTypes[i];

                var metric = ColumnSettingsService.printColumnTitle(statsType.columnObj);
                var title = "NA";

                if(statsType.type=='count') {
                    statsType.columnObj
                    title = L.tsq("Count of strategies matching conditions");
                } else if(statsType.type=='sum') {
                    title = L.tsq("Sum of %s of strategies matching conditions:", [metric]);
                } else if(statsType.type=='avg') {
                    title = L.tsq("Average of %s of strategies matching conditions:", [metric]);
                }

                var conditionsText = "";
                
                var conditionsObj = null;
                if(statsType.conditionsObj) {
                    conditionsObj = statsType.conditionsObj;
                } else if(statsType.condGridInstance.getConditionsObj) {
                    conditionsObj = statsType.condGridInstance.getConditionsObj();
                }

                var conditions = getChildElements(conditionsObj, "Condition");
                for (var j = 0; j < conditions.length; j++) {
                    var condition = conditions[j];

                    conditionsText += "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + ConditionsGridService.printCondition(condition)+"<br>";
                }
                
                text += title+"<br>"+conditionsText+"<br>";
            }
        } catch(err) {
            console.error(err.message)
        }

        return text;
    }

    var instance = this;

    this.config = {
        databank: 'Results', 
        availableDatabanks: [],
        statsTypes: [],
        simpleSettingsText : "Not available"
    }

    this.availableTypes = [
        {value: 'count', name: 'Count', desc: 'number of strategies in databank'},
        {value: 'sum', name: 'Sum of', desc: 'sum of the given metric'},
        {value: 'avg', name: 'Average of', desc: 'average of the given metric'},
    ]

    function onEvent(event, data){
        if(event == SQEvents.get("RELOAD_DATABANKS")){
            loadDatabanks();
        }
    }

    SQEvents.addListener("SettingsFilteringService", [SQEvents.get('RELOAD_DATABANKS')], onEvent);
});