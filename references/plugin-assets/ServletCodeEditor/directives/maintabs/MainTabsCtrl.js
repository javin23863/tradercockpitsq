angular
  .module("app")
  .controller(
    "MainTabsCtrl",
    function (
      $scope,
      $rootScope,
      $timeout,
      SQEvents,
      L,
      CodeEditorService,
      SkinService,
      UniqueIdGenerator
    ) {
      console.log("MainTabsCtrl controller initialized");

      $scope.tabs = [];

      var splitComponentEditors = null;

      CodeEditorService.openedTabs = $scope.tabs;

      $scope.activeTab = null;
      $scope.splitEditTab = null;
      $scope.splitEditTabEditor = null;
      $scope.splitEditTabEditorId = generateEditorID();

      $scope.splitEditIconTooltipOpen = L.tsq("Open Split Editor");
      $scope.splitEditIconTooltipClose = L.tsq("Close Split Editor");

      $scope.instance.add = function (
        file,
        name,
        text,
        select,
        _protected,
        dontNotifyTabsChange,
        callback
      ) {
        var editorId = generateEditorID();

        var item = getItem($scope.tabs, "file", file);
        if (item) {
          console.log("File is already opened in editor.", file);
          return;
        }

        var tab = {
          file: file,
          title: name,
          dataItem: "item" + $scope.tabs.length,
          editorId: editorId,
          editor: null,
          changed: false,
          onClose: onTabClose,
          protected: _protected,
          hasMenu: false,
        };

        if ($scope.tabs.length == 0 || select) {
          $scope.activeTab = tab.file;

          /*             for (var i = 0; i < $scope.tabs.length; i++) {
                            $scope.tabs[i].active = false;
                        } */
          $timeout(function () {
            SQEvents.notifyListeners(SQEvents.get("EDITOR_ACTIVE_CHANGED"), {
              file: tab.file,
            });
          }, 100);
        }

        $scope.tabs.push(tab);

        createEditor(tab, text, dontNotifyTabsChange, callback);

        if (!dontNotifyTabsChange) {
          editorTabsChanged();
        }

        return tab;
      };

      function generateEditorID() {
        while (true) {
          var id =
            "mc-editor-" +
            Math.round(
              Math.pow(36, length + 1) - Math.random() * Math.pow(36, 12)
            )
              .toString(36)
              .slice(1);

          var item = getItem($scope.tabs, "editorId", id);
          if (!item) {
            return id;
          }
        }
      }

      $scope.onTabClose = function (tab) {
        onTabClose(tab);
      };

      function onTabClose(tab) {
        if (tab.changed) {
          $rootScope.showConfirm(
            L.tsq("Warning"),
            L.tsq(
              "File '%s' contains unsaved changes.<br>Do you really want to close it?",
              [tab.title]
            ),
            function (confirmed) {
              if (confirmed) {
                closeTab(tab);
              }
            }
          );
        } else {
          closeTab(tab);
        }
      }

      function closeTab(tab) {
        var tabs = $scope.tabs;

        for (var i = 0; i < tabs.length; i++) {
          if (tabs[i] == tab) {
            //remove tab

            if ($scope.activeTab == tab.file) {
              if (i < tabs.length - 1) {
                $scope.activeTab = tabs[i + 1].file;
              } else {
                $scope.activeTab = tabs.length > 1 ? tabs[i - 1].file : null;
              }
            }

            tab.editor.dispose();

            $scope.tabs.splice(i, 1);
            editorTabsChanged();

            if (
              $scope.splitEditTab !== null &&
              $scope.splitEditTab.file == tab.file
            ) {
              $scope.closeSplitEditor();
            }

            var tab = $scope.instance.getActiveTab();
            if (tab) {
              $timeout(function () {
                tab.editor.layout();
              }, 100);
            }
            break;
          }
        }
      }

      function editorTabsChanged() {
        if ($scope.tabsChanged) {
          $scope.tabsChanged();
        }

        $timeout(
          function () {
            moveTabWhiteUnderline();
            handleHeaderTabs();
          },
          0,
          false
        );

        var files = "";

        var tabs = $scope.tabs;
        for (var i = 0; i < tabs.length; i++) {
          files += tabs[i].file + ",";
        }

        CodeEditorService.saveLastOpenedFiles(files);

        try {
          $scope.$digest();
        } catch (err) {}
      }

      function getMode(file) {
        var ext = file.split(".").pop().toLowerCase();

        switch (ext) {
          case "mqh":
          case "mq4":
            return "cpp";
          case "inc":
          case "tpl":
            return "ace/mode/ftl";
          case "xml":
            return "xml";
          case "html":
            return "html";
        }

        return "java";
      }

      function getIntroCommentPosition(txt) {
        var lines = txt.split("\n");
        for (var i = 0; i < lines.length; i++) {
          var line = lines[i];
          if (line.indexOf("/*") != -1) {
            return {
              column: 1,
              lineNumber: i + 1,
            };
          } else if (line.trim() !== "") {
            break;
          }
        }
        return null;
      }

      function createEditor(
        tab,
        text,
        dontNotifyTabsChange,
        callback,
        isSplitEditor
      ) {
        require.config({
          paths: {
            vs: "libs/vs",
          },
        });

        var editorId = isSplitEditor
          ? $scope.splitEditTabEditorId
          : tab.editorId;

        require(["vs/editor/editor.main"], function () {
          $timeout(function () {
            var skin = SkinService.selectedSkin.name;
            var editor = monaco.editor.create(
              document.getElementById(editorId),
              {
                value: [text].join("\n"),
                readOnly: tab.protected,
                language: getMode(tab.file),
                fontSize: 12,
                contextmenu: true,
                suggestFontSize: 12,
                suggestLineHeight: 20,
                suggest: {
                  hideStatusBar: false,
                  insertMode: "replace",
                },
                minimap: {
                  enabled: false,
                },
                scrollbar: {
                  useShadows: false,

                  verticalHasArrows: true,
                  horizontalHasArrows: true,

                  vertical: "visible",
                  horizontal: "visible",

                  verticalScrollbarSize: 17,
                  horizontalScrollbarSize: 17,
                  arrowSize: 20,
                },
              },
              {
                storageService: {
                  get() {},
                  getBoolean(key) {
                    if (key === "expandSuggestionDocs") return true;

                    return false;
                  },
                  store() {},
                  onWillSaveState() {},
                  onDidChangeStorage() {},
                },
              }
            );

            editor.layout();
            editor.getModel().onDidChangeContent(function (e) {
              tab.changed = true;
              tabStatusChanged(tab);
              if (isSplitEditor) {
                var valueOld = tab.editor.getValue();
                var valueNew = editor.getValue();
                if (valueOld != valueNew) {
                  tab.editor.setValue(valueNew);
                }
              } else if (
                $scope.splitEditTab != null &&
                $scope.splitEditTab.file == tab.file
              ) {
                var valueOld = $scope.splitEditTabEditor.getValue();
                var valueNew = editor.getValue();
                if (valueOld != valueNew) {
                  $scope.splitEditTabEditor.setValue(valueNew);
                }
              }
            });

            if (isSplitEditor) {
              $scope.splitEditTabEditor = editor;
            } else {
              tab.editor = editor;
            }

            tab.minVersion = editor.getModel().getAlternativeVersionId();
            tab.maxVersion = editor.getModel().getAlternativeVersionId();

            //fold all comments
            var position = getIntroCommentPosition(editor.getValue());
            if (position != null) {
              editor.setPosition(position);
              editor.getAction("editor.fold").run();
            }

            if (!dontNotifyTabsChange) {
              editor.focus();
            }

            if (callback) {
              callback();
            }
          });
        });
      }

      $(window).on("resize", function () {
        var tab = $scope.instance.getActiveTab();
        if (tab) {
          $timeout(function () {
            tab.editor.layout();
            if ($scope.splitEditTabEditor != null) {
              $scope.splitEditTabEditor.layout();
            }
          }, 100);
        }
      });

      $scope.afterInit();

      function tabStatusChanged(tab) {
        var model = tab.editor.getModel();
        var currentVersionId = model.getAlternativeVersionId();

        if (tab.maxVersion < currentVersionId) {
          tab.maxVersion = currentVersionId;
        }

        //$scope.instance.tabStatusChanged(um.hasUndo(), um.hasRedo(), tab.changed);
        $scope.instance.tabStatusChanged(
          currentVersionId > tab.minVersion,
          currentVersionId < tab.maxVersion,
          tab.changed
        );
      }

      $scope.onSelectTab = function (tab) {
        var tabs = $scope.tabs;

        var activeTab = null;

        for (var i = 0; i < tabs.length; i++) {
          if (tabs[i].file == tab.file) {
            activeTab = tabs[i];
            $scope.activeTab = tabs[i].file;

            SQEvents.notifyListeners(SQEvents.get("EDITOR_ACTIVE_CHANGED"), {
              file: $scope.activeTab,
            });

            break;
          }
        }

        if (activeTab) {
          tabStatusChanged(activeTab);
        }

        try {
          $scope.$digest();
        } catch (err) {}

        if (activeTab) {
          //editor.focus can't be called when modal dialog is shown above!
          let confirmDialog = document.getElementById('confirmDialog');
          if (!confirmDialog || confirmDialog.style.display!='block'){
            //focus editor after tab selection
            $timeout(function () {
                console.log('Layout editor');
                activeTab.editor.layout();

                console.log('Focus editor');
                activeTab.editor.focus();
            });
          }  else {
            console.log('Skipping editor focus - confirm dialog is shown');
          }
        }else{
          console.log('No active editor found');
        }

        return activeTab;
      };

      $scope.instance.getActiveTab = function () {
        var tabs = $scope.tabs;

        for (var i = 0; i < tabs.length; i++) {
          if (tabs[i].file == $scope.activeTab) {
            return tabs[i];
          }
        }
      };

      $scope.instance.getTabsOrig = function () {
        return $scope.tabs;
      };

      $scope.instance.getTabs = function () {
        var tabs = [];

        for (var i = 0; i < $scope.tabs.length; i++) {
          var data = {
            file: $scope.tabs[i].file,
            data: $scope.tabs[i].editor.getValue(),
            active: $scope.tabs[i].file == $scope.activeTab,
          };

          tabs.push(data);
        }

        return tabs;
      };

      $scope.instance.getEditors = function () {
        var editors = [];

        for (var i = 0; i < $scope.tabs.length; i++) {
          editors.push($scope.tabs[i].editor);
        }

        return editors;
      };

      $scope.instance.selectTab = function (file) {
        var tabs = $scope.tabs;

        for (var i = 0; i < tabs.length; i++) {
          if (tabs[i].file == file) {
            return $scope.onSelectTab(tabs[i]);
          }
        }
      };

      $scope.instance.closeTab = function (file) {
        var tabs = $scope.tabs;

        for (var i = tabs.length - 1; i >= 0; i--) {
          if (tabs[i].file == file) {
            closeTab(tabs[i]);

            break;
          }
        }
      };

      $scope.instance.getTab = function (file) {
        var tabs = $scope.tabs;

        for (var i = 0; i < tabs.length; i++) {
          if (tabs[i].file == file) {
            return tabs[i];
          }
        }

        return null;
      };

      $scope.instance.undo = function () {
        var tab = $scope.instance.getActiveTab();
        if (!tab || !tab.editor) {
          return;
        }

        tab.editor.getModel().undo();
      };

      $scope.instance.redo = function () {
        var tab = $scope.instance.getActiveTab();
        if (!tab || !tab.editor) {
          return;
        }

        tab.editor.getModel().redo();
      };

      $.contextMenu({
        selector: "#mainTabs .editor-part-main .tabs-header",
        callback: function (itemKey, opt, e) {
          $scope.onMenuItemClick(itemKey, $scope.clickedTab);
        },
        items: {
          close: {
            name: "Close",
          },
          "close-all": {
            name: "Close all",
          },
          "close-others": {
            name: "Close Others",
          },
          "close-left": {
            name: "Close Tabs to the Left",
          },
          "close-right": {
            name: "Close Tabs to the Right",
          },
          "split-edit": {
            name: "Open in Split Editor",
          },
          "show-navigator": {
            name: "Show in Navigator",
          },
        },
      });

      $.contextMenu({
        selector: "#mainTabs .editor-part-split .tabs-header",
        callback: function (itemKey, opt, e) {
          if (itemKey == "close") {
            $scope.closeSplitEditor();
          }
          if (itemKey == "open-in-main") {
            $scope.onSelectTab($scope.splitEditTab);
          }
        },
        items: {
          close: {
            name: "Close",
          },
          "open-in-main": {
            name: "Show in Main editor",
          },
        },
      });

      // scrolling header
      var headerSelector = $(
        ".codeeditor-content #mainTabs .quant-tabs .tabs-header"
      )[0];
      function scrollHorizontally(e) {
        e = window.event || e;
        var delta = Math.max(-1, Math.min(1, e.wheelDelta || -e.detail));
        headerSelector.scrollLeft -= delta * 40; // Multiplied by 40
        e.preventDefault();
        moveTabWhiteUnderline();
      }
      if (headerSelector.addEventListener) {
        // IE9, Chrome, Safari, Opera
        headerSelector.addEventListener(
          "mousewheel",
          scrollHorizontally,
          false
        );
        // Firefox
        headerSelector.addEventListener(
          "DOMMouseScroll",
          scrollHorizontally,
          false
        );
      } else {
        // IE 6/7/8
        headerSelector.attachEvent("onmousewheel", scrollHorizontally);
      }

      // scrolling buttons:
      function isHeaderOverflown() {
        let element = $(
          ".codeeditor-content #mainTabs .quant-tabs .tabs-header"
        )[0];
        return element.scrollWidth > element.clientWidth;
      }

      function handleHeaderTabs() {
        if (isHeaderOverflown()) {
          $(".codeeditor-content #mainTabs #left-editor-component").addClass(
            "overflown"
          );
        } else {
          $(".codeeditor-content #mainTabs #left-editor-component").removeClass(
            "overflown"
          );
        }
      }

      $scope.clickedTab = null;

      $scope.showMenu = function (e, tab) {
        $scope.clickedTab = tab;
      };

      $scope.onMenuItemClick = function (action, tab) {
        switch (action) {
          case "close":
            onTabClose(tab);
            break;

          case "close-all":
            for (var i = $scope.tabs.length - 1; i >= 0; i--) {
              onTabClose($scope.tabs[i]);
            }
            break;

          case "close-others":
            for (var i = $scope.tabs.length - 1; i >= 0; i--) {
              if ($scope.tabs[i] == tab) continue;

              onTabClose($scope.tabs[i]);
            }
            break;

          case "close-left":
            var close = false;

            for (var i = $scope.tabs.length - 1; i >= 0; i--) {
              if ($scope.tabs[i] == tab) {
                close = true;
                continue;
              }

              if (close) {
                onTabClose($scope.tabs[i]);
              }
            }
            break;

          case "close-right":
            for (var i = $scope.tabs.length - 1; i >= 0; i--) {
              if ($scope.tabs[i] == tab) break;

              onTabClose($scope.tabs[i]);
            }

            break;
          case "split-edit":
            $scope.openSplitEditor(tab);
            break;
          case "show-navigator":
            $scope.showInNavigator(tab.file);
            break;
        }
      };

      $scope.showInNavigator = function (file) {
        CodeEditorService.showInNavigator(file);
      };

      $scope.openSplitEditor = function (tab) {
        if (tab == undefined) {
          for (var i = 0; i < $scope.tabs.length; i++) {
            if ($scope.activeTab == $scope.tabs[i].file) {
              tab = $scope.tabs[i];
            }
          }
        }
        $scope.splitEditTab = tab;
        if ($scope.splitEditTabEditor != null) {
          $scope.splitEditTabEditor.dispose();
        }
        createEditor(tab, tab.editor.getValue(), false, false, true);

        splitComponentEditors = Split(
          ["#left-editor-component", "#right-editor-component"],
          {
            minSize: 200,
            direction: "horizontal",
            snapOffset: 0,
            onDragEnd: function () {
              window.dispatchEvent(new Event("resize"));
            },
          }
        );

        window.dispatchEvent(new Event("resize"));
        $timeout(
          function () {
            $scope.splitEditTabEditor.layout();
            $scope.splitEditTabEditor.focus();
          },
          100,
          false
        );
      };

      $scope.closeSplitEditor = function () {
        splitComponentEditors.destroy();
        $scope.splitEditTab = null;
        if ($scope.splitEditTabEditor != null) {
          $scope.splitEditTabEditor.dispose();
          $scope.splitEditTabEditor = null;
        }
        window.dispatchEvent(new Event("resize"));
        $timeout(
          function () {
            window.dispatchEvent(new Event("resize"));
          },
          100,
          false
        );
      };

      $(window).on("resize", function () {
        moveTabWhiteUnderline();
        handleHeaderTabs();
      });

      var listenerId = "EditorMainTabsCtrl";
      SQEvents.addListener(
        listenerId,
        [SQEvents.get("EDITOR_ACTIVE_CHANGED")],
        onEvent
      );

      function onEvent(event, data) {
        if (event == SQEvents.get("EDITOR_ACTIVE_CHANGED")) {
          for (var i = 0; i < $scope.tabs.length; i++) {
            if ($scope.tabs[i].file == data.file) {
              if ($("#mainTabs .tabs-header div").get(i)) {
                $("#mainTabs .tabs-header div").get(i).scrollIntoView();
              }
              break;
            }
          }
          moveTabWhiteUnderline();
        }
      }

      function moveTabWhiteUnderline() {
        $timeout(
          function () {
            var mtOffset = $(
              "#mainTabs .editor-part-main .tabs-header"
            ).offset();
            var mtWidth = $(
              "#mainTabs .editor-part-main .tabs-header"
            ).outerWidth(true);
            var offset = $(
              "#mainTabs .editor-part-main .tabs-header div.active"
            ).offset();
            if (!offset) {
              return;
            }
            var x = offset.left;
            var y = offset.top;
            var w = $(
              "#mainTabs .editor-part-main .tabs-header div.active"
            ).outerWidth(true);
            var h = $(
              "#mainTabs .editor-part-main .tabs-header div.active"
            ).outerHeight();

            var diffX = 0;
            if (mtOffset.left > x) {
              diffX = mtOffset.left - x;
              x = mtOffset.left;
            }

            var wDiffX = 0;
            if (x + w > mtOffset.left + mtWidth) {
              wDiffX = x + w - (mtOffset.left + mtWidth);
            }

            $(".main-tab-active-line").css({
              left: x,
              top: y + h,
              width: w - diffX - wDiffX,
            });
          },
          0,
          false
        );
      }
    }
  );
