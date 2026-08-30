//product:BUILDER
//product:RETESTER
//product:OPTIMIZER
//product:TASKMANAGER
angular.module('app.databank.views', []).config(function(sqPluginProvider, SQEventsProvider) {
    
    sqPluginProvider.addPopupWindow("../../../plugins/ResultsDatabankViews/databankViews.html", 'DatabankViewsCtrl', 'SQUANT');

    sqPluginProvider.plugin("ResultsDatabankOptionsAction", 110, {
        title: Ltsq("View")+":",
        class: "pull-right db-views-label",
        controller: ViewSelectCtrl
    });

    SQEventsProvider.add("DB_VIEW_EDIT", "databank-view-edit");
    SQEventsProvider.add("DB_VIEW_CHANGED", "databank-view-changed");             //called after active databank view changed
    SQEventsProvider.add("DATABANK_VIEWS_LOADED", "databank-views-reload");       //called to reload the list of views
    
    function ViewSelectCtrl($rootScope, $scope, UniqueIdGenerator, DatabankViewsService, SQEvents, SQConstants, AppService){

        $scope.onChange = function(tab){
            $scope.selectedOption.value = $scope.selectedOption.value || tab.view;

            DatabankViewsService.changeView(AppService.getProject(), AppService.getDatabank().title, $scope.selectedOption.value, function(){
                tab.view = $scope.selectedOption.value;
                AppService.getDatabank().view = $scope.selectedOption.value;

                SQEvents.notifyListeners(SQEvents.get("DB_VIEW_CHANGED"), $scope.selectedOption.value);
                window.parent.broadcastEvent(SQEvents.get("DB_VIEW_CHANGED"), $scope.selectedOption.value);     //to load last selected view in manage views dialog
            });
        }

        function loadViews(){
            var viewOptions = [];
            var views = angular.copy(SQConstants.getConstants().databankViews);
            var keepCurrentView = false;

            for(var i=0; i<views.length; i++){
                var view = views[i];
                viewOptions.push({title: view.name});
                
                if(view.name == $scope.selectedOption.value){
                    keepCurrentView = true;
                }
            }

            $scope.combo.options = sortByKey(viewOptions, 'title');

            if(!keepCurrentView){
                $scope.selectedOption.value = SQConstants.getConstants().defaultDatabankViews.main;
            }
        }

        function onEvent(event, data){
            if(event == SQEvents.get('DATABANK_VIEWS_LOADED')){
                loadViews();

            } else if(event == SQEvents.get('DATABANK_CHANGED')) {
                if(data.view){      //new databank creation - data = {}
                    $scope.selectedOption.value = data.view;
                }
            } else if(event == SQEvents.get('DATABANK_VIEW_SELECTED')) {
                $scope.selectedOption.value = data;
            }
            else return;

            try { $scope.$digest(); } catch(er) {}
        }

        $scope.$on('$destroy', function(){
            SQEvents.removeListener(listenerId);
            UniqueIdGenerator.removeId(uniqueId);
        });

        $scope.combo.options = [];
        $scope.selectedOption = {
            value : SQConstants.getConstants().defaultDatabankViews.main
        };

        loadViews();

        var uniqueId = UniqueIdGenerator.generateId();
        var listenerId = "ViewSelectCtrl-" + uniqueId;
        SQEvents.addListener(listenerId, [SQEvents.get('DATABANK_VIEWS_LOADED'), SQEvents.get('DATABANK_CHANGED'), SQEvents.get('DATABANK_VIEW_SELECTED')], onEvent);
    }

});