angular.module('app.datasource.yahoo', ['sqplugin']).config(function (sqPluginProvider) {

    sqPluginProvider.plugin(
        "DataSourceYahoo",
        10, {
            title: Ltsq('Yahoo') + ':',
            class: "yahoo",
            source: 'data',
            id: "datamanager-datasource-yahoo-menu"
        }
    );
});