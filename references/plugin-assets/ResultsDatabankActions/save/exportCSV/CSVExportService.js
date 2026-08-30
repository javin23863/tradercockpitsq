angular.module('app').service('CSVExportService', function($rootScope, BackendService, AppService, L) {
    
    this.export = function(path, useComma, strategies){
        var config = {
            projectName : AppService.getProject(),
            databankName : AppService.getDatabank().title,
            path : path,
            strategies : strategies,
            useComma : useComma
        };

        BackendService.sendRequest('resultsDatabankActions/exportDatabankToCSV', config, null, 'POST');
    }

});