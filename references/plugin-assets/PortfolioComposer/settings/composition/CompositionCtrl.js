angular.module('app.portfoliocomposer').controller('CompositionCtrl', function ($scope, $rootScope, PortfolioComposerService, L, SQConstants, AppService, SQWebSocketService, L, SQEvents, SaveButtonService) {
    console.log("PortfolioComposer - composition controller initialized");

    var grid;

    $scope.config = PortfolioComposerService.config;

    $scope.records = 0;
    $scope.selectedRecords = 0;

    function init() {
        initGrid();

        loadGridData();
    }

    function initGrid() {
        if (!grid) {
            grid = new sqGrid("pc-composition-grid");
        } else {
            grid.removeAllRows(true);
        }

        var columns = [
            { title: L.tsq('Strategy name'), type: "text", sort: 'text', align: 'left' },
            { title: L.tsq('Mini equity chart'), type: "text", sort: 'text', align: 'left' },
            { title: L.tsq('Weight %'), type: "text", sort: 'text', align: 'left' },
        ];
        var widths = [ '*', 100, 80];

        grid.enableCheckboxes(true);
        grid.setColumns(columns, !!grid);
        grid.setWidths(widths, !!grid);
        grid.setEmptyGridText(L.tsq('No items available.'));

        grid.defineWidget('sparklinesWidget', sqGridSparklines);
        grid.defineWidget('spinnerWidget', sqGridSpinner);

        grid.onSelectionChanged = function (rowIndexes, values) {
            saveComposition();
        };

        grid.cellEventHandler = function (rowIndex, cellIndex, eventName, args) {
            switch (eventName) {
                case "spinnerUpdated": onGridSpinnerChange(rowIndex, cellIndex, args[0]); break;
            }
        };

        grid.headerRedraw();
        grid.bodyRedraw();
    }

    function onGridSpinnerChange(rowIndex, colIndex, value) {
        if(value == parseFloat(value)) {
            if(value < 0) {
                grid.modifyRowByIndex(0, rowIndex, colIndex);
            }
        } else {
            $rootScope.showError(L.tsq("Weight must be a number"));
            grid.modifyRowByIndex(1, rowIndex, colIndex);
        }

        saveComposition();
    }

    function saveComposition() {
        console.log("Save composition");

        let selectedRowsIndexes = grid.getSelectedRows().selectedRows;

        $scope.config.results.length = 0;
        $scope.config.weights.length = 0;

        for (var i = 0; i < selectedRowsIndexes.length; i++) {
            let rowIndex = selectedRowsIndexes[i];

            $scope.config.results.push(grid.getCellValue(rowIndex, 0));

            var weight = sqGridSpinnerValueGet(grid, rowIndex, 2);
            $scope.config.weights.push(weight);
        }

        $scope.records = grid.rows.length;
        $scope.selectedRecords = grid.getSelectedRows().selectedRows.length;

        try { $scope.$digest(); } catch (err) {}
    }

    $scope.moveUp = function() {
        console.log("Move up selected strategy");

        moveColumnBy(-1);
    }

    $scope.moveDown = function() {
        console.log("Move down selected strategy");

        moveColumnBy(1);
    }

    function moveColumnBy(moveBy) {
        var selectedIndexes = grid.getSelectedRows().selectedRows;
        if(selectedIndexes.length == 0) return;

        //load results
        let results = []
        
        for (var i = 0; i < grid.rows.length; i++) {
            results.push({
                row: grid.rows[i].cells,
                selected: selectedIndexes.includes(i),
                weight: sqGridSpinnerValueGet(grid, i, 2)                
            });
        }

        let index = grid.getSelectedRows().selectedRows[0];
        let newIndex = index + moveBy;
        if (newIndex < 0) {
            newIndex = 0;
        }
        
        if (newIndex > results.length - 1) {
          newIndex = results.length - 1;
        }

        //move row
        results.splice(newIndex, 0, results.splice(index, 1)[0]);

        //update grid
        grid.removeAllRows(true);

        for (var i = 0; i < results.length; i++) {
            var row = results[i].row;
            grid.addRow(row, true);

            let selected = results[i].selected;
            if(selected) grid.selectRow(i);

            let weight = results[i].weight;
            sqGridSpinnerValueSet(grid, i, 2, weight);
        }

        grid.bodyRedraw();
        saveComposition();
    }

    $scope.delete = function() {
        console.log("Delete selected strategies");

        let results = getSelectedReports();

        let data = {
            results: results.join(",")
        }

        $rootScope.showConfirm(L.tsq("Remove reports"), L.tsq("Are you sure you want to remove selected reports (%d)?", [results.length]), function (confirmed) {
            if (confirmed) {
                PortfolioComposerService.removeReports(data);
            }
        }, true);
    }

    $scope.clearAll = function() {
        console.log("Clear all strategies");

        $rootScope.showConfirm(L.tsq("Clear all"), L.tsq("Are you sure you want to clear all the reports?"), function (confirmed) {
            if (confirmed) {
                PortfolioComposerService.removeAllReports();
            }
        }, true);
    } 

    $scope.load = function() {
        console.log("Load strategy");

        var fileExtension = {
            name: 'sq4,str,stp,owf,sqw,sqx',
            description: 'SQX Strategy Files'
        }

        $rootScope.showFilePicker(L.tsq('Select files to load'), "pc-composition-load", true, true, null, fileExtension, null, function (paths) {

            PortfolioComposerService.loadFiles(paths).then(function (result) {
                if (result.error) {
                    $rootScope.showError(L.tsq("Error while loading reports. ") + result.error);

                    window.parent.hidePopup('#loadModal');
                }
            });
        });
    }

    $scope.savePortfolio = function() {
        console.log("Save portfolio");

        let portfolioName = PortfolioComposerService.config.portfolioName;

        if(!portfolioName) {
            $rootScope.showError(L.tsq("You must create a portfolio first."));
            return;
        }

        if(window.top.electron) {
            let data = {
                pathName: "portfolio-composer-save-portfolio",
                extension: {name: 'sqx', description: 'StrategyQuant files (.SQX)'},
                fileName: portfolioName,
                projectName: "PortfolioComposer",
                databankName: "Portfolios"
            }

            SaveButtonService.saveSingleFileJava(data);
            return;
        }
    }

    $scope.save = function() {
        console.log("Save strategy(s)");

        var selectedStrategies = getSelectedReports();
        if(selectedStrategies.length == 0) {
            $rootScope.showError(L.tsq("You must select at least one strategy"));
            return;
        }

        console.log("Save strategy(s), selected strategies", selectedStrategies);

        if(selectedStrategies.length > 1) {
            var filePicker = $rootScope.showFilePicker(L.tsq('Select folder'), "portfolio-composer-save", false, false);
            filePicker.onSelect = function (paths) {
                if (paths) {
                    let data = {
                        folder: paths[0],
                        strategies: selectedStrategies.join(","),
                        projectName: "PortfolioComposer",
                        databankName: "Results"
                    }

                    PortfolioComposerService.saveFiles(data, function(result){
                        $rootScope.showSuccess(result.success);  
                    });
                }
            }
        } else {
            if(window.top.electron) {
                let data = {
                    pathName: "portfolio-composer-save",
                    extension: {name: 'sqx', description: 'StrategyQuant files (.SQX)'},
                    fileName: selectedStrategies[0],
                    projectName: "PortfolioComposer",
                    databankName: "Results"
                }

                SaveButtonService.saveSingleFileJava(data);
                return;
            }
        }
    }

    $scope.addBuyHoldStrategySave = function() {
        console.log("Add buy hold strategy");

        let data = {
            symbol: $scope.setup.symbol
        };

        PortfolioComposerService.addBuyHoldStrategy(data, function(result) {     
            $rootScope.showSuccess(result.success);  

            hidePopup('#addBuyHoldStrPopup');
        });
    }

    $scope.addBuyHoldStrategy = function() {
        showPopup('#addBuyHoldStrPopup');
    }

    init();

    function getSelectedReports() {
        let results = [];

        let selectedRowsIndexes = grid.getSelectedRows().selectedRows;

        for (var i = 0; i < selectedRowsIndexes.length; i++) {
            let rowIndex = selectedRowsIndexes[i];

            results.push(grid.getCellValue(rowIndex, 0));
        }

        return results;
    }

    function setWeights(weights) {
        let selectedRowsIndexes = grid.getSelectedRows().selectedRows;

        if(selectedRowsIndexes.length != weights.length) {
            console.log("PortfolioComposer - cannot set weights, different number of selected strategies and weights");
            return;
        }

        $scope.config.weights.length = 0;

        for (var i = 0; i < selectedRowsIndexes.length; i++) {
            let rowIndex = selectedRowsIndexes[i];
            let weight = parseInt(weights[i]);

            sqGridSpinnerValueSet(grid, rowIndex, 2, weight);
            
            $scope.config.weights.push(weight);
        }

        grid.bodyRedraw();

        $rootScope.showSuccess(L.tsq("Optimal weights set."));
    }

    function updateDatabankStrategies(updates) {
        console.log("PortfolioComposer - update databank strategies", updates);

        if(updates.length > 0 && updates[0].name != "Results") {
            return;
        }

        loadGridData();
    }
    
    function loadGridData() { 
        console.log("PortfolioComposer - loading grid data ...");

        //load results
        let lastComposition = []
        var selectedIndexes = grid.getSelectedRows().selectedRows;

        for (var i = 0; i < grid.rows.length; i++) {
            lastComposition.push({
                row: grid.rows[i].cells,
                selected: selectedIndexes.includes(i),
                weight: sqGridSpinnerValueGet(grid, i, 2),
                name: grid.getCellValue(i, 0)                
            });
        }

        PortfolioComposerService.loadGridData(function (result) {
            //we have to preserve the order of the strategies in the grid
            loadToArray($scope.config.mmMethods, result.mmMethods);
            $scope.config.mmMethodsDifferent = result.mmMethodsDifferent;

            grid.removeAllRows(true);
            
            let alreadyAdded = [];

            //add last grid composition
            let rowIndex = 0;
            for (var i = 0; i < lastComposition.length; i++) {
                let name = lastComposition[i].name;

                let exists = result.rows.some(row => row[0] === name);
                if(exists) {
                    var row = lastComposition[i].row;
    
                    alreadyAdded.push(row[0]);

                    var data = [
                        row[0],
                        row[1],
                        "{{spinnerWidget value='100' min='0' max='10000' step='1'}}"
                    ]
    
                    grid.addRow(data, true);

                    let selected = lastComposition[i].selected;
                    if(selected) grid.selectRow(rowIndex);

                    let weight = lastComposition[i].weight;
                    sqGridSpinnerValueSet(grid, rowIndex, 2, weight);

                    rowIndex++;
                }
            }

            //add new strategies to end of the grid
            for (var i = 0; i < result.rows.length; i++) {
                var row = result.rows[i];
                
                if(alreadyAdded.includes(row[0])) {
                    continue;
                }

                var data = [
                    row[0],
                    row[1],
                    "{{spinnerWidget value='100' min='0' max='10000' step='1'}}"
                ]

                grid.addRow(data, true);
            }

            grid.bodyRedraw();
            saveComposition();
        });
    }

    function onEvent(event, data){
        if(event == SQEvents.get('APP_SWITCHED')) {
            if(data == window.appConfig.product) {
                loadGridData();
            }

        } else if(event == SQEvents.get('PORTFOLIO_COMPOSER_WEIGHTS')){
            const weights = JSON.parse(data);

            setWeights(weights);
        }
    }

    $scope.setup = {
        symbol: 'AAPL.D',
        engine: 'Single-asset cloud strategy',
    }

    var projectName = AppService.getProject();
    var databankChannel = SQConstants.getConstants().channels.databanks;
    SQWebSocketService.subscribe(projectName, databankChannel, "pc-composition", updateDatabankStrategies);

    console.log("PortfolioComposer - subscribed to databank channel", projectName, databankChannel);

    window.top.addListener("PortfolioComposer-composition", [ SQEvents.get('APP_SWITCHED'), SQEvents.get('PORTFOLIO_COMPOSER_WEIGHTS')], onEvent);
});