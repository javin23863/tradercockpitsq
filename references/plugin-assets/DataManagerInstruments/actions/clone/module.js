angular.module('app.instruments').config(function(sqPluginProvider) {
      
    sqPluginProvider.plugin('DataManagerActionInstrument', 80, {
        title: Ltsq('Clone instrument'),
        templateUrl: '../../../plugins/DataManagerInstruments/actions/clone/cloneInstrument.html',
        controller: 'CloneInstrumentCtrl',
        source: 'instruments',
        class: 'clone',
        id: "datamanager-instruments-clone"
    });
});