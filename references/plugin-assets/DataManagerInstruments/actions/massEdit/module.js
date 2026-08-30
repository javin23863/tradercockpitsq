angular.module('app.instruments').config(function (sqPluginProvider) {

    sqPluginProvider.plugin('DataManagerActionInstrument', 90, {
        key: 'mass-edit',
        title: Ltsq('Mass edit instruments'),
        source: 'instruments',
        class: 'mass-edit',
        id: "datamanager-instruments-mass-edit"
    });
});