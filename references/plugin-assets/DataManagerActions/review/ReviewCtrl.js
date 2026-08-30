angular.module('app.data').controller('ReviewCtrl', function ($scope, $rootScope, SQConstants, DMDataService, L, $timeout, SQEvents) {
    console.log("Review controller initialized");

    $scope.dataDetails = null;

	$scope.dataEditing = {changed:false};
    $scope.timeframes = [];
    $scope.timeframe = null;
    $scope.importedTimeframe = null;

    $scope.sessions = SQConstants.getConstants().sessions;
    $scope.session = $scope.sessions[0].name;

    var TF = SQConstants.getConstants().timeframe;

    var availableTimeframes = [{
            type: TF.TICK,
            mins: 0
        },
        {
            type: TF.M1,
            mins: 1
        },
        {
            type: TF.M5,
            mins: 5
        },
        {
            type: TF.M15,
            mins: 15
        },
        {
            type: TF.M30,
            mins: 30
        },
        {
            type: TF.H1,
            mins: 60
        },
        {
            type: TF.H4,
            mins: 240
        },
        {
            type: TF.D1,
            mins: 1440
        },
        {
            type: TF.Weekly,
            mins: 7 * 1440
        },
        {
            type: TF.Monthly,
            mins: 4 * 7 * 1440
        },
    ];

    try{
        var allAvailableTimeframes = SQConstants.getConstants().timeframes;
        allAvailableTimeframes.forEach(function(iAllItem){
            var exists = availableTimeframes.filter(function(i){
                return i.type==iAllItem
            }).length>0;
            if (!exists){
                let firstChar = iAllItem[0];
                let factor = 1;

                let value = iAllItem.substr(1);
                value = parseInt(value);
                let mins = 0;
                if (firstChar=='M'){
                    mins = value;
                }else if (firstChar=='H'){
                    mins = value*60;
                }else if (firstChar=='D'){
                    mins = value*1440;
                }else if (firstChar=='W'){
                    mins = value*7 * 1440;
                }else{
                    return;
                }

                availableTimeframes.push({
                    type: iAllItem,
                    mins: mins
                })
            }
        });
    }catch(err){
        console.error('Error while adding custom timeframes', err);
    }

    $scope.mainTabs = {
        tabs: [{
                title: 'Data',
                dataItem: 'dm-review-data',
                templateUrl: '../../../plugins/DataManagerActions/review/data/reviewData.html',
                controller: 'ReviewDataCtrl',
            },
            {
                title: 'Chart',
                dataItem: 'dm-review-chart',
                templateUrl: '../../../plugins/DataManagerActions/review/chart/reviewChart.html',
                controller: 'ReviewChartCtrl',
            },
            {
                title: 'Analyze data quality',
                dataItem: 'dm-review-quality',
                templateUrl: '../../../plugins/DataManagerActions/review/quality/quality.html',
                controller: 'QualityCtrl',
            }
        ],
        onSelectTab: onTabChange,
    }

    $scope.action.onSelect = function (symbols, grid) {
        console.log("ReviewCtrl");

        if (symbols.length == 0) {
            $rootScope.showError('You have to select a symbol.');
            return;
        }

        if(symbols.customIndicatorData) {
            SQEvents.notifyListeners(SQEvents.get("CUSTOM_DATA_VIEW"), symbols);
            return;
        }

        symbols = DMDataService.filterSymbolsInProgress(symbols, grid);
        if (symbols.length == 0) {
            return;
        }

        $scope.dataDetails = angular.copy(symbols[0]);
        if ($scope.dataDetails.totalRecords == 0) {
            $rootScope.showError(L.tsq("Symbol '%s' doesn't contain any data", [$scope.dataDetails.symbol]));
            return;
        }

        $scope.importedTimeframe = $scope.dataDetails.timeframe;
        $scope.timeframe = $scope.dataDetails.timeframe;
        getTimeframes($scope.timeframe);

        var item = getItem($scope.sessions, "name", $scope.session);
        if(!item && $scope.sessions.length>0) {
            $scope.session = $scope.sessions[0].name;
        }

        print(true);

        showPopup('#reviewDataModal');

        window.dispatchEvent(new Event('resize'));
    }

    function findTimeframeByType(type) {
        for (var i = 0; i < availableTimeframes.length; i++) {
            if (availableTimeframes[i].type == type) {
                return availableTimeframes[i];
            }
        }

        return null;
    }

    function getTimeframes(timeframeType) {
        $scope.timeframes.length = 0;

        if (timeframeType == 'Intraday') {
            $scope.timeframes.push({
                name: TF.Intraday,
                type: TF.Intraday
            });

            return;
        }

        var timeframe = findTimeframeByType(timeframeType);

        for (var i = 0; i < availableTimeframes.length; i++) {
            if (!timeframe || availableTimeframes[i].mins >= timeframe.mins) {
                $scope.timeframes.push(availableTimeframes[i]);
            }
        }
    }

    function onTabChange(tab) {
        if (tab && tab.updateChart) {
            tab.updateChart();
        }
		if (tab && tab.dataEditingObj){
			tab.dataEditingObj($scope.dataEditing);
		}
		
        if (tab.dataItem == "dm-review-data" && $scope.dataDetails) {
            $timeout(function () {
                $scope.dataDetails.timeframe = $scope.timeframe;
                $scope.dataDetails.session = $scope.session;
                
                $scope.mainTabs.tabs[0].print($scope.dataDetails);
            }, 10, false);
        }
    }
	
	$scope.onSave = function(){
		if ($scope.dataEditing.changed==false){
			return;
		}
		
		var dataTab = $scope.mainTabs.tabs[0];
		dataTab.save();
	}

    $scope.onSettingsChange = function () {
        print();
    }

    function print(showFirstTab) {
        if (showFirstTab) {
            $scope.mainTabs.selectTab('dm-review-data');
        }

        $scope.dataDetails.timeframe = $scope.timeframe;
        $scope.dataDetails.session = $scope.session;

        $scope.mainTabs.tabs[0].print($scope.dataDetails);
        $scope.mainTabs.tabs[1].print($scope.dataDetails);
        $scope.mainTabs.tabs[2].print($scope.dataDetails);

        try {
            $scope.$digest();
        } catch (err) {}
    }

});