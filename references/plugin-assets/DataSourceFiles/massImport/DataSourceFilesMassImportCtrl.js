angular.module('app.datasource.files').controller('DataSourceFilesMassImportCtrl', function ($rootScope, $scope, $q, SQEvents, SQConstants, DataSourceFilesService, L) {
    $scope.barDataTypes = angular.copy(SQConstants.getConstants().barDataTypes);

    $scope.action.onSelect = function () {
        $scope.config.timezone = getDefaultTimezone();
        for (var i = 0; i < $scope.timezones.length; i++) {
            if ($scope.timezones[i].value == $scope.config.timezone) {
                $scope.config.timezoneDisplay = $scope.timezones[i].name;
            }
        }

        $scope.instrumentChooser.reset();
        console.log('config', $scope.config);
        console.log("DataSourceFilesMassImportCtrl")
        showPopup('#addMassImportModal');
    }

    $scope.onSelectFolder = function () {
        $rootScope.showFilePicker(L.tsq('Select data folder'), "sourceImport", false, false, null, null, null, function (paths) {
            if (paths) {
                $scope.config.path = paths[0];

                try {
                    $scope.$digest();
                } catch (er) { }
            }
        });
    }

    function getDefaultTimezone() {
        if($scope.timezones.length>0) {
            return $scope.timezones[0].value;
        }
    }

    $scope.onSave = function (form) {
        if (form) {
            $scope.errors = validate(form);
            if ($scope.errors) {
                console.error($scope.errors);
                return;
            }
        }

        try {
            $scope.config.instrument = $scope.instrumentChooser.getInstrument().instrument;
        } catch (err) {
            console.error("Cannot set symbol instrument. " + err);
        }

        if (typeof $scope.config.instrument=='undefined'){
            $rootScope.showError("No instrument is selected."); 
            return;
        }

        DataSourceFilesService.massImport($scope.config,function (result) {
            if (result.success){
               $rootScope.setProgressInfo(L.tsq('Mass import'), L.tsq("Adding symbols..."), 0, function() {
                    DataSourceFilesService.cancelMassImport($scope.config);
                }, true);
            }
        });


        hidePopup('#addMassImportModal');
    }

    $scope.timezones = SQConstants.getConstants().timezones;
    $scope.instrumentChooser = {};

    var TF = SQConstants.getConstants().timeframe;

    $scope.timeframes = [];
    function initTimeframes() {
        $scope.timeframes.length = 0;

   //     $scope.timeframes.push({type: 'Recognize automatically', value: 'auto'});
    //    $scope.timeframes.push({type: TF.Intraday, value: TF.Intraday});
        
        var defaultTimeframes = SQConstants.getConstants().timeframes;
        for (var i = 0; i < defaultTimeframes.length; i++) {
            var tf = defaultTimeframes[i];

            $scope.timeframes.push({type: tf, value: tf});
        }

    }

    initTimeframes();

    $scope.dateFormats = [];
    function loadDateFormats() {
        $q.when(DataSourceFilesService.importGetInfo()).then(function (importInfo) {
            if (importInfo) {
                $scope.dateFormats = importInfo.dateFormats;
            }
        });
    }

    loadDateFormats();

    $scope.config = {
        createStockGroup: false,
        format: "amibroker" ,
        timeframe: "D1",
        path: "",
        exists: "overwrite",
        connection: 'History',
        postfix: '',
        barType: $scope.barDataTypes.startOfBar,
        dateFormat: 'ddMMyyyy'
    };

    $scope.getTimeframeLabel = function () {
        var timeframe = getItem($scope.timeframes, 'value', $scope.config.timeframe);
        return timeframe ? timeframe.type : '';
    }
});