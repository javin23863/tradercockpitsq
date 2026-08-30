angular.module('app.sessions').controller('CloneSessionCtrl', function ($scope, $rootScope, SQConstants, $timeout, SessionService, SQEvents, L) {
    console.log("CloneSession controller initialized");    

    var grid;

    $scope.config = {
        name: null,
        clone: null,
        broker: -1
    };

    $scope.action.onSelect = function(sessions, grid2) {
        console.log("CloneSessionCtrl", sessions, grid2);

        grid = grid2;

        if (sessions.length==0) {
            $rootScope.showError(L.tsq('You have to select some session.'));
            return;
        }

        reloadBrokers();
        var sessionName = getCoreSessionName(sessions[0].name);
        $scope.brokerProfile = $scope.brokerProfiles[0];

        $scope.config.name =  sessions[0].name;
        $scope.config.clone =  sessionName+'Clone';
        $scope.config.broker =  $scope.brokerProfiles[0].id;

        showPopup('#cloneSessionModal');
    }

    function getCoreSessionName(sessionName){
        var sessionsForName = angular.copy(SQConstants.getConstants().sessions).filter(s=>s.name==sessionName);
        var brokerId = null;
        if (sessionsForName.length==1){
            brokerId = sessionsForName[0].broker;
        }

        var brokers = angular.copy(SQConstants.getConstants().brokers).filter(b=>b.id==brokerId);
        if (brokers.length==1){
            var postfix = brokers[0].postfix;
            if (sessionName.endsWith(postfix) && postfix!=''){
                sessionName = sessionName.slice(0, -postfix.length);
            }
        }

        return sessionName;
    }

    $scope.onCloneSession = function(form) {
        if (form) {
            $scope.errors = validate(form);
            if ($scope.errors) {
                console.error($scope.errors);
                return;
            }
        }

        
        SessionService.clone($scope.config, function (result) {
            hidePopup('#cloneSessionModal');
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