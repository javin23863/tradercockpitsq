angular.module('app.resultsdatabankactions.save.csv', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsDatabankAction", 50, {
        title: Ltsq("Save")+":"+Ltsq("Export databank contents"),
        extension : {
            name : "csv",
            description : Ltsq("Comma separated files (.csv)")
        },
        action: 'saveDatabankToCSV',
        controller: function($rootScope, $scope, CSVExportService){

            $scope.onClick = function(item, tab){
                $rootScope.showCSVXLSXExportDialog("DatabankExport", "saveDatabankToCSV", function(path, useComma){
                    var strategies = '';

                    try {
                        var databankGrid = tab.getGrid();

                        if(databankGrid.sortedRowsBuffer && databankGrid.sortedRowsBuffer.length){
                            for(var i=0; i<databankGrid.sortedRowsBuffer.length; i++){
                                var rowIndex = databankGrid.sortedRowsBuffer[i].index;
                                strategies += (i == 0 ? "" : ",") + databankGrid.rows[rowIndex].cells[0];
                            }
                        }
                        else {
                            for(var i=0; i<databankGrid.getNumberOfRows(); i++){
                                strategies += (i == 0 ? "" : ",") + databankGrid.rows[i].cells[0];
                            }
                        }
                    } 
                    catch(err){
                        console.error("Cannot get sorted list of strategies - " + err);
                    }

                    CSVExportService.export(path, useComma, strategies);
                });
            }
        },
        id: "databank-action-save-csv",
        icon: "fa fa-download",
    });
});
