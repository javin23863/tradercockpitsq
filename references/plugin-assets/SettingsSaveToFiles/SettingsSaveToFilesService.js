angular.module('app.settings').service('SettingsSaveToFilesService', function($q, BackendService, AppService, L, DatabankService, SQEvents, SQConstants) {

    function loadAvailableSourceCodeGenerators() {
        var data = InitializationData().results.sourceCode;
        loadToArray(instance.config.generators, data.engineTypes);

        if (data.engineTypes.length > 0 && !instance.config.generator) {
            instance.config.generator = data.engineTypes[0].name;
        }

        var broker = SQConstants.getConstants().broker;
        if (broker) {
            for (var i = instance.config.generators.length - 1; i >= 0; i--) {
                var generator = instance.config.generators[i];

                if (generator.name.indexOf("Pseudo") >= 0) {
                    instance.config.generator = generator.name;
                }
                else if (broker.encryption) {
                    instance.config.generators.splice(i, 1);
                }
            }
        }
    }

    this.loadDatabanks = function() {
        DatabankService.loadVisibleDatabanks(instance.config.availableDatabanks);
    }

    this.saveSettings = function() {
        var xmlDoc = AppService.xmlDoc;
        var saveToFilesObj = AppService.getCurrentTaskTabSettings("SaveToFiles");
        
        addNode('DestDirectorySqx', instance.config.directorySqx, saveToFilesObj, xmlDoc);
        addNode('DestDirectoryStr', instance.config.directoryStr, saveToFilesObj, xmlDoc);
        addNode('DestDirectoryDatabank', instance.config.directoryDatabank, saveToFilesObj, xmlDoc);
        addNode('DestDirectoryTrades', instance.config.directoryTrades, saveToFilesObj, xmlDoc);
        addNode('DestDirectoryHtml', instance.config.directoryHtml, saveToFilesObj, xmlDoc);
        addNode('DestDirectoryPdf', instance.config.directoryPdf, saveToFilesObj, xmlDoc);
        addNode('DestDirectorySC', instance.config.directorySC, saveToFilesObj, xmlDoc);

        addNode('SaveInSqxFormat', instance.config.saveInSqxFormat, saveToFilesObj, xmlDoc);
        addNode('SaveInStrFormat', instance.config.saveInStrFormat, saveToFilesObj, xmlDoc);
        addNode('ExportDatabank', instance.config.exportDatabank, saveToFilesObj, xmlDoc);
        addNode('ExportTrades', instance.config.exportTrades, saveToFilesObj, xmlDoc);
        addNode('SaveInHtmlFormat', instance.config.saveInHtmlFormat, saveToFilesObj, xmlDoc);
        addNode('SaveInPdfFormat', instance.config.saveInPdfFormat, saveToFilesObj, xmlDoc);
        var saveSourceCodeObj = addNode('SaveSourceCode', instance.config.saveSourceCode, saveToFilesObj, xmlDoc);
        saveSourceCodeObj.setAttribute("type", instance.config.generator);
        saveSourceCodeObj.setAttribute("format", instance.config.sourceCodeFormat);

        addNode('OverwriteFiles', instance.config.overwrite, saveToFilesObj, xmlDoc);

        addNode('MNActive', instance.config.handleMagicNumbers, saveToFilesObj, xmlDoc);
        addNode('MNValue', instance.config.startingNumber, saveToFilesObj, xmlDoc);

        addNode('Data', instance.config.data, saveToFilesObj, xmlDoc);
        addNode('Format', instance.config.format, saveToFilesObj, xmlDoc);
        addNode('UseComma', instance.config.useComma, saveToFilesObj, xmlDoc);

        addNode('SetNoteActive', instance.config.setNoteActive, saveToFilesObj, xmlDoc);
        addNode('SetNoteType', instance.config.setNoteType, saveToFilesObj, xmlDoc);
        addNode('SetNoteCustom', instance.config.setNoteCustomText, saveToFilesObj, xmlDoc);

        DatabankService.setTaskInputDatabank(instance.config.databank);
    }
    
    this.loadSettings = function() {
        var xmlDoc = AppService.xmlDoc;

        var saveToFilesObj = AppService.getCurrentTaskTabSettings("SaveToFiles");
        if(!saveToFilesObj) return;

        instance.config.directorySqx = getNodeValue(saveToFilesObj, 'DestDirectorySqx', '');
        instance.config.directoryStr = getNodeValue(saveToFilesObj, 'DestDirectoryStr', '');
        instance.config.directoryDatabank = getNodeValue(saveToFilesObj, 'DestDirectoryDatabank', '');
        instance.config.directoryTrades = getNodeValue(saveToFilesObj, 'DestDirectoryTrades', '');
        instance.config.directoryHtml = getNodeValue(saveToFilesObj, 'DestDirectoryHtml', '');
        instance.config.directoryPdf = getNodeValue(saveToFilesObj, 'DestDirectoryPdf', '');
        instance.config.directorySC = getNodeValue(saveToFilesObj, 'DestDirectorySC', '');

        instance.config.saveInSqxFormat = getNodeBooleanValue(saveToFilesObj, 'SaveInSqxFormat', false);
        instance.config.saveInStrFormat = getNodeBooleanValue(saveToFilesObj, 'SaveInStrFormat', false);
        instance.config.exportDatabank = getNodeBooleanValue(saveToFilesObj, 'ExportDatabank', false);
        instance.config.exportTrades = getNodeBooleanValue(saveToFilesObj, 'ExportTrades', false);
        instance.config.saveInHtmlFormat = getNodeBooleanValue(saveToFilesObj, 'SaveInHtmlFormat', false);
        instance.config.saveInPdfFormat = getNodeBooleanValue(saveToFilesObj, 'SaveInPdfFormat', false);
        instance.config.saveSourceCode = getNodeBooleanValue(saveToFilesObj, 'SaveSourceCode', false);

        var saveSourceCodeObj = getChildElement(saveToFilesObj, "SaveSourceCode");
        instance.config.generator = getAttrStringValue(saveSourceCodeObj, 'type', instance.config.generator);
        instance.config.sourceCodeFormat = getAttrStringValue(saveSourceCodeObj, 'format', instance.config.sourceCodeFormat);

        instance.config.overwrite = getNodeBooleanValue(saveToFilesObj, 'OverwriteFiles', false);

        instance.config.handleMagicNumbers = getNodeBooleanValue(saveToFilesObj, 'MNActive', false);
        instance.config.startingNumber = getNodeIntValue(saveToFilesObj, 'MNValue', 12345);

        instance.config.data = getNodeValue(saveToFilesObj, 'Data', 'all');
        instance.config.format = getNodeValue(saveToFilesObj, 'Format', 'xlsx');
        instance.config.useComma = getNodeBooleanValue(saveToFilesObj, 'UseComma', false);

        instance.config.setNoteActive = getNodeBooleanValue(saveToFilesObj, 'SetNoteActive', false);
        instance.config.setNoteType = getNodeValue(saveToFilesObj, 'SetNoteType', 'timeframe');
        instance.config.setNoteCustomText = getNodeValue(saveToFilesObj, 'SetNoteCustom', '');

        instance.config.databank = DatabankService.getTaskInputDatabank();
    }

    var instance = this;

    this.config = {
        databank: "Results",
        availableDatabanks: [],
        generators: [],
        overwrite: true,

        saveInSqxFormat: false,
        saveInStrFormat: false,
        exportDatabank: false,
        exportTrades: false,
        saveInHtmlFormat: false,
        saveInPdfFormat: false,
        saveSourceCode: false,

        directorySqx: null,
        directoryStr: null,
        directoryDatabank: null,
        directoryTrades: null,
        directoryHtml: null,
        directoryPdf: null,
        directorySC: null,

        handleMagicNumbers : false,
        startingNumber : 12345,

        data : 'all',
        format : 'xlsx',
        useComma : false,

        setNoteActive : false,
        setNoteType : 'timeframe',
        setNoteCustomText : '',

        generator: null,
        sourceCodeFormat: 'el'
    }

    function onEvent(event, data){
        if(event == SQEvents.get("RELOAD_DATABANKS")){
            instance.loadDatabanks();
        }
    }

    SQEvents.addListener("SettingsLoadFromFilesService", [SQEvents.get('RELOAD_DATABANKS')], onEvent);

    loadAvailableSourceCodeGenerators();
});