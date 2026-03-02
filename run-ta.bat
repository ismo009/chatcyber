@echo off
echo ===================================
echo   ChatCyber - Autorite de Confiance
echo ===================================
echo.
echo Demarrage du serveur de l'Autorite de Confiance (port 7777)...
echo.
java -cp target\chatcyber-1.0-SNAPSHOT.jar com.chatcyber.TrustAuthorityApp %1
pause
