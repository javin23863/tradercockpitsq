angular.module('app.algowizard').service('AlgoWizardService', function ($rootScope, $injector, BackendService, SQConstants, SQEvents, AppService, L) {
    console.log("Initializing AlgoWizardService");  
    //- Initialization --------------------------------------------------------------

    var instance = this;

    window.top.awEditorInterface = window.top.awEditorInterface || {};
    
    instance.initEditor = async function() {
        console.log("Initializing AlgoWizard editor");  

        let constants = SQConstants.getConstants();
        let awConfig = await BackendService.httpRequest('/algowizard/getConfig', null);
        
        let ws = await BackendService.httpRequest('/main/getWebSocketPort', null);
        let webSocketUrl = "ws://" + window.location.hostname + ":" + ws.port + "/websocket/updates";

        let customIndicators = await BackendService.httpRequest('/algowizard/customDataIndys', null);

        console.log("AlgoWizard editor websocket url", webSocketUrl);

        await this.loadExamples(function(result){
            if(result){
                initEditorInterface(constants, awConfig, customIndicators.xml, webSocketUrl, result);
            } else {
                initEditorInterface(constants, awConfig, customIndicators.xml, webSocketUrl, []);
            }
        })

        // initEditorInterface(constants, awConfig, customIndicators.xml, webSocketUrl, examples);
    }

    instance.waitForInternalEventHandlerAttached = function(){
        return new Promise(resolve => {
            const intervalHandle = setInterval(() => {
                if(window.top.awEditorInterface.internalEventHandler){
                    clearInterval(intervalHandle);
                    resolve();
                }
            }, 200);
        });
    }

    function initEditorInterface(constants, awConfig, customIndicators, webSocketUrl, examples) {
        let brokers = constants.brokers || [];
        brokers.unshift(SQConstants.getConstants().BrokerNoFilter);

        let symbols = instance.getSymbols();
        let instruments = SQConstants.getConstants().instruments;

        window.top.awEditorInterface.config = {
            platformType: "SQ",
            webSocketUrl: webSocketUrl,
            wizardConfig: awConfig.wizardConfig,
            initialStrategyConfig: awConfig.initialStrategyConfig,
            moneyManagementMethods: constants.mmMethods,
            tradingOptions: constants.tradingOptions,
            timeframes: awConfig.timeframes,
            skins: awConfig.skins,
            symbols: symbols,
            instruments: instruments,
            precisions: constants.precisions,
            engines: constants.engines,
            directions: constants.directions,
            atmTypes: constants.atmTypes,
            atmPositionSizes: constants.atmPositionSizes,
            atmExitLevels: constants.atmExitLevels,
            atmMoveSL2BETypes: constants.atmMoveSL2BETypes,
            commissionMethods: constants.commissionMethods,
            customIndicators: customIndicators,
            sessions: constants.sessions || [],
            recentFiles: awConfig.recentFiles,
            tagCloud: awConfig.blocksTagCloud,
            zoom: awConfig.zoom,
            brokers: brokers,
            examples: examples || [],
            development: window.top.developmentMode,
            aiChatBackendUrls: awConfig.aiChatBackendUrls || undefined,
            volumeProfile: awConfig.volumeProfile || undefined
        };
    
        window.top.awEditorInterface.externalEventHandler = awActionHandler;
    }

    /*
    * internalEventHandler
    */
    instance.editorAction = (actionName = null, params = {}, callback) => {
        instance.waitForInternalEventHandlerAttached().then(() => {
            if(!window.top.awEditorInterface){
                console.error("AW editor interface not set");
                return;
            }
        
            if(!window.top.awEditorInterface.internalEventHandler){
                console.error("Internal event handler in AW editor interface not set");
                return;
            }
        
            let result = window.top.awEditorInterface.internalEventHandler(actionName, params);
            if(callback){
                callback(result);
            }
        });
    }

    /*
    * externalEventHandler
    */
    const awActionHandler = async(actionName, args) => {
        console.info("Editor action called (" + actionName + ")", args);

        switch(actionName) {
            case "loadFromFile": return onLoadFromFile(args);
            case "runBacktest": return onRunBacktest(args);
            case "stopBacktest": return onStopBacktest(args);
            case "showSourceCode": return onShowSourceCode(args);
            case "showBacktestResults": return onShowBacktestResults(args);
            case "goToExamples": return onGoToExamples(args);
            case "loadResultsData": return onLoadResultsData(args);
            case "loadMiniResultsContent": return onLoadMiniResultsContent(args);
            case "performNegation": return onPerformNegation(args);
            case "showNotification": return onShowNotification(args);
            case "saveToRetester": return onSaveToRetester(args);
            case "saveToFile": return onSaveToFile(args);
            case "saveCustomBlocks": return onSaveCustomBlocks(args);
            case "saveBlockGroups": return onSaveBlockGroups(args);
            case "loadCustomBlocks": return onLoadCustomBlocks(args);
            case "loadBlockGroups": return onLoadBlockGroups(args);
            case "negateCustomBlock": return onNegateCustomBlock(args);
            case "saveCustomFile": return onSaveCustomFile(args);
            case "loadFromRecentFile": return loadFromRecentFile(args);
            case "backToEditor": return onBackToEditor();
            case "saveTagCloud": return onSaveTagCloud(args);
            case "loadExample": return onLoadExamples(args);
            case "loadCustomFile": return loadCustomFile(args);
            case "saveTF": return onSaveTF(args);
            case "openLink": return onOpenLink(args);
            case "checkCustomResources": return onCheckCustomResources(args);
            case "resolveCustomResources": return onResolveCustomResources(args);
            case "loadingFalse": return onLoadingFalse(args);
            case "getSourceCode": return getSourceCode(args);
            case "callEndpoint": return callEndpoint(args);
            case "showVolumeProfileDialog": return onShowVolumeProfileDialog();

            default:
                console.error("Unknown AW editor action " + actionName);
                return { error: "Unknown AW editor action " + actionName };
        }
    }

    const getSourceCode = async(args) => {
        return BackendService.httpRequest("/sourcecode/print", args, "POST");
    }

    const callEndpoint = async(args) => {
        return BackendService.httpRequest(args.url, args.data, args.method || "GET");
    }

    const onShowVolumeProfileDialog = async() => {
        window.parent.showPopup('#volumeProfileAddonPopup');
    }

    const onLoadingFalse = async(args) => {
        //ignore
        return;
    }

    const onResolveCustomResources = async(args) =>{
        let result = await BackendService.httpRequest("/algowizard/resolveResources", args, "POST");
        if(result && result.success){
            return result;
        }
        return result;
    }

    const onCheckCustomResources = async(args) =>{
        let result = await BackendService.httpRequest("/algowizard/checkResources", args, "POST");
        if(result && result.success){
            return result;
        }
        return result;
    }

    const onOpenLink = async(args) =>{
        AppService.openBrowser(args.link);
    }

    const onSaveTF = async(args) =>{
        let result = await BackendService.httpRequest("/algowizard/addNewTimeframe", args, "POST");
        if(result && result.success){
            return result;
        }
        return result;
    }

    const onLoadExamples = async(args) =>{
        let params = {
            fileName: args
        }
        await this.loadExample(params, function(result){
            if(result && result.success){
                instance.editorAction("loadStrategy", {
                    strategyXML: result.sourceCode,
                    lastSettings: result.lastSettingsXML,
                    lastSettingsXML: result.lastSettingsXML,
                    fileName: result.fileName
                });
            }
        })
    }

    const onSaveTagCloud = async(args) =>{
        if(args){
            let result = await BackendService.httpRequest("/algowizard/saveBlockToTagCloud", args, "POST");
            if(result && result.success){
                return result;
            }
            return result;
        }
    }

    const loadFromRecentFile = async(args) => {
        let jsonData = {
            filePath: args.filePath,
        };
        let result = await BackendService.httpRequest("/algowizard/loadFile", jsonData, "POST");
        if(result){
            if(result.success) {
                result = {
                    "success": result.success,
                    "strategyXML":  result.strategyConfig,
                    "strategyName": result.strategyName,
                    "recentFiles" : result.recentFiles,
                    "lastSettings" : result.lastSettings,
                    "lastSettingsXML" : result.lastSettings,
                }

                this.showSuccess(result.success);
            }
        }
        return result;
    }

    const onSaveCustomFile = async(args) => { 
        /*
        //this is how args should look like
        var args = {
            actionType: "awExportBlockGroups",
            fileName: "BlockGroupsExport.xml",
            fileContent: "string"
            extension: {
                name: "xml",
                description: "XML Settings Files",
            },
        };*/

        await this.saveFile(args);
    }

    const onSaveCustomBlocks = async(args) => { 
        let data = {
            customBlocksXML: args.customBlocksXML
        }

        await BackendService.httpRequest("/algowizard/saveCustomBlocks", data, "POST");
    }

    const onNegateCustomBlock = async(args) => {
        let data = {
            blockXML: args.block
        }
        let result = await BackendService.httpRequest("/algowizard/negateCustomBlock", data, "POST");
        if(result.error){
            return;
        }

        return result;
    }

    const onSaveBlockGroups = async(args) => { 
        let data = {
            blockGroupsXML: args.blockGroupsXML
        }

        await BackendService.httpRequest("/algowizard/saveBlockGroups", data, "POST");
    }

    const onLoadCustomBlocks = async(args) => { 
        let result = await BackendService.httpRequest("/algowizard/loadCustomBlocks", null, "POST");
        if(result.success) {
            this.showSuccess(result.success);

            instance.editorAction("loadCustomBlocks", {customBlocksXML: result.customBlocksXML});
        }
    }
    
    const onLoadBlockGroups = async(args) => { 
        let result = await BackendService.httpRequest("/algowizard/loadBlockGroups", null, "POST");
        if(result.success) {
            this.showSuccess(result.success);

            instance.editorAction("loadBlockGroups", {blockGroupsXML: result.blockGroupsXML});
        }
    }

    const onBackToEditor = async()=>{
        awShowResults(false, $injector);
    }

    const onGoToExamples = async(args) => { 
        window.parent.showPopup('#awExamplesDialog');
        awShowResults(false, $injector);
    }
    
    const onShowSourceCode = async(args) => { 
        awShowResults(true, $injector);

        let resultsWindow = getResultsWindow();
        if(resultsWindow) resultsWindow.setStrategyXML(args.strategyXML, args.settingsXML);

        $(
            "#overviewTabs .tabs-header > div[data-item='sourceCode']",
            window.parent.document.getElementById("iframe_results_overlayed")
            .contentWindow.document
        ).click();
    }

    const onSaveToFile = async(args) => {  
        let data = {
            actionType: "awSaveFile",
            showFiles: true,
            multiSelectionEnabled: false,
            currentDirectory: args.filePath,
            fileName: args.strategyName,
            extension: {
                name: "sqx",
                description: "Strategy File (SQX)",
            },
        };

        let path = !args.showStrategyNameDialog && args.filePath ? args.filePath :  await this.chooseFileToSave(data);
        if(!path) return;

        data = {
            chooseFolder: false,
            strategyConfig: args.strategyXML,
            lastSettings: args.settingsXML,
            backtested: !!args.backtestID,
            strategyName: args.strategyName,
            filePath: path
        }

        if(args.backtestID) data.backtestID = args.backtestID;

        let result = await BackendService.httpRequest("/algowizard/saveFile", data, "POST");
        if(result.success) {
            this.showSuccess(result.success);
        }

        return result;
    }

    const onSaveToRetester = async(args) => { 
        let data = {
            strategyConfig: args.strategyXML,
            lastSettings: args.settingsXML,
            strategyName: args.strategyName,
            backtested: !!args.backtestID,
        }

        if(args.backtestID) data.backtestID = args.backtestID;

        let result = await BackendService.httpRequest("/algowizard/saveToRetester", data, "POST");
        if(result.success) {
            setTimeout(function() {
                top.switchToApp("RETESTER");
                window.dispatchEvent(new Event("resize"));
            }, 200);
        }
    }
    
    const onShowNotification = async(args) => { 
        console.log("notification", args.type, args.message);

        window.top.sqNotices.addNotice(args.type, args.message);
    }

    const loadCustomFile = async(args) =>{
        let data = {
            actionType: args.actionType,
            showFiles: true,
            multiSelectionEnabled: false,
            currentDirectory: "",
            extension: {
                name: "xml",
                description: "XML Settings Files",
            },
        };

        let path = await this.chooseFile(data);
        if(!path) return;

        data = {
            filePath: path,
        };

        result = await BackendService.httpRequest("/main/loadFile", data, "POST");
        if(result.success) {
            let content = result.fileContent;

            result = {
                success: result.success,
                actionType: args.actionType,
                content: content
            }

            this.showSuccess(result.success);
        }

        return result;
    }

    const onLoadFromFile = async(args) => { 
        let data = {
            actionType: "loadStrategy",
            showFiles: true,
            multiSelectionEnabled: false,
            currentDirectory: "",
            extension: {
                name: "sq4,sqw,str,sqx",
                description: "SQX Strategy Files",
            },
        };

        let path = await this.chooseFile(data);
        if(!path) return;

        data = {
            filePath: path,
        };

        result = await BackendService.httpRequest("algowizard/loadFile", data, "POST");
        if(result.success) {
            result = {
                "success": result.success,
                "strategyXML":  result.strategyConfig,
                "strategyName": result.strategyName,
                "recentFiles" : result.recentFiles,
                "lastSettings" : result.lastSettings
            }

            this.showSuccess(result.success);
        }

        return result;
    }

    const onRunBacktest = async(args) => { 
        args.strategyXML = args.strategyXML.replace(/ /g, "___").replace(/xmlns="[a-zA-Z0-9:;/\.\s\(\)\-\,]*"/gi, "");
        args.settings = args.settings.replace(/ /g, "___").replace(/xmlns="[a-zA-Z0-9:;/\.\s\(\)\-\,]*"/gi, "");

        return BackendService.httpRequest("/algowizard/backtest", args, "POST");
    }

    const onStopBacktest = async(args) => { 
        return BackendService.httpRequest("/algowizard/stopBacktest", args, "POST");
    }

    const onShowBacktestResults = async(args) => { 
        awShowResults(true, $injector);
    }

    const onLoadResultsData = async(args) => {
        console.info("Loading results data...");

        try {
            args.resultsParams = args.resultsParams || { backtestID: args.backtestID, resultKey: "*" };
            if(args.backtestID && !args.resultsParams.backtestID)  args.resultsParams.backtestID = args.backtestID;
            
            //set/reset strategy xml for displaying source code
            let resultsWindow = getResultsWindow();
            if(resultsWindow) resultsWindow.setStrategyXML(args.strategyXML, args.settingsXML);

            if(!args.resultsParams.backtestID) { 
                //strategy without backtest, reset results tab
                console.log("Resetting results...")
                AppService.sendActionRequest('RESULTS', 'showResults', null);
            } else {
                console.log("Applying results...", args.resultsParams);
                AppService.sendActionRequest('RESULTS', 'showResults', args.resultsParams);
            }
        }
        catch(err){
            console.error("Unable to load Results data", err);
            $rootScope.showError("Unable to load Results data - " + err.message);
        }
    }

    const onPerformNegation = async(args) => {
        //args.strategyXML = args.strategyXML.replace(/ /g, "___").replace(/xmlns="[a-zA-Z0-9:;/\.\s\(\)\-\,]*"/gi, "");
        args.strategyXML = args.strategyXML.replace(/xmlns="[a-zA-Z0-9:;/\.\s\(\)\-\,]*"/gi, "");

        let result = await BackendService.httpRequest("/algowizard/negate", args, "POST");
        if(result.error){
            return;
        }

        if(args.callback) {
            args.callback(result.strategyXML);
        }

        return result.strategyXML;
    }

    this.getSymbols = function() {
        let constants = SQConstants.getConstants();
        if(!constants || !constants.data) return [];

        let symbols = constants.data.filter(item=>{
            if((item.symbol.startsWith("[") && item.readyToUse) || !item.symbol.startsWith("[")){
                return item;
            }
        });

        symbols = constants.data.filter(item=>{
            if(item.rows > 0 && item.show){
                return item;
            }
        });

        return symbols;
    }


    this.loadExamples = function(callOnSuccess) {
        BackendService.sendRequest('/AlgoWizard/examples/examples.json', null, callOnSuccess, 'GET', true, false, true);
    } 

    this.loadExample = function(data, callOnSuccess) {
        BackendService.sendRequest('/algowizard/loadExample', data, callOnSuccess, 'GET');
    } 

    // Register only in SQUANT main app (same pattern as AppService) to avoid duplicate onEvent()
    // when multiple iframes each have their own AlgoWizardService instance.
    if (window.appConfig.product == 'SQUANT') {
        SQEvents.addListener('AWEditorIntegration', [SQEvents.get('DATA_CHANGED'), SQEvents.get('CUSTOM_DATA_CHANGED'), SQEvents.get('BROKERS_CHANGED'), SQEvents.get("SESSIONS_CHANGED"), SQEvents.get("CB_CHANGED"), SQEvents.get("RG_CHANGED"), SQEvents.get('LICENSE_CHANGED')], onEvent);
    }

    async function onEvent(event, data) {
        if (event == SQEvents.get('CUSTOM_DATA_CHANGED')) {
            let customIndicators = await BackendService.httpRequest('/algowizard/customDataIndys', null);

            instance.editorAction("setCustomIndicators", {customIndicators : customIndicators.xml});

        } else if(event == SQEvents.get('BROKERS_CHANGED')) {
            let brokers = angular.copy(data);
            brokers.unshift(SQConstants.getConstants().BrokerNoFilter);

            instance.editorAction("setBrokers", {brokers : brokers});

        } else if(event == SQEvents.get('SESSIONS_CHANGED')) {
            var sessions = angular.copy(SQConstants.getConstants().sessions);

            instance.editorAction("setSessions", {sessions : sessions});
        } else if (event == SQEvents.get('DATA_CHANGED')) {

            let symbols = instance.getSymbols();
            instance.editorAction("setSymbols", {symbols : symbols});

        } else if (event == SQEvents.get('INSTRUMENTS_CHANGED')) {
            let instruments = SQConstants.getConstants().instruments;

            instance.editorAction("setInstruments", {instruments : instruments});

        } else if (event == SQEvents.get('CB_CHANGED')) {
            console.log("reload CustomBlocks", data);
            instance.editorAction("loadCustomBlocks", data);

        } else if (event == SQEvents.get('RG_CHANGED')) {
            console.log("reload RandomGroups", data);
            instance.editorAction("loadBlockGroups", data);

        } else if (event == SQEvents.get('LICENSE_CHANGED')) {
            instance.editorAction("setLicenseInfo", {licenseInfo : data, volumeProfile: $rootScope.license.volumeProfile || undefined});
        }
    }

    this.chooseFile = async function(data) {
        return new Promise(resolve => {
            $rootScope.showFilePicker(L.tsq('Select file'), data.actionType, data.showFiles, data.multiSelectionEnabled, data.currentDirectory, data.extension, null, function (paths) {
                let path = paths ? paths[0] : null;
                resolve(path);
            });
        });
    } 

    this.chooseFileToSave = async function(data) {
        return new Promise(resolve => {
            $rootScope.saveFile(L.tsq("Save file"), data.extension, data.actionType, data.fileName, data.fileContent, function(targetPath) {
                window.parent.hidePopup("#saveFileDialog");
                resolve(targetPath);
            });
            
        }); 
    } 

    this.saveFile = async function(data) {
        return new Promise(resolve => {
            $rootScope.saveFile(L.tsq("Save file"), data.extension, data.actionType, data.fileName, data.fileContent);
            resolve(null);
        });
    } 

    this.showSuccess = function(message) {
        window.top.sqNotices.addNotice("success", message);
    }

    this.showError = function(message) {
        window.top.sqNotices.addNotice("error", message);
    }
});