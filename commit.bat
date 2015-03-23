git add -A
git status
::echo commit for change : 
set /p commit_for_change=
git commit -m "%commit_for_change%"
git push oschina master
git push github master
::net stop server
ping /n 8 127.1 >nul
::net start server