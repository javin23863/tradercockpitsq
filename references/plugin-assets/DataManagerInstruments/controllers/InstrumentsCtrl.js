angular.module('app.instruments').controller('InstrumentsController', function ($scope, $rootScope, $timeout, SQEvents, SQConstants, InstrumentService, L) {
    console.log("Instruments controller initialized");

    $scope.isQuantDataManager = isQuantDataManager();

    function initGrids() {
        $timeout(function () {
            initInstrumentsGrid();

            initialized = true;

            $scope.filter.onSearch();
        });
    }

    function initInstrumentsGrid() {
        var columns = [
            { title: L.tsq('Instrument'), type: "text", sort: 'text' },
            { title: L.tsq('Description'), type: "text", sort: 'text' },
            { title: L.tsq('Broker profile'), type: "text", sort: 'text' },
            { title: L.tsq('Point value'), type: "float5", sort: 'number' },
            { title: L.tsq('Pip/Tick size'), type: "float5", sort: 'number' },
            { title: L.tsq('Pip/Tick step'), type: "float5", sort: 'number' },
            { title: L.tsq('Default spread'), type: "float2", sort: 'number' },
            { title: L.tsq('Default slippage'), type: "float2", sort: 'number' },
            { title: L.tsq('Commissions'), type: "text", sort: 'text' },
            { title: L.tsq('Swap'), type: "text", sort: 'text' },
            { title: L.tsq('Data type'), type: "text", sort: 'text' },
            { title: L.tsq('Order size mult.'), type: "float2", sort: 'number' },
            { title: L.tsq('Order size step'), type: "float2", sort: 'number' },
            { title: '', type: "text", sort: 'text' },
            { title: '', type: "text", sort: 'text' }
        ];

        var widths = [150, 150, 150, 100, 120, 100, 100, 100, 150, 100, 100, 100, 100, "*", 20];

        if (isQuantDataManager()) { //remove commission column
            columns.splice(8, 2);
            widths.splice(8, 2);
        }

        if (!grid) {
            grid = new sqGrid("instrumentsGrid");
        } else {
            grid.removeAllRows(true, false, true);
        }

        grid.setFirstColumnAsId(true, false);
        grid.enableCheckboxes(true, false);
        grid.setColumns(columns, !!grid);
        grid.setWidths(widths, !!grid);
        grid.setEmptyGridText(L.tsq('No instruments defined.'));
        grid.setSort(0, false);

        grid.onCellDoubleClick = function (rowIndex) {
            $scope.onEditInstrument();
        };

        grid.onSelectionChanged = function () {
            var data = grid.getSelectedRows().selectedRowsData[0];
            if (!data) return;

            $scope.selectedInstrument = findInstrumentByName(data[0]);
            console.log($scope.selectedInstrument);
            $scope.selectedInstrument.sector = $scope.selectedInstrument.sector || '';
            $scope.selectedInstrument.exchange = $scope.selectedInstrument.exchange || '';
            $scope.selectedInstrument.country = $scope.selectedInstrument.country || '';
        };

        grid.cellEventHandler = function (rowIndex, cellIndex, eventName, args) {
            $scope.tab.callAction(eventName, rowIndex);
        }

    }

    function findInstrumentByName(instrument) {
        for (var i = 0; i < $scope.instruments.length; i++) {
            if ($scope.instruments[i].instrument == instrument) {
                return angular.copy($scope.instruments[i]);
            }
        }

        return null;
    }

    $scope.getBrokerProfileLabel = function(){
        if (!$scope.filter.brokerId) return L.tsq('All broker profiles');

        var brokerProfile = getItem($scope.brokerProfiles, 'id', $scope.filter.brokerId);
        return brokerProfile ? brokerProfile.name : '';
    }

    function updateInstrumentGridData() {
        if (!initialized) return;

        var selectedInstrument;
        var realScrollPosition;
        var originalRowsCount = grid.rows.length;

        var selection = grid.getSelectedRows();
        if (selection.selectedRowsData.length>0){
            selectedInstrument = selection.selectedRowsData[0][0];
            realScrollPosition=grid.scrollerBoxOuter.scrollTop;
        }
        grid.removeAllRows(false, false, true);

        var dataType = $scope.filter.dataType;
        var search = $scope.filter.text;
        var broker = $scope.filter.brokerId;

        grid.loadFromUrl('instruments/list?type=' + dataType + "&search=" + search+"&broker="+broker, function () {

            for (var i = 0; i < grid.rows.length; i++) {
                var link = createActionLink('<i class="fa fa-times" aria-hidden="true"></i>', 'action-link', 'delete', i)
                grid.modifyRowByIndex(link, i, isQuantDataManager() ? 11 : 13, true);
            }

            grid.sortAll();

            if (selectedInstrument){
                const instrumentInGrid = grid.rows.find(r=>r.cells[0]==selectedInstrument);
                if (instrumentInGrid){
                    if (originalRowsCount==grid.rows.length){
                        grid.selectRow(instrumentInGrid.index, true, true);
                        grid.scrollerBoxOuter.scrollTop = realScrollPosition;
                    }else{
                        grid.scrollToRowIndex(instrumentInGrid.index, false);
                    }
                }
            }
        }, true);

        if (!grid.getSelectedRows().selectedRowsData[0]) {
            $scope.selectedInstrument = null;
        }
    }

    $scope.tab.getSelectedRows = function () {
        var selectedInstruments = [];
        var rowData = grid.getSelectedRows().selectedRowsData;

        for (var i = 0; i < rowData.length; i++) {
          var data = rowData[i];

          selectedInstruments.push({
            name: data[0]
          });
        }

        return {
          rows: selectedInstruments,
          grid: grid,
        };
      };

    $scope.onEditInstrument = function () {
        if (!$scope.selectedInstrument) {
            $rootScope.showError(L.tsq('You have to select some record.'));
            return;
        }

        $scope.currentAction = 'Edit';
        $scope.newInstrument = $scope.selectedInstrument;
        
        try {
            $scope.$digest();
        } catch (err) { } //needed because of doubleclick event

        showPopup('#addInstrumentModal');
    }

    function getSelectedInstruments() {
        var rowsData = grid.getSelectedRows().selectedRowsData;
        var instruments = "";

        for (var i = 0; i < rowsData.length; i++) {
            var rowData = rowsData[i];
            instruments += rowData[0] + ",";
        }

        return instruments.substr(0, instruments.length - 1);
    }

    function getSelectedInstrumentsArray() {
        var rowsData = grid.getSelectedRows().selectedRowsData;
        var instruments = []

        for (var i = 0; i < rowsData.length; i++) {
            var rowData = rowsData[i];
            instruments.push(rowData[0]);
        }

        return instruments;
    }

    $scope.onSaveInstrument = function (form) {
        if (form) {
            $scope.errors = validate(form);
            if ($scope.errors) {
                return;
            }
        }

        switch ($scope.currentAction) {
            case 'Add':
                InstrumentService.addInstrument($scope.newInstrument);
                break;
            case 'Edit':
            case 'Mass-Edit':                
                InstrumentService.editInstrument($scope.newInstrument);
                break;
        }
    }

    $scope.onMassSaveInstrument = function (form) {
        if (form) {
            $scope.errors = validate(form);
            if ($scope.errors) {
                return;
            }
        }

        if (!$scope.newInstrument.changeCommisionModel &&
        !$scope.newInstrument.changeDataType &&
        !$scope.newInstrument.changeDefaultSlippage &&
        !$scope.newInstrument.changeDefaultSpread &&
        !$scope.newInstrument.changeMinDistance &&
        !$scope.newInstrument.changeOrderSizeStep &&
        !$scope.newInstrument.changePointValue &&
        !$scope.newInstrument.changeSizeMultiplier &&
        !$scope.newInstrument.changeSwap &&
        !$scope.newInstrument.changeTickSize &&
        !$scope.newInstrument.changeTickStep){
            $rootScope.showError(L.tsq('You have to select some field for change.'));
            return;
        }

        $scope.newInstrument.instruments = getSelectedInstrumentsArray();
        InstrumentService.massEditInstrument($scope.newInstrument, function(result){
            hidePopup('#massEditInstrumentModal');
       });
    }

    function onFilterSearch() {
        if (initialized) {
            $scope.selectedInstrument = null;
            updateInstrumentGridData();
        }
    }

    function onFilterReset() {
        $scope.filter.text = '';
        updateInstrumentGridData();
    }

    function reloadInstrumentsGrid() {
        $scope.filter.onSearch();

        try {
            $scope.$digest();
        } catch (err) { }
    }

    //- Event Handlers --------------------------------------------------------------

    function onEvent(event, data) {
        if (event == SQEvents.get('BROKERS_CHANGED')) {
            reloadBrokers();
        }else if (event == SQEvents.get('INSTRUMENTS_CHANGED')) {
            $scope.instruments = angular.copy(data);

            reloadInstrumentsGrid();
        } else return;

        try {
            $scope.$digest();
        } catch (err) { }
    }

    $scope.$on('$destroy', function () {
        SQEvents.removeListener(listenerId);
    });

    $scope.tab.callAction = function (actionName, rowIndex) {
        switch (actionName) {
            case 'mass-edit':
                if (!$scope.selectedInstrument) {
                    $rootScope.showError(L.tsq('You have to select some record.'));
                    return;
                }

                var selected = grid.getSelectedRows(); 
                if (selected.selectedRowsData.length==1){
                    $scope.onEditInstrument();
                    break;
                }
        
                $scope.currentAction = 'Mass-Edit';
                $scope.newInstrument.brokerId = -1;
                $scope.newInstrument.instrument = '';
                $scope.newInstrument.description = '';
                $scope.newInstrument.exchange = '';
                $scope.newInstrument.country = '';
                $scope.newInstrument.sector = '';
                $scope.newInstrument.dataType = 3;
                $scope.newInstrument.orderSizeMultiplier = $scope.selectedInstrument.orderSizeMultiplier;
                $scope.newInstrument.orderSizeStep = $scope.selectedInstrument.orderSizeStep;
                $scope.newInstrument.minDistance = $scope.selectedInstrument.minDistance;
                $scope.newInstrument.pointValue = $scope.selectedInstrument.pointValue;
                $scope.newInstrument.tickSize = $scope.selectedInstrument.tickSize;
                $scope.newInstrument.tickStep = $scope.selectedInstrument.tickStep;
                $scope.newInstrument.defaultSpread = $scope.selectedInstrument.defaultSpread;
                $scope.newInstrument.defaultSlippage = $scope.selectedInstrument.defaultSlippage;
                        
                $scope.newInstrument.changePointValue = false;
                $scope.newInstrument.changeTickSize = false;
                $scope.newInstrument.changeMinDistance = false;
                $scope.newInstrument.changeTickStep = false;
                $scope.newInstrument.changeDefaultSpread = false;
                $scope.newInstrument.changeDefaultSlippage = false;
                $scope.newInstrument.changeCommisionModel = false;
                $scope.newInstrument.changeDataType = false;
                $scope.newInstrument.changeSizeMultiplier = false;
                $scope.newInstrument.changeOrderSizeStep = false;
                $scope.newInstrument.changeSwap = false;

                try {
                    $scope.$digest();
                } catch (err) { } //needed because of doubleclick event
        
                showPopup('#massEditInstrumentModal');
                break;
            //----------------------------------------------------------------------------
            case 'add':
                $scope.currentAction = 'Add';

                $scope.newInstrument.brokerId = -1;
                $scope.newInstrument.instrument = '';
                $scope.newInstrument.description = '';
                $scope.newInstrument.exchange = '';
                $scope.newInstrument.country = '';
                $scope.newInstrument.sector = '';
                $scope.newInstrument.dataType = 3;
                $scope.newInstrument.orderSizeMultiplier = 1;
                $scope.newInstrument.orderSizeStep = 1;
                $scope.newInstrument.minDistance = 0;

                showPopup('#addInstrumentModal');

                break;

            //----------------------------------------------------------------------------
            case 'delete':
                if (!$scope.selectedInstrument && rowIndex < 0) {
                    $rootScope.showError(L.tsq('You have to select some record.'));
                    return;
                }

                if (!$scope.selectedInstrument && rowIndex >= 0) {
                    var selectedData = [grid.rows[rowIndex].cells];
                    var selectedInstruments = selectedData[0][0];
                    var count = 1;
                }
                else {
                    var selectedInstruments = getSelectedInstruments();
                    var count = selectedInstruments.split(",").length;
                }

                var text = "";
                if (count > 1) {
                    text = L.tsq("Are you sure you want to remove selected instruments (%d)?", [count]);
                } else {
                    text = L.tsq("Are you sure you want to remove instrument '%s'?", [selectedInstruments]);
                }

                $rootScope.showConfirm(count > 1 ? L.tsq("Remove instruments") : L.tsq("Remove instrument"), text,
                    function (confirmed) {
                        if (confirmed) {
                            InstrumentService.removeInstruments(selectedInstruments);
                        }
                    });

                break;

            //----------------------------------------------------------------------------
            case 'save':
                console.log("Save instruments")
                
                var count = 0;
                
                if (!$scope.selectedInstrument && rowIndex >= 0) {
                    var selectedData = [grid.rows[rowIndex].cells];
                    var selectedInstruments = selectedData[0][0];
                    count = 1;
                }
                else {
                    var selectedInstruments = getSelectedInstruments();
                    count = selectedInstruments.split(",").length;
                }

                if (count < 1) {
                    $rootScope.showError(L.tsq('You have to select at least one instrument.'));
                    return;
                }

                var data = {
                    instruments: selectedInstruments
                }

                $rootScope.saveFile(
                    L.tsq("Select file"), 
                    { name: "xml", description: "XML Files" }, 
                    "SaveInstruments", 
                    "Instruments.xml", 
                    null, 
                    function(targetPath) {
                        data.filePath = targetPath;
                        InstrumentService.save(data);
                    }
                );

                break;

            //----------------------------------------------------------------------------
            case 'load':
                console.log("Load instruments")

                var data = {};

                var fileExtension = { name: 'xml', description: 'XML Files' };
                $rootScope.showFilePicker(L.tsq('Select file'), "LoadInstruments", true, false, null, fileExtension, null, function (paths) {
                    if (paths) {
                        data.filePath = paths[0];
                        InstrumentService.load(data);
                    }
                });

                break;
        }
    }

    function reloadBrokers(){
        var brokerProfiles = angular.copy(SQConstants.getConstants().brokers).filter(b=>b.mtUse);
        brokerProfiles.unshift({
            id:-1,
            name:'SQ default'
        });
        $scope.brokerProfiles = brokerProfiles;
    }

    //- Initialization --------------------------------------------------------------

    var grid;
    var initialized = false;

    $scope.dataTypes = angular.copy(SQConstants.getConstants().dataTypes);
    $scope.instruments = angular.copy(SQConstants.getConstants().instruments);

    reloadBrokers();

    $scope.getDataTypeLabel = function () {
        if (!$scope.filter.dataType) return L.tsq('All data types');

        var dataType = getItem($scope.dataTypes, "value", $scope.filter.dataType);
        return dataType ? L.tsq(dataType.name) : '';
    }

    $scope.filter = {
        text: '',
        onSearch: onFilterSearch,
        onReset: onFilterReset,
        dataType: "",
        brokerId: ""
    };

    initGrids();

    $scope.newInstrument = {};
    $scope.selectedInstrument = null;

    var listenerId = "InstrumentsCtrl";
    SQEvents.addListener(listenerId, [
        SQEvents.get('INSTRUMENTS_CHANGED'),
        SQEvents.get('BROKERS_CHANGED'),        
        SQEvents.get('SHOW_INSTRUMENT'),
        SQEvents.get('ADD_INSTRUMENT')
    ], onEvent);

});