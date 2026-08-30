angular
  .module("app")
  .controller(
    "NavigatorCtrl",
    function ($scope, $rootScope, $timeout, SQEvents, CodeEditorService, L) {
      console.log("NavigatorCtrl controller initialized");

      $scope.filterText = null;
      $scope.navTooltip = [];

      $scope.filter = function () {
        reload(undefined, true);
      };

      $scope.$watch("filterText", function (nVal, oVal) {
        if (nVal !== oVal) {
          $scope.filter();
        }
      });

      $scope.developmentMode = $rootScope.developmentMode;

      var treeNew;
      var contextMenu;
      var clickedTarget = null;
      var shouldShowTemplates = false;
      var shouldShowTemplatesAllMissing = false;
      var shouldShowFileItems = false;
      var shouldShowFileItemsForBuiltin = false;
      var shouldShowCompilePlugin = false;

      var treeMenuitems = {
        newFile: {
          name: L.tsq("New file"),
          visible: function (key, opt) {
            return !shouldShowFileItemsForBuiltin;
          },
          icon: function (opt, $itemElement, itemKey, item) {
            $itemElement.html(
              '<i class="fa fa-file-o" aria-hidden="true"></i>' + item.name
            );
            return "context-menu-fa";
          },
        },
        newDir: {
          name: L.tsq("New directory"),
          visible: function (key, opt) {
            return !shouldShowFileItemsForBuiltin;
          },
          icon: function (opt, $itemElement, itemKey, item) {
            $itemElement.html(
              '<i class="fa fa-folder-o" aria-hidden="true"></i>' + item.name
            );
            return "context-menu-fa";
          },
        },
        open: {
          name: L.tsq("Open"),
          visible: function (key, opt) {
            return shouldShowFileItems || shouldShowFileItemsForBuiltin;
          },
          icon: function (opt, $itemElement, itemKey, item) {
            $itemElement.html(
              '<i class="fa fa-folder-open-o" aria-hidden="true"></i>' +
                item.name
            );
            return "context-menu-fa";
          },
        },
        clone: {
          name: L.tsq("Clone"),
          visible: function (key, opt) {
            return shouldShowFileItems || shouldShowFileItemsForBuiltin;
          },
          icon: function (opt, $itemElement, itemKey, item) {
            $itemElement.html(
              '<i class="fa fa-clone" aria-hidden="true"></i>' + item.name
            );
            return "context-menu-fa";
          },
        },
        rename: {
          visible: function (key, opt) {
            return !shouldShowFileItemsForBuiltin;
          },
          name: L.tsq("Rename"),
          icon: function (opt, $itemElement, itemKey, item) {
            $itemElement.html(
              '<i class="fa fa-i-cursor" aria-hidden="true"></i>' + item.name
            );
            return "context-menu-fa";
          },
        },
        delete: {
          visible: function (key, opt) {
            return !shouldShowFileItemsForBuiltin;
          },
          name: L.tsq("Delete"),
          icon: function (opt, $itemElement, itemKey, item) {
            $itemElement.html(
              '<i class="fa fa-trash-o" aria-hidden="true"></i>' + item.name
            );
            return "context-menu-fa";
          },
        },
        reload: {
          name: L.tsq("Reload from Disk"),
          visible: function (key, opt) {
            return shouldShowFileItems && !shouldShowFileItemsForBuiltin;
          },
          icon: function (opt, $itemElement, itemKey, item) {
            $itemElement.html(
              '<i class="fa fa-share" aria-hidden="true"></i>' + item.name
            );
            return "context-menu-fa";
          },
        },
        compilePlugin: {
          name: L.tsq("Compile plugin"),
          visible: function (key, opt) {
            return shouldShowCompilePlugin;
          },
          icon: function (opt, $itemElement, itemKey, item) {
            $itemElement.html(
              '<i class="fa fa-shield" aria-hidden="true"></i>' + item.name
            );
            return "context-menu-fa";
          },
        },
        refresh: {
          visible: function (key, opt) {
            return !shouldShowFileItemsForBuiltin;
          },
          name: L.tsq("Refresh"),
          icon: function (opt, $itemElement, itemKey, item) {
            $itemElement.html(
              '<i class="fa fa-refresh" aria-hidden="true"></i>' + item.name
            );
            return "context-menu-fa";
          },
        },
        templates: {
          name: L.tsq("Source code templates"),
          disabled: true,
          className: "context-menu-title",
          visible: function (key, opt) {
            return shouldShowTemplates;
          },
        },
        sep1: {
          type: "cm_separator",
          disabled: true,
          visible: function (key, opt) {
            return shouldShowTemplates;
          },
        },
      };

      var listenerId = "NavigatorCtrl";
      SQEvents.addListener(
        listenerId,
        [SQEvents.get("EDITOR_RELOAD_NAVIGATOR")],
        onEvent
      );

      function onEvent(event, data) {
        if (event == SQEvents.get("EDITOR_RELOAD_NAVIGATOR")) {
          reload(data);
        }
      }

      $scope.$on("$destroy", function () {
        SQEvents.removeListener(listenerId);
      });

      CodeEditorService.listCodeTypes(function (data) {
        init(data.items);
      });

      function init(codeTypes) {
        $timeout(function () {
          for (var i = 0; i < codeTypes.length; i++) {
            treeMenuitems["openTemplate" + i] = {
              name: codeTypes[i],
              userdata: {
                type: codeTypes[i],
              },
              visible: function (key, opt) {
                return shouldShowTemplates;
              },
              className: "context-menu-template " + codeTypes[i],
              icon: function (opt, $itemElement, itemKey, item) {
                $itemElement.html(
                  '<i class="fa fa-file-code-o" aria-hidden="true"></i>' +
                    item.name
                );
                return "context-menu-fa";
              },
            };
          }

          treeMenuitems["openTemplate-addAllMissingTemplates" + i] = {
            name: L.tsq("Add all missing"),
            className: "context-menu-red",
            userdata: {
              type: "addAllMissingTemplates",
            },
            visible: function (key, opt) {
              return shouldShowTemplates && shouldShowTemplatesAllMissing;
            },
            icon: function (opt, $itemElement, itemKey, item) {
              $itemElement.html(
                '<i class="fa fa-asterisk" aria-hidden="true"></i>' + item.name
              );
              return "context-menu-fa";
            },
          };

          treeNew = new sqTree("filenavigatorNew");
          treeNew.onDblClick = function (fileType, filePath, id, name) {
            if (fileType == "dir" || fileType == "" || filePath == "") {
              return true;
            }
            SQEvents.notifyListeners(SQEvents.get("EDITOR_FILE_SELECTED"), {
              file: filePath,
            });
          };

          treeNew.onClick = function () {
            saveOpenStates();
          };

          treeNew.treeElementInner.addEventListener(
            "contextmenu",
            function (e) {
              var preClickedTarget = $(e.target).closest(".item");
              if (preClickedTarget) {
                clickedTarget = preClickedTarget[0];
                var id = clickedTarget.getAttribute("data-id");
                var file = clickedTarget.getAttribute("data-path");
                var fileType = clickedTarget.getAttribute("data-type");
                var elText = clickedTarget.getAttribute("data-text");

                $(treeNew.treeElementInner)
                  .find(".item.selected")
                  .removeClass("selected");
                $(treeNew.treeElementInner)
                  .find(".item[data-id='" + id + "']")
                  .addClass("selected");

                shouldShowFileItemsForBuiltin = false;
                if (fileType == "dir") {
                  shouldShowFileItems = false;
                } else {
                  if (isItemBuiltin(elText, file)) {
                    shouldShowFileItemsForBuiltin = true;
                    shouldShowFileItems = false;
                  } else {
                    shouldShowFileItemsForBuiltin = false;
                    shouldShowFileItems = true;
                  }
                }

                shouldShowCompilePlugin = shouldShowCompilePluginAction(file, fileType, elText);

                var templates = treeNew.getUserData(id, "templates");
                if (templates && templates.types) {
                  shouldShowTemplates = true;
                  
                  $(".context-menu-template").addClass("hidden");
                  for (var t = 0; t < templates.types.length; t++) {
                    for (itemKey in treeMenuitems) {
                      if (
                        treeMenuitems[itemKey].userdata &&
                        templates.types[t].type ==
                          getContextmenuUserData2(
                            itemKey,
                            "#filenavigatorNew",
                            "type"
                          ) &&
                        templates.types[t].type != "addAllMissingTemplates"
                      ) {
                        setContextmenuUserData(
                          itemKey,
                          "#filenavigatorNew",
                          "missing",
                          templates.types[t].missing
                        );
                        $(".context-menu-item." + templates.types[t].type).removeClass("hidden");
                        $(".context-menu-item." + templates.types[t].type).each(
                          function () {
                            if (templates.types[t].missing) {
                              $(this).addClass("context-menu-red");
                            } else {
                              $(this).removeClass("context-menu-red");
                            }
                          }
                        );
                      }
                    }
                  }

                  if (
                    getItem(templates.types, "type", "addAllMissingTemplates")
                  ) {
                    shouldShowTemplatesAllMissing = true;
                  } else {
                    shouldShowTemplatesAllMissing = false;
                  }
                } else {
                  shouldShowTemplates = false;
                  shouldShowTemplatesAllMissing = false;
                }
              } else {
                clickedTarget = null;
                shouldShowFileItems = false;
                shouldShowTemplates = false;
                shouldShowTemplatesAllMissing = false;
                shouldShowCompilePlugin = false;
              }
            }
          );

          $.contextMenu({
            selector: "#filenavigatorNew",
            events: {
              preShow: function (options) {
                if (clickedTarget == null) {
                  return true;
                } else {
                  var path = clickedTarget.getAttribute("data-path");
                  var elText = clickedTarget.getAttribute("data-text");
                  var fileType = clickedTarget.getAttribute("data-type");
                  if (fileType == "dir" && isItemBuiltin(elText, path)) {
                    return false;
                  }
                }
                return true;
              },
            },
            callback: function (itemKey, opt, e) {
              if (clickedTarget == null) {
                return;
              } else {
                var id = clickedTarget.getAttribute("data-id");
                var file = clickedTarget.getAttribute("data-path");
              }

              switch (itemKey) {
                case "newFile":
                  onNewFile(file);
                  break;
                case "newDir":
                  onNewDir(file);
                  break;
                case "open":
                  onOpen(file, id);
                  break;
                case "clone":
                  onClone(file, id);
                  break;
                case "rename":
                  onRename(file);
                  break;
                case "delete":
                  onDelete(file);
                  break;
                case "reload":
                  onReload(file, id);
                  break;
                case "refresh":
                  onRefresh(file);
                  break;
                case "compilePlugin":
                  onCompilePlugin(file);
                    break;
              }

              if (itemKey.startsWith("openTemplate")) {
                var type = getContextmenuUserData(itemKey, opt.items, "type");
                var missing = getContextmenuUserData(
                  itemKey,
                  opt.items,
                  "missing"
                );
                onOpenTemplate(type, missing, id);
              }
            },
            items: treeMenuitems,
          });

          load();

          var currentMousePos = {
            x: -1,
            y: -1,
          };

          $(document).mousemove(function (event) {
            currentMousePos.x = event.pageX;
            currentMousePos.y = event.pageY;
          });

          treeNew.mouseIn = function (fileType, filePath, id, name, element) {
            $("#nav-popup").hide();

            if (!element.classList.contains("missing-file")) {
              return;
            }

            var templates = treeNew.getUserData(id, "templates");
            if (
              templates === undefined ||
              templates.info === undefined ||
              templates.info.length == 0
            ) {
              return;
            }

            $scope.navTooltip = templates.info;

            var navPos = $(".mainCEFileNav").offset();
            var newX = currentMousePos.x + 5;
            var newY = $(window).height() - currentMousePos.y + 5;

            $("#nav-popup")
              .show()
              .css({
                bottom: newY + "px",
                left: newX + "px",
              });

            $scope.$applyAsync();
          };

          treeNew.mouseOut = function () {
            $("#nav-popup").hide();
          };
        });
      }

      function shouldShowCompilePluginAction(path, fileType, elText) {
        return fileType == "dir" && (path.toLowerCase().replace(/\//gm, "\\").indexOf("\\user\\extend\\plugins\\") > 0);
      }      

      function isItemBuiltin(elText, path) {
        return (
          elText.toLowerCase() == "builtin" ||
          path.toLowerCase().replace(/\//gm, "\\").indexOf("\\user\\extend\\") <
            0
        );
      }

      function reload(file, doNotSaveStates) {
        load($scope.filterText, file, doNotSaveStates);
      }

      function load(filter, file, doNotSaveStates) {
        if (!filter) {
          filter = "";
        }

        if (!doNotSaveStates) {
          saveOpenStates();
        }

        if (!file) {
          file = lastHighlightedFile;
        }

        treeNew.load("codeeditor/list?filter=" + filter, function () {
          if (filter == "") {
            $scope.collapseAll();
          } else {
            $scope.expandAll();
          }

          loadOpenStates();

          SQEvents.notifyListeners(SQEvents.get("FILES_CHANGED"), null);

          if (file) {
            CodeEditorService.showInNavigator(file);

            var fileType = getFileType(file);
            if (fileType == "file") {
              SQEvents.notifyListeners(SQEvents.get("EDITOR_FILE_SELECTED"), {
                file: file,
              });
            }
          }
        });
      }

      function getFileType(file) {
        var thisItemId = treeNew.findIdByUserData("file", file);
        var thisFileType = treeNew.getUserData(thisItemId, "fileType");
        return thisFileType;
      }

      var openStates = [];
      var lastHighlightedFile = "";

      function getContextmenuUserData(itemKey, items, dataName) {
        if (itemKey in items && dataName in items[itemKey].userdata) {
          return items[itemKey].userdata[dataName];
        } else {
          return false;
        }
      }

      function getContextmenuUserData2(itemKey, contextMenuSelector, dataName) {
        for (menuSingleKey in $.contextMenu.menus) {
          if (
            $.contextMenu.menus[menuSingleKey].selector == contextMenuSelector
          ) {
            return $.contextMenu.menus[menuSingleKey].items[itemKey].userdata[
              dataName
            ];
          }
        }
        return false;
      }

      function setContextmenuUserData(
        itemKey,
        contextMenuSelector,
        dataName,
        dataValue
      ) {
        for (menuSingleKey in $.contextMenu.menus) {
          if (
            $.contextMenu.menus[menuSingleKey].selector == contextMenuSelector
          ) {
            $.contextMenu.menus[menuSingleKey].items[itemKey].userdata[
              dataName
            ] = dataValue;
          }
        }
      }

      function saveOpenStates() {
        if (!(!$scope.filterText || $scope.filterText == "")) {
          return;
        }
        openStates.length = 0;
        var openedItems = $(treeNew.treeElementInner).find(".item.expanded");
        for (var i = 0; i < openedItems.length; i++) {
          var id = openedItems[i].getAttribute("data-path");
          openStates.push(id);
        }

        var selectedItem = $(treeNew.treeElementInner).find(".item.selected");
        if (selectedItem.length > 0) {
          lastHighlightedFile = selectedItem[0].getAttribute("data-path");
        }
      }

      function loadOpenStates() {
        for (var i = 0; i < openStates.length; i++) {
          var filePath = openStates[i];
          var thisItemId = treeNew.findIdByUserData("file", filePath);
          $(treeNew.treeElementInner)
            .find(".item[data-id='" + thisItemId + "']")
            .addClass("expanded");
        }
      }

      $scope.collapseAll = function () {
        treeNew.collapseAll();
        treeNew.expandLevels(1);
        treeNew.expandItemWithName("SQ");
      };

      $scope.expandAll = function () {
        treeNew.expandAll();
      };

      function highlightFile(file) {
        if (!file) {
          return;
        }

        var thisItemId = treeNew.findIdByUserData("file", file);

        $(treeNew.treeElementInner)
          .find(".item.selected")
          .removeClass("selected");
        let fileFound = $(treeNew.treeElementInner)
          .find(".item[data-id='" + thisItemId + "']")
          .addClass("selected")
          .get(0);
        //console.log("scrolling to file", thisItemId, file, fileFound);
        if (fileFound) {
          fileFound.scrollIntoView();
        }
      }

      //## ACTIONS ###########################################################################################################################################
      $scope.templates = [];
      $scope.editedItem = {
        name: null,
        template: null,
        file: null,
        ext: "tpl",
        createCodeFile: false,
      };

      $scope.fileExtensions = ["tpl", "inc", "mqh", "txt"];

      //-- New File ----------------------------------------------------------------------------------------------------
      function onNewFile(file) {
        $scope.errors = null;

        $scope.editedItem.createCodeFile = file.includes("\\extend\\Code");
        $scope.editedItem.ext = $scope.editedItem.createCodeFile
          ? "tpl"
          : "java";

        CodeEditorService.listTemplates(file, function (data) {
          loadToArray($scope.templates, data.templates);

          $scope.editedItem.name = "";
          $scope.editedItem.file = file;

          if (/\\extend\\Code\\.*\\blocks/.test(file)) {
            $scope.editedItem.template = "CodeBlocksFile";
          } else if ($scope.templates.length > 0) {
            $scope.editedItem.template = $scope.templates[0].file;
          } else {
            $scope.editedItem.template = "EmptyFile";
          }

          showPopup("#newFileModal");
        });
      }

      $scope.createNewFile = function (form) {
        if (form) {
          $scope.errors = validate(form);
          if ($scope.errors) {
            return;
          }
        }

        CodeEditorService.createNewFile($scope.editedItem, function (data) {
          reload(data.file);
          hidePopup("#newFileModal");

          SQEvents.notifyListeners(SQEvents.get("EDITOR_FILE_SELECTED"), {
            file: data.file,
          });

          $rootScope.showSuccess(data.success);
        });
      };

      $scope.getTemplateLabel = function () {
        var template = getItem(
          $scope.templates,
          "file",
          $scope.editedItem.template
        );
        return template ? L.tsq(template.name) : "";
      };

      //-- New Directory ----------------------------------------------------------------------------------------------------

      function onNewDir(file) {
        $scope.errors = null;

        $scope.editedItem.name = "";
        $scope.editedItem.file = file;

        try {
          $scope.$digest();
        } catch (er) {}

        showPopup("#newDirModal");
      }

      $scope.createNewDir = function (form) {
        if (form) {
          $scope.errors = validate(form);
          if ($scope.errors) {
            return;
          }
        }

        CodeEditorService.createNewDir($scope.editedItem, function (data) {
          reload(data.file);
          hidePopup("#newDirModal");

          $rootScope.showSuccess(data.success);
        });
      };

      //-- Open ----------------------------------------------------------------------------------------------------------
      function onOpen(file, id) {
        SQEvents.notifyListeners(SQEvents.get("EDITOR_FILE_SELECTED"), {
          file: file,
        });
      }

      //-- Rename ----------------------------------------------------------------------------------------------------------
      function onRename(file) {
        $scope.errors = null;

        $scope.editedItem.name = getFileName(file);
        $scope.editedItem.file = file;

        try {
          $scope.$digest();
        } catch (er) {}

        showPopup("#renameFileModal");
      }

      $scope.rename = function (form) {
        if (form) {
          $scope.errors = validate(form);
          if ($scope.errors) {
            return;
          }
        }

        $rootScope.showConfirm(
          L.tsq("User Confirmation"),
          L.tsq("Are you sure you want to rename?"),
          renameConfirm
        );
      };

      var renameConfirm = function () {
        CodeEditorService.rename($scope.editedItem, function (data) {
          reload(data.file);
          hidePopup("#renameFileModal");

          $rootScope.showSuccess(data.success);
        });
      };

      function getFileName(file) {
        var fileName = file.substring(file.lastIndexOf("\\") + 1);

        return fileName.substr(0, fileName.lastIndexOf(".")) || fileName;
      }

      //-- Clone -----------------------------------------------------------------------------------------------------------
      function onClone(file, id) {
        $scope.errors = null;

        $scope.editedItem.name = getFileName(file) + "Clone";
        $scope.editedItem.file = file;

        showPopup("#cloneModal");
      }

      $scope.clone = function (form) {
        if (form) {
          $scope.errors = validate(form);
          if ($scope.errors) {
            return;
          }
        }

        CodeEditorService.clone($scope.editedItem, function (data) {
          reload(data.file);
          hidePopup("#cloneModal");

          $rootScope.showSuccess(data.success);
        });
      };

      //-- Delete -----------------------------------------------------------------------------------------------------------

      function onDelete(file) {
        $scope.editedItem.file = file;

        $rootScope.showConfirm(
          L.tsq("User Confirmation"),
          L.tsq("Are you sure you want to remove this file?"),
          deleteConfirm
        );
      }

      var deleteConfirm = function () {
        CodeEditorService.delete($scope.editedItem.file, function (data) {
          SQEvents.notifyListeners(SQEvents.get("EDITOR_FILES_DELETED"), {
            files: data.files,
          });
          reload();

          $rootScope.showSuccess(data.success);
        });
      };

      //-- Reload -----------------------------------------------------------------------------------------------------------
      function onReload(file, id) {
        var tab = CodeEditorService.mainTabs.getTab(file);
        if (tab == null) {
          return;
        }

        $scope.editedItem.file = file;

        $rootScope.showConfirm(
          L.tsq("User Confirmation"),
          L.tsq(
            "Do you really want to refresh this file from disc? Unsaved changes will be lost."
          ),
          reloadConfirm
        );
      }

      var reloadConfirm = function () {
        CodeEditorService.reload($scope.editedItem.file, function (data) {
          SQEvents.notifyListeners(SQEvents.get("EDITOR_RELOAD_FILE"), {
            file: data.file,
          });
        });
      };
      
      //-- Refresh -----------------------------------------------------------------------------------------------------------
      function onRefresh(file) {
        reload(file);
      }

      function onCompilePlugin(file) {
        CodeEditorService.compilePlugin(file, function (data) {
          $rootScope.setProgressInfo(
            L.tsq("Compilation"),
            L.tsq("Compiling plugin..."),
            "infinite",
            null,
            true
          );
        });
      }

      //-- Open template -----------------------------------------------------------------------------------------------------------
      function onOpenTemplate(type, missing, id) {
        var templates = treeNew.getUserData(id, "templates");

        if (templates === undefined || templates.types === undefined) {
          return;
        }

        for (var i = 0; i < templates.types.length; i++) {
          if (templates.types[i].type == type) {
            var file = templates.types[i].file;

            if (missing || type == "addAllMissingTemplates") {
              CodeEditorService.createNewTemplate(file, function (data) {
                if (data.file.includes(",")) {
                  var paths = data.file.split(",");

                  reload(paths[0]);
                  $rootScope.showSuccess(data.success);

                  for (var p = 0; p < paths.length; p++) {
                    SQEvents.notifyListeners(
                      SQEvents.get("EDITOR_FILE_SELECTED"),
                      {
                        file: paths[p],
                      }
                    );
                  }
                } else {
                  reload(data.file);
                  SQEvents.notifyListeners(
                    SQEvents.get("EDITOR_FILE_SELECTED"),
                    {
                      file: data.file,
                    }
                  );

                  $rootScope.showSuccess(data.success);
                }
              });
            } else {
              SQEvents.notifyListeners(SQEvents.get("EDITOR_FILE_SELECTED"), {
                file: file,
              });
            }
          }
        }
      }

      CodeEditorService.showInNavigator = function (file) {
        var thisItemId = treeNew.findIdByUserData("file", file);
        var thisItem = $(treeNew.treeElementInner).find(
          ".item[data-id='" + thisItemId + "']"
        );
        if (getFileType(file) != "file") {
          thisItem.addClass("expanded");
        }
        var itemParents = thisItem.parents(".items-level");
        itemParents.each(function () {
          $(this).prev().addClass("expanded");
        });

        highlightFile(file);
      };
    }
  );
