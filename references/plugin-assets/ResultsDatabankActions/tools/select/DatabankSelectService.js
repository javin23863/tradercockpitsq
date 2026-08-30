angular.module('app').service('DatabankSelectService', function() {
    
    this.onSelect = function(databankGrid, selectPassed) {
        for(var i=0; i<databankGrid.getNumberOfRows(); i++){
            var value = databankGrid.getCellValue(i, 1);     //Filters result is the second column in Retester databank
            var select = value.indexOf(selectPassed ? "PASSED" : "FAILED") >= 0;

            databankGrid.setRowChecked(i, select, false, true);
        }

        databankGrid.bodyRedraw();

        if(databankGrid.onSelectionChanged){
            databankGrid.onSelectionChanged();
        }
    } 

});