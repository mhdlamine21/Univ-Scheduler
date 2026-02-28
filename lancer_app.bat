@echo off
title UNIV-SCHEDULER
cd /d "%~dp0"
echo Lancement de UNIV-SCHEDULER (JavaFX)
java -cp "target\classes;target\scheduler-1.0.0.jar" scheduler.Launcher
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Une erreur est survenue lors de l'execution.
    pause
)
