angular.module('app.customdata').controller('DMCustomDataCtrl', function ($scope, $rootScope, $timeout, L, SQConstants, SQEvents, DMCustomDataService, SQWebSocketService, AppService) {

    $scope.customDataTypes = SQConstants.getConstants().customDataTypes;

    function initGrid() {
        $timeout(function () {
            grid = new sqGrid("customDataGrid");

            var columns = [{
                title: L.tsq('Name'),
                type: "text",
                sort: 'text'
            },
            {
                title: L.tsq('Values'),
                type: "text",
                sort: 'text'
            },
            {
                title: L.tsq('Data type'),
                type: "text",
                sort: 'text'
            },
            {
                title: L.tsq('Timeframe'),
                type: "text",
                sort: 'text'
            },
            {
                title: L.tsq('Date from'),
                type: "text",
                sort: 'text'
            },
            {
                title: L.tsq('Date to'),
                type: "text",
                sort: 'text'
            },
            {
                title: L.tsq('Total Days'),
                type: "float2",
                sort: 'number'
            },
            {
                title: L.tsq('Total Records'),
                type: "float2",
                sort: 'number'
            },
            {
                title: '',
                type: "text"
            },
            {
                title: '',
                type: "text",
                sort: 'text'
            }
            ];
            var widths = [140, "9%", "9%", "5%", "6%", "6%", "6%", "6%", "*", 20];

            grid.defineWidget('progress-bar', sqGridProgressBar);
            grid.defineWidget('tooltipWidget', sqGridTooltip);
            grid.defineWidget('checkboxWidget', sqGridCheckbox);

            grid.setFirstColumnAsId(true, false);
            grid.enableCheckboxes(true, false);
            grid.setColumns(columns, !!grid);
            grid.setWidths(widths, !!grid);
            grid.setEmptyGridText(L.tsq('No External indicators defined.'));

            grid.onCellClick = function (row, col) {
                $scope.selectedRow = grid.getRowId(row);
            };

            grid.onCellDoubleClick = function (row) {
                $scope.selectedRow = grid.getRowId(row);

                callAction('edit', row);
            };

            grid.cellEventHandler = function (rowIndex, cellIndex, eventName, args) {
                callAction(eventName, args[0], args);
            }

            grid.setFirstColumnAsId(true, false);

            reloadDataGrid();

            $scope.filter.onSearch();

            grid.setSort(0, false);

        }, 0, false);
    }

    function reloadDataGrid() {
        $scope.currentData = [];
        if ($scope.data) {
            for (var i = 0; i < $scope.data.length; i++) {
                var data = $scope.data[i];
                $scope.currentData.push(data);
            }

            $scope.filter.onSearch();
        }

        try {
            $scope.$digest();
        } catch (err) { }
    }

    function updateGridData(data) {
        console.log("External indicators list", data)

        var actions = loadGridProgressBarActions(grid);
        grid.removeAllRows(true, true, true);

        if (data) {
            for (var i = 0; i < data.length; i++) {
                grid.appendData([
                    [
                        data[i].name,
                        data[i].values,
                        dataTypeToString(data[i].dataType),
                        data[i].timeframe ? data[i].timeframe : '',
                        data[i].dateFrom == 0 ? "" : timeToDateString(data[i].dateFrom),
                        data[i].dateTo == 0 ? "" : timeToDateString(data[i].dateTo),
                        data[i].totalDays,
                        data[i].rows,
                        "{{progress-bar}}",
                        createActionLink('<i class="fa fa-times" aria-hidden="true"></i>', 'action-link', 'delete', i)
                    ]
                ], true);

                var rowIndex = grid.getRowIndex(data[i].name);
                grid.setUserData(rowIndex, 'dataType', data[i].dataType);
                grid.setUserData(rowIndex, 'id', data[i].id);
                grid.setUserData(rowIndex, 'valuesObject', data[i].valuesObject);
            }
        }

        if (!grid.getSelectedRows().selectedRowsData.length) {
            $scope.selectedRow = null;
        }


        applyGridProgressBarActions(grid, actions);

        grid.sortAll();

        $scope.totalRecords = grid.getNumberOfRows();

        try { $scope.$digest(); } catch (err) { }
    }

    function dataTypeToString(type) {
        var dataType = getItem($scope.customDataTypes, "value", type);
        return dataType ? dataType.name : '';
    }

    //- Event Handlers --------------------------------------------------------------

    function onEvent(event, data) {
        if (!grid) {
            return;
        }

        if (event == SQEvents.get('CUSTOM_DATA_CHANGED')) {
            $scope.data = angular.copy(SQConstants.getConstants().customdata);
            reloadDataGrid();
        }

        try {
            $scope.$digest();
        } catch (err) { }
    }

    function onWebSocketData(args) {
        if (!grid) {
            return;
        }

        if (args.DMCustomDataProgresses) {
            var progresses = args.DMCustomDataProgresses;

            var appInProgress = false;

            for (var i = 0; i < progresses.length; i++) {
                var progress = progresses[i];

                var rowIndex = grid.getRowIndex(progress.key);
                if (rowIndex < 0) {
                    continue;
                }

                var tooltipText = "";
             
                if (progress.pct == -1) {
                    tooltipText = progress.message;
                }

                if (progress.pct == 100) {
                    tooltipText = "";
                }

                grid.setUserData(rowIndex, 'progress', progress.pct);
                grid.setUserData(rowIndex, 'progressText', progress.message);
                grid.setUserData(rowIndex, 'tooltipText', tooltipText);
                grid.setUserData(rowIndex, 'paused', progress.paused);
                grid.setUserData(rowIndex, 'actionLink', progress.actionLink);
                grid.setUserData(rowIndex, 'callAction', DMCustomDataService.callAction);

                if (!appInProgress && progress.pct >= 0 && progress.pct < 100) {
                    appInProgress = true;
                }
            }

            grid.bodyRedraw();

            displayAppMenuProgress(appInProgress);
        }
    }

    function displayAppMenuProgress(inProgress) {
        if (isQuantDataManager()) {
            return;
        }

        if (inProgress) {
            window.parent.document.getElementById(window.appConfig.product + "-menuprogress").style.display = "block";
        } else {
            window.parent.document.getElementById(window.appConfig.product + "-menuprogress").style.display = "none";
        }
    }

    function passesFilter(item) {
        var search = $scope.filter.text.toLowerCase();

        if (item.name.toLowerCase().indexOf(search) < 0) {
            return false;
        }

        if ($scope.filter.dataType && item.dataType != $scope.filter.dataType) {
            return false;
        }

        return true;
    }

    $scope.openHelp = function () {
        AppService.openHelp('custom-data-indicators');
    }

    $scope.tab.getSelectedRows = function () {
        return {
            rows: getSelectedRows(),
            grid: grid,
            customIndicatorData: true
        };
    }

    function getSelectedRows(rowIndex) {
        return {
            rows: getSelectedData(),
            grid: grid,
            customIndicatorData: true
        };
    }

    function getSelectedData(rowIndex) {
        var selectedData = [];
        var rowData = [];

        if (rowIndex >= 0) {
            rowData.push(grid.getRowData(rowIndex).cells);
        } else {
            rowData = grid.getSelectedRows().selectedRowsData;
        }

        for (var i = 0; i < rowData.length; i++) {
            var data = rowData[i];

            var rowIndex = grid.getRowIndex(data[0]);
            var id = grid.getUserData(rowIndex, 'id');
            var dataType = grid.getUserData(rowIndex, 'dataType');
            var valuesObject = grid.getUserData(rowIndex, 'valuesObject');

            selectedData.push({
                rowIndex: rowIndex,
                id: id,
                name: data[0],
                values: data[1],
                dataType: dataType,
                timeframe: data[3],
                dateFrom: data[4],
                dateTo: data[5],
                totalRecords: data[7],
                valuesObject: valuesObject,
            });
        }

        return selectedData;
    }

    $scope.tab.callAction = function (actionName) {
        callAction(actionName);
    }

    $scope.callAction = function (actionName) {
        callAction(actionName);
    }

   function callAction(actionName, rowIndex, args) {
        rowIndex = parseInt(rowIndex);

        var selectedData = getSelectedData(rowIndex);

        switch (actionName) {
            //----------------------------------------------------------------------------
            case 'edit':
                SQEvents.notifyListeners(SQEvents.get("CUSTOM_DATA_EDIT"), selectedData[0]);
                break;

            //----------------------------------------------------------------------------
            case 'delete':
                var count = selectedData.length;

                if (count > 1) {
                    $scope.dataDeleteConfirmText = L.tsq("Do you want to remove selected indicators (%d) or only clear their data ?", [count]);
                } else {
                    $scope.dataDeleteConfirmText = L.tsq("Do you want to remove indicator '%s' or only clear its data ?", [selectedData[0].name]);
                }

                showPopup("#customDataDeleteConfirmPopup");
                break;

            //----------------------------------------------------------------------------
            case 'deleteconfirm':
                hidePopup("#customDataDeleteConfirmPopup");

                DMCustomDataService.removeData({data: getNames(selectedData)});

                break;

            //----------------------------------------------------------------------------
            case 'clear':
                hidePopup("#customDataDeleteConfirmPopup");

                if (selectedData.length == 0) {
                    $rootScope.showError(L.tsq('You have to select at least one indicator.'));
                    return;
                }

                DMCustomDataService.clearData({data: getNames(selectedData)});

                break;

            //----------------------------------------------------------------------------
            case 'save':
                console.log("Save external indicators")
                
                if (selectedData.length == 0) {
                    $rootScope.showError(L.tsq('You have to select at least one indicator.'));
                    return;
                }

                var fileExtension = { name: 'xml', description: 'XML Files' };
                
                var data = {
                    data: getNames(selectedData)
                }

                $rootScope.saveFile(
                    L.tsq("Select file"), 
                    { name: "xml", description: "XML Files" }, 
                    "SaveExternalIndicators", 
                    "ExternalIndicators.xml", 
                    null, 
                    function(targetPath) {
                        data.filePath = targetPath;
                        DMCustomDataService.save(data);
                    }
                );

                break;

            //----------------------------------------------------------------------------
            case 'load':
                console.log("Load external indicators")

                var data = {};

                var fileExtension = { name: 'xml', description: 'XML Files' };
                $rootScope.showFilePicker(L.tsq('Select file'), "LoadExternalIndicators", true, false, null, fileExtension, null, function (paths) {
                    if (paths) {
                        data.filePath = paths[0];
                        DMCustomDataService.load(data);
                    }
                });

                break;
        }

        try {
            $scope.$digest();
        } catch (err) { }
    }

    function getNames(data) {
        var names = "";

        for (var i = 0; i < data.length; i++) {
            names += data[i].name + ",";
        }

        names = names.substr(0, names.length - 1);

        return names;
    }

    $scope.getDataTypeLabel = function () {
        if (!$scope.filter.dataType) return L.tsq('All data types');

        var dataType = getItem($scope.customDataTypes, "value", $scope.filter.dataType);
        return dataType ? L.tsq(dataType.name) : '';
    }

    var grid;

    $scope.totalRecords = 0;
    $scope.currentData = [];
    $scope.data = angular.copy(SQConstants.getConstants().customdata);

    $scope.dataDetails = {
        oldName: null,
        newName: null,
        values: '1',
        type: $scope.customDataTypes[0].value
    }

    initGrid();

    $scope.filter = {
        text: '',
        onSearch: function () {
            if (grid) {
                var data = [];

                for (var i = 0; i < $scope.currentData.length; i++) {
                    var item = $scope.currentData[i];
                    if (passesFilter(item)) {
                        data.push(item);
                    }
                }

                updateGridData(data);
            }
        },
        onReset: function () {
            $scope.filter.text = '';
            reloadDataGrid();
        },
        dataType: null,
    };

    var TF = SQConstants.getConstants().timeframe;  

    SQEvents.addListener('DMCustomData', [SQEvents.get('CUSTOM_DATA_CHANGED')], onEvent);
    SQWebSocketService.subscribeGeneral('DMCustomData', onWebSocketData);
});