angular.module('app.databanks').service('DatabankService', function ($rootScope, $q, SQEvents, BackendService, AppService, SQConstants, L) {

    this.databankList = function (callOnSuccess) {
        if (!databanksPromise) {
            loadDatabanks();
        }

        databanksPromise.then(callOnSuccess);
    }

    function loadDatabanks() {
        var deferred = $q.defer();
        databanksPromise = deferred.promise;

        var projectName = AppService.getProject();
        if (projectName == 'TaskManager') {
            instance.databanks.length = 0;
            deferred.resolve();
            return;
        }

        BackendService.getPromise('project/databankList', { 'projectName': projectName }).then(function (result) {
            if (result.data.success) {
                refreshDatabanks(result.data.databanks);
            }
            else {
                $rootScope.showError(result.data.error);
            }

            deferred.resolve();
        });
    }

    function refreshDatabanks(newDatabanks) {
        var oldDatabanks = angular.copy(instance.databanks);
        loadToArray(instance.databanks, newDatabanks);

        //keep databanks display settings
        for (var i = 0; i < instance.databanks.length; i++) {
            var db = instance.databanks[i];
            db.title = db.name;

            var oldDb = getItem(oldDatabanks, 'name', db.name);
            if (oldDb) {
                db.display = oldDb.display;
            }
        }
    }

    this.loadVisibleDatabanks = function (databanksArray, callback, includeResults) {
        instance.databankList(function () {
            var allDatabanks = angular.copy(instance.databanks);

            var isBuilder = window.appConfig.product == "BUILDER";
            databanksArray.length = 0;

            for (var i = 0; i < allDatabanks.length; i++) {
                var databank = allDatabanks[i];

                if ((includeResults || databank.name != SQConstants.getConstants().gedatabanks.results || !isBuilder) && databank.display !== false) {
                    databanksArray.push(databank);
                }
            }

            if (databanksArray.length == 0) {
                databanksArray.push({ title: "Results", name: "Results" });
            }

            if (callback) callback();
        });
    }

    this.getColumnTypeDefinition = function (column) {
        var def = getDatabankColumnType(column);

        if (column.sampleType >= SQConstants.getConstants().sampleTypes.isv && column.sampleType <= SQConstants.getConstants().sampleTypes.isv10) {
            def += ",background-isv";
        } else if (column.sampleType >= SQConstants.getConstants().sampleTypes.out && column.sampleType <= SQConstants.getConstants().sampleTypes.oos10) {
            def += ",background-oos";
        }

        switch (column.class) {
            case "AvgLoss":
            case "GrossLoss":
            case "ActualDD":
            case "Drawdown":
            case "MaxLoss":
                def += ",value-negative";
                break;
            case "AvgWin":
            case "GrossProfit":
                def += ",value-positive";
                break;
        }

        return def;
    }

    this.getColumnResetValue = function (columnType) {
        switch (columnType) {
            case "text":
            case "dateTime":
            case "filterResult":
                return "";
            default:
                return 0;
        }
    }

    function getDatabankColumnType(column) {
        if ((column.class == "NetProfit" || column.class == "MaxProfit") && column.type == 'Decimal2PL') {
            return "dollarsColoured";
        }

        return getColumnType(column.type, column.plType);
    }

    /**
     * returns an array of tasks that use the specified databank
     */
    this.checkDatabankUsed = function (databankName) {
        var taskNames = [];

        let projectConfigObj = SQConstants.getProjectConfigObj(AppService.getProject());
        
        if (!projectConfigObj) { console.error("Cannot find project config of project " + AppService.getProject()); return; }

        for (let taskFileName in projectConfigObj.tasks) {
            let taskXML = projectConfigObj.tasks[taskFileName];
            let elSettings = xmlToObject(taskXML).find("Settings")[0];
            
            let task = getTaskByXMLFile(AppService.getProjectConfig(), taskFileName);
            var taskName = getAttrStringValue(task, "title", "??");
            var taskType = getAttrStringValue(task, "type", "??");

            console.error(taskName, elSettings);

            var databanks = instance.getTaskDatabanks(elSettings);
            databanks = instance.removeUnusedDatabanks(taskType, databanks, elSettings);

            var databank = getItem(databanks, 'value', databankName);

            if (databank) taskNames.push(taskName);
        }

        return taskNames;
    }

    this.isDefaultDatabank = function (name) {
        var defaultNames = SQConstants.getConstants().gedatabanks;

        switch (name) {
            case defaultNames.results:
            case defaultNames.initialPopulation:
            case defaultNames.lastGeneration:
            case defaultNames.strategiesToImprove:
            case defaultNames.strategiesToOptimize:
            case defaultNames.existingPortfolio:
            case defaultNames.simpleStrategies:
                return true;
            default:
                return false;
        }
    }

    this.removeUnusedDatabanks = function(taskType, databanks, elSettings){
        if(taskType == "Build"){
            var whatToBuildElem = getChildElement(elSettings, 'WhatToBuild');
            var strategyTypeElem = getChildElement(whatToBuildElem, 'StrategyType');
            var strategyType = getAttrStringValue(strategyTypeElem, "type", "??");
            if(strategyType != "improve"){
                databanks = databanks.filter((databank) => {
                    return databank.name != "Input"
                })
            }
        }
        return databanks
    }

    this.getTaskDatabanks = function (taskElem) {
        var databanks = [];
        var settingsElem;

        if (taskElem) {
            settingsElem = taskElem;
        }
        else {
            settingsElem = AppService.getTaskConfig();
        }

        if (settingsElem) {
            var databanksElem = getChildElement(settingsElem, 'Databanks');
            if (databanksElem) {
                var databankElems = getChildElements(databanksElem, 'Databank');

                for (var d = 0; d < databankElems.length; d++) {
                    var databankElem = databankElems[d];
                    databanks.push({
                        name: getAttrValue(databankElem, 'name'),
                        value: getAttrValue(databankElem, 'value'),
                        label: getAttrValue(databankElem, 'label')
                    });
                }
            }
        }

        return databanks;
    }

    this.removeTaskDatabankSettings = function (attribute, value) {
        var settingsElem = AppService.getTaskConfig();
        if (!settingsElem) return;

        var databanksElem = getChildElement(settingsElem, 'Databanks');
        if (!databanksElem) return;

        var databankElems = getChildElements(databanksElem, 'Databank');
        for (var d = 0; d < databankElems.length; d++) {
            var databankElem = databankElems[d];
            var attrValue = getAttrValue(databankElem, attribute);

            if (attrValue == value) {
                databanksElem.removeChild(databankElem);
            }
        }
    }

    this.createDatabank = function (databankName, position, callOnSuccess, allowPredefined) {
        var data = {
            projectName: AppService.getProject(),
            databankName: databankName,
            predefined: allowPredefined,      //signalizes that any name can be used (including predefined databank names)
            position: position
        }

        BackendService.sendRequest('project/createDatabank', data, function (result) {
            SQConstants.setProjectConfig(AppService.getProject(), result.xmlConfig);

            AppService.reloadConfig(function () {
                SQEvents.notifyListeners(SQEvents.get('RELOAD_DATABANKS'), {});
                SQEvents.notifyListeners(SQEvents.get('DATABANK_UPDATED'), {});

                if (callOnSuccess) {
                    callOnSuccess(result);
                }
            });
        });
    }

    this.removeDatabank = function (databankName, callOnSuccess, allowPredefined) {
        var data = {
            projectName: AppService.getProject(),
            databankName: databankName,
            predefined: allowPredefined      //signalizes that any name can be used (including predefined databank names)
        }

        BackendService.sendRequest('project/removeDatabank', data, function (result) {
            SQConstants.setProjectConfig(AppService.getProject(), result.xmlConfig);

            AppService.reloadConfig(function () {
                SQEvents.notifyListeners(SQEvents.get('RELOAD_DATABANKS'), {});

                if (callOnSuccess) {
                    callOnSuccess(result);
                }
            });
        });
    }

    this.renameDatabank = function (oldName, newName, callOnSuccess) {
        var data = {
            projectName: AppService.getProject(),
            databankName: oldName,
            newName: newName
        }

        BackendService.sendRequest("project/renameDatabank", data, function (result) {
            deleteSelectedStrategy();

            SQConstants.setProjectConfigObj(AppService.getProject(), result.projectConfig);
            
            let currentTask = angular.copy(AppService.getTask());

            AppService.reloadConfig(function () {
                AppService.setTask(currentTask.type, currentTask.name, currentTask.title, currentTask.typeOrig, currentTask.taskXMLFile);
                
                var databank = getItem(instance.databanks, "name", oldName);
                if (databank) {
                    databank.name = newName;
                    databank.title = newName;
                }
                else {
                    console.error("Cannot rename - Databank '" + oldName + "' not found");
                }

                callOnSuccess();
            });
        }, 'POST');
    }

    this.moveDatabank = function (databankName, moveLeft, callOnSuccess) {
        var data = {
            projectName: AppService.getProject(),
            databankName: databankName,
            moveLeft: moveLeft
        }

        BackendService.sendRequest("project/moveDatabank", data, function (result) {
            refreshDatabanks(result.databanks);
            instance.saveConfig(instance.databanks);
            callOnSuccess();
        });
    }

    this.moveDatabankToPosition = function (databankName, newPosition, callOnSuccess) {
        var data = {
            projectName: AppService.getProject(),
            databankName: databankName,
            newPosition: newPosition
        }

        BackendService.sendRequest("project/moveDatabankToPosition", data, function (result) {
            refreshDatabanks(result.databanks);
            instance.saveConfig(instance.databanks);
            callOnSuccess();
        });
    }

    this.loadFiles = function (filePaths, clearDatabank) {
        var deferred = $q.defer();
        var data = {
            projectName: AppService.getProject(),
            databankName: AppService.getDatabank().title,
            filePaths: filePaths,
            clear: clearDatabank
        }
        var url = 'project/loadFilesToDatabank';

        BackendService.getPromise(url, data, 'POST').then(function (result) {
            if (result.data.success) {
                console.log("Files loaded.");
                deferred.resolve({
                    success: true,
                    rows: result.data.rows
                });
            }
            else {
                deferred.resolve({
                    success: false,
                    error: result.data.error
                });
                console.log("Files not loaded. Error: " + result.data.error);
            }
        });

        return deferred.promise;
    }

    /**
     * Saves selected reports 
     */
    this.saveReports = function (filePath, strategies) {
        var deferred = $q.defer();
        var data = {
            projectName: AppService.getProject(),
            databankName: AppService.getDatabank().title,
            strategies: strategies,
            filePath: filePath
        }
        var url = 'project/saveReports';

        BackendService.getPromise(url, data, 'POST').then(function (result) {
            if (result.data.success) {
                console.log(result.data.success);
                deferred.resolve({
                    success: true,
                    rows: result.data.rows
                });
            }
            else if (result.data.error) {
                deferred.resolve({
                    success: false,
                    error: result.data.error
                });
                console.log("Reports not saved. Error: " + result.data.error);
            }
        });

        return deferred.promise;
    }

    /**
     * Removes selected reports from the databank
     */
    this.removeReports = function (strategies) {
        var deferred = $q.defer();
        var data = {
            projectName: AppService.getProject(),
            databankName: AppService.getDatabank().title,
            strategies: strategies
        }
        var url = 'project/removeReports';

        BackendService.getPromise(url, data, 'POST').then(function (result) {
            if (result.data.success) {
                if (selectedStrategyDeleted(strategies)) {
                    console.log("Selected strategy deleted. ", selectedStrategy.strategyName);
                }

                deferred.resolve({
                    success: true
                });
            }
            else {
                deferred.resolve({
                    success: false,
                    error: result.data.error
                });

                deleteSelectedStrategy();
            }
        });

        return deferred.promise;
    }

    /**
     * Creates portfolio from selected strategies
     */
    this.createPortfolio = function (strategies, initialBalanceType) {
        var deferred = $q.defer();
        var data = {
            projectName: AppService.getProject(),
            databankName: AppService.getDatabank().title,
            strategies: strategies,
            initialBalanceType: initialBalanceType
        }
        var url = 'project/createPortfolio';

        BackendService.getPromise(url, data, 'POST').then(function (result) {
            if (result.data.success) {
                deferred.resolve({
                    success: true
                });
            }
            else {
                deferred.resolve({
                    success: false,
                    error: result.data.error
                });
            }
        });

        return deferred.promise;
    }

    this.mergeStrategies = function (strategies, data) {
        var deferred = $q.defer();

        data.projectName = AppService.getProject();
        data.databankName = AppService.getDatabank().title;
        data.strategies = strategies;

        var url = 'project/mergeStrategies';

        BackendService.getPromise(url, data, 'POST').then(function (result) {
            if (result.data.success) {
                deferred.resolve({
                    success: true
                });
            }
            else {
                deferred.resolve({
                    success: false,
                    error: result.data.error
                });
            }
        });

        return deferred.promise;
    }

    this.splitStrategies = function (strategies) {
        var deferred = $q.defer();

        var data = {};
        data.projectName = AppService.getProject();
        data.databankName = AppService.getDatabank().title;
        data.strategies = strategies;

        var url = 'project/splitStrategies';

        BackendService.getPromise(url, data, 'POST').then(function (result) {
            if (result.data.success) {
                deferred.resolve({
                    success: true
                });
            }
            else {
                deferred.resolve({
                    success: false,
                    error: result.data.error
                });
            }
        });

        return deferred.promise;
    }

    this.moveToPortfolioMaster = function (strategies) {
        var deferred = $q.defer();

        var data = {};
        data.projectName = AppService.getProject();
        data.databankName = AppService.getDatabank().title;
        data.strategies = strategies;

        var url = 'project/moveStrategiesToPM';

        BackendService.getPromise(url, data, 'POST').then(function (result) {
            if (result.data.success) {
                deferred.resolve({
                    success: true
                });
            }
            else {
                deferred.resolve({
                    success: false,
                    error: result.data.error
                });
            }
        });

        return deferred.promise;
    }

    this.moveToPortfolioComposer = function (strategies) {
        var deferred = $q.defer();

        var data = {};
        data.projectName = AppService.getProject();
        data.databankName = AppService.getDatabank().title;
        data.strategies = strategies;

        var url = 'project/moveStrategiesToPC';

        BackendService.getPromise(url, data, 'POST').then(function (result) {
            if (result.data.success) {
                deferred.resolve({
                    success: true
                });
            }
            else {
                deferred.resolve({
                    success: false,
                    error: result.data.error
                });
            }
        });

        return deferred.promise;
    }

    this.mergeWFStrategies = function (strategies) {
        var deferred = $q.defer();

        var data = {};
        data.projectName = AppService.getProject();
        data.databankName = AppService.getDatabank().title;
        data.strategies = strategies;

        var url = 'project/mergeWFStrategies';

        BackendService.getPromise(url, data, 'POST').then(function (result) {
            if (result.data.success) {
                deferred.resolve({
                    success: true
                });
            }
            else {
                deferred.resolve({
                    success: false,
                    error: result.data.error
                });
            }
        });

        return deferred.promise;
    }
    
    /**
     * Removes all reports from the databank
     */
    this.removeAllReports = function (projectName, databankName) {
        var deferred = $q.defer();
        var data = {
            projectName: AppService.getProject(),
            databankName: AppService.getDatabank().title
        }
        var url = 'project/removeAllReports';

        BackendService.getPromise(url, data).then(function (result) {
            if (result.data.success) {
                if (selectedStrategyDeleted('ALL')) {
                    console.log("Selected strategy deleted. ", selectedStrategy.strategyName);

                    deleteSelectedStrategy();
                }

                deferred.resolve({
                    success: true
                });
            }
            else {
                deferred.resolve({
                    success: false,
                    error: result.data.error
                });
            }
        });

        return deferred.promise;
    }

    this.cutStrategies = function (strategies) {
        var data = {
            projectName: AppService.getProject(),
            databankName: AppService.getDatabank().title,
            strategies: strategies
        }

        BackendService.sendRequest("project/cutStrategies", data, function (result) {
            $rootScope.showSuccess(result.success);
        }, 'POST');
    }

    this.copyStrategies = function (strategies) {
        var data = {
            projectName: AppService.getProject(),
            databankName: AppService.getDatabank().title,
            strategies: strategies
        }

        BackendService.sendRequest("project/copyStrategies", data, function (result) {
            $rootScope.showSuccess(result.success);
        }, 'POST');
    }

    this.pasteStrategies = function () {
        var data = {
            projectName: AppService.getProject(),
            databankName: AppService.getDatabank().title
        }

        BackendService.sendRequest("project/pasteStrategies", data, function (result) {
            $rootScope.showSuccess(result.success);
        }, 'POST');
    }

    this.clearAllDatabanks = function () {
        var data = {
            projectName: AppService.getProject()
        };

        BackendService.sendRequest("project/clearAllDatabanks", data, function (result) {
            deleteSelectedStrategy();
            $rootScope.showSuccess(L.tsq("Databanks cleared"));
        });
    }

    function deleteSelectedStrategy() {
        dataItems.length = 0;

        selectedStrategy = {
            projectName: AppService.getProject(),
            databankName: AppService.getDatabank().title,
            strategyName: null,
            resultInfo: SQConstants.NO_RESULT_CHOSEN,
            isPortfolio: false,
            taskType: window.appConfig.task.type,
            taskName: window.appConfig.task.name,
            product: window.appConfig.product,
            isTaskManager: window.appConfig.product == 'TASKMANAGER',
            dataItems: dataItems,
            noResultItems: []
        };

        SQEvents.notifyListeners(SQEvents.get('STRATEGY_SELECTED'), null);
        AppService.sendActionRequest('RESULTS', 'showResults', selectedStrategy);
    }

    function selectedStrategyDeleted(deletedStrategies) {
        var projectName = AppService.getProject();
        var databankName = AppService.getDatabank().title;

        if (!selectedStrategy || !deletedStrategies) {
            return false;
        }

        if (deletedStrategies == 'ALL') {
            if (selectedStrategy.projectName == projectName && selectedStrategy.databankName == databankName) {
                return true;
            }
        } else {
            deletedStrategies = ',' + deletedStrategies + ',';

            if (selectedStrategy.projectName == projectName && selectedStrategy.databankName == databankName && deletedStrategies.includes(',' + selectedStrategy.strategyName + ',')) {
                return true;
            }
        }

        return false;
    }

    this.setDatabankColumnValue = function (strategyName, column, value) {
        var data = {
            projectName: AppService.getProject(),
            databankName: AppService.getDatabank().title,
            strategyName: strategyName,
            column: column,
            value: value
        };

        BackendService.sendRequest('project/setDatabankColumnValue', data);
    }

    this.selectStrategy = function (strategyName, databankName) {
        var db = databankName || AppService.getDatabank().title;
        
        if (!strategyName) {
            deleteSelectedStrategy();
        }
        else {
            instance.loadDataItems(strategyName, db, function (data) {
                if (data.strDoesntExist) {
                    return;
                }

                selectedStrategy = {
                    projectName: AppService.getProject(),
                    databankName: db,
                    strategyName: strategyName,
                    isPortfolio: data.isPortfolio,
                    resultInfo: data.resultInfo,
                    taskType: window.appConfig.task.type,
                    taskName: window.appConfig.task.name,
                    product: window.appConfig.product,
                    isTaskManager: window.appConfig.product == 'TASKMANAGER',
                    dataItems: data.dataItems,
                    resultTypes: data.resultTypes,
                    noResultItems: data.noResultItems
                };

                AppService.sendActionRequest('RESULTS', 'showResults', selectedStrategy);
            });
        }
    }

    this.loadDataItems = function (strategyName, databankName, callback) {
        var data = {
            projectName: AppService.getProject(),
            databankName: databankName,
            reportName: strategyName
        }
        var url = 'project/getDataItems';

        BackendService.getPromise(url, data).then(function (result) {
            if (result.data.success && result.data.dataItems) {
                dataItems = result.data.dataItems;
            } else if (result.data.error) {
                $rootScope.showError(result.data.error);
            }

            if (callback) {
                callback(result.data);
            }
        });
    }

    this.getSelectedStrategy = function () {
        return selectedStrategy;
    }

    this.getDataItems = function () {
        return dataItems;
    }

    this.getDatabanks = function (projectName) {
        return instance.databanks;
    }

    this.getGridData = function (url) {
        return BackendService.getPromise(url);
    }

    this.loadGridData = function (databankName, first1000, refreshAction, gridLastChangeTime, callback) {
        var args = {
            projectName: AppService.getProject(),
            databankName: databankName,
            first1000: first1000,
            lastChangeTime: gridLastChangeTime,
            refreshAction: refreshAction
        }
        BackendService.sendRequest('project/loadGridData', args, callback, 'POST');
    }

    this.changeMaxRecords = function (projectName, databankName, maxRecords) {
        var data = {
            projectName: projectName,
            databankName: databankName,
            maxRecords: maxRecords
        }

        return BackendService.getPromise('project/changeMaxRecords', data);
    }

    this.changeLimitRecords = function (projectName, databankName, limitRecords) {
        var data = {
            projectName: projectName,
            databankName: databankName,
            limitRecords: limitRecords
        }

        return BackendService.getPromise('project/changeLimitRecords', data);
    }

    this.cancelRecordsSaving = function () {
        return BackendService.getPromise('project/cancelRecordsSaving', {});
    }

    this.cancelRecordsDeleting = function () {
        return BackendService.getPromise('project/cancelRecordsDeleting', {});
    }

    this.cancelRecordsLoading = function () {
        return BackendService.getPromise('project/cancelRecordsLoading', {});
    }

    this.changeRankColumn = function (projectName, databankName, columnTitle) {
        var deferred = $q.defer();
        var data = {
            projectName: projectName,
            databankName: databankName,
            columnTitle: columnTitle
        }

        BackendService.sendRequest('project/changeRankColumn', data, function (result) {
            if (!result.success) {
                $rootScope.showError(result.error);

                if (result.usedColumn && result.usedColumn != column) {
                    deferred.resolve({ success: false, usedColumn: result.usedColumn });
                }
                else {
                    deferred.resolve({ success: false });
                }
            }
            else {
                deferred.resolve({ success: true });
            }
        });
        return deferred.promise;
    }

    this.getTaskInputDatabank = function () {
        var resultsDbName = SQConstants.getConstants().gedatabanks.results;

        if (!AppService.getTask() || !AppService.getTask().name) return resultsDbName;

        var databanksObj = AppService.getCurrentTaskTabSettings("Databanks");
        var databankObjs = getChildElements(databanksObj, 'Databank');

        var inputDBElem = findDatabankByName(databankObjs, "Input");
        if (!inputDBElem) {
            console.error("Input databank not found for task '" + AppService.getTask().name + "'. Using Results...");
            return resultsDbName;
        }
        else {
            var name = getAttrStringValue(inputDBElem, 'value', resultsDbName);
            return name && name != 'null' ? name : resultsDbName;
        }
    }

    this.setTaskInputDatabank = function (databankName) {
        var databanksObj = AppService.getCurrentTaskTabSettings("Databanks");
        var databankObjs = getChildElements(databanksObj, 'Databank');

        var inputDBElem = findDatabankByName(databankObjs, "Input", true);
        if (!inputDBElem) {
            inputDBElem = createChild(databanksObj, 'Databank', AppService.xmlDoc, true);
            inputDBElem.setAttribute("label", "Input databank");
            inputDBElem.setAttribute("name", "Input");
        }
        inputDBElem.setAttribute("value", databankName);
    }

    this.getTaskOutputDatabank = function () {
        var resultsDbName = SQConstants.getConstants().gedatabanks.results;

        if (!AppService.getTask() || !AppService.getTask().name) return resultsDbName;

        var databanksObj = AppService.getCurrentTaskTabSettings("Databanks");
        var databankObjs = getChildElements(databanksObj, 'Databank');

        var outputDBElem = findDatabankByName(databankObjs, "Output");
        if (!outputDBElem) {
            console.error("Output databank not found for task '" + AppService.getTask().name + "'. Using Results...");
            return resultsDbName;
        }
        else {
            var name = getAttrStringValue(outputDBElem, 'value', resultsDbName);
            return name && name != 'null' ? name : resultsDbName;
        }
    }

    this.setTaskOutputDatabank = function (databankName) {
        var databanksObj = AppService.getCurrentTaskTabSettings("Databanks");
        var databankObjs = getChildElements(databanksObj, 'Databank');

        var outputDBElem = findDatabankByName(databankObjs, "Output", true);
        if (!outputDBElem) {
            outputDBElem = createChild(databanksObj, 'Databank', AppService.xmlDoc, true);
            outputDBElem.setAttribute("label", "Output databank");
            outputDBElem.setAttribute("name", "Output");
        }
        outputDBElem.setAttribute("value", databankName);
    }

    this.resetTaskDatabanks = function () {
        var databanksObj = AppService.getCurrentTaskTabSettings("Databanks");
        removeAllChildren(databanksObj);

        var outputDBElem = createChild(databanksObj, 'Databank', AppService.xmlDoc, true);
        outputDBElem.setAttribute("label", "Output databank");
        outputDBElem.setAttribute("name", "Output");
        outputDBElem.setAttribute("value", SQConstants.getConstants().gedatabanks.results);
    }

    function findDatabankByName(databanks, name, removeOthers) {
        if (removeOthers) {
            var firstFound = null;

            for (let i = databanks.length - 1; i >= 0; i--) {
                let dbName = getAttrValue(databanks[i], "name", null);
                if (dbName == name) {
                    if (!firstFound) {
                        firstFound = databanks[i];
                    }
                    else {
                        databanks[i].parentNode.removeChild(databanks[i]);
                    }
                }
            }

            return firstFound;
        }
        else {
            for (let i = 0; i < databanks.length; i++) {
                let dbName = getAttrValue(databanks[i], "name", null);
                if (dbName == name) {
                    return databanks[i];
                }
            }
        }
    }

    this.saveConfig = function (databanks, dontSendRequest) {
        var xmlObj = AppService.xmlConfig;
        var xmlDoc = AppService.xmlDoc;

        var projectElem = xmlObj.find('Project')[0];

        var databanksElem = getChildElement(projectElem, 'Databanks');
        if (databanksElem) {
            removeAllChildren(databanksElem);
        }
        else {
            databanksElem = createChild(projectElem, 'Databanks', xmlDoc, true);
        }

        for (var i = 0; i < databanks.length; i++) {
            var databank = databanks[i];
            var databankElem = createChild(databanksElem, 'Databank', xmlDoc, true);

            databankElem.setAttribute("name", databank.title);
            databankElem.setAttribute("view", databank.view);
            databankElem.setAttribute("syncType", databank.syncType);
            databankElem.setAttribute("position", databank.position);
        }

        if (!dontSendRequest) {
            AppService.updateProjectXML();
        }
    }

    this.getRetestSelected = function () {
        var databanksObj = AppService.getCurrentTaskTabSettings("Databanks");
        retestOnlySelected = getAttrBooleanValue(databanksObj, "retestSelected", false);
        return retestOnlySelected;
    }

    this.setRetestSelected = function (value) {
        var databanksObj = AppService.getCurrentTaskTabSettings("Databanks");
        databanksObj.setAttribute("retestSelected", "" + value);
        retestOnlySelected = value;
    }

    this.performCustomAnalysis = function(data) { 
        var deferred = $q.defer();

        BackendService.sendRequest('project/runCustomAnalysis', data, function (result) {
            if (!result.success) {
                $rootScope.showError(result.error);
                deferred.resolve({ success: false });
            } else {
                deferred.resolve({ success: true });
            }
        });
        return deferred.promise;
    }

    this.setNote = function(data) { 
        var deferred = $q.defer();

        BackendService.sendRequest('project/setNote', data, function (result) {
            if (!result.success) {
                $rootScope.showError(result.error);
                deferred.resolve({ success: false });
            } else {
                deferred.resolve({ success: true });
            }
        }, 'POST');
        return deferred.promise;
    }

    this.getStrategiesToRetest = function () {
        var inputDatabank = instance.getTaskInputDatabank();

        for (var i = 0; i < instance.databankTabs.tabs.length; i++) {
            var databankTab = instance.databankTabs.tabs[i];

            if (databankTab.title == inputDatabank) {
                try {
                    return databankTab.getSelectedStrategies(true).split(",");
                }
                catch (err) {
                    $rootScope.showError(L.tsq("Cannot create list of selected strategies - '%s'", [err]));
                    return null;
                }
            }
        }

        $rootScope.showError(L.tsq("Cannot create list of selected strategies - databank '%s' not found", [inputDatabank]));
        return null;
    }

    function onEvent(event, data) {
        if (event == SQEvents.get("DATABANK_DISPLAY")) {
            let databank = getItem(instance.databanks, 'name', data.name);
            if (databank) {
                databank.display = data.show;
            }

            $rootScope.$evalAsync();
        }
        else if (event == SQEvents.get("APP_PROJECT_CHANGED")) {
            if (window.appConfig.product != "TASKMANAGER") return;

            instance.projectName = data;

            if (data != null || !isMainApp()) {
                loadDatabanks();
            }
        }
        else if (event == SQEvents.get("RELOAD_DATABANKS")) {
            if (!data || data.action != "setProject") {
                loadDatabanks();
            }
        }
        else if (event == SQEvents.get("TASK_ADDED")) {
            loadDatabanks();
        }
        else if (event == SQEvents.get("DATABANK_UPDATED")) {
            if (data.databanks && data.databanks.length) {
                for (let a = 0; a < data.databanks.length; a++) {
                    let db = data.databanks[a];
                    let databank = getItem(instance.databanks, 'name', db.name);

                    if (db.records && databank) {
                        databank.records = db.records;
                    }
                }
            }
        }
    }

    var instance = this;
    var dataItems = [];
    var selectedStrategy;
    var databanksPromise;
    var retestOnlySelected;

    this.databankTabs = {
        tabs: []
    };

    this.selectedStrategies = '';

    this.projectName = AppService.getProject();
    this.databanks = [];

    SQEvents.addListener("DatabankService", [
        SQEvents.get("DATABANK_DISPLAY"),
        SQEvents.get("APP_PROJECT_CHANGED"),
        SQEvents.get('RELOAD_DATABANKS'),
        SQEvents.get('DATABANK_UPDATED'),
        SQEvents.get('TASK_ADDED')
    ], onEvent);

});