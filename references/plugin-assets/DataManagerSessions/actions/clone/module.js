angular.module('app.sessions').config(function(sqPluginProvider) {
      
    sqPluginProvider.plugin('DataManagerActionSession', 80, {
        title: Ltsq('Clone session'),
        templateUrl: '../../../plugins/DataManagerSessions/actions/clone/cloneSession.html',
        controller: 'CloneSessionCtrl',
        source: 'sessions',
        class: 'clone',
        id: "datamanager-session-clone"
    });
});