angular.module('app').service('DatabankSyncService', function(AppService, SQConstants, BackendService) {

    function loadOptions(){
        var syncTypes = SQConstants.getConstants().databankSyncTypes;
        var commonOptions = [];
        var immediateType = null;

        for (let type in syncTypes) {
            if(syncTypes[type].indexOf("Immediate") >= 0){
                immediateType = syncTypes[type];
                continue;
            }

            commonOptions.push({
                title: syncTypes[type],
                icon: (type == "never") ? "fa fa-times" : "fa fa-clock-o",
            });
        }

        commonOptions.sort(sortFunction);

        databankOptions = [
            { title: Ltsq("Sync to files now"), icon: "fa fa-refresh" },
            { title: "────────────────────", separator: true }
        ];

        databankOptions = databankOptions.concat(commonOptions);

        globalOptions = [
            { title: immediateType, icon: "fa fa-refresh" },
            { title: "────────────────────", separator: true }
        ];

        globalOptions = globalOptions.concat(commonOptions);
    }

    function sortFunction(a, b){
        var partsA = a.title.split(" ");
        var partsB = b.title.split(" ");

        var timeUnitA = partsA[partsA.length - 1];
        var timeUnitB = partsB[partsB.length - 1];

        var countA = partsA[partsA.length - 2];
        var countB = partsB[partsB.length - 2];

        if(timeUnitA == "never" || (timeUnitA == "minutes" && timeUnitB.indexOf("hour") == 0)){
            return -1;
        }
        else if(timeUnitB == "never" || (timeUnitA.indexOf("hour") == 0 && timeUnitB == "minutes")){
            return 1;
        }
        else {
            return parseInt(countA) - parseInt(countB);
        }
    }

    this.getGlobalOptions = function(){
        return globalOptions;
    }

    this.getDatabankOptions = function(){
        return databankOptions;
    }

    this.synchronizeDatabank = function(databankName){
        BackendService.sendRequest("project/synchronizeDatabank", {
            projectName: AppService.getProject(),
            databankName: databankName
        });
    }

    this.setSyncTypeToDatabank = function(databankName, syncType){
        BackendService.sendRequest("project/setDatabankSynchronization", {
            projectName: AppService.getProject(),
            databankName: databankName,
            syncType: syncType
        }, 
        function (result) {
            saveConfig(databankName, syncType);
        });
    }

    function saveConfig(databankName, syncType) {
        var databank = AppService.getDatabank();
        if (databank) {
            databank.syncType = syncType;
        }

        var elProject = AppService.getProjectConfig();
        var elDatabanks = getChildElement(elProject, "Databanks");
        var databankElems = getChildElements(elDatabanks, "Databank");

        for (var i = 0; i < databankElems.length; i++) {
            var dbName = getAttrValue(databankElems[i], "name", null);
            if (dbName == databankName) {
                databankElems[i].setAttribute("syncType", syncType);
                break;
            }
        }

        AppService.updateProjectXML();
    }

    var databankOptions = [];
    var globalOptions = [];
    
    loadOptions();

});