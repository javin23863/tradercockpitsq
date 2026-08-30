angular.module('app').service('SaveButtonService', function($rootScope, BackendService, AppService, SQConstants) {

    this.onItemClick = function(item) {
        instance.config.extension = item.extension;
        instance.config.action = item.action;
        instance.config.projectName = AppService.getProject();
        instance.config.databankName = AppService.getDatabank().title;

        $(".databank-tab .toolbar .dropdown").removeClass('open');

        window.parent.callAction("saveStrategies", angular.copy(instance.config));
    }

    this.saveStrategies = function(config){
        instance.callback(config);
    }

    this.save = function(config, callback){
        BackendService.sendRequest('project/saveReports', config, callback, 'POST');
    }

    this.saveSingleFileJava = function(config){
        console.log("saveSingleFileJava: ", config);
        
        const dialogConfig = {
            title: "Save file",
            filters: [],
            defaultPath: config.fileName,
            properties: ['createDirectory ']
        };

        var params = angular.copy(config);
        if(params.extensions && params.extensions.length > 0){
            params.extension = params.extensions;
            params.extensions = null;
        }

        if(params.extension) {
            if (typeof params.extension === 'string') {
                dialogConfig.filters.push({ 
                    name: 'Custom File Type', 
                    extensions: params.extension.split(',')
                });
            } else if (Array.isArray(params.extension)) {
                params.extension.forEach(function(ext) {
                    dialogConfig.filters.push({ 
                        name: ext.description, 
                        extensions: ext.name.split(',')
                    });
                });
            } else if (params.extension.description && params.extension.name) {
                dialogConfig.filters.push({ 
                    name: params.extension.description, 
                    extensions: params.extension.name.split(',')
                });
            }
        }

        window.top.electron.openDialog('showSaveDialog', dialogConfig).then(result => {
            console.log("File selected", result)

            if (!result.filePath) {
                return;
            }

            params.filePath = result.filePath;

            BackendService.sendRequest('project/saveSingleFileJava', params, function(result) {
                saveLastFolder(params.pathName, result.folder);
               
                if(result.success && !result.canceled){
                    $rootScope.showSuccess(result.success);
                }   
            }, 'POST');
        });
    }

    function saveLastFolder(actionType, path) {
        if(!path) {
            return;
        }

        BackendService.sendRequest('dirs/setLastFolderUsed', {path: path, actionType: actionType}, function(result) {
            SQConstants.getSettings()[actionType] = result.path;
        });
    }

    $rootScope.saveStrategies = this.saveStrategies;

    var instance = this;

    this.config = {
        prefix : '',
        sufix : '',
        fileName : '',
        extension : '',
        handleMagicNumbers : false,
        startingNumber : 12345,

        exportTradesActive : false,
        data : 'all',
        format : 'xlsx',
        useComma : false,

        setNoteActive : false,
        setNoteType : 'timeframe',
        setNoteCustomText : '',
    };
    
    this.callback = null;

});