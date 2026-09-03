; TraderCockpit Windows installer (NSIS)
; Requires makensis on the operator machine. CI produces the zip + setup.ps1 when makensis is absent.

!include "MUI2.nsh"

!ifndef PRODUCT_VERSION
  !define PRODUCT_VERSION "0.0.0"
!endif
!ifndef STAGING_DIR
  !define STAGING_DIR "dist\windows\release"
!endif
!ifndef OUT_FILE
  !define OUT_FILE "dist\windows\release\TraderCockpit-Setup.exe"
!endif

Name "TraderCockpit ${PRODUCT_VERSION}"
OutFile "${OUT_FILE}"
InstallDir "$LOCALAPPDATA\Programs\TraderCockpitSQ"
RequestExecutionLevel user
ShowInstDetails show

Page directory
Page instfiles

Section "TraderCockpit"
  SetOutPath "$INSTDIR"
  File "${STAGING_DIR}\TraderCockpit.exe"
  File "${STAGING_DIR}\install_windows_desktop.py"
  File "${STAGING_DIR}\release-manifest.json"

  ; Start Menu shortcut via the canonical Python installer (identity-safe).
  nsExec::ExecToLog 'python "$INSTDIR\install_windows_desktop.py" --exe "$INSTDIR\TraderCockpit.exe" --install-dir "$INSTDIR"'
  Pop $0
  DetailPrint "install_windows_desktop.py exit code: $0"
SectionEnd

Function .onInit
  ; Refuse to install into the other TraderCockpit product data root.
  StrCmp $INSTDIR "$LOCALAPPDATA\TraderCockpit" 0 +2
    MessageBox MB_ICONSTOP "Refusing to install into the other TraderCockpit product identity."
    Abort
FunctionEnd
