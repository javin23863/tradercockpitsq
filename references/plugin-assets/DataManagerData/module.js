angular.module('app.data', ['sqplugin']).config(function (sqPluginProvider, SQEventsProvider) {

    SQEventsProvider.add("DATA_ITEMS_SELECTED", 'data-items-selected');
    SQEventsProvider.add("ADD_INSTRUMENT", 'add-instrument');
    SQEventsProvider.add("INSTRUMENT_CREATED", 'instrument-created');
    SQEventsProvider.add("DOWNLOAD_DATA", 'download-data');

    sqPluginProvider.plugin("MainTab", 10, {
        title: Ltsq('Data'),
        help: Ltsq('Data are imported historical data into the program. Data manager allows you to import history from files, or to download it directly from online sources.<br/>Every data is linked with instrument that contains the definition of the imported symbol.'),
        dataItem: 'data',
        templateUrl: '../../../plugins/DataManagerData/data.html',
        controller: 'DMDataCtrl',
        menuActive: 'data-sources'
    });
});