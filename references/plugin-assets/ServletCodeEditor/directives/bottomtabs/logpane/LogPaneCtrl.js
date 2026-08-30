angular.module('app').controller('LogPaneCtrl', function ($scope, $timeout, L, SQWebSocketService, SQEvents) {
    console.log("LogPaneCtrl controller initialized");
     
    var logGrid;   
    
    $timeout(function() {
        if (!logGrid) {
            logGrid = new sqGrid("logGrid");
        } else {
            logGrid.removeAllRows(true);
        }

        var columns = [
            { title: L.tsq('Description'), type: "text", sort: 'text', align: 'left' },
            { title: L.tsq('Type'), type: "text", sort: 'text', align: 'left' },
            { title: L.tsq('File'), type: "text", sort: 'text', align: 'left' },
            { title: L.tsq('Line'), type: "text", sort: 'text', align: 'left' },
            { title: L.tsq('Column'), type: "text", sort: 'text', align: 'left' }
        ];
        var widths = [900, 100, 200, 180, '*' ];

        logGrid.enableCheckboxes(false);
        logGrid.setColumns(columns, !!logGrid);
        logGrid.setWidths(widths, !!logGrid);
        logGrid.setEmptyGridText(L.tsq('No data.'));
        logGrid.disableSorting();

        logGrid.headerRedraw();
        
        logGrid.onCellDoubleClick = function (rowIndex) {
            var line = logGrid.getCellValue(rowIndex, 3);  
            var file = logGrid.getUserData(rowIndex, 'file');

            if(file) {
                SQEvents.notifyListeners(SQEvents.get('EDITOR_HIGHLIGHT_LINE'), { file: file, line: line });          
            }  
        };
    });
    
    function onWebSocketData(data) {
        if(data.codeEditor && data.codeEditor.compilationResult) { //print compilation result
            logGrid.removeAllRows(true, true);
            logGrid.loadFromJSON(data.codeEditor.compilationResult);
                        
            SQEvents.notifyListeners(SQEvents.get('EDITOR_SELECT_BOTTOM_TAB'), 'log');  
        }
    }

    SQWebSocketService.subscribeGeneral("LogPaneCtrl-" + window.appConfig.appCode, onWebSocketData);

});