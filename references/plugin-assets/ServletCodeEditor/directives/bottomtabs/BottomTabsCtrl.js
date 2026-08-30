angular.module('app').controller('BottomTabsCtrl', function ($scope, $rootScope, SQEvents) {
    console.log("BottomTabsCtrl controller initialized");
   
    $scope.bottomTabs = {
        tabs: [
            {
                title: 'Log',
                dataItem: 'log',
                templateUrl: '../../../plugins/ServletCodeEditor/directives/bottomtabs/logpane/logpane.html',
                controller: 'LogPaneCtrl',
            },
            {
                title: 'Search',
                dataItem: 'search',
                templateUrl: '../../../plugins/ServletCodeEditor/directives/bottomtabs/searchpane/searchpane.html',
                controller: 'SearchPaneCtrl',
            }
        ]
    }      
    
    function onEvent(event, data){
        if(event == SQEvents.get('EDITOR_SELECT_BOTTOM_TAB')){
            $scope.bottomTabs.selectTab(data);
        }
        else return;
        
        try { $scope.$digest(); } catch(err) {}
    }
    
    $scope.$on('$destroy', function(){
        SQEvents.removeListener(listenerId);
    });

    var listenerId = "BottomTabsCtrl";
    SQEvents.addListener(listenerId, [SQEvents.get('EDITOR_SELECT_BOTTOM_TAB')], onEvent);

});