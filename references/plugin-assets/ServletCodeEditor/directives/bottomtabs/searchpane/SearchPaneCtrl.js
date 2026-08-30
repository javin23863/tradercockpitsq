angular.module('app').controller('SearchPaneCtrl', function ($scope, L, $timeout, SQEvents) {
    console.log("SearchPaneCtrl controller initialized");

    $scope.info = L.tsq("No search results available.");

    var tree;

    $timeout(function () {

        tree = new sqTree("search-tree");

        tree.onDblClick = function (fileType, filePath, id, name) {
            var file = tree.getUserData(id, "file");
            if (!file) {
                return true;
            }

            var line = tree.getUserData(id, "line");

            SQEvents.notifyListeners(SQEvents.get('EDITOR_HIGHLIGHT_LINE'), {
                file: file,
                line: line
            });
        }

    });

    function onEvent(event, eventData) {
        if (event == SQEvents.get('EDITOR_SEARCH_RESULT')) {
            data = eventData;

            $scope.info = L.tsq("Searched '%s' - %d matches", [data.text, data.matches]);

            tree.loadJson(data);
            tree.collapseAll();

            SQEvents.notifyListeners(SQEvents.get('EDITOR_SELECT_BOTTOM_TAB'), 'search');
        }
        else return;

        try { $scope.$digest(); } catch (err) { }
    }

    $scope.$on('$destroy', function () {
        SQEvents.removeListener(listenerId);
    });

    var listenerId = "SearchPaneCtrl";
    SQEvents.addListener(listenerId, [SQEvents.get('EDITOR_SEARCH_RESULT')], onEvent);

});