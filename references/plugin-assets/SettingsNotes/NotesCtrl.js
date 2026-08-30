angular.module('app.settings').controller('NotesCtrl', function ($rootScope, $scope, $q, $element, SQEvents, AppService, $timeout, L) {
    console.log("Notes controller initialized");

    var elemContentEditable = $($element).find(".notesContainer");

    function init() {
        settingsObj = AppService.getTaskConfig();
        var notesElem = getChildElement(settingsObj, 'Notes', true);

        var notesHtml = "";

        if (notesElem && notesElem.childNodes[0]) {
            notesHtml = notesElem.childNodes[0].nodeValue;
        }
        else {
            notesHtml = "";
        }

        elemContentEditable.html(notesHtml);
        try { $scope.$digest(); } catch (err) { }
    }

    elemContentEditable.on('focus', function () {
        var $this = $(this);
        $this.data('before', $this.html());
    }).on('blur keyup paste input', function () {
        var $this = $(this);
        if ($this.data('before') !== $this.html()) {
            $this.data('before', $this.html());
            $scope.noteEdited($this.html());
        }
    });

    $('.notesBold')[0].onmousedown = function () {
        document.execCommand('bold', null, null);
        return false;
    };

    $('.notesItalic')[0].onmousedown = function () {
        document.execCommand('italic', null, null);
        return false;
    };

    $('.notesUnderline')[0].onmousedown = function () {
        document.execCommand('underline', null, null);
        return false;
    };

    $('.notesOl')[0].onmousedown = function () {
        document.execCommand('insertOrderedList', null, null);
        return false;
    };

    $('.notesUl')[0].onmousedown = function () {
        document.execCommand('insertUnorderedList', null, null);
        return false;
    };

    $('.notesHr')[0].onmousedown = function () {
        document.execCommand('insertHTML', null, '<hr/>');
        return false;
    };
    $('.notesLink')[0].onmousedown = function () {
        var linkUrl = prompt("URL:", "http://");
        if (linkUrl != null) {
            document.execCommand('createLink', null, linkUrl);
        }
        return false;
    };

    $('.notesLeft')[0].onmousedown = function () {
        document.execCommand('justifyLeft', null, null);
        return false;
    };

    $('.notesRight')[0].onmousedown = function () {
        document.execCommand('justifyRight', null, null);
        return false;
    };

    $('.notesCenter')[0].onmousedown = function () {
        document.execCommand('justifyCenter', null, null);
        return false;
    };

    $('.notesIndentMore')[0].onmousedown = function () {
        document.execCommand('indent', null, null);
        return false;
    };

    $('.notesIndentLess')[0].onmousedown = function () {
        document.execCommand('outdent', null, null);
        return false;
    };

    $('.notesToolbar span').click(function () {
        $('.notesContainer').change();
    });

    //Saves max once per second. This function is called after every typed character.
    $scope.onSave = function () {
        AppService.updateTaskXML().then(function (result) {
            if (!result || !result.data.error) {
                $rootScope.showSuccess(L.tsq("Notes saved"));
                settingsChanged = false;
            }
            else {
                $rootScope.showError(L.tsq("Notes saving failed") + "-" + result.data.error);
            }
        });
    }

    //- Event Handlers --------------------------------------------------------------

    $scope.tab.shouldSaveSettings = function () {
        var shouldSaveSettings = settingsChanged;
        settingsChanged = false;
        return shouldSaveSettings;
    }

    $scope.noteEdited = function (htmlContent) {
        if (!$scope.tab.display || htmlContent === undefined) {
            return;
        }

        if (!settingsObj) {
            settingsObj = AppService.getTaskConfig();
            if (!settingsObj) {
                $rootScope.showError(L.tsq("Auto saving failed, Task config element not found"));
                return;
            }
        }

        addNode('Notes', htmlContent, settingsObj, AppService.xmlDoc);

        settingsChanged = true;
    }

    function onEvent(event, data) {
        if (event == SQEvents.get('SETTINGS_TAB_RELOAD')) {
            if ($scope.tab.display && (data == $scope.tab.title || data == 'all')) {
                init();
            }
        }
        else if (event == SQEvents.get('SETTINGS_TAB_ACTIVE')) {
            var active = data.title == $scope.tab.title;
            if (active) {
                if (!loaded) {
                    init();
                    loaded = true;
                }

                $timeout(function () {
                    $('.notesContainer')[0].focus();
                }, 0, false);
            }

            watchersHandler.onActivated(active);
        }
        else return;

        try { $scope.$digest(); } catch (er) { }
    }

    $scope.$on('$destroy', function () {
        SQEvents.removeListener(listenerId);
    });

    //- Initialization --------------------------------------------------------------

    var loaded = false;

    var settingsChanged = false;
    var settingsObj = null;

    var listenerId = "NotesCtrl";
    SQEvents.addListener(listenerId, [SQEvents.get('SETTINGS_TAB_RELOAD'), SQEvents.get('SETTINGS_TAB_ACTIVE')], onEvent);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function () { watchersHandler.onActivated(false); }, 0, false);

});