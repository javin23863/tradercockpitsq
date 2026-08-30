angular.module('app.sessions').config(function (sqPluginProvider) {

    sqPluginProvider.plugin('DataManagerActionSession', 10, {
        key: 'add',
        title: Ltsq('Add session'),
        source: 'sessions',
        class: 'add',
        id: "datamanager-session-add"
    });
});