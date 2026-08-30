angular.module('app.settings').controller('SettingsNotificationCtrl', function ($rootScope, $scope, $timeout, $q, $element, AppService, SettingsNotificationService, SQEvents) {
    console.log("SettingsNotification controller initialized");

    function init() {
        loaded = false;

        SettingsNotificationService.loadSettings($scope.config);

        loaded = true;
    }

    $scope.getNotificationTypeLabel = function(){
        var type = getItem($scope.config.types, 'key', $scope.config.type);
        return type ? type.name : '';
    }

    //- Event Handlers --------------------------------------------------------------

    $scope.tab.shouldSaveSettings = function(){
        var shouldSaveSettings = settingsChanged;
        settingsChanged = false;
        return shouldSaveSettings;
    }

    function onSettingsChange(){
        if(!loaded) {
            return;
        }

        settingsChanged = true;
        SettingsNotificationService.saveSettings($scope.config);
    }

    var destroyConfigWatch = $scope.$watch("config", function(newValue, oldValue) {        
        onSettingsChange();
    }, true);

    function onEvent(event, data){
        if(event == SQEvents.get('SETTINGS_TAB_ACTIVE')){
            var active = data.title == $scope.tab.title;    
            watchersHandler.onActivated(active);

        } else if(event == SQEvents.get('SETTINGS_TAB_RELOAD')) {
            if($scope.tab.display && (data == $scope.tab.title || data == 'all')) {
                init();
            }
        }
        else return;
        
        try { $scope.$digest(); } catch(er) {}
    }

    $scope.$on('$destroy', function() {
        destroyConfigWatch();
        SQEvents.removeListener(listenerId);
    });

    //- Initialization --------------------------------------------------------------

    var loaded = false;
    var settingsChanged = false;
    var saveDeferred = $q.defer();

    $scope.config = SettingsNotificationService.config;

    var listenerId = "SettingsNotificationCtrl";
    SQEvents.addListener(listenerId, [SQEvents.get('SETTINGS_TAB_ACTIVE'), SQEvents.get('RELOAD_DATABANKS'), SQEvents.get('SETTINGS_TAB_RELOAD')], onEvent);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function(){ watchersHandler.onActivated(false); }, 0, false);

});