angular.module('app.instruments').controller('CloneInstrumentCtrl', function ($scope, $rootScope, SQConstants, $timeout, InstrumentService, SQEvents, L) {
    console.log("CloneInstrument controller initialized");    

    var grid;

    $scope.config = {
        name: null,
        clone: null,
        broker: -1
    };

    $scope.action.onSelect = function(instruments, grid2) {
        console.log("CloneInstrumentCtrl", instruments, grid2);

        grid = grid2;

        if (instruments.length==0) {
            $rootScope.showError(L.tsq('You have to select some instrument.'));
            return;
        }

        reloadBrokers();
        var instrumentName = getCoreInstrumentName(instruments[0].name);
        $scope.brokerProfile = $scope.brokerProfiles[0];

        $scope.config.name =  instruments[0].name;
        $scope.config.clone =  instrumentName+'Clone';
        $scope.config.broker =  $scope.brokerProfiles[0].id;

        showPopup('#cloneInstrumentModal');
    }

    function getCoreInstrumentName(instrumentName){
        var instrumentsForName = angular.copy(SQConstants.getConstants().instruments).filter(s=>s.instrument==instrumentName);
        var brokerId = null;
        if (instrumentsForName.length==1){
            brokerId = instrumentsForName[0].broker;
        }

        var brokers = angular.copy(SQConstants.getConstants().brokers).filter(b=>b.id==brokerId);
        if (brokers.length==1){
            var postfix = brokers[0].postfix;
            if (instrumentName.endsWith(postfix) && postfix!=''){
                instrumentName = instrumentName.slice(0, -postfix.length);
            }
        }

        return instrumentName;
    }

    $scope.onCloneInstrument = function(form) {
        if (form) {
            $scope.errors = validate(form);
            if ($scope.errors) {
                console.error($scope.errors);
                return;
            }
        }

        
        InstrumentService.clone($scope.config, function (result) {
            hidePopup('#cloneInstrumentModal');
        });
    }

    function reloadBrokers(){
        var brokerProfiles = angular.copy(SQConstants.getConstants().brokers).filter(b=>b.mtUse);
        brokerProfiles.unshift({
            id:-1,
            name:'SQ default'
        });
        $scope.brokerProfiles = brokerProfiles;
    }    

    $scope.onBrokerChange = function(){
        $scope.brokerProfile = getItem($scope.brokerProfiles, 'id', $scope.config.broker);
    }

    $scope.getBrokerProfileLabel = function(){
        var brokerProfile = getItem($scope.brokerProfiles, 'id', $scope.config.broker);
        return brokerProfile ? brokerProfile.name : '';
    } 
});